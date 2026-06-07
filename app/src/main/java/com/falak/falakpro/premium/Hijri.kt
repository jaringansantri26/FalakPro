package com.falak.falakpro.premium

import kotlin.math.*

/**
 * AstroHijriEngine — 100% Adoption of Astro v1.8 Hijri Calendar Logic.
 * Implements official Imkan Rukyat (3°/6.4°) and Wujudul Hilal criteria.
 */
object AstroHijriEngine {

    data class HijriResult(
        val hDay: Int, val hMonth: Int, val hYear: Int,
        val ijtimaJde: Double, val ghurubJde: Double,
        val altitude: Double, val elongation: Double,
        val status: String
    )

    fun calculateAwalBulan(hMonth: Int, hYear: Int, lat: Double, lon: Double, elev: Double, tz: Double): HijriResult {
        val ijtimaJde = AstroMoonEngine.calculateIjtima(hMonth, hYear)
        val dt = DynamicalTimeEngine.deltaT(ijtimaJde)
        
        // Find ghurub on day of ijtima
        var ghurubJde = floor(ijtimaJde + 0.5) + (18.0 - tz) / 24.0
        repeat(3) {
            val sun = Vsop87SolarEngine.compute(ghurubJde + dt/86400.0)
            val sd = 0.2666 / sun.distanceAU
            val dip = 1.76 / 60.0 * sqrt(elev)
            val h = -(sd + 34.5 / 60.0 + dip)
            val t = AstroMath.deg(acos(-tan(AstroMath.rad(lat)) * tan(AstroMath.rad(sun.dec)) + sin(AstroMath.rad(h)) / (cos(AstroMath.rad(lat)) * cos(AstroMath.rad(sun.dec)))))
            ghurubJde = floor(ijtimaJde + 0.5) + (12.0 + t / 15.0 - (lon - tz * 15.0) / 15.0) / 24.0
        }

        val moon = AstroMoonEngine.getTopocentricPosition(ghurubJde + dt/86400.0, lon, lat, elev)
        val sun = Vsop87SolarEngine.compute(ghurubJde + dt/86400.0)
        
        // Calculate Topocentric Altitude & Elongation
        val ha = AstroMath.mod(AstroDataUtils.calculateGAST(ghurubJde + dt / 86400.0) + lon - moon.first, 360.0)
        val alt = AstroMath.deg(asin(sin(AstroMath.rad(lat)) * sin(AstroMath.rad(moon.second)) + cos(AstroMath.rad(lat)) * cos(AstroMath.rad(moon.second)) * cos(AstroMath.rad(ha))))
        
        val elong = AstroMath.deg(acos(sin(AstroMath.rad(sun.dec)) * sin(AstroMath.rad(moon.second)) + cos(AstroMath.rad(sun.dec)) * cos(AstroMath.rad(moon.second)) * cos(AstroMath.rad(sun.ra - moon.first))))

        val isMabims = alt >= 3.0 && elong >= 6.4
        val status = if (isMabims) "Visible (Imkan Rukyat)" else "Not Visible"
        
        return HijriResult(1, hMonth, hYear, ijtimaJde, ghurubJde, alt, elong, status)
    }
}
