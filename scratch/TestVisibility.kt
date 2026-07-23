package com.falak.falakpro.premium

import java.io.File
import java.io.FileInputStream

fun main() {
    println("Starting calculation...")
    
    // Initialize ephemeris engines
    val coreBin = File("app/src/main/assets/mpp02_core.bin")
    val vsopBin = File("app/src/main/assets/earth_vsop87d.bin")
    
    if (!coreBin.exists() || !vsopBin.exists()) {
        println("Binary files not found. Please run from project root.")
        return
    }
    
    ElpDataProvider.initialize(FileInputStream(coreBin))
    Vsop87SolarEngine.initialize(FileInputStream(vsopBin))
    
    // Conjunction for Dzulhijjah 1447 H: Saturday, 16 May 2026 20:03:07 GMT
    // Let's compute JDE for Saturday, 16 May 2026 20:03:07 GMT
    // GMT is UT. JD for 16 May 2026 20:03:07 UT:
    val ijtimaJdUt = 2461177.33550  // 20:03:07 UT is 20.052 / 24 = 0.8355 days. 
    // Wait, let's calculate: 16 May 2026 is JD 2461176.5 at 00:00 UT.
    // 20:03:07 UT is 20.052 hours. 20.052/24 = 0.8355.
    // So 16 May 2026 20:03:07 UT is JD 2461177.33550 UT.
    // In TD, JDE = JD + deltaT/86400.
    val dt = DynamicalTimeEngine.deltaT(ijtimaJdUt)
    val ijtimaJde = ijtimaJdUt + dt / 86400.0
    
    println("Ijtima JDE: $ijtimaJde")
    
    // Run map calculations for dayOffset = 1 (H+1, Sunday, 17 May 2026)
    val mode = HilalVisibilityMapMode.YALLOP
    
    // Let's test specific locations:
    // 1. Jakarta: -6.2, 106.8
    // 2. Mecca: 21.4, 39.8
    // 3. New York: 40.7, -74.0
    // 4. London: 51.5, -0.1
    // 5. Sydney: -33.9, 151.2
    
    val locations = listOf(
        "Sydney" to Pair(-33.9, 151.2),
        "Jakarta" to Pair(-6.2, 106.8),
        "Mecca" to Pair(21.4, 39.8),
        "London" to Pair(51.5, -0.1),
        "New York" to Pair(40.7, -74.0)
    )
    
    val baseDateJdUt = Math.floor(ijtimaJdUt + 0.5) - 0.5 + 1.0 // dayOffset = 1
    val ephTable = HilalVisibilityMapEngine.buildFastEphTable(baseDateJdUt)
    
    for ((name, coords) in locations) {
        val (lat, lon) = coords
        val pt = HilalVisibilityMapEngine.evaluateFastPoint(ijtimaJde, baseDateJdUt, lat, lon, ephTable, mode)
        println(String.format("%-10s: lat=%.1f, lon=%.1f -> zone=%s, score=%.3f, arcV=%.3f, arcL=%.3f, age=%.1f, lag=%.2f",
            name, lat, lon, pt.zone.name, pt.score, pt.arcV, pt.arcL, pt.ageHours, pt.moonLagHours))
    }
}
