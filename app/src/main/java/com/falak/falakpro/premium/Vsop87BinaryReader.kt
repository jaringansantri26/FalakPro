package com.falak.falakpro.premium

import android.content.Context
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Vsop87BinaryReader — Pembaca file earth_vsop87d.bin dari Android Assets
 *
 * Format Binary (Little-Endian):
 *   [13 bytes] Magic: "VSOP87D_EARTH"
 *   [4 bytes]  Version (int32 LE) = 1
 *
 *   Untuk setiap variabel (L, B, R):
 *     [4 bytes] Jumlah seri (int32 LE) = 6
 *     Untuk setiap order (0..5):
 *       [4 bytes] Jumlah term (int32 LE)
 *       Untuk setiap term:
 *         [8 bytes] A (double LE)
 *         [8 bytes] B (double LE)
 *         [8 bytes] C (double LE)
 */
object Vsop87BinaryReader {

    private const val MAGIC = "VSOP87D_EARTH"
    private const val ASSET_FILE = "earth_vsop87d.bin"

    /**
     * Membaca file dari Android Assets dan mengembalikan Vsop87Data
     */
    fun load(context: Context): Vsop87Data {
        return context.assets.open(ASSET_FILE).use { stream ->
            read(stream)
        }
    }

    fun load(stream: InputStream): Vsop87Data {
        return read(stream)
    }

    private fun read(stream: InputStream): Vsop87Data {
        // Baca semua bytes sekaligus (lebih efisien untuk file biner)
        val bytes = stream.readBytes()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        // Verifikasi magic header (13 bytes)
        val magicBytes = ByteArray(MAGIC.length)
        buf.get(magicBytes)
        val magic = String(magicBytes, Charsets.US_ASCII)
        require(magic == MAGIC) {
            "File bukan VSOP87D Earth yang valid! Magic: '$magic'"
        }

        // Baca versi (int32)
        val version = buf.int
        require(version == 1) { "Versi tidak dikenal: $version" }

        // Baca tiga variabel: L, B, R
        val L = readVariable(buf)
        val B = readVariable(buf)
        val R = readVariable(buf)

        return Vsop87Data(L, B, R)
    }

    /**
     * Baca satu variabel (L / B / R) yang terdiri dari 6 order
     */
    private fun readVariable(buf: ByteBuffer): Array<Array<DoubleArray>> {
        val numSeries = buf.int  // = 6
        return Array(numSeries) { readSeries(buf) }
    }

    /**
     * Baca satu seri (misalnya L0) yang terdiri dari N term
     * Setiap term = [A, B, C]
     */
    private fun readSeries(buf: ByteBuffer): Array<DoubleArray> {
        val numTerms = buf.int
        return Array(numTerms) {
            val a = buf.double
            val b = buf.double
            val c = buf.double
            doubleArrayOf(a, b, c)
        }
    }
}
