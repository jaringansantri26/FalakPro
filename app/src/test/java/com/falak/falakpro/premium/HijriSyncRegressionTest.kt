package com.falak.falakpro.premium

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class HijriSyncRegressionTest {

    @Test
    fun indonesianCalendarHijriFollowsWesternmostReferenceMuharram1448() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }
        CalendarFunctions.clearStartJdeCache()

        val jde = CalendarFunctions.gregorianToJde(2026, 6, 16.0)
        val hijri = CalendarFunctions.getIndonesianCalendarHijri(jde, "Mabims Baru")

        assertEquals(Triple(1448, 1, 1), hijri)
    }
}
