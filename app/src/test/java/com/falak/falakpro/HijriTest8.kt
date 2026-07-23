package com.falak.falakpro

import org.junit.Test
import com.falak.falakpro.premium.CalendarFunctions
import kotlin.math.*

class HijriTest8 {
    @Test
    fun testHijri() {
        // Tabular start of month 11 is 2461148.5
        val tabStart11 = CalendarFunctions.hijriToJde(1447, 11, 1)
        println("Tabular 1447-11-1 is $tabStart11")
        
        // Month 11 is 30 days tabularly.
        // So 30th day is tabStart11 + 29
        val jd30 = tabStart11 + 29
        println("Tabular 1447-11-30 is $jd30")
        println("jdeToHijri(jd30) = ${CalendarFunctions.jdeToHijri(jd30)}")
        
        // What about jd30 + 1?
        val jd31 = jd30 + 1
        println("Tabular 1447-11-30 + 1 is $jd31")
        println("jdeToHijri(jd31) = ${CalendarFunctions.jdeToHijri(jd31)}")
        
        // What about tabStart11 - 1?
        val jd0 = tabStart11 - 1
        println("Tabular 1447-11-1 - 1 is $jd0")
        println("jdeToHijri(jd0) = ${CalendarFunctions.jdeToHijri(jd0)}")
    }
}
