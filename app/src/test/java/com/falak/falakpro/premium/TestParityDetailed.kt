package com.falak.falakpro.premium

import java.util.*
import kotlin.math.*

/**
 * Script Uji Akurasi FalakPro vs NASA/Stellarium
 * Tanggal: 10 Mei 2002 & 24 Mei 2026
 */
fun main() {
    // Parameter Pengujian dari Screenshot Stellarium
    val y = 2026; val m = 5; val d = 24
    val hUT = 13.0 + 2.0/60.0 // 13:02 UT (20:02 WIB)
    val lat = -6.2
    val lon = 106.8
    val elev = 50.0

    val jdUT = SolarFunctions.gregorianToJde(y, m, d, hUT)
    val deltaT = 69.0 // Estimasi Delta T 2026
    val jdTD = jdUT + deltaT / 86400.0

    println("--- PARAMETER UJI ---")
    println("JD (UT) : $jdUT")
    println("JD (TD) : $jdTD")
    println("Delta T  : $deltaT s")

    // 1. UJI MATAHARI
    val sun = Vsop87SolarEngine.compute(jdTD)
    val gast = AstroDataUtils.calculateGAST(jdUT) // GAST HARUS PAKAI JD UT!
    val lhaSun = AstroMath.mod(gast + lon - sun.ra, 360.0)
    
    // Azimuth/Altitude (Airless)
    val (altSun, azSun) = AstroTransform.equatorialToHorizontal(lhaSun, sun.dec, lat)

    println("\n--- HASIL MATAHARI (FalakPro) ---")
    println("App RA  : ${formatDms(sun.ra)} (Stellarium: 04h 05m 25s = 61.35°)")
    println("App Dec : ${formatDms(sun.dec)} (Stellarium: +20° 49' 49\")")
    println("Azimuth : ${formatDms(azSun)} (Stellarium: 290° 31' 32\")")
    println("Altitude: ${formatDms(altSun)} (Stellarium: -34° 10' 26\")")

    // 2. UJI BULAN (TOPOSENTRIK)
    val moonTopo = AstroMoonEngine.getTopocentricPosition(jdUT, lon, lat, elev)
    val lhaMoon = AstroMath.mod(gast + lon - moonTopo.first, 360.0)
    val (altMoon, azMoon) = AstroTransform.equatorialToHorizontal(lhaMoon, moonTopo.second, lat)

    println("\n--- HASIL BULAN (FalakPro) ---")
    println("App RA  : ${formatDms(moonTopo.first)} (Stellarium: 11h 07m 18s = 166.82°)")
    println("App Dec : ${formatDms(moonTopo.second)} (Stellarium: +04° 35' 10\")")
    println("Azimuth : ${formatDms(azMoon)} (Stellarium: 301° 22' 15\")")
    println("Altitude: ${formatDms(altMoon)} (Stellarium: +68° 13' 33\")")
}

fun formatDms(deg: Double): String {
    val d = floor(abs(deg)).toInt()
    val m = floor((abs(deg) - d) * 60.0).toInt()
    val s = (abs(deg) - d - m/60.0) * 3600.0
    return String.format("%s%02d° %02d' %05.2f\"", if(deg<0) "-" else "+", d, m, s)
}
