package com.falak.falakpro.premium

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor

class NasaApparentLongitudeComparisonTest {

    @Test
    fun apparentLongitude2026Jun16_00ut_againstNasaHorizons() {
        loadEphemerisAssets()

        val jdUt = CalendarFunctions.gregorianToJde(2026, 6, 16.0)
        val deltaT = DynamicalTimeEngine.deltaT(jdUt)
        val jdeTd = jdUt + deltaT / 86400.0
        val horizonsTtUtc = 69.184
        val horizonsLikeJdeTd = jdUt + horizonsTtUtc / 86400.0

        val sun = Vsop87SolarEngine.compute(jdeTd)
        val rows = mutableListOf(
            ComparisonRow(
                body = "Matahari",
                source = "earth_vsop87d.bin",
                nasaDeg = 84.8903183,
                falakProDeg = normalize360(sun.longitudeEcliptic)
            )
        )

        val lunarAssets = listOf(
            "mpp02_core.bin",
            "mpp02_core_from_elpmp02_de405.bin",
            "mpp02_core_from_elpmp02_de405_falakpro_compatible.bin"
        )
        for (asset in lunarAssets) {
            resetElpDataProvider()
            File("src/main/assets/$asset").inputStream().use {
                ElpDataProvider.initialize(it)
            }
            val moon = ElpMpp02LunarEngine.computeGeometric(jdeTd)
            val moonHorizonsTime = ElpMpp02LunarEngine.computeGeometric(horizonsLikeJdeTd)
            rows += ComparisonRow(
                body = "Bulan",
                source = asset,
                nasaDeg = 97.4704812,
                falakProDeg = normalize360(moon.longitudeEcliptic)
            )
            rows += ComparisonRow(
                body = "Bulan",
                source = "$asset @ TT-UTC 69.184s",
                nasaDeg = 97.4704812,
                falakProDeg = normalize360(moonHorizonsTime.longitudeEcliptic)
            )
        }

        val report = buildString {
            appendLine("Perbandingan apparent ecliptic longitude geosentris")
            appendLine("Tanggal: 2026-06-16 00:00 UT")
            appendLine("Acuan NASA/JPL Horizons: ObsEcLon, geosentris Bumi, apparent, ecliptic-of-date")
            appendLine("JD UT : ${"%.8f".formatUs(jdUt)}")
            appendLine("Delta T aplikasi : ${"%.3f".formatUs(deltaT)} detik")
            appendLine("JDE TD: ${"%.8f".formatUs(jdeTd)}")
            appendLine("Pembanding waktu NASA/Horizons modern: TT-UTC ${"%.3f".formatUs(horizonsTtUtc)} detik")
            appendLine()
            appendLine("Objek     Sumber                                             NASA deg       FalakPro deg   Selisih arcsec    NASA DMS          FalakPro DMS")
            appendLine("------------------------------------------------------------------------------------------------------------------------------------------")
            for (row in rows) {
                appendLine(
                    "%-9s %-50s %-14.7f %-14.7f %+14.3f   %-15s %s".format(
                        Locale.US,
                        row.body,
                        row.source,
                        row.nasaDeg,
                        row.falakProDeg,
                        signedArcsecDifference(row.falakProDeg, row.nasaDeg),
                        dms(row.nasaDeg),
                        dms(row.falakProDeg)
                    )
                )
            }
        }

        val output = File("build/reports/nasa-apparent-longitude-20260616.txt")
        output.parentFile?.mkdirs()
        output.writeText(report)
        println(report)
        assertTrue(output.isFile)
    }

    private fun loadEphemerisAssets() {
        File("src/main/assets/iau2000a_nutation.bin").inputStream().use {
            Iau2006Nutation.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }
    }

    private data class ComparisonRow(
        val body: String,
        val source: String,
        val nasaDeg: Double,
        val falakProDeg: Double
    )

    private fun resetElpDataProvider() {
        val field = ElpDataProvider::class.java.getDeclaredField("flatData")
        field.isAccessible = true
        field.set(null, null)
    }

    private fun signedArcsecDifference(valueDeg: Double, referenceDeg: Double): Double {
        val diff = ((valueDeg - referenceDeg + 540.0) % 360.0) - 180.0
        return diff * 3600.0
    }

    private fun normalize360(value: Double): Double {
        val normalized = value % 360.0
        return if (normalized < 0.0) normalized + 360.0 else normalized
    }

    private fun dms(value: Double): String {
        val sign = if (value < 0.0) "-" else ""
        val absValue = abs(value)
        val d = floor(absValue).toInt()
        val minuteFull = (absValue - d) * 60.0
        val m = floor(minuteFull).toInt()
        val s = (minuteFull - m) * 60.0
        return String.format(Locale.US, "%s%03d deg %02d' %05.2f\"", sign, d, m, s)
    }

    private fun String.formatUs(vararg args: Any): String =
        String.format(Locale.US, this, *args)
}
