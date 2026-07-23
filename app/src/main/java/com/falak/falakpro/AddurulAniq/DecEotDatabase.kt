package com.falak.falakpro.AddurulAniq

import android.content.Context
import com.falak.falakpro.DecEotData
import com.falak.falakpro.DecEotReader
import java.io.File
import java.io.FileOutputStream

class DecEotDatabase(context: Context) {
    private val data = loadData(context.applicationContext)

    fun getData(tanggal: Int, bulan: Int): DecEotData? {
        return data.firstOrNull { it.tanggal == tanggal && it.bulan == bulan }
    }

    companion object {
        private const val ASSET_NAME = "dec_eot.bin"
        private val lock = Any()

        @Volatile
        private var cachedData: List<DecEotData>? = null

        private fun loadData(context: Context): List<DecEotData> {
            cachedData?.let { return it }
            return synchronized(lock) {
                cachedData ?: readData(context).also { cachedData = it }
            }
        }

        private fun readData(context: Context): List<DecEotData> {
            val file = File(context.cacheDir, ASSET_NAME)
            if (!file.exists()) {
                context.assets.open(ASSET_NAME).use { input ->
                    FileOutputStream(file).use { output -> input.copyTo(output) }
                }
            }
            return DecEotReader.readBin(file)
        }
    }
}
