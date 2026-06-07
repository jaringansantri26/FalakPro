package com.falak.falakpro.AddurulAniq

import android.content.Context
import com.falak.falakpro.DecEotData
import com.falak.falakpro.DecEotReader
import java.io.File
import java.io.FileOutputStream

class DecEotDatabase(context: Context) {
    private val data = mutableListOf<DecEotData>()

    init {
        val fileName = "dec_eot.bin"
        val file = File(context.cacheDir, fileName)

        if (!file.exists()) {
            context.assets.open(fileName).use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        }

        data.addAll(DecEotReader.Companion.readBin(file))
    }

    fun getData(tanggal: Int, bulan: Int): DecEotData? {
        return data.find { it.tanggal == tanggal && it.bulan == bulan }
    }
}