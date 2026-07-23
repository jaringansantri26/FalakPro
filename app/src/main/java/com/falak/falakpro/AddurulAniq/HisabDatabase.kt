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

class HisabDatabase(context: Context) {
    private val store = loadStore(context.applicationContext)

    fun getMajmuah(key: Int) = store.majmuah[key]

    fun getMabsuthoh(key: Int) = store.mabsuthoh[key]

    fun getBulan(key: Int) = store.bulan[key]

    private data class Store(
        val majmuah: Map<Int, HisabRecord>,
        val mabsuthoh: Map<Int, HisabRecord>,
        val bulan: Map<Int, HisabRecord>
    )

    companion object {
        private const val ASSET_NAME = "harokat_ijtima.bin"
        private val lock = Any()

        @Volatile
        private var cachedStore: Store? = null

        private fun loadStore(context: Context): Store {
            cachedStore?.let { return it }
            return synchronized(lock) {
                cachedStore ?: readStore(context).also { cachedStore = it }
            }
        }

        private fun readStore(context: Context): Store {
            val buffer = context.assets.open(ASSET_NAME).use { input ->
                ByteBuffer.wrap(input.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
            }

            val majmuah = mutableMapOf<Int, HisabRecord>()
            val mabsuthoh = mutableMapOf<Int, HisabRecord>()
            val bulan = mutableMapOf<Int, HisabRecord>()

            val majmuahCount = buffer.int
            val mabsuthohCount = buffer.int
            val bulanCount = buffer.int

            repeat(majmuahCount) {
                val rec = readRecord(buffer)
                majmuah[rec.key] = rec
            }
            repeat(mabsuthohCount) {
                val rec = readRecord(buffer)
                mabsuthoh[rec.key] = rec
            }
            repeat(bulanCount) {
                val rec = readRecord(buffer)
                bulan[rec.key] = rec
            }

            return Store(majmuah, mabsuthoh, bulan)
        }

        private fun readRecord(buffer: ByteBuffer): HisabRecord {
            return HisabRecord(
                key = buffer.int,
                a = buffer.double,
                f = buffer.double,
                m1 = buffer.double,
                m = buffer.double
            )
        }
    }
}
