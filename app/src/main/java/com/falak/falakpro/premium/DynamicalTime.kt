package com.falak.falakpro.premium

import kotlin.math.*

/**
 * DynamicalTime — Implementasi Delta-T sesuai standar Astro
 */
object DynamicalTime {

    /**
     * Menghitung Delta T (selisih TT - UT1) dalam detik
     * Berdasarkan algoritma yang diberikan oleh USER
     */
    fun deltaT(jd: Double): Double {
        // Konversi JDE ke Gregorian untuk mendapatkan tahun desimal
        val (year, month, day) = CalendarFunctions.jdeToGregorian(jd)
        
        // Perkiraan hari dalam tahun (day of year)
        // JD awal tahun (1 Januari jam 00:00)
        val jdStartOfYear = CalendarFunctions.gregorianToJde(year, 1, 1.0)
        // JD akhir tahun (31 Desember jam 24:00 atau 1 Januari tahun depan jam 00:00)
        val jdEndOfYear = CalendarFunctions.gregorianToJde(year + 1, 1, 1.0)
        
        val daysInYear = jdEndOfYear - jdStartOfYear
        val dayCount = jd - jdStartOfYear
        
        val dY = year + dayCount / daysInYear
        
        val kU: Double
        var dltT: Double

        if (dY <= -500) {
            kU = (dY - 1820) / 100
            dltT = -20.0 + 32.0 * (kU * kU)
        } else if (dY <= 500) {
            kU = dY / 100
            dltT = 10583.6 - 1014.41 * kU + 33.78311 * (kU * kU) - 5.952053 * (kU * kU * kU) - 
                   0.1798452 * (kU.pow(4)) + 0.022174192 * (kU.pow(5)) + 0.0090316521 * (kU.pow(6))
        } else if (dY <= 1600) {
            kU = (dY - 1000) / 100
            dltT = 1574.2 - 556.01 * kU + 71.23472 * (kU * kU) + 0.319781 * (kU * kU * kU) - 
                   0.8503463 * (kU.pow(4)) - 0.005050998 * (kU.pow(5)) + 0.0083572073 * (kU.pow(6))
        } else if (dY <= 1700) {
            kU = (dY - 1600) / 100
            dltT = 120.0 - 98.08 * kU - 153.2 * (kU * kU) + (kU * kU * kU) / 0.007129
        } else if (dY <= 1800) {
            kU = (dY - 1700) / 100
            dltT = 8.83 + 16.03 * kU - 59.285 * (kU * kU) + 133.36 * (kU * kU * kU) - (kU.pow(4)) / 0.01174
        } else if (dY <= 1860) {
            kU = (dY - 1800) / 100
            dltT = 13.72 - 33.2447 * kU + 68.612 * (kU * kU) + 4111.6 * (kU * kU * kU) - 
                   37436.0 * (kU.pow(4)) + 121272.0 * (kU.pow(5)) - 1699000.0 * (kU.pow(6)) + 87500.0 * (kU.pow(7))
        } else if (dY <= 1900) {
            kU = (dY - 1860) / 100
            dltT = 7.62 + 57.37 * kU - 2517.54 * (kU * kU) + 16806.68 * (kU * kU * kU) - 
                   44736.24 * (kU.pow(4)) + (kU.pow(5)) / 0.00000233174
        } else if (dY <= 1920) {
            kU = (dY - 1900) / 100
            dltT = -2.79 + 149.4119 * kU - 598.939 * (kU * kU) + 6196.6 * (kU * kU * kU) - 19700.0 * (kU.pow(4))
        } else if (dY <= 1941) {
            kU = (dY - 1920) / 100
            dltT = 21.20 + 84.493 * kU - 761.0 * (kU * kU) + 2093.6 * (kU * kU * kU)
        } else if (dY <= 1961) {
            kU = (dY - 1950) / 100
            dltT = 29.07 + 40.7 * kU - (kU * kU) / 0.0233 + (kU * kU * kU) / 0.002547
        } else if (dY <= 1986) {
            kU = (dY - 1975) / 100
            dltT = 45.45 + 106.7 * kU - (kU * kU) / 0.026 - (kU * kU * kU) / 0.000718
        } else if (dY <= 2005) {
            kU = (dY - 2000) / 100
            dltT = 63.86 + 33.45 * kU - 603.74 * (kU * kU) + 1727.5 * (kU * kU * kU) + 
                   65181.4 * (kU.pow(4)) + 237359.9 * (kU.pow(5))
        } else if (dY <= 2015) {
            kU = dY - 2005
            dltT = 64.69 + 0.293 * kU
        } else if (dY <= 3000) {
            kU = dY - 2015
            dltT = 67.62 + 0.3645 * kU + 0.0039755 * (kU * kU)
        } else {
            dltT = 0.0
        }

        if (dY < 1955 || dY > 2005) {
            val sCorr = -0.000012932 * (dY - 1955).pow(2.0)
            dltT += sCorr
        }

        return dltT
    }
}
