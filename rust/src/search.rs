//! CPU nonce-search loop. Each worker thread grabs a 64-bit nonce range
//! from the atomic counter and walks it; the 128-bit nonce is
//! `thread_seed << 64 | local_nonce`.

use crate::poseidon2b::poseidon2b_compress;
use crate::NoidTemplate;
use std::sync::atomic::{AtomicBool, AtomicU64, Ordering};
use std::sync::Arc;
use std::thread;

/// Atomic counter shared by all worker threads. Each worker atomically
/// fetches a 64-bit chunk and walks its local sub-range.
pub struct NonceCounter {
    pub hi: AtomicU64,
    pub lo: AtomicU64,
}

impl NonceCounter {
    pub fn new() -> Self {
        Self { hi: AtomicU64::new(0), lo: AtomicU64::new(0) }
    }

    /// Reserve `chunk_size` nonces for this thread. Returns (start_hi, start_lo, end_hi, end_lo).
    pub fn reserve(&self, chunk_size: u64) -> (u64, u64, u64, u64) {
        let prev = self.lo.fetch_add(chunk_size, Ordering::Relaxed);
        let mut carry = prev;
        let mut hi_delta = 0u64;
        if carry.checked_add(chunk_size).is_none() {
            // Overflow — bump hi
            let total = prev as u128 + chunk_size as u128;
            carry = total as u64;
            hi_delta = (total >> 64) as u64;
            self.hi.fetch_add(hi_delta, Ordering::Relaxed);
        }
        let cur_hi = self.hi.load(Ordering::Relaxed);
        let start_hi = cur_hi - hi_delta;
        let start_lo = prev;
        let end_lo = carry;
        let end_hi = cur_hi;
        (start_hi, start_lo, end_hi, end_lo)
    }
}

/// Multi-threaded CPU search. Returns `Some((nonce, hash_rate))` if a winning nonce is found.
///
/// `stop_flag` is an Arc so it can be safely shared into worker threads
/// (a plain `&AtomicBool` would fail the borrow checker's lifetime check
/// when captured by `thread::spawn`).
pub fn run_cpu_search(
    template: &NoidTemplate,
    threads: u32,
    stop_flag: Arc<AtomicBool>,
    progress_callback: Option<extern "C" fn(rate: u64, ctx: *mut std::os::raw::c_void)>,
    ctx: *mut std::os::raw::c_void,
) -> Option<(u128, u64)> {
    let counter = Arc::new(NonceCounter::new());
    let stop = Arc::new(AtomicBool::new(false));
    let found = Arc::new(std::sync::Mutex::new(None::<u128>));
    let hash_counter = Arc::new(AtomicU64::new(0));
    let chunk_size: u64 = 1 << 16; // 65k nonces per chunk per thread

    let handles: Vec<_> = (0..threads)
        .map(|_| {
            let counter = Arc::clone(&counter);
            let stop = Arc::clone(&stop);
            let found = Arc::clone(&found);
            let hash_counter = Arc::clone(&hash_counter);
            let header = template.header;
            let target = template.target;
            let ext_stop = Arc::clone(&stop_flag);

            thread::spawn(move || {
                let mut local_hashes: u64 = 0;
                let mut buf = [0u8; 80 + 16]; // header + nonce
                buf[..80].copy_from_slice(&header);

                while !stop.load(Ordering::Relaxed) && !ext_stop.load(Ordering::Relaxed) {
                    let (sh, sl, eh, el) = counter.reserve(chunk_size);
                    let mut hi = sh;
                    let mut lo = sl;

                    loop {
                        if stop.load(Ordering::Relaxed) || ext_stop.load(Ordering::Relaxed) {
                            break;
                        }
                        // Pack 128-bit nonce into buf[80..96] little-endian
                        buf[80..88].copy_from_slice(&lo.to_le_bytes());
                        buf[88..96].copy_from_slice(&hi.to_le_bytes());

                        let h = poseidon2b_compress(&buf[..96]);
                        local_hashes += 1;

                        if lt_target(&h, &target) {
                            let nonce = ((hi as u128) << 64) | (lo as u128);
                            let mut guard = found.lock().unwrap();
                            if guard.is_none() {
                                *guard = Some(nonce);
                                stop.store(true, Ordering::Relaxed);
                            }
                            return local_hashes;
                        }

                        lo = lo.wrapping_add(1);
                        if lo == 0 { hi = hi.wrapping_add(1); }
                        if (hi == eh && lo >= el) || (hi > eh) {
                            break;
                        }
                    }
                    hash_counter.fetch_add(local_hashes, Ordering::Relaxed);
                    local_hashes = 0;
                }
                hash_counter.fetch_add(local_hashes, Ordering::Relaxed);
                local_hashes
            })
        })
        .collect();

    // Reporter thread — calls the progress callback ~1/sec with the global hashrate.
    // Raw pointers (*mut c_void) are not Send by default — wrap in a
    // SendWrapper so the closure can be moved across threads.
    struct SendCtx(*mut std::os::raw::c_void);
    unsafe impl Send for SendCtx {}
    let reporter = {
        let stop = Arc::clone(&stop);
        let hash_counter = Arc::clone(&hash_counter);
        // Only wrap if a callback was actually provided; otherwise drop ctx.
        let ctx_opt: Option<SendCtx> = if progress_callback.is_some() && !ctx.is_null() {
            Some(SendCtx(ctx))
        } else { None };
        thread::spawn(move || {
            let mut last_total: u64 = 0;
            while !stop.load(Ordering::Relaxed) {
                thread::sleep(std::time::Duration::from_millis(1000));
                let cur = hash_counter.load(Ordering::Relaxed);
                let rate = cur.saturating_sub(last_total);
                last_total = cur;
                if let (Some(cb), Some(c)) = (progress_callback, ctx_opt.as_ref()) {
                    cb(rate, c.0);
                }
            }
        })
    };

    for h in handles { let _ = h.join(); }
    stop.store(true, Ordering::Relaxed);
    let _ = reporter.join();

    let total = hash_counter.load(Ordering::Relaxed);
    let guard = found.lock().unwrap();
    guard.map(|n| (n, total))
}

/// Returns true if `h < target` interpreted as 256-bit big-endian unsigned ints.
#[inline]
fn lt_target(h: &[u8; 32], target: &[u8; 32]) -> bool {
    for i in 0..32 {
        if h[i] != target[i] {
            return h[i] < target[i];
        }
    }
    false // equal — not strictly less
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn counter_reserves_unique_ranges() {
        let c = NonceCounter::new();
        let (sh, sl, eh, el) = c.reserve(1000);
        assert_eq!(sh, 0);
        assert_eq!(sl, 0);
        assert_eq!(eh, 0);
        assert_eq!(el, 1000);

        let (sh2, sl2, eh2, el2) = c.reserve(1000);
        assert_eq!(sh2, 0);
        assert_eq!(sl2, 1000);
        assert_eq!(eh2, 0);
        assert_eq!(el2, 2000);
    }

    #[test]
    fn lt_target_basic() {
        let small = [0x00u8; 32];
        let big = [0xFFu8; 32];
        assert!(lt_target(&small, &big));
        assert!(!lt_target(&big, &small));
        assert!(!lt_target(&small, &small));
    }
}
