package com.falak.falakpro.premium

import com.falak.falakpro.KriteriaHilal
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale
import kotlin.math.*

/**
 * Data class representing the result of a Hilal (new moon) calculation.
 */
data class HilalResult(
    val hijriYear: Int,
    val hijriMonth: Int,
    val gregorianDate: LocalDateTime,
    val julianDay: Double,
    val deltaT: Double,
    val algorithm: String,
    val geoLongitude: Double,
    val topoLongitude: Double,
    val ghurubSun: String,
    val ghurubMoon: String,
    val locationName: String = "",
    val isVisibleMabimsBaru: Boolean = false,
    val isVisibleMabimsLama: Boolean = false,
    val isVisibleWujudulHilal: Boolean = false,
    val isVisibleKghtTurki: Boolean = false,
    val isVisibleKghtMuhammadiyah: Boolean = false,
    val isVisibleLapan: Boolean = false,
    val isVisibleYallop: Boolean = false,
    val isVisibleOdeh: Boolean = false,
    val isVisibleDanjon: Boolean = false,
    val kghtTurkiLocation: String = "",
    val kghtMuhammadiyahLocation: String = "",
    val kghtTurkiAltitudeTopoStr: String = "",
    val kghtTurkiElongationGeoStr: String = "",
    val kghtMuhammadiyahAltitudeGeoStr: String = "",
    val kghtMuhammadiyahElongationGeoStr: String = "",
    val summary: String = "",
    val elongation: String,
    val illumination: String,
    val semidiameter: String,
    val conclusion: String,
    val ijtimaGeoStr: String,
    val ijtimaTopoStr: String,
    val altHaqiqiStr: String,
    val altMariStr: String,
    val saatPerhitunganStr: String,
    val bujurBulanStr: String,
    val bujurMatahariStr: String,
    val lintangBulanStr: String,
    val lintangMatahariStr: String,
    val raBulanStr: String,
    val raMatahariStr: String,
    val decBulanStr: String,
    val decMatahariStr: String,
    val azBulanStr: String,
    val azMatahariStr: String,
    val altGeoBulanStr: String,
    val altTopoBulanAtasStr: String,
    val altTopoBulanTengahStr: String,
    val altTopoBulanBawahStr: String,
    val altMariBulanAtasStr: String,
    val altMariBulanTengahStr: String,
    val altMariBulanBawahStr: String,
    val lebarSabitStr: String,
    val elongasiGeoStr: String,
    val elongasiTopoStr: String,
    val bestTimeStr: String,
    val arahTerbenamBulanStr: String,
    val hpBulanStr: String,
    val rangeQOdehStr: String,
    val jarakBumiBulanStr: String,
    val altGeoBulanTengahStr: String,
    val altTopoMatahariStr: String,
    val conclusionImkanurRukyat: String,
    val conclusionWujudulHilal: String,
    val dayOfIjtimaInPrevMonth: Int = 29
)

private fun formatDmsAngle(deg: Double): String {
    if (deg.isNaN()) return "0° 00’ 00,000’’"
    val d = abs(deg)
    val degInt = floor(d).toInt()
    val m = (d - degInt) * 60.0
    val minInt = floor(m).toInt()
    val s = (m - minInt) * 60.0
    return String.format(Locale.US, "%d° %02d’ %05.3f’’", degInt, minInt, if (s.isNaN()) 0.0 else s).replace('.', ',')
}

private fun formatDmsAngleWithSign(deg: Double): String {
    if (deg.isNaN()) return "+00° 00’ 00’’"
    val signStr = if (deg < 0) "-" else "+"
    val d = abs(deg)
    val degInt = floor(d).toInt()
    val m = (d - degInt) * 60.0
    val minInt = floor(m).toInt()
    val s = (m - minInt) * 60.0
    val sInt = if (s.isNaN()) 0 else s.roundToInt()
    val degNorm = if (sInt == 60) degInt + (minInt + 1) / 60 else degInt
    val minNorm = if (sInt == 60) (minInt + 1) % 60 else minInt
    val sNorm = if (sInt == 60) 0 else sInt
    return String.format(Locale.US, "%s%d° %02d’ %02d’’", signStr, degNorm, minNorm, sNorm)
}

private fun formatDms3AnglePrecise(deg: Double, withSign: Boolean = true): String {
    if (deg.isNaN()) return if (withSign) "+000° 00’ 00,000’’" else "000° 00’ 00,000’’"
    val signStr = if (deg < 0) "-" else if (withSign) "+" else ""
    val dFull = abs(deg)
    val degInt = floor(dFull).toInt()
    val mFull = (dFull - degInt) * 60.0
    val minInt = floor(mFull).toInt()
    val sFull = (mFull - minInt) * 60.0
    return "%s%03d° %02d’ %06.3f’’".format(Locale.US, signStr, degInt, minInt, sFull).replace('.', ',')
}

private fun formatDms3AngleWithSign(deg: Double): String {
    if (deg.isNaN()) return "+000° 00’ 00’’"
    val signStr = if (deg < 0) "-" else "+"
    val dFull = abs(deg)
    val degInt = floor(dFull).toInt()
    val mFull = (dFull - degInt) * 60.0
    val minInt = floor(mFull).toInt()
    val sFull = (mFull - minInt) * 60.0
    val sInt = sFull.roundToInt()

    val dFinal: Int; val mFinal: Int; val sFinal: Int
    if (sInt >= 60) {
        val mCarry = minInt + 1
        if (mCarry >= 60) {
            dFinal = degInt + 1; mFinal = 0; sFinal = 0
        } else {
            dFinal = degInt; mFinal = mCarry; sFinal = 0
        }
    } else {
        dFinal = degInt; mFinal = minInt; sFinal = sInt
    }
    return "%s%03d° %02d’ %02d’’".format(Locale.US, signStr, dFinal, mFinal, sFinal)
}

private fun formatHmsAngleWithSign(raDeg: Double): String {
    if (raDeg.isNaN()) return "+00h 00m 00s"
    val h = floor(raDeg / 15.0).toInt()
    val m = floor((raDeg / 15.0 - h) * 60.0).toInt()
    val s = ((raDeg / 15.0 - h) * 60.0 - m) * 60.0
    val sInt = if (s.isNaN()) 0 else s.roundToInt()
    val hNorm = if (sInt == 60) h + (m + 1) / 60 else h
    val mNorm = if (sInt == 60) (m + 1) % 60 else m
    val sNorm = if (sInt == 60) 0 else sInt
    return String.format(Locale.US, "+%dh %02dm %02ds", hNorm, mNorm, sNorm)
}

private fun equatorialToEcliptic(ra: Double, dec: Double, epsDeg: Double): Pair<Double, Double> {
    val rRA = AstroMath.rad(ra); val rDec = AstroMath.rad(dec); val rEps = AstroMath.rad(epsDeg)
    val sinBeta = sin(rDec) * cos(rEps) - cos(rDec) * sin(rEps) * sin(rRA)
    val beta = AstroMath.deg(asin(sinBeta.coerceIn(-1.0, 1.0)))
    val y = sin(rRA) * cos(rEps) + tan(rDec) * sin(rEps)
    val x = cos(rRA)
    val lambda = AstroMath.mod(AstroMath.deg(atan2(y, x)), 360.0)
    return Pair(lambda, beta)
}

private fun calculateAzimuth(haDeg: Double, decDeg: Double, latDeg: Double): Double {
    val rHA  = AstroMath.rad(haDeg)
    val rDec = AstroMath.rad(decDeg)
    val rLat = AstroMath.rad(latDeg)
    val y = -sin(rHA) * cos(rDec)
    val x = sin(rDec) * cos(rLat) - cos(rDec) * sin(rLat) * cos(rHA)
    return AstroMath.mod(AstroMath.deg(atan2(y, x)), 360.0)
}

private fun formatIjtimaDateOnly(jdLocal: Double): String {
    if (jdLocal.isNaN()) return "N/A"
    // Tambahkan 1e-6 untuk menghindari float precision error di batas tengah malam
    val (y, m, dDouble) = CalendarFunctions.jdeToGregorian(jdLocal + 1e-6)
    val dInt = dDouble.toInt()
    val idx = ((floor(jdLocal + 1.5).toLong()) % 7).toInt()
    val dayName = CalendarFunctions.DAYS_ARABIC.getOrElse(idx) { "" }
    val monthName = when(m) {
        1 -> "Januari"; 2 -> "Februari"; 3 -> "Maret"; 4 -> "April"; 5 -> "Mei"; 6 -> "Juni"
        7 -> "Juli"; 8 -> "Agustus"; 9 -> "September"; 10 -> "Oktober"; 11 -> "November"; 12 -> "Desember"
        else -> ""
    }
    return "$dayName, $dInt $monthName $y M"
}

