package com.falak.falakpro.AddurulAniq

class HarokatPosisiCalculator(
    private val sheets: List<PosisiSheet>
) {
    fun findMain(sheetName: String, key: Int): MainRow? {
        val sheet = sheets.find { it.name.trim().uppercase() == sheetName.uppercase() }
        return sheet?.mainRows?.find { it.tahun == key }
    }

    fun findTime(sheetName: String, key: Int): TimeRow? {
        val sheet = sheets.find { it.name.trim().uppercase() == sheetName.uppercase() }
        return sheet?.timeRows?.find { it.waktu == key }
    }
}
