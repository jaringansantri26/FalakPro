package com.falak.falakpro

import org.junit.Test
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.HilalEngine
import com.falak.falakpro.premium.DynamicalTimeEngine
import com.falak.falakpro.premium.Vsop87SolarEngine
import com.falak.falakpro.premium.ElpMpp02LunarEngine
import com.falak.falakpro.premium.AstroDataUtils
import com.falak.falakpro.premium.AstroMath
import kotlin.math.*

class HijriTest {
    @Test
    fun testHijri() {
        val lat = -6.2
        val lon = 106.8
        val elev = 0.0
        val tz = 7.0
        val criteria = "Mabims Baru"
        
        for (mH in 11..12) {
            val startJde = CalendarFunctions.getStartJdeOfHijriMonth(1447, mH, lat, lon, elev, tz, criteria)
            val gc = CalendarFunctions.jdeToGregorian(startJde + 0.5)
            println("Month $mH (1447 AH) starts on Gregorian: ${gc.first}-${gc.second}-${gc.third}")
        }
    }
}
