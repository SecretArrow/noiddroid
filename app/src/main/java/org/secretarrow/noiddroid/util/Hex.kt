package org.secretarrow.noiddroid.util

/**
 * Hex string <-> ByteArray conversion utilities.
 *
 * parano1d's getBlockTemplate RPC returns the 80-byte header schedule and
 * 32-byte target as hex strings. The Rust core wants raw bytes — these
 * helpers do the round-trip.
 *
 * Kept as top-level functions (not member extensions) so they can be
 * called from any context (service, view-model, tests) without scope
 * surprises.
 */

private val HEX_CHARS = "0123456789abcdef".toCharArray()

/** Decode a hex string to a ByteArray. Odd-length input is rejected. */
fun hexDecode(s: String): ByteArray {
    require(s.length % 2 == 0) { "Hex string has odd length: ${s.length}" }
    return ByteArray(s.length / 2) { i ->
        val hi = Character.digit(s[i * 2], 16)
        val lo = Character.digit(s[i * 2 + 1], 16)
        if (hi < 0 || lo < 0) {
            throw IllegalArgumentException("Non-hex char at index ${i * 2} in: $s")
        }
        ((hi shl 4) or lo).toByte()
    }
}

/** Encode a ByteArray as a lowercase hex string. */
fun hexEncode(b: ByteArray): String {
    val sb = StringBuilder(b.size * 2)
    for (byte in b) {
        val v = byte.toInt() and 0xFF
        sb.append(HEX_CHARS[v ushr 4])
        sb.append(HEX_CHARS[v and 0x0F])
    }
    return sb.toString()
}

/** Parse a hex-or-decimal string to ULong, returning null on failure. */
fun safeToULong(s: String): ULong? {
    if (s.isBlank()) return null
    if (s.startsWith("0x") || s.startsWith("0X")) {
        return s.drop(2).toULongOrNull(16)
    }
    return s.toULongOrNull()?.let { return it }
        ?: s.toULongOrNull(16)
}
