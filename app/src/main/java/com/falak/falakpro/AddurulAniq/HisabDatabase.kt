package com.falak.falakpro.AddurulAniq

import android.content.Context
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class HisabRecord(
    val key: Int,
    val a: Double,
    val f: Double,
    val m1: Double,
    val m: Double
)

class HisabDatabase(
    context: Context
) {

    private val majmuah = mutableMapOf<Int, HisabRecord>()
    private val mabsuthoh = mutableMapOf<Int, HisabRecord>()
    private val bulan = mutableMapOf<Int, HisabRecord>()

    init {

        val bytes = context.assets
            .open("harokat_ijtima.bin")
            .readBytes()

        val buffer = ByteBuffer.wrap(bytes)
        buffer.order(ByteOrder.LITTLE_ENDIAN)

        val majmuahCount = buffer.int
        val mabsuthohCount = buffer.int
        val bulanCount = buffer.int

        repeat(majmuahCount) {
            val rec = read(buffer)
            majmuah[rec.key] = rec
        }

        repeat(mabsuthohCount) {
            val rec = read(buffer)
            mabsuthoh[rec.key] = rec
        }

        repeat(bulanCount) {
            val rec = read(buffer)
            bulan[rec.key] = rec
        }
    }

    private fun read(
        buffer: ByteBuffer
    ): HisabRecord {
        return HisabRecord(
            key = buffer.int,
            a = buffer.double,
            f = buffer.double,
            m1 = buffer.double,
            m = buffer.double
        )
    }

    fun getMajmuah(key: Int) =
        majmuah[key]

    fun getMabsuthoh(key: Int) =
        mabsuthoh[key]

    fun getBulan(key: Int) =
        bulan[key]
}