private fun formatIjtimaTimeOnly(jdLocal: Double): String {
    if (jdLocal.isNaN()) return "N/A"
    val (_, _, dDouble) = CalendarFunctions.jdeToGregorian(jdLocal)
    val f = dDouble - dDouble.toInt()
    val totalSeconds = f * 86400.0
    val h = (totalSeconds / 3600).toInt()
    val min = ((totalSeconds % 3600) / 60).toInt()
    val s = (totalSeconds % 60.0).let { if (it.isNaN()) 0 else it.roundToInt() }
    val hNorm = if (s == 60) h + (min + 1) / 60 else h
    val minNorm = if (s == 60) (min + 1) % 60 else min
    val sNorm = if (s == 60) 0 else s
    return String.format(Locale.US, "%02d:%02d:%02d", hNorm, minNorm, sNorm)
}

private fun formatIjtimaTimeIndo(jdLocal: Double, tz: Double): String {
    if (jdLocal.isNaN()) return "N/A"
    val tzStr = if (tz == 7.0) "WIB" else if (tz == 8.0) "WITA" else if (tz == 9.0) "WIT" else "LT"
    val timeLocalStr = formatIjtimaTimeOnly(jdLocal)
    val timeUtStr = formatIjtimaTimeOnly(jdLocal - tz / 24.0)
    return "$timeLocalStr $tzStr | $timeUtStr UT"
}

private fun formatGhurubTime(jdUt: Double, tz: Double): String {
    if (jdUt.isNaN()) return "N/A"
    val jdLocal = jdUt + tz / 24.0
    val jdMidnight = floor(jdLocal + 0.5) - 0.5
    val f = jdLocal - jdMidnight
    val totalSeconds = f * 86400.0
    val h = (totalSeconds / 3600).toInt()
    val min = ((totalSeconds % 3600) / 60).toInt()
    val s = (totalSeconds % 60.0).let { if (it.isNaN()) 0 else it.roundToInt() }
    val hNorm = if (s == 60) (h + (min + 1) / 60) else h
    val minNorm = if (s == 60) (min + 1) % 60 else min
    val sNorm = if (s == 60) 0 else s
    val tzStr = if (tz == 7.0) "WIB" else if (tz == 8.0) "WITA" else if (tz == 9.0) "WIT" else "GMT+%.0f".format(tz)
    return String.format(Locale.US, "%02d:%02d:%02d %s", hNorm, minNorm, sNorm, tzStr)
}

object HilalEngine {
    private val khgtGlobalCache = mutableMapOf<String, GlobalKhgtScanResult>()

    private enum class KhgtGlobalTarget {
        TURKI,
        MUHAMMADIYAH,
        BOTH
    }

    private data class GlobalKhgtScanResult(
        val turkiTopoAltitude: Double,
        val turkiGeoElongation: Double,
        val turkiLocation: String,
        val muhammadiyahGeoAltitude: Double,
        val muhammadiyahGeoElongation: Double,
        val muhammadiyahLocation: String
    ) {
        val isTurkiVisible: Boolean
            get() = turkiTopoAltitude >= 5.0 && turkiGeoElongation >= 8.0

        val isMuhammadiyahVisible: Boolean
            get() = muhammadiyahGeoAltitude >= 5.0 && muhammadiyahGeoElongation >= 8.0
    }

    private fun normalizeLongitude180(lon: Double): Double {
        var x = lon % 360.0
        if (x > 180.0) x -= 360.0
        if (x < -180.0) x += 360.0
        return x
    }

    private fun geoElongationDeg(sunRa: Double, sunDec: Double, moonRa: Double, moonDec: Double): Double {
        val c = sin(AstroMath.rad(sunDec)) * sin(AstroMath.rad(moonDec)) +
                cos(AstroMath.rad(sunDec)) * cos(AstroMath.rad(moonDec)) * cos(AstroMath.rad(sunRa - moonRa))
        return AstroMath.deg(acos(c.coerceIn(-1.0, 1.0)))
    }

    private fun altitudeFromRaDec(raDeg: Double, decDeg: Double, latDeg: Double, lonDeg: Double, gastDeg: Double): Double {
        val ha = AstroMath.mod(gastDeg + lonDeg - raDeg, 360.0)
        val sinAlt = sin(AstroMath.rad(latDeg)) * sin(AstroMath.rad(decDeg)) +
                cos(AstroMath.rad(latDeg)) * cos(AstroMath.rad(decDeg)) * cos(AstroMath.rad(ha))
        return AstroMath.deg(asin(sinAlt.coerceIn(-1.0, 1.0)))
    }

    private data class KhgtPoint(
        val name: String,
        val lat: Double,
        val lon: Double
    )

    private data class KhgtCandidate(
        val lat: Double,
        val lon: Double,
        val approxGeoAlt: Double,
        val approxTopoAlt: Double,
        val approxElong: Double,
        val score: Double
    )

    private data class KhgtEphData(
        val jdUt: Double,
        val moonRa: Double,
        val moonDec: Double,
        val moonDistanceKm: Double,
        val sunRa: Double,
        val sunDec: Double,
        val moonLon: Double,
        val sunLon: Double
    ) {
        val elong: Double
            get() {
                var d = abs(moonLon - sunLon) % 360.0
                if (d > 180.0) d = 360.0 - d
                return d
            }
    }

    /**
     * Pre-scan global ringan ala peta visibilitas/Yallop.
     *
     * Prinsipnya sama dengan HilalMapEngine:
     * - VSOP/ELP hanya dipakai untuk membuat tabel efemeris per jam.
     * - Grid 47.291 titik tidak memanggil VSOP/ELP satu per satu.
     * - Grid hanya memakai interpolasi RA/Dec/jarak + rumus horizontal cepat.
     * - Titik mustahil dibuang.
     * - Titik kandidat terbaik baru dihitung ulang dengan VSOP/ELP presisi.
     */
    private fun buildKhgtEphTable(jdUtIjtima: Double): List<KhgtEphData> {
        return (0..48).map { h ->
            val jdUt = jdUtIjtima + (h - 6) / 24.0
            val jde = jdUt + DynamicalTimeEngine.deltaT(jdUt) / 86400.0
            val sun = Vsop87SolarEngine.compute(jde)
            val moon = ElpMpp02LunarEngine.computeGeometric(jde)
            KhgtEphData(
                jdUt = jdUt,
                moonRa = moon.ra,
                moonDec = moon.dec,
                moonDistanceKm = moon.distanceAU * 149597870.7,
                sunRa = sun.ra,
                sunDec = sun.dec,
                moonLon = moon.longitudeEcliptic,
                sunLon = sun.longitudeEcliptic
            )
        }
    }

    private fun interpolateAngleDeg(a0: Double, a1: Double, f: Double): Double {
        var d = (a1 - a0) % 360.0
        if (d > 180.0) d -= 360.0
        if (d < -180.0) d += 360.0
        return AstroMath.mod(a0 + f * d, 360.0)
    }

    private fun interpolateKhgtEph(jdUt: Double, table: List<KhgtEphData>): KhgtEphData {
        val raw = (jdUt - table.first().jdUt) * 24.0
        val i0 = floor(raw).toInt().coerceIn(0, table.size - 2)
        val f = (raw - i0).coerceIn(0.0, 1.0)
        val p0 = table[i0]
        val p1 = table[i0 + 1]
        return KhgtEphData(
            jdUt = jdUt,
            moonRa = interpolateAngleDeg(p0.moonRa, p1.moonRa, f),
            moonDec = p0.moonDec + f * (p1.moonDec - p0.moonDec),
            moonDistanceKm = p0.moonDistanceKm + f * (p1.moonDistanceKm - p0.moonDistanceKm),
            sunRa = interpolateAngleDeg(p0.sunRa, p1.sunRa, f),
            sunDec = p0.sunDec + f * (p1.sunDec - p0.sunDec),
            moonLon = interpolateAngleDeg(p0.moonLon, p1.moonLon, f),
            sunLon = interpolateAngleDeg(p0.sunLon, p1.sunLon, f)
        )
    }

    /**
     * Ghurub cepat untuk pre-scan.
     * Ini bukan keputusan final; hanya untuk mencari kandidat.
     * Final tetap memakai findFirstSunsetAfter() + VSOP/ELP penuh.
     */
    private fun approximateLocalSunsetUt(jdUtAnchor: Double, latDeg: Double, lonDeg: Double): Double {
        val day0 = floor(jdUtAnchor + 0.5) - 0.5
        // 18:15 waktu lokal rata-rata; cukup untuk prefilter S-curve seperti peta visibilitas.
        return day0 + 18.25 / 24.0 - (lonDeg / 360.0)
    }

    fun findSunsetNear(jdUtApprox: Double, lat: Double, lon: Double): Double? {
        var ghurubJd = jdUtApprox
        for (iter in 0..10) {
            val dt = DynamicalTimeEngine.deltaT(ghurubJd)
            val jde = ghurubJd + dt / 86400.0
            val sun = Vsop87SolarEngine.compute(jde)
            val gast = AstroDataUtils.calculateGAST(ghurubJd)
            val ha = AstroMath.mod(gast + lon - sun.ra, 360.0)
            val haRad = AstroMath.rad(ha)
            val latRad = AstroMath.rad(lat)
            val decRad = AstroMath.rad(sun.dec)
            val sdS = (959.63 / 3600.0) / sun.distanceAU
            val targetAlt = -0.8333 - sdS
            val sinAlt = kotlin.math.sin(latRad) * kotlin.math.sin(decRad) + kotlin.math.cos(latRad) * kotlin.math.cos(decRad) * kotlin.math.cos(haRad)
            val alt = AstroMath.deg(kotlin.math.asin(sinAlt.coerceIn(-1.0, 1.0)))
            val denom = -2.0 * kotlin.math.PI * kotlin.math.cos(latRad) * kotlin.math.cos(decRad) * kotlin.math.sin(haRad)
            if (kotlin.math.abs(denom) < 1e-10) break
            val deltaJd = AstroMath.rad(alt - targetAlt) / denom
            ghurubJd -= deltaJd
            if (kotlin.math.abs(deltaJd) < 1e-6) break
        }
        return ghurubJd
    }

