package com.falak.falakpro.AddurulAniq

import android.content.Context
import java.util.Locale

class PosisiDatabase(context: Context) {
    private val store = loadStore(context.applicationContext)

    fun getMajmuah(key: Int) = findMain(key, "MAJMU'AH", "MAJMUAH")

    fun getMabsuthoh(key: Int) = findMain(key, "MABSUTHOH", "MABSUTOH")

    fun getBulan(key: Int) = findMain(key, "BULAN")

    fun getHari(key: Int) = findMain(key, "HARI")

    fun getJam(key: Int) = findTime(key, "JAM")

    fun getMenit(key: Int) = findTime(key, "MENIT")

    fun getDetik(key: Int) = findTime(key, "DETIK")

    fun getSFTJam(key: Int) = findTime(key, "SFT JAM", "SFTJAM")

    fun getSFTMenit(key: Int) = findTime(key, "SFT MENIT", "SFTMENIT")

    fun getSFTDetik(key: Int) = findTime(key, "SFT DETIK", "SFTDETIK")

    private fun findMain(key: Int, vararg sheetNames: String): MainRow? {
        for (sheetName in sheetNames) {
            store.mainRowsBySheet[normalizeSheetName(sheetName)]?.get(key)?.let { return it }
        }
        return null
    }

    private fun findTime(key: Int, vararg sheetNames: String): TimeRow? {
        for (sheetName in sheetNames) {
            store.timeRowsBySheet[normalizeSheetName(sheetName)]?.get(key)?.let { return it }
        }
        return null
    }

    private data class Store(
        val mainRowsBySheet: Map<String, Map<Int, MainRow>>,
        val timeRowsBySheet: Map<String, Map<Int, TimeRow>>
    )

    companion object {
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
            val mainRows = mutableMapOf<String, Map<Int, MainRow>>()
            val timeRows = mutableMapOf<String, Map<Int, TimeRow>>()

            for (sheet in HarokatPosisiReader(context).read()) {
                val name = normalizeSheetName(sheet.name)
                if (sheet.mainRows.isNotEmpty()) {
                    mainRows[name] = sheet.mainRows.associateBy { it.tahun }
                }
                if (sheet.timeRows.isNotEmpty()) {
                    timeRows[name] = sheet.timeRows.associateBy { it.waktu }
                }
            }

            return Store(mainRows, timeRows)
        }

        private fun normalizeSheetName(value: String): String {
            return value.trim().uppercase(Locale.ROOT)
        }
    }
}
