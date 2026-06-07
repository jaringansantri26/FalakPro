package com.falak.falakpro.premium

/**
 * Vsop87Data — Struktur data hasil parsing earth_vsop87d.bin
 *
 * L[order][termIndex] = DoubleArray(A, B, C)
 * B[order][termIndex] = DoubleArray(A, B, C)
 * R[order][termIndex] = DoubleArray(A, B, C)
 *
 * order: 0..5 (L0..L5, B0..B5, R0..R5)
 */
data class Vsop87Data(
    val L: Array<Array<DoubleArray>>,  // Longitude series
    val B: Array<Array<DoubleArray>>,  // Latitude series
    val R: Array<Array<DoubleArray>>   // Radius vector series
) {
    override fun equals(other: Any?) = other is Vsop87Data
    override fun hashCode() = javaClass.hashCode()
}
