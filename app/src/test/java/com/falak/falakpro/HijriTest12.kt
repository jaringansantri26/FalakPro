package com.falak.falakpro

import org.junit.Test
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.HilalEngine
import com.falak.falakpro.premium.DynamicalTimeEngine
import com.falak.falakpro.premium.Vsop87SolarEngine
import com.falak.falakpro.premium.ElpMpp02LunarEngine
import com.falak.falakpro.premium.ElpDataProvider
import java.io.File
import kotlin.math.round

class HijriTest12 {
    @Test
    fun testHijriUiLogic() {
        val f = File("src/main/assets/mpp02_core.bin")
        if (f.exists()) ElpDataProvider.initialize(f.inputStream())
        val f2 = File("src/main/assets/earth_vsop87d.bin")
        if (f2.exists()) Vsop87SolarEngine.initialize(f2.inputStream())

        val lat = -6.2
        val lon = 106.8
        val elev = 0.0
        val tz = 7.0
        
        val snappedLat = round(lat * 10.0) / 10.0
        val snappedLon = round(lon * 10.0) / 10.0
        val snappedElev = round(elev / 10.0) * 10.0
        val hijriCriteria = "Mabims Baru"
        
        val year = 2026
        val month = 4 // May (0-indexed in Java Calendar, but UI passes 4 for May?)
        // Wait, UI passes month (0-11). So May is month=4.
        
        val jdCenter = CalendarFunctions.gregorianToJde(year, month + 1, 15.0)
        val approx   = CalendarFunctions.jdeToHijri(jdCenter)
        val estHY    = approx.first
        val estHM    = approx.second
        
        val anchors = mutableListOf<Pair<Triple<Int, Int, Int>, Double>>() // ((Y,M,D), StartJd)
        for (off in -2..2) {
            var ty = estHY; var tm = estHM + off
            while (tm > 12) { tm -= 12; ty++ }
            while (tm < 1) { tm += 12; ty-- }
            val sJde = CalendarFunctions.getStartJdeOfHijriMonth(ty, tm, snappedLat, snappedLon, snappedElev, tz, hijriCriteria)
            anchors.add(Triple(ty, tm, 1) to sJde)
        }
        anchors.sortBy { it.second } 
        
        for (day in 15..20) {
            val jde = CalendarFunctions.gregorianToJde(year, month + 1, day.toDouble())
            var hY = 0; var hM = 0; var hD = 0
            CalendarFunctions.getHijriDateFromMonthAnchors(jde, tz, anchors)?.let {
                hY = it.first
                hM = it.second
                hD = it.third
            }
            if (hY == 0) { // Deep fallback
                val fb = CalendarFunctions.getCorrectedHijri(jde, snappedLat, snappedLon, snappedElev, tz)
                hY=fb.first; hM=fb.second; hD=fb.third
            }
            println("UI Logic: 2026-05-$day = $hY-$hM-$hD")
        }
    }
}
