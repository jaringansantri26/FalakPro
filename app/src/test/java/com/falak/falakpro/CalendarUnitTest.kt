package com.falak.falakpro

import com.falak.falakpro.premium.CalendarFunctions
import org.junit.Test

class CalendarUnitTest {
    @Test
    fun testCalendar() {
        val lat = -6.2
        val lon = 106.8
        val tz = 7.0
        val elev = 50.0
        val startDhul = CalendarFunctions.getStartJdeOfHijriMonth(1447, 12, lat, lon, elev, tz)
        val startMuh = CalendarFunctions.getStartJdeOfHijriMonth(1448, 1, lat, lon, elev, tz)
        
        println("Dzulhijjah 1447: $startDhul")
        println("Muharram 1448: $startMuh")
        println("Days: ${startMuh - startDhul}")
    }
}
