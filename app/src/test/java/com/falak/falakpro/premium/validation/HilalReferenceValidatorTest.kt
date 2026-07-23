package com.falak.falakpro.premium.validation
import com.falak.falakpro.premium.HilalEngine
import kotlin.math.abs

/**
 * Validator awal bulan untuk menguji konsistensi engine FalakPro terhadap data rujukan.
 *
 * Taruh file ini di:
 * app/src/test/java/com/falak/falakpro/premium/validation/HilalReferenceValidatorTest.kt
 *
 * Catatan:
 * - Isi angka reference dari data rujukan yang dipercaya: JPL/Horizons, BMKG, kitab/software falak, atau data observatorium.
 * - Toleransi default dibuat realistis untuk audit awal, bukan sertifikasi final.
 * - Kalau engine butuh file binary VSOP/ELP, pastikan test menginisialisasi asset atau jalankan sebagai androidTest.
 */
class HilalReferenceValidatorTest {

    data class Case(
        val name: String,
        val hijriYear: Int,
        val hijriMonth: Int,
        val latitude: Double,
        val longitude: Double,
        val elevation: Double,
        val timezone: Double,

        // Isi nilai rujukan:
        val refIjtimaLocalIso: String? = null, // contoh: "2026-04-17T18:21:00"
        val refSunsetLocal: String? = null,    // contoh: "17:52:10"
        val refAltitudeDeg: Double? = null,    // tinggi hilal mar'i/tengah, derajat desimal
        val refElongationDeg: Double? = null,  // elongasi geosentris/toposentris, derajat desimal
        val refMoonAgeHours: Double? = null    // umur hilal saat ghurub, jam
    )

    private val cases = listOf(
        Case(
            name = "Syawal 1447 H - Jakarta",
            hijriYear = 1447,
            hijriMonth = 10,
            latitude = -6.175392,
            longitude = 106.827153,
            elevation = 8.0,
            timezone = 7.0,
            // TODO isi dari data rujukan sampeyan
            refIjtimaLocalIso = null,
            refSunsetLocal = null,
            refAltitudeDeg = null,
            refElongationDeg = null,
            refMoonAgeHours = null
        ),
        Case(
            name = "Ramadan 1447 H - Jakarta",
            hijriYear = 1447,
            hijriMonth = 9,
            latitude = -6.175392,
            longitude = 106.827153,
            elevation = 8.0,
            timezone = 7.0,
            // TODO isi dari data rujukan sampeyan
            refIjtimaLocalIso = null,
            refSunsetLocal = null,
            refAltitudeDeg = null,
            refElongationDeg = null,
            refMoonAgeHours = null
        )
    )

    @org.junit.Test
    fun validateAwalBulanAgainstReference() {
        val report = StringBuilder()
        report.appendLine("=== FalakPro Hilal Validation ===")
        report.appendLine("Model target: Meeus AA 2nd Ed + VSOP87D Sun + ELP/MPP02 Moon")
        report.appendLine()

        cases.forEach { c ->
            val result = HilalEngine.calculateHilalStart(
                hijriYear = c.hijriYear,
                hijriMonth = c.hijriMonth,
                latitude = c.latitude,
                longitude = c.longitude,
                elevation = c.elevation,
                timezone = c.timezone
            )

            report.appendLine("CASE: ${c.name}")
            report.appendLine("Ijtima geo : ${result.ijtimaGeoStr.replace("\n", " | ")}")
            report.appendLine("Ghurub sun : ${result.ghurubSun}")
            report.appendLine("Alt mar'i  : ${result.altMariBulanTengahStr}")
            report.appendLine("Elongasi   : ${result.elongasiTopoStr}")
            report.appendLine("Kesimpulan : ${result.conclusion}")
            report.appendLine()

            // Parser string dibuat toleran karena output aplikasi memakai format Indonesia.
            val altDeg = parseDmsToDegrees(result.altMariBulanTengahStr)
            val elongDeg = parseDmsToDegrees(result.elongasiTopoStr)

            c.refAltitudeDeg?.let {
                assertNear("${c.name} altitude", altDeg, it, tolerance = 0.05) // 3 arcmin
            }

            c.refElongationDeg?.let {
                assertNear("${c.name} elongation", elongDeg, it, tolerance = 0.05) // 3 arcmin
            }

            c.refSunsetLocal?.let {
                val got = result.ghurubSun.substringBefore(" ")
                assertTimeNear("${c.name} sunset", got, it, toleranceSeconds = 90)
            }

            // IjtimaLocalIso dan MoonAgeHours sengaja belum dipaksa karena output current engine berupa string lokal.
            // Nanti bisa dibuat parsing khusus kalau format final sudah dibakukan.
        }

        println(report.toString())
    }

    private fun assertNear(label: String, got: Double, expected: Double, tolerance: Double) {
        val diff = abs(got - expected)
        assert(diff <= tolerance) {
            "$label meleset. got=$got expected=$expected diff=$diff tolerance=$tolerance"
        }
    }

    private fun assertTimeNear(label: String, got: String, expected: String, toleranceSeconds: Int) {
        val g = parseHms(got)
        val e = parseHms(expected)
        var diff = abs(g - e)
        if (diff > 43200) diff = 86400 - diff
        assert(diff <= toleranceSeconds) {
            "$label meleset. got=$got expected=$expected diff=${diff}s tolerance=${toleranceSeconds}s"
        }
    }

    private fun parseHms(s: String): Int {
        val parts = s.trim().split(":")
        require(parts.size >= 2) { "Format waktu tidak valid: $s" }
        val h = parts[0].toInt()
        val m = parts[1].toInt()
        val sec = parts.getOrNull(2)?.toIntOrNull() ?: 0
        return h * 3600 + m * 60 + sec
    }

    private fun parseDmsToDegrees(text: String): Double {
        // Contoh diterima:
        // +003° 12’ 40’’
        // -01° 05’ 02,300’’
        // 003° 12’ 40,5’’
        val clean = text
            .replace(",", ".")
            .replace("−", "-")
            .replace("’", "'")
            .replace("‘", "'")
            .replace("’’", "\"")
            .replace("”", "\"")
            .trim()

        val sign = if (clean.contains("-")) -1.0 else 1.0
        val regex = Regex("""([+-]?\d+(?:\.\d+)?)\D+(\d+(?:\.\d+)?)\D+(\d+(?:\.\d+)?)""")
        val m = regex.find(clean) ?: error("Tidak bisa parse DMS: $text")
        val d = abs(m.groupValues[1].toDouble())
        val min = m.groupValues[2].toDouble()
        val sec = m.groupValues[3].toDouble()
        return sign * (d + min / 60.0 + sec / 3600.0)
    }
}
