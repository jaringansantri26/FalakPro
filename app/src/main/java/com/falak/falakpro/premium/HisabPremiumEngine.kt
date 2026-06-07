package com.falak.falakpro.premium

import android.content.Context
import kotlin.math.*

/**
 * HisabPremiumEngine — Engine Hisab Hilal Presisi Tinggi
 */
class HisabPremiumEngine(context: Context) {

    init {
        // Initialize engines with binary data
        context.assets.open("mpp02_core.bin").use {
            ElpDataProvider.initialize(it)
        }
        context.assets.open("earth_vsop87d.bin").use {
            Vsop87SolarEngine.initialize(it)
        }
    }

    data class SunData(
        val ra: Double, val dec: Double, val distAU: Double, val sd: Double,
        val eot: Double, val azimuth: Double, val altitude: Double, val obliquity: Double
    )

    data class MoonData(
        val ra: Double, val dec: Double, val distKm: Double, val hp: Double, val sd: Double,
        val altGeometric: Double, val altTopocentric: Double, val altApparent: Double,
        val azimuth: Double, val elongation: Double
    )

    data class IjtimaData(val jde: Double, val dateGregorian: String, val timeLocal: Double, val timeUtc: Double)

    data class HisabPremiumResult(
        val sun: SunData, val moon: MoonData, val ijtima: IjtimaData,
        val ghurubJde: Double, val ghurubLocal: Double, val muktsulHilal: Double,
        val ghurubHilal: Double, val elongasiToposentrik: Double, val nurul: Double,
        val tanggalMasehi: Int, val bulanMasehi: Int, val tahunMasehi: Int,
        val hari: String, val pasaran: String
    )

    fun gregorianToJde(day: Int, month: Int, year: Int, hourUT: Double = 12.0): Double {
        var y = year; var m = month; if (m <= 2) { y--; m += 12 }
        val A = (y / 100); val B = 2 - A + (A / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5 + hourUT / 24.0
    }

    private fun jdeToGregorian(jde: Double): Triple<Int, Int, Int> {
        val z = floor(jde + 0.5).toInt(); val f = (jde + 0.5) - z
        val alpha = floor((z - 1867216.25) / 36524.25).toInt(); val a = z + 1 + alpha - alpha / 4
        val b = a + 1524; val c = floor((b - 122.1) / 365.25).toInt(); val d = floor(365.25 * c).toInt()
        val e = floor((b - d) / 30.6001).toInt(); val day = b - d - floor(30.6001 * e).toInt()
        val month = if (e < 14) e - 1 else e - 13; val year = if (month > 2) c - 4716 else c - 4715
        return Triple(day, month, year)
    }

    private fun computeGhurubJde(dateJde: Double, lat: Double, lon: Double, heightM: Double, tz: Double): Double {
        val target = -0.8333 + (-0.0347 * sqrt(heightM))
        var jde = dateJde
        repeat(5) {
            val sun = Vsop87SolarEngine.compute(jde)
            val jdeTD = jde + 69.0 / 86400.0 // Assuming a constant Delta T for now
            val gast = AstroDataUtils.calculateGAST(jdeTD)
            val ha = (gast + lon - sun.ra).let { if(it > 180) it - 360 else if(it < -180) it + 360 else it }
            val alt = Math.toDegrees(asin(sin(Math.toRadians(lat)) * sin(Math.toRadians(sun.dec)) + cos(Math.toRadians(lat)) * cos(Math.toRadians(sun.dec)) * cos(Math.toRadians(ha))))
            val denom = -360.0 * cos(Math.toRadians(sun.dec)) * cos(Math.toRadians(lat)) * sin(Math.toRadians(ha))
            if (abs(denom) > 1e-10) jde -= (alt - target) / denom
        }
        return jde
    }

    private fun computeIjtimaJde(approxJde: Double): Double {
        var lo = approxJde - 5.0; var hi = approxJde + 5.0
        fun diff(j: Double): Double {
            val sun = Vsop87SolarEngine.compute(j)
            val moon = ElpMpp02LunarEngine.computeGeometric(j)
            var d = (moon.longitudeEcliptic - sun.longitudeEcliptic).mod(360.0)
            return if (d > 180) d - 360 else d
        }
        repeat(50) { val mid = (lo + hi) / 2.0; if (diff(lo) * diff(mid) <= 0) hi = mid else lo = mid }
        return (lo + hi) / 2.0
    }

    fun hitung(m: Int, y: Int, lat: Double, lon: Double, h: Double, tz: Double): HisabPremiumResult {
        val approxJde = gregorianToJde(1, m, y, 12.0)
        val ijtimaJde = computeIjtimaJde(approxJde)
        val (id, im, iy) = jdeToGregorian(ijtimaJde)
        val ghurubJde = computeGhurubJde(gregorianToJde(id, im, iy, 12.0), lat, lon, h, tz)
        
        val sun = Vsop87SolarEngine.compute(ghurubJde)
        val moon = ElpMpp02LunarEngine.computeGeometric(ghurubJde)
        
        return HisabPremiumResult(
            sun = SunData(sun.ra, sun.dec, sun.distanceAU, 0.267, 0.0, 0.0, 0.0, AstroDataUtils.calculateTrueObliquity(ghurubJde)),
            moon = MoonData(moon.ra, moon.dec, moon.distanceAU * 149597870.7, moon.horizontalParallax, moon.semidiameter, 0.0, 0.0, 0.0, 0.0, 0.0),
            ijtima = IjtimaData(ijtimaJde, "N/A", 0.0, 0.0),
            ghurubJde = ghurubJde, ghurubLocal = 0.0, muktsulHilal = 0.0, ghurubHilal = 0.0,
            elongasiToposentrik = 0.0, nurul = 0.0,
            tanggalMasehi = id, bulanMasehi = im, tahunMasehi = iy, hari = "N/A", pasaran = "N/A"
        )
    }
}
