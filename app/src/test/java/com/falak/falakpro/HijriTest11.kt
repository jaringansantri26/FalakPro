package com.falak.falakpro

import org.junit.Test
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.HilalEngine
import com.falak.falakpro.premium.DynamicalTimeEngine
import com.falak.falakpro.premium.Vsop87SolarEngine
import com.falak.falakpro.premium.ElpMpp02LunarEngine
import com.falak.falakpro.premium.ElpDataProvider
import java.io.File

class HijriTest11 {
    @Test
    fun testHijri() {
        val f = File("src/main/assets/mpp02_core.bin")
        if (f.exists()) ElpDataProvider.initialize(f.inputStream())
        val f2 = File("src/main/assets/earth_vsop87d.bin")
        if (f2.exists()) Vsop87SolarEngine.initialize(f2.inputStream())

        val tz = 7.0
        val criteria = "Mabims Baru"
        
        // Loop through longitudes from 90 to 140 to see where it is not visible!
        for (lon in listOf(95.0, 100.0, 106.8, 110.0, 120.0, 130.0, 140.0)) {
            val lat = -6.2
            val elev = 0.0
            
            val startJde11 = CalendarFunctions.getStartJdeOfHijriMonth(1447, 11, lat, lon, elev, tz, criteria)
            val startJde12 = CalendarFunctions.getStartJdeOfHijriMonth(1447, 12, lat, lon, elev, tz, criteria)
            val len = startJde12 - startJde11
            println("Lon $lon: Length of Month 11 = $len")
        }
    }
}
