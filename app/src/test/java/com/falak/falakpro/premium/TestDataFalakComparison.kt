package com.falak.falakpro.premium

import org.junit.Test
import java.io.File
import java.util.*
import kotlin.math.*

class TestDataFalakComparison {

    @Test
    fun runTestDataFalakComparison() {
        val outputFile = File("C:/FalakPro/test_output.txt")
        outputFile.writeText("")
        fun printOut(s: String) {
            println(s)
            outputFile.appendText(s + "\n")
        }

        // Initialize Engines
        val elpFile = File("src/main/assets/mpp02_core.bin")
        val vsopFile = File("src/main/assets/earth_vsop87d.bin")
        
        if (elpFile.exists()) ElpDataProvider.initialize(elpFile.inputStream())
        if (vsopFile.exists()) Vsop87SolarEngine.initialize(vsopFile.inputStream())

        printOut("===============================================================")
    
    testZulqadah1447(::printOut)
    
    val d = 17; val m = 5; val y = 2026
    
        printOut("\n--- DATA MATAHARI (00:00 & 12:00 UT) ---")
        for (h in listOf(0, 12)) {
            val jd = AstroTime.kmjd(d, m, y, h.toDouble(), 0.0)
            val jdeTD = jd + DynamicalTimeEngine.deltaT(jd) / 86400.0
            
            val sun = Vsop87SolarEngine.compute(jdeTD)
            val obliq = AstroDataUtils.calculateTrueObliquity(jdeTD)
            val eotHours = AstroSolarEngine.getEquationOfTime(jdeTD)
            val sdSunDeg = AstroSolarEngine.getSemidiameter(jdeTD)
            
            printOut("Jam $h:00 UT:")
            printOut("  App Ecliptic Longitude : ${formatDmsTest(sun.longitudeEcliptic)}")
            printOut("  App Ecliptic Latitude  : ${String.format(Locale.US, "%.2f\"", sun.latitudeEcliptic * 3600.0)}")
            printOut("  App Right Ascension    : ${formatDmsTest(sun.ra)}")
            printOut("  App Declination        : ${formatDmsSignedTest(sun.dec)}")
            printOut("  True Geocentric Dist   : ${String.format(Locale.US, "%.7f", sun.distanceAU)}")
            printOut("  Semi Diameter          : ${formatSdDegTest(sdSunDeg)}")
            printOut("  True Obliquity         : ${formatDmsTest(obliq)}")
            printOut("  Equation of Time       : ${formatEoTMinSecTest(eotHours * 60.0)}")
        }

