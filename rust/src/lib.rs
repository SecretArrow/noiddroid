//! Noiddroid core — parano1d Poseidon2b external nonce-search worker.
//!
//! This crate is compiled to `libnoiddroid_core.so` for `aarch64-linux-android`
//! and loaded via JNI from Kotlin. It exposes three entry points:
//!
//!   1. `noid_search_cpu`      — multi-threaded CPU nonce search
//!   2. `noid_search_gpu`      — Vulkan-dispatched nonce search (delegated to C++)
//!   3. `noid_poseidon2b_hash` — single-block reference hash for tests
//!
//! ## Important — what this implements and what it does not
//!
//! The parano1d network (https://docs.parano1d.org/mining) defines a custom
//! PoW called "Proof-native PoW" where miners search a 128-bit nonce over a
//! `Poseidon2b` header schedule produced by the proving full node. The
//! canonical implementation lives in the upstream parano1d repo.
//!
//! This file is a **best-effort re-implementation from public Poseidon-2 / Binius
//! literature** (Horst, Grassi, Rechberger et al., 2023; the Poseidon2 paper).
//! It is not an official port and MUST be validated against the parano1d test
//! vectors before being trusted with real blocks. The hash below is a valid
//! Poseidon-2 permutation over the BN254 S-box and round constants derived
//! from a fixed seed; correctness against upstream is tracked in
//! `tests/integration.rs` and gated behind the `noid_validate_upstream` feature.
//!
//! ## Threat model
//!
//! * The Kotlin side never receives the block body, State witness or
//!   HistoryStep witness — only an immutable 80-byte header schedule and a
//!   32-byte target. The worker cannot alter the template.
//! * Templates are single-use and expire after 30 seconds (handled Kotlin-side).
//! * Found nonces are submitted via `submitBlock` and re-validated by the node.

#![deny(unsafe_op_in_unsafe_fn)]

pub mod poseidon2b;
pub mod search;

use std::os::raw::{c_char, c_int, c_void};
use std::slice;

/// Opaque miner context. Returned from `noid_miner_create`, freed by
/// `noid_miner_destroy`. Holds thread pool state and per-thread nonces.
#[repr(C)]
pub struct NoidMiner {
    inner: Box<MinerInner>,
}

#[derive(Default)]
struct MinerInner {
    /// CPU thread count (0 = use all visible logical cores).
    pub threads: u32,
    /// Last reported hashrate across all threads.
    pub hashrate: u64,
    /// Stop flag — set to `1` to request a clean shutdown of all worker threads.
    pub stop_flag: std::sync::atomic::AtomicBool,
}

/// Header schedule as returned by `getBlockTemplate` — 80 bytes for the
/// Poseidon2b input + 32-byte target (big-endian bytes, network order).
#[repr(C)]
pub struct NoidTemplate {
    pub header: [u8; 80],
    pub target: [u8; 32],
    pub template_id: u64,
}

/// Result of a nonce search: either a winning nonce or `noid == 0` if exhausted.
#[repr(C)]
#[derive(Default, Clone, Copy)]
pub struct NoidSolution {
    pub template_id: u64,
    pub nonce_lo: u64,
    pub nonce_hi: u64,
    pub hash_rate: u64,
}

#[no_mangle]
pub extern "C" fn noid_miner_create(threads: c_int) -> *mut NoidMiner {
    let inner = Box::new(MinerInner {
        threads: if threads <= 0 {
            num_cpus()
        } else {
            threads.max(1) as u32
        },
        ..Default::default()
    });
    Box::into_raw(NoidMiner { inner })
}

#[no_mangle]
pub extern "C" fn noid_miner_destroy(m: *mut NoidMiner) {
    if !m.is_null() {
        unsafe { drop(Box::from_raw(m)) };
    }
}

#[no_mangle]
pub extern "C" fn noid_miner_stop(m: *mut NoidMiner) {
    if m.is_null() {
        return;
    }
    unsafe {
        (*m).inner.stop_flag.store(true, std::sync::atomic::Ordering::SeqCst);
    }
}

#[no_mangle]
pub extern "C" fn noid_miner_hashrate(m: *const NoidMiner) -> u64 {
    if m.is_null() {
        return 0;
    }
    unsafe { (*m).inner.hashrate }
}

/// CPU nonce search entry-point. Blocks the calling thread until either:
///   * a solution is found (returns true)
///   * the stop flag is set (returns false)
///   * the search space is exhausted (returns false)
///
/// `template_ptr` must point to a `NoidTemplate` (no transfer of ownership).
/// `solution_ptr` must point to a caller-owned `NoidSolution` (will be filled).
/// `progress_callback` is called every ~1 second with the current hashrate.
#[no_mangle]
pub extern "C" fn noid_search_cpu(
    miner: *mut NoidMiner,
    template_ptr: *const NoidTemplate,
    solution_ptr: *mut NoidSolution,
    progress_callback: Option<extern "C" fn(rate: u64, ctx: *mut c_void)>,
    ctx: *mut c_void,
) -> c_int {
    if miner.is_null() || template_ptr.is_null() || solution_ptr.is_null() {
        return -1;
    }
    let miner = unsafe { &mut *miner };
    let template = unsafe { &*template_ptr };
    let solution = unsafe { &mut *solution_ptr };

    match search::run_cpu_search(
        template,
        miner.threads,
        &miner.stop_flag,
        progress_callback,
        ctx,
    ) {
        Some((nonce, hash_rate)) => {
            solution.template_id = template.template_id;
            solution.nonce_lo = nonce as u64;
            solution.nonce_hi = (nonce >> 64) as u64;
            solution.hash_rate = hash_rate;
            miner.hashrate = hash_rate;
            1
        }
        None => {
            solution.template_id = template.template_id;
            miner.hashrate = 0;
            0
        }
    }
}

#[no_mangle]
pub extern "C" fn noid_poseidon2b_hash(
    input: *const u8,
    input_len: usize,
    out: *mut u8,
    out_len: usize,
) -> c_int {
    if input.is_null() || out.is_null() {
        return -1;
    }
    if out_len < 32 {
        return -2;
    }
    let bytes = unsafe { slice::from_raw_parts(input, input_len) };
    let h = poseidon2b::poseidon2b_compress(bytes);
    let out_slice = unsafe { slice::from_raw_parts_mut(out, 32) };
    out_slice.copy_from_slice(&h);
    0
}

fn num_cpus() -> u32 {
    // SAFETY: `sysconf` on Android arm64 returns the online CPU count.
    // Fallback: 4 threads on failure.
    extern "C" {
        fn sysconf(name: c_int) -> c_long;
    }
    // _SC_NPROCESSORS_ONLN == 97 on Android / POSIX
    let n = unsafe { sysconf(97) };
    if n <= 0 { 4 } else { n as u32 }
}

// Silence unused `c_char` import if no code currently uses it.
#[allow(dead_code)]
fn _unused() { let _ = c_char::default(); }
