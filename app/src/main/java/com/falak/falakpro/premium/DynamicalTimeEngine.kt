package com.falak.falakpro.premium

import kotlin.math.*

/**
 * DynamicalTimeEngine — 100% PERSIS v1.8 Parity Edition.
 * Implementation of both DeltaT (complex) and DeltaT2 (simple) from PERSIS.
 */
object DynamicalTimeEngine {

    /**
     * PERSIS DeltaT (Complex version used in Local Eclipse)
     */
    fun deltaT(jde: Double): Double {
        val cal = jdeToCalendar(jde)
        val thnM = cal[0].toLong()
        
        val jdAw = gregorianToJde(1, 1, thnM.toInt(), 0.0)
        val jdAk = gregorianToJde(31, 12, thnM.toInt(), 24.0)
        
        val dY = thnM + (jde - jdAw) / (jdAk - jdAw)
        var dltT = computeBaseDeltaT(dY)

        val sCorr: Double
        if (dY < 1955 || dY > 2005) {
            sCorr = -0.000012932 * (dY - 1955).pow(2.0)
            dltT += sCorr
        } else {
            sCorr = 0.0
        }
        
        return dltT
    }

    /**
     * PERSIS DeltaT2 (Simple version used in SBesselian/Global)
     */
    fun deltaT2(jde: Double): Double {
        val cal = jdeToCalendar(jde)
        val thnM = cal[0].toLong()
        val blnM = cal[1]
        
        val dY = thnM + (blnM - 0.5) / 12.0
        return computeBaseDeltaT(dY)
    }

    private fun computeBaseDeltaT(dY: Double): Double {
        val kU: Double
        val dltT: Double
        
        if (dY <= -500) {
            kU = (dY - 1820) / 100.0
            dltT = -20 + 32 * (kU * kU)
        } else if ((dY > -500) && (dY <= 500)) {
            kU = dY / 100.0
            dltT = 10583.6 - 1014.41 * kU + 33.78311 * (kU * kU) - 5.952053 * (kU * kU * kU) - 0.1798452 * (kU * kU * kU * kU) + 0.022174192 * (kU * kU * kU * kU * kU) + 0.0090316521 * (kU * kU * kU * kU * kU * kU)
        } else if ((dY > 500) && (dY <= 1600)) {
            kU = (dY - 1000) / 100.0
            dltT = 1574.2 - 556.01 * kU + 71.23472 * (kU * kU) + 0.319781 * (kU * kU * kU) - 0.8503463 * (kU * kU * kU * kU) - 0.005050998 * (kU * kU * kU * kU * kU) + 0.0083572073 * (kU * kU * kU * kU * kU * kU)
        } else if ((dY > 1600) && (dY <= 1700)) {
            kU = (dY - 1600) / 100.0
            dltT = 120 - 98.08 * kU - 153.2 * (kU * kU) + (kU * kU * kU) / 0.007129
        } else if ((dY > 1700) && (dY <= 1800)) {
            kU = (dY - 1700) / 100.0
            dltT = 8.83 + 16.03 * kU - 59.285 * (kU * kU) + 133.36 * (kU * kU * kU) - (kU * kU * kU * kU) / 0.01174
        } else if ((dY > 1800) && (dY <= 1860)) {
            kU = (dY - 1800) / 100.0
            dltT = 13.72 - 33.2447 * kU + 68.612 * (kU * kU) + 4111.6 * (kU * kU * kU) - 37436 * (kU * kU * kU * kU) + 121272 * (kU * kU * kU * kU * kU) - 1699000 * (kU * kU * kU * kU * kU * kU) + 87500 * (kU * kU * kU * kU * kU * kU * kU)
        } else if ((dY > 1860) && (dY <= 1900)) {
            kU = (dY - 1860) / 100.0
            dltT = 7.62 + 57.37 * kU - 2517.54 * (kU * kU) + 16806.68 * (kU * kU * kU) - 44736.24 * (kU * kU * kU * kU) + (kU * kU * kU * kU * kU) / 0.00000233174
        } else if ((dY > 1900) && (dY <= 1920)) {
            kU = (dY - 1900) / 100.0
            dltT = -2.79 + 149.4119 * kU - 598.939 * (kU * kU) + 6196.6 * (kU * kU * kU) - 19700 * (kU * kU * kU * kU)
        } else if ((dY > 1920) && (dY <= 1941)) {
            kU = (dY - 1920) / 100.0
            dltT = 21.20 + 84.493 * kU - 761.00 * (kU * kU) + 2093.6 * (kU * kU * kU)
        } else if ((dY > 1941) && (dY <= 1961)) {
            kU = (dY - 1950) / 100.0
            dltT = 29.07 + 40.7 * kU - (kU * kU) / 0.0233 + (kU * kU * kU) / 0.002547
        } else if ((dY > 1961) && (dY <= 1986)) {
            kU = (dY - 1975) / 100.0
            dltT = 45.45 + 106.7 * kU - (kU * kU) / 0.026 - (kU * kU * kU) / 0.000718
        } else if ((dY > 1986) && (dY <= 2005)) {
            kU = (dY - 2000) / 100.0
            dltT = 63.86 + 33.45 * kU - 603.74 * (kU * kU) + 1727.5 * (kU * kU * kU) + 65181.4 * (kU * kU * kU * kU) + 237359.9 * (kU * kU * kU * kU * kU)
        } else if ((dY > 2005) && (dY <= 2015)) {
            kU = dY - 2005
            dltT = 64.69 + 0.293 * kU
        } else if ((dY > 2015) && (dY <= 3000)) {
            kU = dY - 2015
            dltT = 67.62 + 0.3645 * kU + 0.0039755 * (kU * kU)
        } else {
            dltT = 0.0
        }
        return dltT
    }

    private fun gregorianToJde(day: Int, month: Int, year: Int, hourUT: Double): Double {
        var y = year; var m = month; if (m <= 2) { y--; m += 12 }
        val a = y / 100; val b = 2 - a + (a / 4)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5 + hourUT / 24.0
    }

    private fun jdeToCalendar(jde: Double): IntArray {
        val jd = jde + 0.5; val z = floor(jd).toLong()
        val a = if (z < 2299161) z else { val alpha = floor((z - 1867216.25) / 36524.25).toLong(); z + 1 + alpha - floor(alpha / 4.0).toLong() }
        val b = a + 1524; val c = floor((b - 122.1) / 365.25).toLong(); val d = floor(365.25 * c).toLong(); val e = floor((b - d) / 30.6001).toLong()
        val month = (if (e < 14) e - 1 else e - 13).toInt()
        val year = (if (month > 2) c - 4716 else c - 4715).toInt()
        return intArrayOf(year, month)
    }
}