        printOut("\n--- DATA BULAN (00:00 UT) ---")
        val jd0 = AstroTime.kmjd(d, m, y, 0.0, 0.0)
        val jdeTD0 = jd0 + DynamicalTimeEngine.deltaT(jd0) / 86400.0
        val sun0 = Vsop87SolarEngine.compute(jdeTD0)
        val moon0 = ElpMpp02LunarEngine.computeGeometric(jdeTD0)
        val illum0 = LunarFunctions.moonIllumination(
            sun0.ra, sun0.dec, sun0.distanceAU,
            moon0.ra, moon0.dec, moon0.distanceAU * 149597870.7
        )
        printOut("Jam 00:00 UT:")
        printOut("  App Longitude          : ${formatDmsTest(moon0.longitudeEcliptic)}")
        printOut("  App Latitude           : ${formatDmsSignedTest(moon0.latitudeEcliptic)}")
        printOut("  App Right Ascension    : ${formatDmsTest(moon0.ra)}")
        printOut("  App Declination        : ${formatDmsSignedTest(moon0.dec)}")
        printOut("  Horizontal Parallax    : ${formatDmsTest(moon0.horizontalParallax)} (in deg) | in min: ${String.format(Locale.US, "%.2f'", moon0.horizontalParallax * 60.0)}")
        printOut("  Semi Diameter          : ${formatSdDegTest(moon0.semidiameter)}")
        printOut("  Angle Bright Limb      : ${formatDmsTest(illum0.positionAngleBright)}")
        printOut("  Fraction Illumination  : ${String.format(Locale.US, "%.2f %%", illum0.illuminatedFraction * 100.0)}")

        printOut("\n===============================================================")
        printOut("      UJI PERBANDINGAN ALMANAK NAUTIKA (NAUTICAL ALMANAC)      ")
        printOut("               Tanggal: 10 Mei 2002 M                          ")
        printOut("===============================================================")
        val d2 = 10; val m2 = 5; val y2 = 2002
        printOut("\n--- MATAHARI & BULAN (10h UT) ---")
        val h2 = 10
        val jd2 = AstroTime.kmjd(d2, m2, y2, h2.toDouble(), 0.0)
        val jdeTD2 = jd2 + DynamicalTimeEngine.deltaT(jd2) / 86400.0

        val sun2 = Vsop87SolarEngine.compute(jdeTD2)
        val ghaSun2 = AstroMath.mod(AstroDataUtils.calculateGAST(jdeTD2) - sun2.ra, 360.0)

        val moon2 = ElpMpp02LunarEngine.computeGeometric(jdeTD2)
        val ghaMoon2 = AstroMath.mod(AstroDataUtils.calculateGAST(jdeTD2) - moon2.ra, 360.0)

        val jdNext2 = AstroTime.kmjd(d2, m2, y2, h2 + 1.0, 0.0)
        val jdeNext2 = jdNext2 + DynamicalTimeEngine.deltaT(jdNext2) / 86400.0
        val moonNext2 = ElpMpp02LunarEngine.computeGeometric(jdeNext2)
        val ghaMoonNext2 = AstroMath.mod(AstroDataUtils.calculateGAST(jdeNext2) - moonNext2.ra, 360.0)

        val diffGha2 = AstroMath.mod(ghaMoonNext2 - ghaMoon2, 360.0)
        val vMin2 = (diffGha2 - 14.316666666666666) * 60.0

        val dMin2 = abs(moonNext2.dec - moon2.dec) * 60.0
        val hpMin2 = moon2.horizontalParallax * 60.0

        printOut("Jam 10:00 UT:")
        printOut("  SUN GHA                : ${formatGhaDecTest(ghaSun2)}")
        printOut("  SUN Dec                : ${formatAlmanacDecTest(sun2.dec)}")
        printOut("  MOON GHA               : ${formatGhaDecTest(ghaMoon2)}")
        printOut("  MOON v                 : ${String.format(Locale.US, "%.1f'", vMin2)}")
        printOut("  MOON Dec               : ${formatAlmanacDecTest(moon2.dec)}")
        printOut("  MOON d                 : ${String.format(Locale.US, "%.1f'", dMin2)}")
        printOut("  MOON HP                : ${String.format(Locale.US, "%.1f'", hpMin2)}")
        
        printOut("\n--- SUMMARY TABLES (10 Mei 2002) ---")
        val jd0_2 = AstroTime.kmjd(d2, m2, y2, 0.0, 0.0)
        val jde0_2 = jd0_2 + DynamicalTimeEngine.deltaT(jd0_2) / 86400.0
        val eot0_2 = AstroSolarEngine.getEquationOfTime(jde0_2) * 60.0

        val jd12_2 = AstroTime.kmjd(d2, m2, y2, 12.0, 0.0)
        val jde12_2 = jd12_2 + DynamicalTimeEngine.deltaT(jd12_2) / 86400.0
        val eot12_2 = AstroSolarEngine.getEquationOfTime(jde12_2) * 60.0
        val merPassSun2 = 12.0 - AstroSolarEngine.getEquationOfTime(jde12_2)

        val upper2 = findMoonMeridianPassageTest(d2, m2, y2, 0.0)
        val lower2 = findMoonMeridianPassageTest(d2, m2, y2, 180.0)

        val ageDays2 = calculateMoonAgeTest(d2, m2, y2)
        val sun0_2 = Vsop87SolarEngine.compute(jde0_2)
        val moon0_2 = ElpMpp02LunarEngine.computeGeometric(jde0_2)
        val illum0_2 = LunarFunctions.moonIllumination(
            sun0_2.ra, sun0_2.dec, sun0_2.distanceAU,
            moon0_2.ra, moon0_2.dec, moon0_2.distanceAU * 149597870.7
        )

        printOut("  SUN Eqn. of Time 00h   : ${formatEoTMinSecTest(eot0_2)}")
        printOut("  SUN Eqn. of Time 12h   : ${formatEoTMinSecTest(eot12_2)}")
        printOut("  SUN Mer. Pass.         : ${formatHourMinTest(merPassSun2)}")
        printOut("  MOON Mer. Pass. Upper  : ${if (upper2 != null) formatHourMinTest(upper2) else "-"}")
        printOut("  MOON Mer. Pass. Lower  : ${if (lower2 != null) formatHourMinTest(lower2) else "-"}")
        printOut("  MOON Age               : ${String.format(Locale.US, "%02d hari (%.1f h)", floor(ageDays2).toInt(), ageDays2)}")
        printOut("  MOON Phase             : ${String.format(Locale.US, "%.1f %%", illum0_2.illuminatedFraction * 100.0)}")
    }

    fun testZulqadah1447(printOut: (String) -> Unit) {
        printOut("\n===============================================================")
        printOut("      UJI PERBANDINGAN DZULQA'DAH 1447 H (BANDUNG)             ")
        printOut("               Tanggal: 17 April 2026 M                        ")
        printOut("===============================================================")

        val lat = -7.083333333333333 // 07° 05' LS
        val lon = 107.6 // 107° 36' BT
        val elev = 700.0
        val tz = 7.0

        val hYear = 1447
        val hMonth = 11 // Dzulqa'dah

        val res = HilalEngine.calculateHilalStart(hYear, hMonth, lat, lon, elev, tz)

        printOut("Saat Perhitungan   : ${res.saatPerhitunganStr}")
        printOut("Ijtima Geosentris  : ${res.ijtimaGeoStr}")
        printOut("Ijtima Toposentris : ${res.ijtimaTopoStr}")
        printOut("Ghurub Matahari    : ${res.ghurubSun}")
        printOut("Ghurub Bulan       : ${res.ghurubMoon}")
        printOut("\n--- POSISI SAAT SUNSET ---")
        printOut("T. Bujur Bulan     : ${res.bujurBulanStr}  | T. Bujur Matahari : ${res.bujurMatahariStr}")
        printOut("T. Lintang Bulan   : ${res.lintangBulanStr}  | T. Lintang Matahari : ${res.lintangMatahariStr}")
        printOut("T. RA Bulan        : ${res.raBulanStr}  | T. RA Matahari    : ${res.raMatahariStr}")
        printOut("T. Dec Bulan       : ${res.decBulanStr}  | T. Dec Matahari    : ${res.decMatahariStr}")
        printOut("T. Azimuth Bulan   : ${res.azBulanStr}  | T. Azimuth Matahari : ${res.azMatahariStr}")
        printOut("T. Tinggi Matahari : ${res.altTopoMatahariStr}")
        printOut("T. Tinggi Bulan (Mid): ${res.altTopoBulanTengahStr}")
        printOut("M. Tinggi Bulan (Mid): ${res.altMariBulanTengahStr}")
        printOut("G. Elongasi        : ${res.elongasiGeoStr}")
        printOut("T. Elongasi        : ${res.elongasiTopoStr}")
        printOut("T. Lebar Sabit     : ${res.lebarSabitStr}")
        printOut("Range Q Odeh       : ${res.rangeQOdehStr}")
    }

fun formatDmsTest(deg: Double): String {
    val absDeg = abs(deg)
    var d = floor(absDeg).toInt()
    var m = floor((absDeg - d) * 60.0).toInt()
    var s = round(((absDeg - d) * 60.0 - m) * 60.0).toInt()
    if (s >= 60) { s = 0; m++ }
    if (m >= 60) { m = 0; d++ }
    return String.format(Locale.US, "%02d° %02d' %02d\"", d, m, s)
}

fun formatDmsSignedTest(deg: Double): String {
    val absDeg = abs(deg)
    var d = floor(absDeg).toInt()
    var m = floor((absDeg - d) * 60.0).toInt()
    var s = round(((absDeg - d) * 60.0 - m) * 60.0).toInt()
    if (s >= 60) { s = 0; m++ }
    if (m >= 60) { m = 0; d++ }
    val sign = if (deg >= 0) "+" else "-"
    return String.format(Locale.US, "%s%02d° %02d' %02d\"", sign, d, m, s)
}

fun formatSdDegTest(deg: Double): String {
    val m = floor(deg * 60.0).toInt()
    val s = (deg * 60.0 - m) * 60.0
    return String.format(Locale.US, "%02d' %05.2f\"", m, s)
}

fun formatEoTMinSecTest(eotMin: Double): String {
    val sign = if (eotMin < 0) "-" else ""
    val absMin = abs(eotMin)
    val m = floor(absMin).toInt()
    val s = round((absMin - m) * 60.0).toInt()
    return String.format(Locale.US, "%s%02dm %02ds", sign, m, s)
}

fun formatGhaDecTest(deg: Double): String {
    val d = floor(deg).toInt()
    val m = (deg - d) * 60.0
    return String.format(Locale.US, "%03d° %04.1f'", d, m)
}

fun formatAlmanacDecTest(dec: Double): String {
    val absDec = abs(dec)
    val d = floor(absDec).toInt()
    val m = (absDec - d) * 60.0
    val prefix = if (dec >= 0) "N" else "S"
    return String.format(Locale.US, "%s%02d° %04.1f'", prefix, d, m)
}

fun formatHourMinTest(hours: Double): String {
    val h = floor(hours).toInt()
    val m = round((hours - h) * 60.0).toInt()
    return String.format(Locale.US, "%02dh %02dm", h, m)
}

fun findMoonMeridianPassageTest(d: Int, m: Int, y: Int, targetGHA: Double): Double? {
    for (h in 0..23) {
        val jd1 = AstroTime.kmjd(d, m, y, h.toDouble(), 0.0)
        val jde1 = jd1 + DynamicalTimeEngine.deltaT(jd1) / 86400.0
        val gha1 = AstroMath.mod(AstroDataUtils.calculateGAST(jde1) - ElpMpp02LunarEngine.computeGeometric(jde1).ra, 360.0)

        val jd2 = AstroTime.kmjd(d, m, y, h + 1.0, 0.0)
        val jde2 = jd2 + DynamicalTimeEngine.deltaT(jd2) / 86400.0
        val gha2 = AstroMath.mod(AstroDataUtils.calculateGAST(jde2) - ElpMpp02LunarEngine.computeGeometric(jde2).ra, 360.0)

        var g1 = gha1
        var g2 = gha2
        if (g2 < g1) { g2 += 360.0 }
        var tg = targetGHA
        if (tg < g1 && (tg + 360.0) <= g2) { tg += 360.0 }

        if (tg in g1..g2) {
            val fraction = (tg - g1) / (g2 - g1)
            return h + fraction
        }
    }
    return null
}

fun calculateMoonAgeTest(d: Int, m: Int, y: Int): Double {
    val jdCurr = AstroTime.kmjd(d, m, y, 0.0, 0.0)
    val k = LunarFunctions.approximateK(y, m, LunarFunctions.LunarPhase.NEW_MOON)
    var jdeNM = LunarFunctions.lunarPhaseJde(k)
    if (jdeNM > jdCurr) {
        jdeNM = LunarFunctions.lunarPhaseJde(k - 1.0)
    }
    return jdCurr - jdeNM
}
}