    private fun findFirstSunsetAfter(jdUtAnchor: Double, lat: Double, lon: Double): Double? {
        var ghurubJd = approximateLocalSunsetUt(jdUtAnchor, lat, lon)
        if (ghurubJd < jdUtAnchor - 0.02) ghurubJd += 1.0

        repeat(2) {
            for (iter in 0..5) {
                val dt = DynamicalTimeEngine.deltaT(ghurubJd)
                val jde = ghurubJd + dt / 86400.0
                val sun = Vsop87SolarEngine.compute(jde)
                val gast = AstroDataUtils.calculateGAST(ghurubJd)
                val ha = AstroMath.mod(gast + lon - sun.ra, 360.0)
                val haRad = AstroMath.rad(ha)
                val latRad = AstroMath.rad(lat)
                val decRad = AstroMath.rad(sun.dec)
                val sdS = (959.63 / 3600.0) / sun.distanceAU
                val targetAlt = -0.8333 - sdS
                val sinAlt = sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(haRad)
                val alt = AstroMath.deg(asin(sinAlt.coerceIn(-1.0, 1.0)))
                val denom = -2.0 * PI * cos(latRad) * cos(decRad) * sin(haRad)
                if (abs(denom) < 1e-10) break
                val deltaJd = AstroMath.rad(alt - targetAlt) / denom
                ghurubJd -= deltaJd
            }
            if (ghurubJd >= jdUtAnchor - 0.0001) return ghurubJd
            ghurubJd += 1.0
        }
        return null
    }

    private fun isAmericanLandmass(lat: Double, lon: Double): Boolean {
        val lon180 = normalizeLongitude180(lon)
        val continentalNorthAmerica = lat in 14.0..72.0 && lon180 in -130.0..-52.0
        val centralAmerica = lat in 7.0..24.0 && lon180 in -118.0..-77.0
        val caribbean = lat in 10.0..28.0 && lon180 in -85.0..-52.0
        val southAmerica = lat in -56.0..13.0 && lon180 in -82.0..-34.0
        return isAlaskaKhgtRegion(lat, lon) || continentalNorthAmerica || centralAmerica || caribbean || southAmerica
    }

    private fun isAlaskaKhgtRegion(lat: Double, lon: Double): Boolean {
        val lon180 = normalizeLongitude180(lon)
        return lat in 50.0..72.0 && lon180 in -170.0..-130.0
    }

    private fun isTurkeyExcludedAlaskaRegion(lat: Double, lon: Double): Boolean {
        val lon180 = normalizeLongitude180(lon)
        val aleutianFox = lat in 50.0..56.5 && lon180 in -170.0..-155.0
        val alaskaPeninsula = lat in 54.0..59.5 && lon180 in -165.0..-155.0
        return aleutianFox || alaskaPeninsula
    }

    private fun isIjtimaBeforeFajrNewZealand(jdUtIjtima: Double): Boolean {
        // Pendekatan ringan untuk syarat koreksi KHGT: ijtimak di New Zealand
        // terjadi sebelum fajar. New Zealand memakai UTC+13 pada Februari
        // dan UTC+12 di luar daylight saving; dilonggarkan hingga jam 07:00 lokal
        // agar kasus kritis batas (seperti Ramadhan 1447) tetap masuk.
        val jdNzLocal = jdUtIjtima + 13.0 / 24.0
        val localMidnight = floor(jdNzLocal + 0.5) - 0.5
        val localHour = (jdNzLocal - localMidnight) * 24.0
        return localHour < 7.5
    }

    private fun buildKhgtCandidates(jdUtIjtima: Double, target: KhgtGlobalTarget): List<KhgtCandidate> {
        val table = buildKhgtEphTable(jdUtIjtima)
        val candidates = ArrayList<KhgtCandidate>(1024)
        val alaskaCandidates = ArrayList<KhgtCandidate>(256)

        // Batas PKG-1: pemenuhan kriteria harus terjadi sebelum 24:00 UTC
        // pada tanggal konjungsi UTC. Ini jauh lebih benar daripada memakai
        // window tetap +1,35 hari dari ijtimak.
        val utcMidnightLimit = floor(jdUtIjtima - 0.5) + 1.5
        val americaCorrectionLimit = utcMidnightLimit + 1.35
        val allowAmericaCorrection = if (target == KhgtGlobalTarget.MUHAMMADIYAH || target == KhgtGlobalTarget.BOTH) {
            true
        } else {
            isIjtimaBeforeFajrNewZealand(jdUtIjtima)
        }

        // Meng-cover 47.291 titik: lintang -65..65 dan bujur -180..180 per 1 derajat.
        // Penting: loop ini TIDAK memanggil VSOP/ELP per titik; hanya interpolasi tabel.
        for (latInt in -65..65) {
            val lat = latInt.toDouble()
            for (lonInt in -180..180) {
                val lon = lonInt.toDouble()
                val jdSunsetApprox = approximateLocalSunsetUt(jdUtIjtima, lat, lon)

                // PKG-1: ghurub setelah ijtimak dan sebelum 24:00 UTC.
                // PKG-2/koreksi: bila lewat 24:00 UTC, hanya diterima di kawasan Amerika
                // dan ijtimak New Zealand terjadi sebelum fajar.
                val beforeMidnightUtc = jdSunsetApprox <= utcMidnightLimit
                val americaCorrection = allowAmericaCorrection && isAmericanLandmass(lat, lon) && jdSunsetApprox <= americaCorrectionLimit
                if (jdSunsetApprox < jdUtIjtima || (!beforeMidnightUtc && !americaCorrection)) continue

                val eph = interpolateKhgtEph(jdSunsetApprox, table)
                val elong = eph.elong

                // Filter sengaja dilonggarkan. Ramadan 1447 berada di daerah batas;
                // filter terlalu ketat akan membuang kandidat Alaska/Amerika sebelum refinement.
                if (elong < 6.5) continue

                val gast = AstroDataUtils.calculateGAST(jdSunsetApprox)
                val geoAlt = altitudeFromRaDec(eph.moonRa, eph.moonDec, lat, lon, gast)

                val topoAlt = if (target == KhgtGlobalTarget.MUHAMMADIYAH) {
                    geoAlt
                } else {
                    val hpDeg = AstroMath.deg(asin((6378.14 / eph.moonDistanceKm).coerceIn(-1.0, 1.0)))
                    geoAlt - hpDeg * cos(AstroMath.rad(geoAlt))
                }

                val scoreGeo = min(geoAlt - 5.0, elong - 8.0)
                val score = when (target) {
                    KhgtGlobalTarget.TURKI -> min(topoAlt - 5.0, elong - 8.0)
                    KhgtGlobalTarget.MUHAMMADIYAH -> scoreGeo
                    KhgtGlobalTarget.BOTH -> max(scoreGeo, min(topoAlt - 5.0, elong - 8.0))
                }
                val candidate = KhgtCandidate(lat, lon, geoAlt, topoAlt, elong, score)

                if (americaCorrection && isAlaskaKhgtRegion(lat, lon)) {
                    alaskaCandidates.add(candidate)
                }

                // Longgarkan margin agar titik kritis tidak gugur di pre-scan.
                if (geoAlt < 2.0 && topoAlt < 2.0) continue

                candidates.add(candidate)
            }
        }

        // Kandidat dibatasi supaya refinement presisi tetap ringan.
        // Muhammadiyah tidak perlu kandidat toposentrik; Alaska kritis tetap ditambahkan terpisah.
        val candidateLimit = when (target) {
            KhgtGlobalTarget.MUHAMMADIYAH -> 320
            KhgtGlobalTarget.TURKI -> 450
            KhgtGlobalTarget.BOTH -> 600
        }
        val primaryCandidates = candidates
            .sortedWith(
                compareByDescending<KhgtCandidate> { it.score }
                    .thenByDescending { it.approxGeoAlt }
                    .thenByDescending { it.approxTopoAlt }
            )
            .take(candidateLimit)

        return (primaryCandidates + alaskaCandidates)
            .distinctBy { it.lat to it.lon }
    }

