package com.falak.falakpro.AddurulAniq

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Tadil31Record(
    val values: List<Double>
)

class TadilDatabase31(context: Context) {
    val data: Map<Int, Tadil31Record> = loadData(context.applicationContext)

    companion object {
        private const val ASSET_NAME = "ta_dil.bin"
        private const val RECORD_VALUE_COUNT = 31
        private val lock = Any()

        @Volatile
        private var cachedData: Map<Int, Tadil31Record>? = null

        private fun loadData(context: Context): Map<Int, Tadil31Record> {
            cachedData?.let { return it }
            return synchronized(lock) {
                cachedData ?: readData(context).also { cachedData = it }
            }
        }

        private fun readData(context: Context): Map<Int, Tadil31Record> {
            val bytes = context.assets.open(ASSET_NAME).use { it.readBytes() }
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val recordCount = bytes.size / (RECORD_VALUE_COUNT * java.lang.Double.BYTES)

            return buildMap {
                repeat(recordCount) { index ->
                    val values = List(RECORD_VALUE_COUNT) { buffer.double }
                    put(index, Tadil31Record(values))
                }
            }
        }
    }
}
