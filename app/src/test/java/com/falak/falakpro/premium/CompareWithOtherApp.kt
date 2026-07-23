package com.falak.falakpro.premium

import org.junit.Test
import java.util.Locale

class CompareWithOtherApp {
    @Test
    fun runComparison() {
        val jdUt = CalendarFunctions.gregorianToJde(2026, 6, 15.0) + (10.0 + 44.0 / 60.0 + 30.0 / 3600.0) / 24.0
        val jde = jdUt + DynamicalTimeEngine.deltaT(jdUt) / 86400.0

        val sun = Vsop87SolarEngine.compute(jde)
        val moon = ElpMpp02LunarEngine.computeGeometric(jde)

        val gast = AstroDataUtils.calculateGAST(jde)

        fun fmt(deg: Double): String {
            val sign = if (deg < 0) "-" else "+"
            val a = kotlin.math.abs(deg)
            val d = a.toInt()
            val m = ((a - d) * 60.0).toInt()
            val s = ((a - d) * 60.0 - m) * 60.0
            return String.format(Locale.US, "%s%02d° %02d' %05.2f\"", sign, d, m, s)
        }

        println("--- FALAKPRO RESULTS ---")
        println("Sun Apparent Longitude: " + fmt(sun.longitudeEcliptic))
        println("Sun Apparent Latitude: " + fmt(sun.latitudeEcliptic))
        println("Sun Right Ascension: " + fmt(sun.ra))
        println("Sun Declination: " + fmt(sun.dec))
        println("Sun GHA: " + fmt(AstroMath.mod(gast - sun.ra, 360.0)))

        println("Moon Apparent Longitude: " + fmt(moon.longitudeEcliptic))
        println("Moon Apparent Latitude: " + fmt(moon.latitudeEcliptic))
        println("Moon Right Ascension: " + fmt(moon.ra))
        println("Moon Declination: " + fmt(moon.dec))
        println("Moon GHA: " + fmt(AstroMath.mod(gast - moon.ra, 360.0)))
    }
}
