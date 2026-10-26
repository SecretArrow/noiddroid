package org.secretarrow.noiddroid.mine

/**
 * JNI bridge to libnoiddroid.so. Wraps the Rust core + Vulkan compute shim.
 *
 * Lifecycle: [create] -> [searchCpu]/[searchGpu] -> [stop] -> [destroy].
 * All native handles are kept as opaque [Long] values; this object owns them.
 */
object MinerNative {
    init {
        // Defensive — System.loadLibrary is idempotent.
        try {
            System.loadLibrary("noiddroid")
            loaded = true
        } catch (t: Throwable) {
            // Fall through — methods below will return failure codes.
            error = t
        }
    }

    @Volatile
    var loaded: Boolean = false
        private set

    @Volatile
    var error: Throwable? = null
        private set

    /** Returns an opaque native miner handle, or 0 on failure. */
    fun create(threads: Int): Long = ifLoaded(0L) { nativeMinerCreate(threads) }

    fun destroy(handle: Long) = ifLoaded(Unit) { nativeMinerDestroy(handle) }

    fun stop(handle: Long) = ifLoaded(Unit) { nativeMinerStop(handle) }

    fun hashrate(handle: Long): Long = ifLoaded(0L) { nativeMinerHashrate(handle) }

    /**
     * Blocking CPU nonce search. Returns a [SearchResult].
     *
     * @param handle from [create].
     * @param header 80-byte immutable template header.
     * @param target 32-byte target threshold (big-endian).
     * @param templateId node-assigned template id.
     * @param onProgress called from a worker thread with the current H/s rate.
     */
    fun searchCpu(
        handle: Long,
        header: ByteArray,
        target: ByteArray,
        templateId: Long,
        onProgress: ((Long) -> Unit)? = null,
    ): SearchResult {
        require(header.size == 80) { "header must be 80 bytes, was ${header.size}" }
        require(target.size == 32) { "target must be 32 bytes, was ${target.size}" }
        val cb = onProgress?.let { ProgressCallbackBridge(it) }
        val sol = LongArray(4)
        val rc =
            ifLoaded(-1) {
                nativeSearchCpu(handle, header, target, templateId, cb, sol)
            }
        return SearchResult(
            rc = rc,
            templateId = sol[0],
            nonceLo = sol[1].toULong(),
            nonceHi = sol[2].toULong(),
            hashRate = sol[3],
        )
    }

    /**
     * Blocking GPU (Vulkan) nonce search. Returns a [SearchResult].
     * If Vulkan init fails, returns rc = -1 — caller should fall back to [searchCpu].
     */
    fun searchGpu(
        header: ByteArray,
        target: ByteArray,
        templateId: Long,
        onProgress: ((Long) -> Unit)? = null,
    ): SearchResult {
        require(header.size == 80) { "header must be 80 bytes" }
        require(target.size == 32) { "target must be 32 bytes" }
        val cb = onProgress?.let { ProgressCallbackBridge(it) }
        val sol = LongArray(4)
        val rc =
            ifLoaded(-1) {
                nativeSearchGpu(header, target, templateId, cb, sol)
            }
        return SearchResult(
            rc = rc,
            templateId = sol[0],
            nonceLo = sol[1].toULong(),
            nonceHi = sol[2].toULong(),
            hashRate = sol[3],
        )
    }

    /** Convenience one-shot hash. Useful for tests and verification UI. */
    fun poseidon2bHash(input: ByteArray): ByteArray? =
        ifLoaded(null) {
            nativePoseidon2bHash(input)
        }

    private inline fun <T> ifLoaded(
        default: T,
        block: () -> T,
    ): T =
        if (loaded) {
            try {
                block()
            } catch (t: UnsatisfiedLinkError) {
                error = t
                default
            }
        } else {
            default
        }

    // ── JNI declarations ──────────────────────────────────────────────
    @JvmStatic external fun nativeMinerCreate(threads: Int): Long

    @JvmStatic external fun nativeMinerDestroy(handle: Long)

    @JvmStatic external fun nativeMinerStop(handle: Long)

    @JvmStatic external fun nativeMinerHashrate(handle: Long): Long

    @JvmStatic external fun nativeSearchCpu(
        handle: Long,
        header: ByteArray,
        target: ByteArray,
        templateId: Long,
        progressCallback: ProgressCallbackBridge?,
        solution: LongArray,
    ): Int

    @JvmStatic external fun nativeSearchGpu(
        header: ByteArray,
        target: ByteArray,
        templateId: Long,
        progressCallback: ProgressCallbackBridge?,
        solution: LongArray,
    ): Int

    @JvmStatic external fun nativePoseidon2bHash(input: ByteArray): ByteArray
}

/** JNI-visible callback adapter so the C++ side can invoke the Kotlin lambda. */
abstract class ProgressCallbackBridge(
    private val block: (Long) -> Unit,
) {
    fun onProgress(rate: Long) = block(rate)
}

data class SearchResult(
    /** Return code: 1 = solution found, 0 = search exhausted/stopped, <0 = error. */
    val rc: Int,
    val templateId: Long,
    val nonceLo: ULong,
    val nonceHi: ULong,
    val hashRate: Long,
) {
    val found: Boolean get() = rc == 1
    val nonceBytes: ByteArray
        get() {
            val buf = ByteArray(16)
            buf.copyOfRange(0, 8).also { /* leave empty */ }
            for (i in 0 until 8) buf[i] = (nonceLo shr (i * 8)).toByte()
            for (i in 0 until 8) buf[i + 8] = (nonceHi shr (i * 8)).toByte()
            return buf
        }
}
