package com.falak.falakpro.premium

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class WaktuShalatImsakRegressionTest {

    @Test
    fun imsakIsTenMinutesBeforeDisplayedSubuh() {
        loadSolarAssets()

        val hasil = MesinWaktuShalat.hitung(
            konteks = null,
            tahun = 2026,
            bulan = 6,
            hari = 16,
            lintang = -6.3041,
            bujur = 107.3188,
            elevasi = 38.1,
            zonaWaktu = 7.0,
            kriteria = MesinWaktuShalat.KRITERIA_LFNU,
            ikhSubuh = 2,
            pembulatan = MesinWaktuShalat.ModePembulatan.NORMAL
        )

        val imsak = hasil.first { it.nama == "Imsak" }.teksWaktu
        val subuh = hasil.first { it.nama == "Subuh" }.teksWaktu

        assertEquals(10, minutesOfDay(subuh) - minutesOfDay(imsak))
    }

    private fun loadSolarAssets() {
        File("src/main/assets/iau2000a_nutation.bin").inputStream().use {
            Iau2006Nutation.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }
    }

    private fun minutesOfDay(value: String): Int {
        val parts = value.split(":")
        return parts[0].toInt() * 60 + parts[1].toInt()
    }
}
