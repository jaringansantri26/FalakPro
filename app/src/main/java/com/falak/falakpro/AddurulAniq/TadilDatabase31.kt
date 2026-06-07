package com.falak.falakpro.AddurulAniq

import android.content.Context
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class Tadil31Record(
    val values: List<Double>
)

class TadilDatabase31(context: Context) {
    val data = mutableMapOf<Int, Tadil31Record>()

    init {
        try {
            val inputStream: InputStream = context.assets.open("ta_dil.bin")
            val size = inputStream.available()
            val bytes = inputStream.readBytes()
            inputStream.close()

            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val recordCount = size / (31 * 8)
            
            for (i in 0 until recordCount) {
                val list = mutableListOf<Double>()
                for (j in 0 until 31) {
                    list.add(buffer.double)
                }
                data[i] = Tadil31Record(list)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