    /**
     * Evaluasi KHGT global kandidat:
     * - Pre-scan ala peta Yallop untuk membuang titik mustahil.
     * - Refinement presisi hanya untuk kandidat terbaik.
     * - Turki memakai tinggi toposentrik dan elongasi geosentrik.
     * - Muhammadiyah memakai tinggi dan elongasi geosentrik.
     */
    private fun scanKhgtGlobalSunsetLine(
        ijtimaGeoJde: Double,
        target: KhgtGlobalTarget = KhgtGlobalTarget.BOTH
    ): GlobalKhgtScanResult {
        val cacheKey = String.format(Locale.US, "KHGT_TOPO_ALT_GEO_ELONG:%s:%.5f", target.name, ijtimaGeoJde)
        khgtGlobalCache[cacheKey]?.let { return it }

        val dtI = DynamicalTimeEngine.deltaT(ijtimaGeoJde)
        val jdUtIjtima = ijtimaGeoJde - dtI / 86400.0

        var bestTurkiTopoAlt = -999.0
        var bestTurkiElongGeo = -999.0
        var bestTurkiScore = -999.0
        var bestTurkiLoc = ""

        var bestMuhammadiyahGeoAlt = -999.0
        var bestMuhammadiyahElongGeo = -999.0
        var bestMuhammadiyahScore = -999.0
        var bestMuhammadiyahLoc = ""

        val utcMidnightLimit = floor(jdUtIjtima - 0.5) + 1.5
        val americaCorrectionLimit = utcMidnightLimit + 1.35
        val allowAmericaCorrectionTurki = isIjtimaBeforeFajrNewZealand(jdUtIjtima)
        // Muhammadiyah menerima kriteria di Amerika asalkan Ijtima terjadi pada hari konjungsi UTC
        val allowAmericaCorrectionMuhammadiyah = true

        val candidates = buildKhgtCandidates(jdUtIjtima, target)
        val scanTurki = target == KhgtGlobalTarget.TURKI || target == KhgtGlobalTarget.BOTH
        val scanMuhammadiyah = target == KhgtGlobalTarget.MUHAMMADIYAH || target == KhgtGlobalTarget.BOTH

        for (c in candidates) {
            val sunsetUt = findFirstSunsetAfter(jdUtIjtima, c.lat, c.lon) ?: continue
            val beforeMidnightUtc = sunsetUt <= utcMidnightLimit
            
            val americaCorrectionT = allowAmericaCorrectionTurki && isAmericanLandmass(c.lat, c.lon) && sunsetUt <= americaCorrectionLimit
            val americaCorrectionM = allowAmericaCorrectionMuhammadiyah && isAmericanLandmass(c.lat, c.lon) && sunsetUt <= americaCorrectionLimit
            
            if (!beforeMidnightUtc && !americaCorrectionT && !americaCorrectionM) continue
            val jde = sunsetUt + DynamicalTimeEngine.deltaT(sunsetUt) / 86400.0
            val gast = AstroDataUtils.calculateGAST(sunsetUt)

            val sun = Vsop87SolarEngine.compute(jde)
            val moonGeo = ElpMpp02LunarEngine.computeGeometric(jde)
            val elongGeo = geoElongationDeg(sun.ra, sun.dec, moonGeo.ra, moonGeo.dec)

            val geoAltMoon = altitudeFromRaDec(moonGeo.ra, moonGeo.dec, c.lat, c.lon, gast)

            val locText = String.format(Locale.US, "%.2f°, %.2f°", c.lat, c.lon)

            if (scanTurki && !isTurkeyExcludedAlaskaRegion(c.lat, c.lon) && (beforeMidnightUtc || americaCorrectionT)) {
                val moonTopo = AstroMoonEngine.getTopocentricPosition(sunsetUt, c.lon, c.lat, 0.0)
                val topoAltMoon = altitudeFromRaDec(moonTopo.first, moonTopo.second, c.lat, c.lon, gast)
                val turkiScore = min(topoAltMoon - 5.0, elongGeo - 8.0)
                if (turkiScore > bestTurkiScore) {
                    bestTurkiScore = turkiScore
                    bestTurkiTopoAlt = topoAltMoon
                    bestTurkiElongGeo = elongGeo
                    bestTurkiLoc = locText
                }
            }

            if (scanMuhammadiyah && (beforeMidnightUtc || americaCorrectionM)) {
                val muhammadiyahScore = min(geoAltMoon - 5.0, elongGeo - 8.0)
                if (muhammadiyahScore > bestMuhammadiyahScore) {
                    bestMuhammadiyahScore = muhammadiyahScore
                    bestMuhammadiyahGeoAlt = geoAltMoon
                    bestMuhammadiyahElongGeo = elongGeo
                    bestMuhammadiyahLoc = locText
                }
            }

            // Jangan break saat salah satu/dua varian sudah lolos.
            // Ramadan 1447 adalah kasus batas: Muhammadiyah dapat lolos secara geosentrik
            // sementara Turki tetap gagal secara toposentrik. Scan kandidat harus selesai
            // agar keduanya punya titik terbaik masing-masing.
        }

        val result = GlobalKhgtScanResult(
            turkiTopoAltitude = bestTurkiTopoAlt,
            turkiGeoElongation = bestTurkiElongGeo,
            turkiLocation = bestTurkiLoc,
            muhammadiyahGeoAltitude = bestMuhammadiyahGeoAlt,
            muhammadiyahGeoElongation = bestMuhammadiyahElongGeo,
            muhammadiyahLocation = bestMuhammadiyahLoc
        )
        khgtGlobalCache[cacheKey] = result
        return result
    }

    private fun checkKhgtGlobalTurki(ijtimaGeoJde: Double): Boolean =
        scanKhgtGlobalSunsetLine(ijtimaGeoJde, KhgtGlobalTarget.TURKI).isTurkiVisible

    private fun checkKhgtGlobalMuhammadiyah(ijtimaGeoJde: Double): Boolean =
        scanKhgtGlobalSunsetLine(ijtimaGeoJde, KhgtGlobalTarget.MUHAMMADIYAH).isMuhammadiyahVisible

    /**
     * Hitung visibilitas hilal lokal untuk penetapan awal bulan Hijriyah.
     *
     * Lebih presisi dari pendekatan lama di CalendarFunctions karena:
     *   1. Ghurub matahari SEJATI (solver iteratif Newton, bukan asumsi 18:00 LT)
     *   2. Elongasi geosentris SEJATI (sudut antara pusat Matahari–Bulan, bukan
     *      sekadar selisih bujur ekliptika yang mengabaikan lintang Bulan)
     *   3. Tinggi hilal TOPOSENTRIS (sudah dikoreksi parallax horiz. Bulan: Δh ≈ −HP·cos(h))
     *
     * Dipanggil oleh CalendarFunctions.getStartJdeOfHijriMonth agar
     * kalender Hijriyah menggunakan kriteria imkanur rukyah yang benar.
     *
     * @param ijtimaGeoJde  JDE (Dynamical Time) konjungsi geosentris (dari Meeus)
     * @param lat           Lintang pengamat (°, positif = Utara)
     * @param lon           Bujur pengamat (°, positif = Timur)
     * @param elev          Ketinggian tempat di atas permukaan laut (meter)
     * @param criteria      Nama kriteria: "Mabims Baru", "Mabims Lama", "Wujudul Hilal",
     *                      "Turki Global", "LAPAN", "Yallop", "ODEH", "Danjon Limit"
     * @return true = hilal IMKAN (bulan baru besok), false = ISTIKMAL (bulan 30 hari)
     */
    fun computeHilalVisibility(
        ijtimaGeoJde: Double,
        lat: Double,
        lon: Double,
        elev: Double,
        criteria: String,
        evalSunsetUt: Double? = null
    ): Boolean {
        // Konversi JDE (TD) → JD UT untuk perhitungan sidereal & sunset
        val dtGeo    = DynamicalTimeEngine.deltaT(ijtimaGeoJde)
        val jdUtIjtima = ijtimaGeoJde - dtGeo / 86400.0

        // ── A. Waktu Ghurub Matahari Sejati (solver Newton iteratif) ──────────
        val ghurubSunUt = evalSunsetUt ?: findFirstSunsetAfter(jdUtIjtima, lat, lon) ?: return false
        val dtSunset    = DynamicalTimeEngine.deltaT(ghurubSunUt)
        val ghurubJde   = ghurubSunUt + dtSunset / 86400.0

        // ── B. Posisi Bulan & Matahari saat Ghurub ────────────────────────────
        val sun     = Vsop87SolarEngine.compute(ghurubJde)
        val moonGeo = ElpMpp02LunarEngine.computeGeometric(ghurubJde)
        val gast    = AstroDataUtils.calculateGAST(ghurubSunUt)

        // ── C. Elongasi Geosentris Sejati ─────────────────────────────────────
        val elongGeo = geoElongationDeg(sun.ra, sun.dec, moonGeo.ra, moonGeo.dec)

        // ── D. Tinggi Hilal Toposentris ───────────────────────────────────────
        // Langkah 1: tinggi geosentris dari jam windu + RA/Dec Bulan
        val moonGeoAlt = altitudeFromRaDec(moonGeo.ra, moonGeo.dec, lat, lon, gast)
        // Langkah 2: koreksi parallax (Bulan tampak lebih rendah karena parallax)
        //            Δalt ≈ −HP·cos(alt)  (Meeus Ch. 40)
        val moonTopoAlt = moonGeoAlt - moonGeo.horizontalParallax * cos(AstroMath.rad(moonGeoAlt))

        // ── E. Syarat Ijtima Sebelum Ghurub ──────────────────────────────────
        val beforeGhurub = ijtimaGeoJde < ghurubJde

        // ── F. Kriteria Imkanur Rukyah ────────────────────────────────────────
        return when (criteria) {

            // Wujudul Hilal (Muhammadiyah): cukup ijtima sebelum ghurub
            "Wujudul Hilal", "KGHT Muhammadiyah" ->
                beforeGhurub

            // MABIMS Lama (1992): ijtima sebelum ghurub & tinggi ≥ 2°
            "Mabims Lama" ->
                beforeGhurub && moonTopoAlt >= 2.0

            // MABIMS Baru (2021, standar RI/Brunei/Malaysia/Singapura):
            //   ijtima sebelum ghurub & tinggi toposentris ≥ 3° & elongasi ≥ 6,4°
            "Mabims Baru" ->
                beforeGhurub && moonTopoAlt >= 3.0 && elongGeo >= 6.4

            // LAPAN: sama parameter Mabims Baru (hisab imkanur rukyah)
            "LAPAN" ->
                beforeGhurub && moonTopoAlt >= 3.0 && elongGeo >= 6.4

            // Istanbul / Turki Global (2016): tinggi toposentris ≥ 5° & elongasi ≥ 8°
            "Turki Global", "KGHT Turki" ->
                moonTopoAlt >= 5.0 && elongGeo >= 8.0

            // Yallop / ODEH: tinggi ≥ 3° & elongasi ≥ 6°
            "Yallop", "ODEH" ->
                moonTopoAlt >= 3.0 && elongGeo >= 6.0

            // Danjon Limit: elongasi minimum fisik ≥ 7°
            "Danjon Limit" ->
                elongGeo >= 7.0

            // Default = MABIMS Baru
            else ->
                beforeGhurub && moonTopoAlt >= 3.0 && elongGeo >= 6.4
        }
    }


