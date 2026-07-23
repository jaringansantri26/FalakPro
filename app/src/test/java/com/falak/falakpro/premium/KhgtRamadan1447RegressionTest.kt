package com.falak.falakpro.premium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class KhgtRamadan1447RegressionTest {

    @Test
    fun ramadan1447SeparatesTurkeyAndMuhammadiyahCriteria() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }

        val result = HilalEngine.calculateHilalStart(
            hijriYear = 1447,
            hijriMonth = 9,
            latitude = -6.175392,
            longitude = 106.827153,
            elevation = 8.0,
            timezone = 7.0
        )

        assertFalse(
            "KGHT Turki harus belum memenuhi untuk 1 Ramadan 1447 H",
            result.isVisibleKghtTurki
        )
        assertTrue(
            "KHGT Muhammadiyah harus sudah memenuhi untuk 1 Ramadan 1447 H",
            result.isVisibleKghtMuhammadiyah
        )
    }
}
