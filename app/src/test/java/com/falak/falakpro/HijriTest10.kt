package com.falak.falakpro

import org.junit.Test
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.HilalEngine
import com.falak.falakpro.premium.DynamicalTimeEngine
import com.falak.falakpro.premium.Vsop87SolarEngine
import com.falak.falakpro.premium.ElpMpp02LunarEngine
import com.falak.falakpro.premium.ElpDataProvider
import java.io.File

class HijriTest10 {
    @Test
    fun testHijri() {
        val f = File("src/main/assets/mpp02_core.bin")
        if (f.exists()) ElpDataProvider.initialize(f.inputStream())
        val f2 = File("src/main/assets/earth_vsop87d.bin")
        if (f2.exists()) Vsop87SolarEngine.initialize(f2.inputStream())

        val lat = -6.2
        val lon = 106.8
        val elev = 0.0
        val tz = 7.0
        val criteria = "Mabims Baru"
        
        for (day in 15..20) {
            val jd = CalendarFunctions.gregorianToJde(2026, 5, day.toDouble())
            val h = CalendarFunctions.getCorrectedHijri(jd, lat, lon, elev, tz)
            println("2026-05-$day = ${h.first}-${h.second}-${h.third}")
        }
    }
}
