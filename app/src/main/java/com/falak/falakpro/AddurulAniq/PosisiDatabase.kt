package com.falak.falakpro.AddurulAniq

import android.content.Context

class PosisiDatabase(context: Context) {
    private val sheets: List<PosisiSheet> = HarokatPosisiReader(context).read()

    fun getMajmuah(key: Int) = findMain("MAJMU'AH", key) ?: findMain("MAJMUAH", key)
    fun getMabsuthoh(key: Int) = findMain("MABSUTHOH", key) ?: findMain("MABSUTOH", key)
    fun getBulan(key: Int) = findMain("BULAN", key)
    fun getHari(key: Int) = findMain("HARI", key)
    
    fun getJam(key: Int) = findTime("JAM", key)
    fun getMenit(key: Int) = findTime("MENIT", key)
    fun getDetik(key: Int) = findTime("DETIK", key)

    fun getSFTJam(key: Int) = findTime("SFT JAM", key) ?: findTime("SFTJAM", key)
    fun getSFTMenit(key: Int) = findTime("SFT MENIT", key) ?: findTime("SFTMENIT", key)
    fun getSFTDetik(key: Int) = findTime("SFT DETIK", key) ?: findTime("SFTDETIK", key)

    private fun findMain(sheetName: String, key: Int): MainRow? {
        val sheet = sheets.find { it.name.trim().uppercase() == sheetName.trim().uppercase() }
        return sheet?.mainRows?.find { it.tahun == key }
    }

    private fun findTime(sheetName: String, key: Int): TimeRow? {
        val sheet = sheets.find { it.name.trim().uppercase() == sheetName.trim().uppercase() }
        return sheet?.timeRows?.find { it.waktu == key }
    }
}
