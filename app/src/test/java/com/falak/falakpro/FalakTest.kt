package com.falak.falakpro

import org.junit.Test
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.Vsop87SolarEngine
import com.falak.falakpro.premium.ElpDataProvider
import java.io.File
import java.io.FileInputStream

class FalakTest {
    @Test
    fun testIjtima() {
        val v = File("src/main/assets/earth_vsop87d.bin")
        val e = File("src/main/assets/mpp02_core.bin")
        Vsop87SolarEngine.initialize(FileInputStream(v))
        ElpDataProvider.initialize(FileInputStream(e))
        CalendarFunctions.clearStartJdeCache()

        val jd = CalendarFunctions.getStartJdeOfHijriMonth(1447, 10, -6.31, 107.31, 38.0, 7.0, "Mabims Baru")
        val greg = CalendarFunctions.jdeToGregorian(jd)
        println("START SYAWAL 1447 (Mabims Baru): " + greg.first + "-" + greg.second + "-" + greg.third)
        
        val jdW = CalendarFunctions.getStartJdeOfHijriMonth(1447, 10, -6.31, 107.31, 38.0, 7.0, "Wujudul Hilal")
        val gregW = CalendarFunctions.jdeToGregorian(jdW)
        println("START SYAWAL 1447 (Wujudul Hilal): " + gregW.first + "-" + gregW.second + "-" + gregW.third)
    }

    @Test
    fun testMoonJplComparison() {
        val v = File("src/main/assets/earth_vsop87d.bin")
        val e = File("src/main/assets/mpp02_core.bin")
        Vsop87SolarEngine.initialize(FileInputStream(v))
        ElpDataProvider.initialize(FileInputStream(e))

        // 2026-May-17 00:00:00 UT
        val jdUt = 2461177.5
        val deltaT = com.falak.falakpro.premium.DynamicalTimeEngine.deltaT(jdUt)
        val jde = jdUt + deltaT / 86400.0

        val moon = com.falak.falakpro.premium.ElpMpp02LunarEngine.computeGeometric(jde)
        val sun = Vsop87SolarEngine.compute(jde)
        val illum = com.falak.falakpro.premium.LunarFunctions.moonIllumination(
            sun.ra, sun.dec, sun.distanceAU,
            moon.ra, moon.dec, moon.distanceAU * 149597870.7
        )

        // Convert RA (degrees) to HMS
        val raHours = (moon.ra % 360.0) / 15.0
        val raH = raHours.toInt()
        val raM = ((raHours - raH) * 60.0).toInt()
        val raS = ((raHours - raH) * 60.0 - raM) * 60.0

        // Convert RA (degrees) to DMS
        val raDeg = moon.ra.toInt()
        val raMinD = ((moon.ra - raDeg) * 60.0).toInt()
        val raSecD = ((moon.ra - raDeg) * 60.0 - raMinD) * 60.0

        // Convert DEC (degrees) to DMS
        val decAbs = kotlin.math.abs(moon.dec)
        val decD = decAbs.toInt()
        val decM = ((decAbs - decD) * 60.0).toInt()
        val decS = ((decAbs - decD) * 60.0 - decM) * 60.0
        val decSign = if (moon.dec >= 0) "+" else "-"

        println("==================================================")
        println("FALAKPRO MOON COMPUTATION (17 May 2026 00:00:00 UT)")
        println("==================================================")
        println("Apparent RA (HMS)     : %02dh %02dm %05.2fs".format(raH, raM, raS))
        println("Apparent RA (Degrees) : %02d° %02d' %05.2f\"".format(raDeg, raMinD, raSecD))
        println("Apparent DEC          : %s%02d° %02d' %05.2f\"".format(decSign, decD, decM, decS))
        println("Ecliptic Longitude    : %.6f°".format(moon.longitudeEcliptic))
        println("Ecliptic Latitude     : %.6f°".format(moon.latitudeEcliptic))
        println("Illumination          : %.4f %%".format(illum.illuminatedFraction * 100.0))
        println("Semi-Diameter         : %.2f\"".format(moon.semidiameter * 3600.0))
        println("==================================================")
    }
}
