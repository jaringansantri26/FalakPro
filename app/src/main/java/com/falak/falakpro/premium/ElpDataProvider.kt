package com.falak.falakpro.premium

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * ElpDataProvider — 100% PERSIS v1.8 Parity Edition.
 * High-precision lunar data provider for ELP/MPP02 theory.
 */
object ElpDataProvider {

    private val LON_COUNTS = intArrayOf(12337, 1199, 219, 2)
    private val LAT_COUNTS = intArrayOf(7380, 516, 52)
    private val DIST_COUNTS = intArrayOf(12819, 1165, 210, 2)

    private var flatData: DoubleArray? = null

    val isInitialized: Boolean get() = flatData != null

    fun initialize(inputStream: InputStream) {
        if (flatData != null) return
        try {
            val bytes = inputStream.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val totalTerms = LON_COUNTS.sum() + LAT_COUNTS.sum() + DIST_COUNTS.sum()
            
            val data = DoubleArray(totalTerms * 6)
            for (i in 0 until totalTerms * 6) {
                if (buffer.hasRemaining()) {
                    data[i] = buffer.double
                }
            }
            flatData = data
            
            // Critical Validation: Check Distance Constant (Term 1 of R0)
            val distOffset = (LON_COUNTS.sum() + LAT_COUNTS.sum()) * 6
            val r0Constant = data[distOffset]
            if (abs(r0Constant - 385000.5) > 0.1) {
                // Data offset mismatch detected
                flatData = null
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLongitudeSum(jdeTD: Double) = getSum(0, LON_COUNTS, jdeTD)
    fun getLatitudeSum(jdeTD: Double) = getSum(LON_COUNTS.sum() * 6, LAT_COUNTS, jdeTD)
    fun getDistanceSum(jdeTD: Double) = getSum((LON_COUNTS.sum() + LAT_COUNTS.sum()) * 6, DIST_COUNTS, jdeTD)

    private fun getSum(startOffset: Int, counts: IntArray, jdeTD: Double): Double {
        val data = flatData ?: return 0.0
        val t = (jdeTD - 2451545.0) / 36525.0
        val t2 = t * t; val t3 = t * t2; val t4 = t * t3
        
        var totalResult = 0.0
        var pointer = startOffset
        
        for (order in counts.indices) {
            var orderSum = 0.0
            val n = counts[order]
            val limit = pointer + n * 6
            
            while (pointer < limit) {
                val amp = data[pointer]
                val p0 = data[pointer + 1]
                val p1 = data[pointer + 2]
                val p2 = data[pointer + 3]
                val p3 = data[pointer + 4]
                val p4 = data[pointer + 5]
                
                val arg = p0 + p1 * t + p2 * t2 + p3 * t3 + p4 * t4
                orderSum += amp * sin(arg)
                pointer += 6
            }
            
            totalResult += orderSum * t.pow(order.toDouble())
        }
        return totalResult
    }
}
