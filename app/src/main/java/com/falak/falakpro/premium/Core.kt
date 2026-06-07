package com.falak.falakpro.premium

import kotlin.math.*

/**
 * AstroMath — Adopted from official MathFunction.kt.
 */
object AstroMath {
    fun deg(x: Double) = Math.toDegrees(x)
    fun rad(x: Double) = Math.toRadians(x)
    fun mod(x: Double, y: Double) = x - y * floor(x / y)

    // Solar-eclipse Besselian constants used by NASA eclipse predictions.
    const val EARTH_EQUATORIAL_RADIUS_M = 6378137.0
    const val EARTH_POLAR_RADIUS_M = 6356752.0
    const val EARTH_FLATTENING = 1.0 / 298.257223563
    const val EARTH_ECCENTRICITY_SQUARED = 2.0 * EARTH_FLATTENING - EARTH_FLATTENING * EARTH_FLATTENING
    const val SOLAR_PARALLAX_ARCSEC = 8.794
    const val SOLAR_ECLIPSE_PENUMBRAL_K = 0.2725076
    const val SOLAR_ECLIPSE_UMBRAL_K = 0.2722810
    const val BESSELIAN_PENUMBRAL_CONE_SINE = 0.004664026
    const val BESSELIAN_UMBRAL_CONE_SINE = 0.004640784
    const val SIDEREAL_DEGREES_PER_UT_SECOND = 0.00417807
    
    fun sign(x: Double): Int = when {
        x > 0 -> 1
        x < 0 -> -1
        else -> 0
    }

    fun interpolationFromFiveTabularValues(yM2: Double, yM1: Double, y0: Double, yP1: Double, yP2: Double, opt: Int): Double {
        val a = yM1 - yM2; val b = y0 - yM1; val c = yP1 - y0; val d = yP2 - yP1
        val e = b - a; val f = c - b; val g = d - c
        val h = f - e; val j = g - f; val k = j - h
        return when (opt) {
            0 -> y0
            1 -> (b + c) / 2.0 - (h + j) / 12.0
            2 -> f / 2.0 - k / 24.0
            3 -> (h + j) / 12.0
            4 -> k / 24.0
            else -> y0
        }
    }

    fun roundTo(x: Double, decimals: Int): Double {
        val factor = 10.0.pow(decimals)
        return if (x >= 0) floor(x * factor + 0.5) / factor
               else -floor(abs(x) * factor + 0.5) / factor
    }
}

/**
 * AstroTime — Adopted from official JulianDay.kt and DynamicalTime.kt.
 */
object AstroTime {
    
    fun kmjd(d: Int, m: Int, y: Int, hourDes: Double = 0.0, tz: Double = 0.0): Double {
        var yy = y.toLong(); var mm = m.toLong()
        val dd = d + (hourDes - tz) / 24.0
        if (mm <= 2) { yy--; mm += 12 }
        
        val bb = if ((y + m / 100.0 + d / 10000.0) >= 1582.1015) {
            val aa = floor(yy / 100.0)
            2 - aa + floor(aa / 4.0)
        } else 0.0
        
        return floor(365.25 * (yy + 4716)) + floor(30.6001 * (mm + 1)) + dd + bb - 1524.5
    }

}