    fun calculateMeeusIjtima(hijriYear: Int, hijriMonth: Int): Double {
        // --- 100% PERSIS v1.8 QUADRATIC CONJUNCTION INTERPOLATION ---

        // 1. Initial guess using standard Meeus formula (Ch. 47)
        val jdeArith = CalendarFunctions.hijriToJde(hijriYear, hijriMonth, 1).toDouble()
        val k = round((jdeArith - 2451550.09766) / 29.530588861)
        val t = k / 1236.85
        // Persis Moon Phase constant (0.00015437)
        val jdeStart = 2451550.09766 + 29.530588861 * k + 0.00015437 * t * t - 0.00000015 * t * t * t

        // 2. Sample 3 points (±1 hour)
        val x1 = jdeStart - 1.0 / 24.0
        val x2 = jdeStart
        val x3 = jdeStart + 1.0 / 24.0

        // Calculate diff at each point (Sun - Moon)
        fun diffLon(jd: Double): Double {
            val sun = Vsop87SolarEngine.compute(jd)
            val moon = ElpMpp02LunarEngine.computeGeometric(jd)
            var diff = sun.longitudeEcliptic - moon.longitudeEcliptic
            while (diff < -180.0) diff += 360.0
            while (diff > 180.0) diff -= 360.0
            return diff
        }

        val y1 = diffLon(x1)
        val y2 = diffLon(x2)
        val y3 = diffLon(x3)

        // Quadratic factor (Meeus Ch. 3)
        val a = y2 - y1
        val b = y3 - y2
        val c = b - a

        var n0 = 0.0
        repeat(2) {
            n0 = -2.0 * y2 / (a + b + c * n0)
        }

        return jdeStart + n0 / 24.0
    }

    private fun targetArithJde(yH: Int, mH: Int): Double = CalendarFunctions.hijriToJde(yH, mH, 1)

    fun calculateHilalStart(
        hijriYear: Int,
        hijriMonth: Int,
        latitude: Double,
        longitude: Double,
        elevation: Double,
        timezone: Double,
        algorithm: String = "VSOP87D & ELP/MPP02, Jumlah koreksi 38.326 suku koreksi",
        selectedKriteria: KriteriaHilal? = null
    ): HilalResult {
        // 1. Ijtima Geosentris menggunakan Jean Meeus Chapter 49 (Phases of the Moon)
        // Standar Rinto Anugraha "Mekanika Benda Langit"
        val ijtimaGeoJde = calculateMeeusIjtima(hijriYear, hijriMonth)

        val dtGeo = DynamicalTimeEngine.deltaT(ijtimaGeoJde)
        val ijtimaGeoLocal = ijtimaGeoJde - dtGeo / 86400.0 + timezone / 24.0
        val geoLon = Vsop87SolarEngine.compute(ijtimaGeoJde).longitudeEcliptic

        // floor(JDE_local + 1.5) % 7 → 0=Ahad, 1=Senin, 2=Selasa, 3=Rabu, 4=Kamis, 5=Jum'at, 6=Sabtu
        // Diverifikasi: 17 April 2026 (Jum'at) → idx=5 → harus = "Jum'at" ✓
        val ijtimaDayIdx = ((floor(ijtimaGeoLocal + 1.5).toLong()) % 7).toInt()
        val ijtimaDayName = when(ijtimaDayIdx) {
            0 -> "Ahad"; 1 -> "Senin"; 2 -> "Selasa"; 3 -> "Rabu"; 4 -> "Kamis"; 5 -> "Jum'at"; 6 -> "Sabtu"
            else -> ""
        }

        // JDE 2451545.0 (Noon Jan 1 2000) adalah Pahing.
        // (floor(JD_UT) + 1) % 5 gives Pahing=1.
        val pasaranNames = listOf("Kliwon", "Legi", "Pahing", "Pon", "Wage")
        // ijtimaGeoLocal is local time. Convert to local JDN.
        val localJdn = floor(ijtimaGeoJde + timezone / 24.0 + 0.5)
        val pIdx = ((localJdn.toLong() + 1) % 5).toInt()
        val pName = pasaranNames[pIdx]

        val dateComponent = formatIjtimaDateOnly(ijtimaGeoLocal).replaceFirst(",", " $pName,")
        val ijtimaGeoStr = "$dateComponent\nJam ${formatIjtimaTimeIndo(ijtimaGeoLocal, timezone)}\nBujur Ekliptika Geosentris: ${formatDms3AnglePrecise(geoLon, withSign = false)}"

        var loTopo = ijtimaGeoJde - 0.5; var hiTopo = ijtimaGeoJde + 0.5
        fun diffTopo(j: Double): Double {
            val jdUt = j - DynamicalTimeEngine.deltaT(j) / 86400.0
            val moonTopo = AstroMoonEngine.getTopocentricPosition(jdUt, longitude, latitude, elevation)
            val eps = AstroMath.rad(AstroDataUtils.calculateTrueObliquity(j))
            val yM = sin(AstroMath.rad(moonTopo.first)) * cos(eps) + tan(AstroMath.rad(moonTopo.second)) * sin(eps)
            val xM = cos(AstroMath.rad(moonTopo.first))
            val moonTopoLon = AstroMath.mod(AstroMath.deg(atan2(yM, xM)), 360.0)

            val sunGeo = Vsop87SolarEngine.compute(j)
            val raS = AstroMath.rad(sunGeo.ra)
            val decS = AstroMath.rad(sunGeo.dec)
            val hpS = AstroMath.rad(AstroMath.SOLAR_PARALLAX_ARCSEC / 3600.0)
            val gast = AstroDataUtils.calculateGAST(jdUt)
            val haS = AstroMath.mod(gast + longitude - sunGeo.ra, 360.0)
            val u = atan(0.99664719 * tan(AstroMath.rad(latitude)))
            val x = cos(u) + (elevation / 6378140.0) * cos(AstroMath.rad(latitude))
            val deltaRAS = AstroMath.deg(atan2(-x * sin(hpS) * sin(AstroMath.rad(haS)), cos(decS) - x * sin(hpS) * cos(AstroMath.rad(haS))))
            val raSTopo = AstroMath.rad(sunGeo.ra + deltaRAS)
            val y = 0.99664719 * sin(u) + (elevation / 6378140.0) * sin(AstroMath.rad(latitude))
            val decSTopo = atan2((sin(decS) - y * sin(hpS)) * cos(AstroMath.rad(deltaRAS)), cos(decS) - x * sin(hpS) * cos(AstroMath.rad(haS)))

            val yS = sin(raSTopo) * cos(eps) + tan(decSTopo) * sin(eps)
            val xS = cos(raSTopo)
            val sunTopoLon = AstroMath.mod(AstroMath.deg(atan2(yS, xS)), 360.0)

            return AstroMath.mod(moonTopoLon - sunTopoLon + 180.0, 360.0) - 180.0
        }
        repeat(40) {
            val mid = (loTopo + hiTopo) / 2.0
            if (diffTopo(loTopo) * diffTopo(mid) <= 0) hiTopo = mid else loTopo = mid
        }
        val ijtimaTopoJde = (loTopo + hiTopo) / 2.0

        val jdUtTopo = ijtimaTopoJde - DynamicalTimeEngine.deltaT(ijtimaTopoJde) / 86400.0
        val sunGeoTopo = Vsop87SolarEngine.compute(ijtimaTopoJde)
        val raST = AstroMath.rad(sunGeoTopo.ra)
        val decST = AstroMath.rad(sunGeoTopo.dec)
        val hpST = AstroMath.rad(AstroMath.SOLAR_PARALLAX_ARCSEC / 3600.0)
        val gastT = AstroDataUtils.calculateGAST(jdUtTopo)
        val haST = AstroMath.mod(gastT + longitude - sunGeoTopo.ra, 360.0)
        val uT = atan(0.99664719 * tan(AstroMath.rad(latitude)))
        val xT = cos(uT) + (elevation / 6378140.0) * cos(AstroMath.rad(latitude))
        val deltaRAST = AstroMath.deg(atan2(-xT * sin(hpST) * sin(AstroMath.rad(haST)), cos(decST) - xT * sin(hpST) * cos(AstroMath.rad(haST))))
        val raSTopoT = AstroMath.rad(sunGeoTopo.ra + deltaRAST)
        val yT = 0.99664719 * sin(uT) + (elevation / 6378140.0) * sin(AstroMath.rad(latitude))
        val decSTopoT = atan2((sin(decST) - yT * sin(hpST)) * cos(AstroMath.rad(deltaRAST)), cos(decST) - xT * sin(hpST) * cos(AstroMath.rad(haST)))
        val epsT = AstroMath.rad(AstroDataUtils.calculateTrueObliquity(ijtimaTopoJde))
        val yST = sin(raSTopoT) * cos(epsT) + tan(decSTopoT) * sin(epsT)
        val xST = cos(raSTopoT)
        val topoLon = AstroMath.mod(AstroMath.deg(atan2(yST, xST)), 360.0)
        val dtTopo = DynamicalTimeEngine.deltaT(ijtimaTopoJde)
        val ijtimaTopoLocal = ijtimaTopoJde - dtTopo / 86400.0 + timezone / 24.0
        val dateComponentTopo = formatIjtimaDateOnly(ijtimaTopoLocal).replaceFirst(",", " $pName,")
        val ijtimaTopoStr = "$dateComponentTopo\nJam ${formatIjtimaTimeIndo(ijtimaTopoLocal, timezone)}\nBujur Ekliptika Toposentris: ${formatDms3AnglePrecise(topoLon, withSign = false)}"

        // --- MEEUS AA ED 2 SUNSET CALCULATION ---
        var ghurubSunJd = floor(ijtimaGeoJde + timezone / 24.0 + 0.5) - 0.5 - (timezone / 24.0) + (17.5 / 24.0) // Start search around 17:30 LT
        val cJDN = floor(ijtimaGeoJde + 0.5 + (timezone / 24.0))

        repeat(3) {
            val dtS = DynamicalTimeEngine.deltaT(ghurubSunJd)
            val jdeS = ghurubSunJd + dtS / 86400.0
            val sun = Vsop87SolarEngine.compute(jdeS)

            // Meeus constants
            val sdS = (959.63 / 3600.0) / sun.distanceAU // Semidiameter in degrees
            val rfS = 34.0 / 60.0 // Standard refraction at horizon (Meeus Ch. 15)
            val dipS = 0.0293 * sqrt(max(0.0, elevation))
            val altTargetS = 0.0 - sdS - rfS - dipS

            val cosHaS = (sin(AstroMath.rad(altTargetS)) - sin(AstroMath.rad(latitude)) * sin(AstroMath.rad(sun.dec))) /
                    (cos(AstroMath.rad(latitude)) * cos(AstroMath.rad(sun.dec)))
            val haS = AstroMath.deg(acos(cosHaS.coerceIn(-1.0, 1.0)))
            val kwd = longitude / 15.0 - timezone
            val hourLT = haS / 15.0 + 12.0 - (SolarFunctions.equationOfTime(jdeS) / 60.0) - kwd
            ghurubSunJd = cJDN - 0.5 + (hourLT - timezone) / 24.0
        }

        // Convert to hour for display or further use
        val ghurubSunLTOur = (ghurubSunJd + timezone / 24.0 - (floor(ghurubSunJd + timezone / 24.0 + 0.5) - 0.5)) * 24.0

        var ghurubMoonJd = ghurubSunJd
        repeat(5) {
            val dt = DynamicalTimeEngine.deltaT(ghurubMoonJd)
            val moonTopo = AstroMoonEngine.getTopocentricPosition(ghurubMoonJd, longitude, latitude, elevation)
            val mGeo = ElpMpp02LunarEngine.computeGeometric(ghurubMoonJd + dt / 86400.0)
            val sd = mGeo.semidiameter
            val dip = 1.76 / 60.0 * sqrt(max(0.0, elevation))
            val targetH = -(sd + 34.5 / 60.0 + dip)
            val cosHA = (sin(AstroMath.rad(targetH)) - sin(AstroMath.rad(latitude)) * sin(AstroMath.rad(moonTopo.second))) / (cos(AstroMath.rad(latitude)) * cos(AstroMath.rad(moonTopo.second)))
            val ha = if (cosHA < -1.0) 180.0 else if (cosHA > 1.0) 0.0 else AstroMath.deg(acos(cosHA.coerceIn(-1.0, 1.0)))
            val gast = AstroDataUtils.calculateGAST(ghurubMoonJd + dt / 86400.0)
            val currentHA = AstroMath.mod(gast + longitude - moonTopo.first + 180.0, 360.0) - 180.0
            val diffHA = ha - currentHA
            ghurubMoonJd += diffHA / 347.8
        }

        val dtSun = DynamicalTimeEngine.deltaT(ghurubSunJd)
        val jdeSun = ghurubSunJd + dtSun / 86400.0
        val sunGeoSunset = Vsop87SolarEngine.compute(jdeSun)
        val moonGeoSunset = ElpMpp02LunarEngine.computeGeometric(jdeSun)
        val moonTopoSunset = AstroMoonEngine.getTopocentricPosition(ghurubSunJd, longitude, latitude, elevation)

        val gastSunset = AstroDataUtils.calculateGAST(ghurubSunJd)

        // SUN TOPOCENTRIC ALTITUDE (ht)
        // Precise topocentric RA/Dec already calculated in the flow below (raSunTopo, decSunTopo)
        // We'll move the RA/Dec Topo calculation earlier to use it for altTopoSun
        val raS_r = AstroMath.rad(sunGeoSunset.ra); val decS_r = AstroMath.rad(sunGeoSunset.dec)
        val hpSunArcsec = Math.toDegrees(asin(6378.14 / (sunGeoSunset.distanceAU * 149597870.7))) * 3600.0
        val hpS_r = AstroMath.rad(hpSunArcsec / 3600.0)
        val gastS_r = AstroDataUtils.calculateGAST(ghurubSunJd)
        val haS_r = AstroMath.mod(gastS_r + longitude - sunGeoSunset.ra, 360.0)
        val uS_r = atan(0.99664719 * tan(AstroMath.rad(latitude)))
        val xS_r = cos(uS_r) + (elevation / 6378140.0) * cos(AstroMath.rad(latitude))
        val deltaRAS_r = AstroMath.deg(atan2(-xS_r * sin(hpS_r) * sin(AstroMath.rad(haS_r)), cos(decS_r) - xS_r * sin(hpS_r) * cos(AstroMath.rad(haS_r))))
        val raSunTopo = sunGeoSunset.ra + deltaRAS_r
        val yS_r = 0.99664719 * sin(uS_r) + (elevation / 6378140.0) * sin(AstroMath.rad(latitude))
        val decSunTopo = AstroMath.deg(atan2((sin(decS_r) - yS_r * sin(hpS_r)) * cos(AstroMath.rad(deltaRAS_r)), cos(decS_r) - xS_r * sin(hpS_r) * cos(AstroMath.rad(haS_r))))

        val haTopoSunSunset = AstroMath.mod(gastSunset + longitude - raSunTopo, 360.0)
        val sinAltTopoSun = sin(AstroMath.rad(latitude)) * sin(AstroMath.rad(decSunTopo)) +
                cos(AstroMath.rad(latitude)) * cos(AstroMath.rad(decSunTopo)) * cos(AstroMath.rad(haTopoSunSunset))
        val altTopoSun = AstroMath.deg(asin(sinAltTopoSun.coerceIn(-1.0, 1.0)))
        val altTopoMatahariStr = formatDms3AngleWithSign(altTopoSun)

        // MOON DATA
        val haGeoSunset = AstroMath.mod(gastSunset + longitude - moonGeoSunset.ra, 360.0)
        val sinAltGeo = sin(AstroMath.rad(latitude)) * sin(AstroMath.rad(moonGeoSunset.dec)) + cos(AstroMath.rad(latitude)) * cos(AstroMath.rad(moonGeoSunset.dec)) * cos(AstroMath.rad(haGeoSunset))
        val altHaqiqiGeo = AstroMath.deg(asin(sinAltGeo.coerceIn(-1.0, 1.0)))
        val altHaqiqiStr = formatDms3AngleWithSign(altHaqiqiGeo)
        val altGeoBulanStr = formatDms3AngleWithSign(altHaqiqiGeo)

        val haTopoSunset = AstroMath.mod(gastSunset + longitude - moonTopoSunset.first, 360.0)
        val sinAltTopo = sin(AstroMath.rad(latitude)) * sin(AstroMath.rad(moonTopoSunset.second)) + cos(AstroMath.rad(latitude)) * cos(AstroMath.rad(moonTopoSunset.second)) * cos(AstroMath.rad(haTopoSunset))
        val altTopoGeo = AstroMath.deg(asin(sinAltTopo.coerceIn(-1.0, 1.0)))

        // Refraksi & Dip (Meeus Standard)
        val refBulan = AstroTransform.atmosphericRefraction(altTopoGeo)
        val dip = AstroTransform.dipCorrection(elevation)
        val altMariTopo = altTopoGeo + refBulan + abs(dip)
        val altMariStr = formatDms3AngleWithSign(altMariTopo)

        val altTopoBulanTengahStr = formatDms3AngleWithSign(altTopoGeo)
        val sdDegMoon = moonGeoSunset.semidiameter
        val altTopoBulanAtasStr = formatDms3AngleWithSign(altTopoGeo + sdDegMoon)
        val altTopoBulanBawahStr = formatDms3AngleWithSign(altTopoGeo - sdDegMoon)

        val refUpper = AstroTransform.atmosphericRefraction(altTopoGeo + sdDegMoon)
        val refLower = AstroTransform.atmosphericRefraction(altTopoGeo - sdDegMoon)
        val altMariBulanTengahStr = formatDms3AngleWithSign(altTopoGeo + refBulan + abs(dip))
        val altMariBulanAtasStr = formatDms3AngleWithSign(altTopoGeo + sdDegMoon + refUpper + abs(dip))
        val altMariBulanBawahStr = formatDms3AngleWithSign(altTopoGeo - sdDegMoon + refLower + abs(dip))

        val elongCos = sin(AstroMath.rad(sunGeoSunset.dec)) * sin(AstroMath.rad(moonTopoSunset.second)) + cos(AstroMath.rad(sunGeoSunset.dec)) * cos(AstroMath.rad(moonTopoSunset.second)) * cos(AstroMath.rad(sunGeoSunset.ra - moonTopoSunset.first))
        val elong = AstroMath.deg(acos(elongCos.coerceIn(-1.0, 1.0)))
        val elongStr = formatDms3AngleWithSign(elong)

        val illum = LunarFunctions.moonIllumination(sunGeoSunset.ra, sunGeoSunset.dec, sunGeoSunset.distanceAU, moonGeoSunset.ra, moonGeoSunset.dec, moonGeoSunset.distanceAU * 149597870.7)
        val illumPct = illum.illuminatedFraction * 100.0
        val illumStr = String.format(Locale.US, "%.2f %%", illumPct).replace('.', ',')

        val sdDeg = moonGeoSunset.semidiameter
        val sdStr = formatDmsAngle(sdDeg)

        val ghurubDayIdx = ((floor(ghurubSunJd + timezone / 24.0 + 1.5).toLong()) % 7).toInt()
        val ghurubDayName = when(ghurubDayIdx) {
            0 -> "Ahad"; 1 -> "Senin"; 2 -> "Selasa"; 3 -> "Rabu"; 4 -> "Kamis"; 5 -> "Jum'at"; 6 -> "Sabtu"
            else -> ""
        }

        // Hitung parameter visibilitas dasar seawal mungkin agar bisa dipakai di summary
        val visMabimsBaruTmp = altMariTopo >= 3.0 && elong >= 6.4

        val saatPerhitunganStr = "${ghurubDayName}, ${formatIjtimaDateOnly(ghurubSunJd + timezone / 24.0)} M / JD ${String.format(Locale.US, "%.5f", ghurubSunJd).replace('.', ',')}"
        val moonTopoEcl = equatorialToEcliptic(moonTopoSunset.first, moonTopoSunset.second, AstroDataUtils.calculateTrueObliquity(jdeSun))
        val sunTopoEcl = equatorialToEcliptic(raSunTopo, decSunTopo, AstroDataUtils.calculateTrueObliquity(jdeSun))

        val bujurBulanStr = formatDms3AngleWithSign(moonTopoEcl.first)
        val bujurMatahariStr = formatDms3AngleWithSign(sunTopoEcl.first)
        val lintangBulanStr = formatDms3AngleWithSign(moonTopoEcl.second)
        val lintangMatahariStr = formatDms3AngleWithSign(sunTopoEcl.second)

        val raBulanStr = formatHmsAngleWithSign(moonTopoSunset.first)
        val raMatahariStr = formatHmsAngleWithSign(raSunTopo)
        val decBulanStr = formatDms3AngleWithSign(moonTopoSunset.second)
        val decMatahariStr = formatDms3AngleWithSign(decSunTopo)

        val haMoonTopoSunset = AstroMath.mod(gastSunset + longitude - moonTopoSunset.first, 360.0)
        val haSunTopoSunset = AstroMath.mod(gastSunset + longitude - raSunTopo, 360.0)
        val azBulanStr = formatDms3AngleWithSign(calculateAzimuth(haMoonTopoSunset, moonTopoSunset.second, latitude))
        val azMatahariStr = formatDms3AngleWithSign(calculateAzimuth(haSunTopoSunset, decSunTopo, latitude))

        val lebarSabitDeg = sdDeg * (1.0 - cos(AstroMath.rad(elong)))
        val lebarSabitStr = formatDms3AngleWithSign(lebarSabitDeg)

        val elongGeoCos = sin(AstroMath.rad(sunGeoSunset.dec)) * sin(AstroMath.rad(moonGeoSunset.dec)) + cos(AstroMath.rad(sunGeoSunset.dec)) * cos(AstroMath.rad(moonGeoSunset.dec)) * cos(AstroMath.rad(sunGeoSunset.ra - moonGeoSunset.ra))
        val elongGeo = AstroMath.deg(acos(elongGeoCos.coerceIn(-1.0, 1.0)))
        val elongasiGeoStr = formatDms3AngleWithSign(elongGeo)
        val elongasiTopoStr = formatDms3AngleWithSign(elong)

        val bestTimeJd = ghurubSunJd + (ghurubMoonJd - ghurubSunJd) * 4.0 / 9.0
        val bestTimeStr = formatGhurubTime(bestTimeJd, timezone)

        val dtMoonset = DynamicalTimeEngine.deltaT(ghurubMoonJd)
        val moonTopoMoonset = AstroMoonEngine.getTopocentricPosition(ghurubMoonJd, longitude, latitude, elevation)
        val gastMoonset = AstroDataUtils.calculateGAST(ghurubMoonJd + dtMoonset / 86400.0)
        val haMoonMoonset = AstroMath.mod(gastMoonset + longitude - moonTopoMoonset.first, 360.0)
        val arahTerbenamBulanStr = formatDms3AngleWithSign(calculateAzimuth(haMoonMoonset, moonTopoMoonset.second, latitude))

        val hpBulanStr = formatDms3AngleWithSign(moonGeoSunset.horizontalParallax)

        val rAltT = altMariTopo + abs(altTopoSun)
        val wOdeh = lebarSabitDeg * 60.0
        val qOdeh = rAltT - (-0.1018 * wOdeh*wOdeh*wOdeh + 0.7319 * wOdeh*wOdeh - 6.3226 * wOdeh + 7.1651)
        val qYallop = (rAltT - (11.8371 - 6.3226 * wOdeh + 0.7319 * wOdeh*wOdeh - 0.1018 * wOdeh*wOdeh*wOdeh)) / 10.0

        val rangeQOdehStr = String.format(Locale.US, "%.3f", qOdeh).replace('.', ',')
        val jarakBumiBulanStr = String.format(Locale.US, "%.2f km", moonGeoSunset.distanceAU * 149597870.7).replace('.', ',')

        val hiMonthNamesPersis = listOf(
            "Al-Muharram", "Shafar", "Rabi‘ul Awwal", "Rabi‘ul Akhir",
            "Jumadal Ula", "Jumadal Akhirah", "Rajab", "Sya‘ban",
            "Ramadhan", "Syawwal", "Dzul Qa‘dah", "Dzul Hijjah"
        )

        val visMabimsBaru  = altMariTopo >= 3.0 && elong >= 6.4
        val visMabimsLama  = altMariTopo >= 2.0 && elong >= 3.0 && (ghurubSunJd - (ijtimaGeoJde - dtGeo / 86400.0)) * 24.0 >= 8.0
        val visWujud       = altMariTopo > 0.0 && ijtimaGeoJde < ghurubSunJd

        // KGHT global tidak memakai markaz lokal saja.
        // Turki: altitude bulan TOPOSENTRIK >= 5° dan elongasi GEOSENTRIK >= 8° di salah satu titik garis ghurub global.
        // Muhammadiyah: altitude bulan GEOSENTRIK >= 5° dan elongasi GEOSENTRIK >= 8° di salah satu titik garis ghurub global.
        val needsKhgtGlobalScan = selectedKriteria == null ||
                selectedKriteria == KriteriaHilal.KGHT_TURKI ||
                selectedKriteria == KriteriaHilal.KGHT_MUHAMMADIYAH
        val khgtGlobalTarget = when (selectedKriteria) {
            KriteriaHilal.KGHT_TURKI -> KhgtGlobalTarget.TURKI
            KriteriaHilal.KGHT_MUHAMMADIYAH -> KhgtGlobalTarget.MUHAMMADIYAH
            else -> KhgtGlobalTarget.BOTH
        }
        val khgtScanResult = if (needsKhgtGlobalScan) {
            scanKhgtGlobalSunsetLine(ijtimaGeoJde, khgtGlobalTarget)
        } else {
            GlobalKhgtScanResult(
                turkiTopoAltitude = -999.0,
                turkiGeoElongation = -999.0,
                turkiLocation = "",
                muhammadiyahGeoAltitude = -999.0,
                muhammadiyahGeoElongation = -999.0,
                muhammadiyahLocation = ""
            )
        }
        val visKghtTurki = khgtScanResult.isTurkiVisible
        val visKghtMuhammadiyah = khgtScanResult.isMuhammadiyahVisible

        val visLapan       = altMariTopo >= 2.0 && elong >= 3.0
        val visYallop      = qYallop > -0.293
        val visOdeh        = qOdeh > 0.0
        val visDanjon      = elongGeo >= 7.0

        val statusStr = if (visMabimsBaru) "Visible (Mabims Baru)" else "Not Visible"
        val (yG, mG, dG) = CalendarFunctions.jdeToGregorian(ijtimaGeoLocal)
        val gregorianDt = LocalDateTime.of(yG, mG, dG.toInt(), 0, 0, 0)

        // ijtimaGeoLocal sudah include timezone — JANGAN tambah timezone lagi
        val dayIdxIjtima = ((floor(ijtimaGeoLocal + 1.5).toLong()) % 7).toInt()
        val dayInIndo = when(dayIdxIjtima) {
            0 -> "Ahad"; 1 -> "Senin"; 2 -> "Selasa"; 3 -> "Rabu"; 4 -> "Kamis"; 5 -> "Jum'at"; 6 -> "Sabtu"
            else -> ""
        }

        // Hitung hari Ijtima jatuh di hari ke-berapa dalam bulan Hijriah sebelumnya
        // Gunakan Kriteria Mabims Baru untuk menentukan awal bulan sebelumnya (agar Syawal 1447 = 28 hari saat Ijtima)
        val prevHijriMonth = if (hijriMonth == 1) 12 else hijriMonth - 1
        val prevHijriYear  = if (hijriMonth == 1) hijriYear - 1 else hijriYear
        val startPrevMonthJde = CalendarFunctions.getStartJdeOfHijriMonth(prevHijriYear, prevHijriMonth, latitude, longitude, elevation, timezone, "Mabims Baru")
        val dayOfIjtimaInPrevMonth = (floor(ijtimaGeoJde - dtGeo / 86400.0) - floor(startPrevMonthJde) + 1).toInt().coerceAtLeast(1)

        val monthGregName = when(mG) { 1->"Januari"; 2->"Februari"; 3->"Maret"; 4->"April"; 5->"Mei"; 6->"Juni"; 7->"Juli"; 8->"Agustus"; 9->"September"; 10->"Oktober"; 11->"November"; 12->"Desember"; else->"" }
        val targetMonthName = hiMonthNamesPersis.getOrElse((hijriMonth - 1).coerceIn(0, 11)) { "" }
        val targetYear = hijriYear
        val conclusion = "1 $targetMonthName $targetYear H = $dayInIndo, ${dG.toInt()} $monthGregName $yG M ($statusStr)"

        val conclMabimsBaru = "Mabims Baru: " + (if (visMabimsBaru) "Imkan" else "Tidak")
        val conclMabimsLama = "Mabims Lama: " + (if (visMabimsLama) "Imkan" else "Tidak")
        val conclWujud      = "Wujudul Hilal: " + (if (visWujud) "Wujud" else "Tidak")
        val conclTurki      = if (needsKhgtGlobalScan && khgtGlobalTarget != KhgtGlobalTarget.MUHAMMADIYAH) "KGHT Turki: " + (if (visKghtTurki) "Imkan" else "Tidak") else "KGHT Turki: Tidak dihitung"
        val conclMuham      = if (needsKhgtGlobalScan && khgtGlobalTarget != KhgtGlobalTarget.TURKI) "KGHT Muhammadiyah: " + (if (visKghtMuhammadiyah) "Imkan" else "Tidak") else "KGHT Muhammadiyah: Tidak dihitung"
        val conclLapan      = "LAPAN: " + (if (visLapan) "Imkan" else "Tidak")
        val conclYallop     = "Yallop: " + (if (visYallop) "Imkan" else "Tidak")
        val conclOdeh       = "Odeh: " + (if (visOdeh) "Imkan" else "Tidak")
        val conclDanjon     = "Danjon: " + (if (visDanjon) "Visible" else "Not")

        val fullSummary = """
        $conclusion
        $conclMabimsBaru | $conclMabimsLama
        $conclWujud | $conclTurki
        $conclMuham | $conclLapan
        $conclYallop | $conclOdeh | $conclDanjon
    """.trimIndent()

        return HilalResult(
            hijriYear = hijriYear,
            hijriMonth = hijriMonth,
            gregorianDate = gregorianDt,
            julianDay = ijtimaGeoJde,
            deltaT = dtGeo,
            algorithm = algorithm,
            geoLongitude = geoLon,
            topoLongitude = topoLon,
            ghurubSun = formatGhurubTime(ghurubSunJd, timezone),
            ghurubMoon = formatGhurubTime(ghurubMoonJd, timezone),
            locationName = "Lokasi: $latitude, $longitude",
            isVisibleMabimsBaru = visMabimsBaru,
            isVisibleMabimsLama = visMabimsLama,
            isVisibleWujudulHilal = visWujud,
            isVisibleKghtTurki = visKghtTurki,
            isVisibleKghtMuhammadiyah = visKghtMuhammadiyah,
            isVisibleLapan = visLapan,
            isVisibleYallop = visYallop,
            isVisibleOdeh = visOdeh,
            isVisibleDanjon = visDanjon,
            kghtTurkiLocation = khgtScanResult.turkiLocation,
            kghtMuhammadiyahLocation = khgtScanResult.muhammadiyahLocation,
            kghtTurkiAltitudeTopoStr = formatDms3AngleWithSign(khgtScanResult.turkiTopoAltitude),
            kghtTurkiElongationGeoStr = formatDms3AngleWithSign(khgtScanResult.turkiGeoElongation),
            kghtMuhammadiyahAltitudeGeoStr = formatDms3AngleWithSign(khgtScanResult.muhammadiyahGeoAltitude),
            kghtMuhammadiyahElongationGeoStr = formatDms3AngleWithSign(khgtScanResult.muhammadiyahGeoElongation),
            summary = fullSummary,
            elongation = elongStr,
            illumination = illumStr,
            semidiameter = sdStr,
            conclusion = conclusion,
            ijtimaGeoStr = ijtimaGeoStr,
            ijtimaTopoStr = ijtimaTopoStr,
            altHaqiqiStr = altHaqiqiStr,
            altMariStr = altMariStr,
            saatPerhitunganStr = saatPerhitunganStr,
            bujurBulanStr = bujurBulanStr,
            bujurMatahariStr = bujurMatahariStr,
            lintangBulanStr = lintangBulanStr,
            lintangMatahariStr = lintangMatahariStr,
            raBulanStr = raBulanStr,
            raMatahariStr = raMatahariStr,
            decBulanStr = decBulanStr,
            decMatahariStr = decMatahariStr,
            azBulanStr = azBulanStr,
            azMatahariStr = azMatahariStr,
            altGeoBulanStr = altGeoBulanStr,
            altTopoBulanAtasStr = altTopoBulanAtasStr,
            altTopoBulanTengahStr = altTopoBulanTengahStr,
            altTopoBulanBawahStr = altTopoBulanBawahStr,
            altMariBulanAtasStr = altMariBulanAtasStr,
            altMariBulanTengahStr = altMariBulanTengahStr,
            altMariBulanBawahStr = altMariBulanBawahStr,
            lebarSabitStr = lebarSabitStr,
            elongasiGeoStr = elongasiGeoStr,
            elongasiTopoStr = elongasiTopoStr,
            bestTimeStr = bestTimeStr,
            arahTerbenamBulanStr = arahTerbenamBulanStr,
            hpBulanStr = hpBulanStr,
            rangeQOdehStr = rangeQOdehStr,
            jarakBumiBulanStr = jarakBumiBulanStr,
            altGeoBulanTengahStr = altGeoBulanStr,
            altTopoMatahariStr = altTopoMatahariStr,
            conclusionImkanurRukyat = statusStr,
            conclusionWujudulHilal = if (visWujud) "Wujud" else "Belum Wujud",
            dayOfIjtimaInPrevMonth = dayOfIjtimaInPrevMonth
        )
    }
}
