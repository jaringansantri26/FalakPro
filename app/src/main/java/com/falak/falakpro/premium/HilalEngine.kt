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
    private const val EARTH_RADIUS_KM = AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_KM
    private const val SUN_RADIUS_KM = 695700.0
    private const val AU_KM = 149597870.7

    private val khgtGlobalCache = mutableMapOf<String, GlobalKhgtScanResult>()

    private enum class HorizonBody {
        SUN,
        MOON
    }

    private data class TopocentricBodyPosition(
        val ra: Double,
        val dec: Double,
        val distanceKm: Double,
        val semidiameter: Double,
        val altitude: Double
    )

    private data class VisibilityParameters(
        val geoElongation: Double,
        val topoElongation: Double,
        val arcVYallop: Double,
        val arcVOdeh: Double,
        val crescentWidthYallopArcMin: Double,
        val crescentWidthOdehArcMin: Double
    )

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

    private fun azimuthFromRaDec(raDeg: Double, decDeg: Double, latDeg: Double, lonDeg: Double, gastDeg: Double): Double {
        val ha = AstroMath.mod(gastDeg + lonDeg - raDeg, 360.0)
        return calculateAzimuth(ha, decDeg, latDeg)
    }

    private fun angularDistanceAbs(a: Double, b: Double): Double {
        var d = abs(a - b) % 360.0
        if (d > 180.0) d = 360.0 - d
        return d
    }

    private fun odehArcV(arcL: Double, daz: Double): Double {
        val cosDaz = cos(AstroMath.rad(daz))
        if (abs(cosDaz) < 1e-12) return 90.0
        return AstroMath.deg(acos((cos(AstroMath.rad(arcL)) / cosDaz).coerceIn(-1.0, 1.0)))
    }

    private fun topocentricEquatorial(
        ra: Double,
        dec: Double,
        horizontalParallax: Double,
        jdUt: Double,
        lat: Double,
        lon: Double,
        elevation: Double
    ): Pair<Double, Double> {
        val gast = AstroDataUtils.calculateGAST(jdUt)
        val ha = AstroMath.mod(gast + lon - ra, 360.0)
        val u = atan(0.99664719 * tan(AstroMath.rad(lat)))
        val x = cos(u) + (elevation / AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_M) * cos(AstroMath.rad(lat))
        val y = 0.99664719 * sin(u) + (elevation / AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_M) * sin(AstroMath.rad(lat))
        val hpRad = AstroMath.rad(horizontalParallax)
        val haRad = AstroMath.rad(ha)
        val decRad = AstroMath.rad(dec)
        val deltaRa = AstroMath.deg(
            atan2(
                -x * sin(hpRad) * sin(haRad),
                cos(decRad) - x * sin(hpRad) * cos(haRad)
            )
        )
        val raTopo = AstroMath.mod(ra + deltaRa, 360.0)
        val decTopo = AstroMath.deg(
            atan2(
                (sin(decRad) - y * sin(hpRad)) * cos(AstroMath.rad(deltaRa)),
                cos(decRad) - x * sin(hpRad) * cos(haRad)
            )
        )
        return raTopo to decTopo
    }

    private fun topocentricBodyPosition(
        body: HorizonBody,
        jdUt: Double,
        lat: Double,
        lon: Double,
        elevation: Double
    ): TopocentricBodyPosition {
        val dt = DynamicalTimeEngine.deltaT(jdUt)
        val jde = jdUt + dt / 86400.0
        val gast = AstroDataUtils.calculateGAST(jdUt)
        return when (body) {
            HorizonBody.SUN -> {
                val sun = Vsop87SolarEngine.compute(jde)
                val distanceKm = sun.distanceAU * AU_KM
                val hp = AstroMath.deg(asin((EARTH_RADIUS_KM / distanceKm).coerceIn(-1.0, 1.0)))
                val (raTopo, decTopo) = topocentricEquatorial(sun.ra, sun.dec, hp, jdUt, lat, lon, elevation)
                val alt = altitudeFromRaDec(raTopo, decTopo, lat, lon, gast)
                val sd = AstroMath.deg(asin((SUN_RADIUS_KM / distanceKm).coerceIn(-1.0, 1.0)))
                TopocentricBodyPosition(raTopo, decTopo, distanceKm, sd, alt)
            }
            HorizonBody.MOON -> {
                val moonGeo = ElpMpp02LunarEngine.computeGeometric(jde)
                val moonTopo = AstroMoonEngine.getTopocentricPosition(jdUt, lon, lat, elevation)
                val distanceKm = moonGeo.distanceAU * AU_KM
                val alt = altitudeFromRaDec(moonTopo.first, moonTopo.second, lat, lon, gast)
                TopocentricBodyPosition(moonTopo.first, moonTopo.second, distanceKm, moonGeo.semidiameter, alt)
            }
        }
    }

    private fun apparentUpperLimbAltitude(
        body: HorizonBody,
        jdUt: Double,
        lat: Double,
        lon: Double,
        elevation: Double
    ): Double {
        val pos = topocentricBodyPosition(body, jdUt, lat, lon, elevation)
        val refraction = when (body) {
            HorizonBody.SUN -> AstroTransform.AA_HORIZON_REFRACTION_DEG
            HorizonBody.MOON -> AstroTransform.AA_MOON_HORIZON_REFRACTION_DEG
        }
        val dip = abs(AstroTransform.dipCorrection(elevation))
        return pos.altitude + pos.semidiameter + refraction + dip
    }

    private fun refineUpperLimbSet(
        body: HorizonBody,
        guessJdUt: Double,
        lat: Double,
        lon: Double,
        elevation: Double
    ): Double {
        var t = guessJdUt
        val dt = 20.0 / 86400.0
        repeat(10) {
            val f = apparentUpperLimbAltitude(body, t, lat, lon, elevation)
            val fp = apparentUpperLimbAltitude(body, t + dt, lat, lon, elevation)
            val fm = apparentUpperLimbAltitude(body, t - dt, lat, lon, elevation)
            val derivative = (fp - fm) / (2.0 * dt)
            if (!derivative.isFinite() || abs(derivative) < 1e-9) return@repeat
            val step = (f / derivative).coerceIn(-0.08, 0.08)
            t -= step
            if (abs(step) < 0.1 / 86400.0) return t
        }

        var lo = t - 0.12
        var hi = t + 0.12
        var fLo = apparentUpperLimbAltitude(body, lo, lat, lon, elevation)
        var fHi = apparentUpperLimbAltitude(body, hi, lat, lon, elevation)
        if (fLo * fHi > 0.0) return t

        repeat(32) {
            val mid = (lo + hi) / 2.0
            val fMid = apparentUpperLimbAltitude(body, mid, lat, lon, elevation)
            if (fLo * fMid <= 0.0) {
                hi = mid
                fHi = fMid
            } else {
                lo = mid
                fLo = fMid
            }
        }
        return (lo + hi) / 2.0
    }

    private fun visibilityParametersAt(
        jdUt: Double,
        lat: Double,
        lon: Double,
        elevation: Double
    ): VisibilityParameters {
        val dt = DynamicalTimeEngine.deltaT(jdUt)
        val jde = jdUt + dt / 86400.0
        val gast = AstroDataUtils.calculateGAST(jdUt)
        val sunGeo = Vsop87SolarEngine.compute(jde)
        val moonGeo = ElpMpp02LunarEngine.computeGeometric(jde)
        val sunTopo = topocentricBodyPosition(HorizonBody.SUN, jdUt, lat, lon, elevation)
        val moonTopo = topocentricBodyPosition(HorizonBody.MOON, jdUt, lat, lon, elevation)

        val geoArcL = geoElongationDeg(sunGeo.ra, sunGeo.dec, moonGeo.ra, moonGeo.dec)
        val topoArcL = geoElongationDeg(sunTopo.ra, sunTopo.dec, moonTopo.ra, moonTopo.dec)
        val sunGeoAlt = altitudeFromRaDec(sunGeo.ra, sunGeo.dec, lat, lon, gast)
        val moonGeoAlt = altitudeFromRaDec(moonGeo.ra, moonGeo.dec, lat, lon, gast)
        val sunAz = azimuthFromRaDec(sunTopo.ra, sunTopo.dec, lat, lon, gast)
        val moonAz = azimuthFromRaDec(moonTopo.ra, moonTopo.dec, lat, lon, gast)
        val daz = angularDistanceAbs(sunAz, moonAz)

        val lunarParallaxArcMin = moonGeo.horizontalParallax * 60.0
        val sdTopoArcMin = moonGeo.semidiameter * 60.0 *
                (1.0 + sin(AstroMath.rad(moonTopo.altitude)) * sin(AstroMath.rad(lunarParallaxArcMin / 60.0)))
        val widthYallop = sdTopoArcMin * (1.0 - cos(AstroMath.rad(geoArcL)))
        val widthOdeh = sdTopoArcMin * (1.0 - cos(AstroMath.rad(topoArcL)))

        return VisibilityParameters(
            geoElongation = geoArcL,
            topoElongation = topoArcL,
            arcVYallop = moonGeoAlt - sunGeoAlt,
            arcVOdeh = odehArcV(topoArcL, daz),
            crescentWidthYallopArcMin = widthYallop,
            crescentWidthOdehArcMin = widthOdeh
        )
    }

    private data class KhgtPoint(
        val name: String,
        val lat: Double,
        val lon: Double
    )

    private val khgtCoastalCoordinates = listOf(
        // Benua Amerika (garis pantai barat)
        66.0 to -167.0,
        65.6 to -168.0,
        65.0 to -166.7,
        64.0 to -160.9,
        63.0 to -164.7,
        62.0 to -165.7,
        61.0 to -165.15,
        60.0 to -164.25,
        59.0 to -161.8,
        58.0 to -157.65,
        57.0 to -158.6,
        56.0 to -161.34,
        55.0 to -163.135,
        54.47 to -164.91,
        55.1455 to -161.9115,
        55.8667 to -159.0917,
        56.91 to -156.8761,
        57.98 to -155.0634,
        58.9095 to -153.3422,
        59.2379 to -151.7492,
        60.0 to -149.17,
        60.1734 to -148.0171,
        60.8405 to -147.5411,
        60.2734 to -146.5899,
        60.2235 to -144.9226,
        60.025 to -144.2634,
        59.9793 to -143.8926,
        60.06 to -143.5,
        60.08 to -143.0,
        60.1 to -142.7711,
        60.0 to -142.0,
        59.9518 to -141.7253,
        59.9564 to -141.5422,
        60.0228 to -141.3088,
        59.8875 to -141.4553,
        59.7 to -140.2559,
        59.83 to -139.7844,
        60.0 to -139.6304,
        59.5482 to -139.85,
        59.0339 to -138.312,
        57.943 to -136.5359,
        56.1892 to -134.659,
        55.9182 to -133.8442,
        54.7096 to -132.7822,
        54.81 to -130.9054,
        54.0 to -130.53,
        53.0 to -129.52,
        52.0 to -128.26,
        51.0 to -128.26,
        50.0 to -127.45,
        49.0 to -125.67,
        48.0 to -124.72,
        47.0 to -124.21,
        46.0 to -123.95,
        45.0 to -124.0,
        44.0 to -124.2,
        43.0 to -124.39,
        42.0 to -124.26,
        41.0 to -124.36,
        40.0 to -124.09,
        39.0 to -123.76,
        38.0 to -122.98,
        37.0 to -122.68,
        36.0 to -121.5409,
        35.0 to -120.75,
        34.0 to -118.92,
        33.0 to -117.33,
        32.0 to -116.87,
        31.0 to -116.29,
        30.0 to -115.81,
        29.0 to -114.67,
        28.0 to -114.05,
        27.845 to -115.08,
        27.0 to -114.05,
        26.0 to -112.18,
        25.0 to -112.18,
        24.0 to -110.83,
        23.0 to -110.0,
        23.414 to -109.443,
        24.0 to -110.05,
        25.0 to -110.725,
        26.0 to -111.3842,
        27.0 to -111.9518,
        28.0 to -112.8124,
        29.0 to -113.673,
        30.0 to -114.5153,
        31.6041 to -114.8632,
        31.1665 to -113.1054,
        30.0 to -112.6476,
        29.0 to -112.3729,
        28.0 to -111.256,
        27.0 to -109.956,
        26.0 to -109.48,
        25.0 to -108.125,
        24.0 to -106.9531,
        23.03 to -106.27,
        22.0 to -105.61,
        21.0 to -105.74,
        20.38 to -105.65,
        20.0 to -105.5555,
        19.0 to -104.347,
        18.0 to -102.88,
        17.0 to -100.21,
        16.0 to -97.87,
        15.68 to -96.51,
        16.19 to -94.81,
        15.5 to -93.25,
        15.0 to -92.747,
        14.0 to -91.557,
        13.0 to -87.62,
        12.0 to -86.655,
        11.0 to -85.9,
        10.0 to -85.76,
        9.0 to -83.65,
        8.0 to -82.947,
        8.335 to -82.783,
        8.27 to -82.19,
        8.1 to -81.74,
        7.764 to -81.61,
        7.6 to -81.0,
        7.25 to -80.9333,
        7.47 to -80.0,
        8.136 to -80.5,
        8.64 to -79.76,
        9.014 to -79.322,
        8.7925 to -78.85,
        8.358 to -78.402,
        7.0 to -77.692,
        6.0 to -77.3764,
        5.0 to -77.3764,
        4.27 to -77.51,
        4.0 to -77.427,
        3.82 to -77.143,
        3.0 to -77.674,
        2.6656 to -77.9212,
        2.6428 to -78.37,
        2.181 to -78.7131,
        2.0 to -78.7,
        1.7737 to -78.59,
        1.7874 to -78.8733,
        1.6547 to -79.0,
        1.4534 to -78.8779,
        1.078 to -79.1663,
        1.0 to -79.5691,
        0.817 to -80.45,
        0.0 to -80.1,
        -0.3687 to -80.5,
        -0.9 to -80.59,
        -1.0 to -80.9,
        -2.0 to -80.75,
        -2.195 to -80.99,
        -2.7343 to -80.2878,
        -3.0 to -79.83,
        -4.0 to -80.99,
        -4.2 to -81.2125,
        -4.65 to -81.33,
        -5.0 to -81.07,
        -5.1955 to -81.21,
        -5.7925 to -80.9058,
        -6.0 to -81.135,
        -7.0 to -79.83,
        -8.0 to -79.235,
        -9.0 to -78.66,
        -10.0 to -78.2,
        -11.0 to -77.67,
        -12.0 to -77.15,
        -13.0 to -76.52,
        -14.0 to -76.2822,
        -15.0 to -75.5177,
        -16.0 to -74.0254,
        -17.0 to -72.0662,
        -17.69 to -71.3841,
        -18.0 to -70.931,
        -19.0 to -70.313,
        -20.0 to -70.1252,
        -21.0 to -70.1481,
        -22.0 to -70.1756,
        -23.0 to -70.3541,
        -23.059 to -70.5693,
        -23.53 to -70.6379,
        -23.4753 to -70.5189,
        -24.0 to -70.5235,
        -25.0 to -70.5235,
        -25.5135 to -70.6379,
        -25.7817 to -70.7524,
        -26.0 to -70.6425,
        -27.0 to -70.8027,
        -27.11 to -70.9446,
        -28.0 to -71.1552,
        -29.0 to -71.4985,
        -29.2559 to -71.4802,
        -29.3238 to -71.3383,
        -30.0 to -71.4207,
        -30.2336 to -71.6358,
        -31.0 to -71.6587,
        -32.0 to -71.5397,
        -33.0 to -71.5397,
        -33.4175 to -71.677,
        -33.7835 to -71.8052,
        -34.0 to -71.8968,
        -35.0 to -72.2035,
        -36.0 to -72.7803,
        -37.0 to -73.1831,
        -37.2336 to -73.3433,
        -37.1534 to -73.5722,
        -37.7226 to -73.6528,
        -38.0 to -73.4605,
        -38.4316 to -73.5612,
        -39.0 to -73.3369,
        -39.3832 to -73.2087,
        -40.0 to -73.5383,
        -40.0 to -73.726,
        -41.0 to -73.923,
        -42.0 to -74.0327,
        -43.0 to -74.2616,
        -43.2606 to -74.3852,
        -43.3771 to -73.7168,
        -41.8 to -73.5017,
        -41.8546 to -73.0348,
        -42.1573 to -72.6136,
        -42.2929 to -72.8606,
        -43.0 to -72.8105,
        -44.0 to -73.2683,
        -45.0 to -73.3324,
        -46.0 to -73.4709,
        -47.0 to -74.1118,
        -48.0 to -75.4283,
        -49.0 to -75.6755,
        -50.0 to -75.4159,
        -51.0 to -75.036,
        -51.6321 to -75.2878,
        -52.0 to -75.036,
        -53.0 to -74.4913,
        -54.0 to -73.42,
        -55.0 to -71.0626,
        -55.6916 to -68.0659,
        -55.9385 to -67.2877,
        // Indonesia
        5.907311 to 95.216947, // Sabang
        -5.374663 to 102.231905, // Enggano
        4.795872 to 108.021239, // Natuna
        -7.07392 to 106.531587, // Cibeas
        -8.849911 to 115.161592, // Pecatu
        4.152672 to 117.700964, // Sebatik
        5.565697 to 126.589592, // Miangas
        -11.007544 to 122.874742, // Rote Ndao
        -8.493832 to 140.40062, // Merauke
        -2.533 to 140.717 // Jayapura
    )

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

    private fun approximateLocalSunsetUt(jdUtAnchor: Double, lat: Double, lon: Double): Double {
        // Estimate sunset to be at 18:00 local time.
        // JD local noon = floor(jdUtAnchor + lon/360 + 0.5)
        // 18:00 local is 0.25 days after noon local.
        // JD UT = JD local - lon/360
        return floor(jdUtAnchor + lon / 360.0 + 0.5) + 0.25 - lon / 360.0
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

    /**
     * Evaluasi KHGT global kandidat dengan titik pesisir yang dioptimasi:
     * - Turki memakai tinggi toposentrik dan elongasi geosentrik.
     * - Muhammadiyah memakai tinggi dan elongasi geosentrik.
     */
    private fun scanKhgtGlobalSunsetLine(
        ijtimaGeoJde: Double,
        target: KhgtGlobalTarget = KhgtGlobalTarget.BOTH
    ): GlobalKhgtScanResult {
        val cacheKey = String.format(Locale.US, "KHGT_COASTAL:%s:%.5f", target.name, ijtimaGeoJde)
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
        
        // Aturan Selandia Baru: Jika Imkan terjadi SETELAH 00:00 UT, Ijtima harus sudah terjadi sebelum Fajar di NZ.
        val allowAmericaCorrection = isIjtimaBeforeFajrNewZealand(jdUtIjtima)

        val scanTurki = target == KhgtGlobalTarget.TURKI || target == KhgtGlobalTarget.BOTH
        val scanMuhammadiyah = target == KhgtGlobalTarget.MUHAMMADIYAH || target == KhgtGlobalTarget.BOTH

        for ((lat, lon) in khgtCoastalCoordinates) {
            val sunsetUt = findFirstSunsetAfter(jdUtIjtima, lat, lon) ?: continue
            val beforeMidnightUtc = sunsetUt <= utcMidnightLimit
            val isAmerica = lon < -30.0 // Hanya benua Amerika yang bernilai bujur barat besar
            val americaCorrection = allowAmericaCorrection && isAmerica && sunsetUt <= americaCorrectionLimit
            
            if (!beforeMidnightUtc && !americaCorrection) continue

            val jde = sunsetUt + DynamicalTimeEngine.deltaT(sunsetUt) / 86400.0
            val gast = AstroDataUtils.calculateGAST(sunsetUt)

            val sun = Vsop87SolarEngine.compute(jde)
            val moonGeo = ElpMpp02LunarEngine.computeGeometric(jde)
            val elongGeo = geoElongationDeg(sun.ra, sun.dec, moonGeo.ra, moonGeo.dec)

            val geoAltMoon = altitudeFromRaDec(moonGeo.ra, moonGeo.dec, lat, lon, gast)
            val locText = String.format(Locale.US, "%.2f°, %.2f°", lat, lon)

            if (scanTurki && (beforeMidnightUtc || americaCorrection)) {
                val moonTopo = AstroMoonEngine.getTopocentricPosition(sunsetUt, lon, lat, 0.0)
                val topoAltMoon = altitudeFromRaDec(moonTopo.first, moonTopo.second, lat, lon, gast)
                val turkiScore = min(topoAltMoon - 5.0, elongGeo - 8.0)
                if (turkiScore > bestTurkiScore) {
                    bestTurkiScore = turkiScore
                    bestTurkiTopoAlt = topoAltMoon
                    bestTurkiElongGeo = elongGeo
                    bestTurkiLoc = locText
                }
            }

            if (scanMuhammadiyah && (beforeMidnightUtc || americaCorrection)) {
                val muhammadiyahScore = min(geoAltMoon - 5.0, elongGeo - 8.0)
                if (muhammadiyahScore > bestMuhammadiyahScore) {
                    bestMuhammadiyahScore = muhammadiyahScore
                    bestMuhammadiyahGeoAlt = geoAltMoon
                    bestMuhammadiyahElongGeo = elongGeo
                    bestMuhammadiyahLoc = locText
                }
            }
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

            // Wujudul Hilal: cukup ijtima sebelum ghurub
            "Wujudul Hilal" ->
                beforeGhurub

            // KGHT Muhammadiyah (Global Scan)
            "KGHT Muhammadiyah" ->
                checkKhgtGlobalMuhammadiyah(ijtimaGeoJde)

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

            // Istanbul / Turki Global (2016): tinggi toposentris ≥ 5° & elongasi ≥ 8° (Global Scan)
            "Turki Global", "KGHT Turki" ->
                checkKhgtGlobalTurki(ijtimaGeoJde)

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
            val sunTopoLon = sunGeo.longitudeEcliptic

            return AstroMath.mod(moonTopoLon - sunTopoLon + 180.0, 360.0) - 180.0
        }
        repeat(40) {
            val mid = (loTopo + hiTopo) / 2.0
            if (diffTopo(loTopo) * diffTopo(mid) <= 0) hiTopo = mid else loTopo = mid
        }
        val ijtimaTopoJde = (loTopo + hiTopo) / 2.0

        val jdUtTopo = ijtimaTopoJde - DynamicalTimeEngine.deltaT(ijtimaTopoJde) / 86400.0
        val epsT = AstroMath.rad(AstroDataUtils.calculateTrueObliquity(ijtimaTopoJde))
        val moonTopoAtIjtima = AstroMoonEngine.getTopocentricPosition(jdUtTopo, longitude, latitude, elevation)
        val yMT = sin(AstroMath.rad(moonTopoAtIjtima.first)) * cos(epsT) + tan(AstroMath.rad(moonTopoAtIjtima.second)) * sin(epsT)
        val xMT = cos(AstroMath.rad(moonTopoAtIjtima.first))
        val topoLon = AstroMath.mod(AstroMath.deg(atan2(yMT, xMT)), 360.0)
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
            val rfS = AstroTransform.AA_HORIZON_REFRACTION_DEG
            val dipS = abs(AstroTransform.dipCorrection(elevation))
            val altTargetS = 0.0 - sdS - rfS - dipS

            val cosHaS = (sin(AstroMath.rad(altTargetS)) - sin(AstroMath.rad(latitude)) * sin(AstroMath.rad(sun.dec))) /
                    (cos(AstroMath.rad(latitude)) * cos(AstroMath.rad(sun.dec)))
            val haS = AstroMath.deg(acos(cosHaS.coerceIn(-1.0, 1.0)))
            val kwd = longitude / 15.0 - timezone
            val hourLT = haS / 15.0 + 12.0 - (SolarFunctions.equationOfTime(jdeS) / 60.0) - kwd
            ghurubSunJd = cJDN - 0.5 + (hourLT - timezone) / 24.0
        }
        ghurubSunJd = refineUpperLimbSet(HorizonBody.SUN, ghurubSunJd, latitude, longitude, elevation)

        // Convert to hour for display or further use
        val ghurubSunLTOur = (ghurubSunJd + timezone / 24.0 - (floor(ghurubSunJd + timezone / 24.0 + 0.5) - 0.5)) * 24.0

        var ghurubMoonJd = ghurubSunJd
        repeat(5) {
            val dt = DynamicalTimeEngine.deltaT(ghurubMoonJd)
            val moonTopo = AstroMoonEngine.getTopocentricPosition(ghurubMoonJd, longitude, latitude, elevation)
            val mGeo = ElpMpp02LunarEngine.computeGeometric(ghurubMoonJd + dt / 86400.0)
            val sd = mGeo.semidiameter
            val dip = abs(AstroTransform.dipCorrection(elevation))
            val targetH = -(sd + AstroTransform.AA_MOON_HORIZON_REFRACTION_DEG + dip)
            val cosHA = (sin(AstroMath.rad(targetH)) - sin(AstroMath.rad(latitude)) * sin(AstroMath.rad(moonTopo.second))) / (cos(AstroMath.rad(latitude)) * cos(AstroMath.rad(moonTopo.second)))
            val ha = if (cosHA < -1.0) 180.0 else if (cosHA > 1.0) 0.0 else AstroMath.deg(acos(cosHA.coerceIn(-1.0, 1.0)))
            val gast = AstroDataUtils.calculateGAST(ghurubMoonJd)
            val currentHA = AstroMath.mod(gast + longitude - moonTopo.first + 180.0, 360.0) - 180.0
            val diffHA = ha - currentHA
            ghurubMoonJd += diffHA / 347.8
        }
        ghurubMoonJd = refineUpperLimbSet(HorizonBody.MOON, ghurubMoonJd, latitude, longitude, elevation)

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
        val hpSunArcsec = AstroTransform.solarParallax(sunGeoSunset.distanceAU) * 3600.0
        val hpS_r = AstroMath.rad(hpSunArcsec / 3600.0)
        val gastS_r = AstroDataUtils.calculateGAST(ghurubSunJd)
        val haS_r = AstroMath.mod(gastS_r + longitude - sunGeoSunset.ra, 360.0)
        val uS_r = atan(0.99664719 * tan(AstroMath.rad(latitude)))
        val xS_r = cos(uS_r) + (elevation / AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_M) * cos(AstroMath.rad(latitude))
        val deltaRAS_r = AstroMath.deg(atan2(-xS_r * sin(hpS_r) * sin(AstroMath.rad(haS_r)), cos(decS_r) - xS_r * sin(hpS_r) * cos(AstroMath.rad(haS_r))))
        val raSunTopo = sunGeoSunset.ra + deltaRAS_r
        val yS_r = 0.99664719 * sin(uS_r) + (elevation / AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_M) * sin(AstroMath.rad(latitude))
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

        val elongCos = sin(AstroMath.rad(decSunTopo)) * sin(AstroMath.rad(moonTopoSunset.second)) + cos(AstroMath.rad(decSunTopo)) * cos(AstroMath.rad(moonTopoSunset.second)) * cos(AstroMath.rad(raSunTopo - moonTopoSunset.first))
        val elong = AstroMath.deg(acos(elongCos.coerceIn(-1.0, 1.0)))
        val elongStr = formatDms3AngleWithSign(elong)

        val illum = LunarFunctions.moonIllumination(sunGeoSunset.ra, sunGeoSunset.dec, sunGeoSunset.distanceAU, moonGeoSunset.ra, moonGeoSunset.dec, moonGeoSunset.distanceAU * 149597870.7)
        val illumPct = illum.illuminatedFraction * 100.0
        val illumStr = String.format(Locale.US, "%.2f %%", illumPct).replace('.', ',')

        val sdDeg = moonGeoSunset.semidiameter
        val sdStr = formatDmsAngle(sdDeg)

        // Hitung parameter visibilitas dasar seawal mungkin agar bisa dipakai di summary
        val visMabimsBaruTmp = altMariTopo >= 3.0 && elong >= 6.4

        val saatPerhitunganStr = "${formatIjtimaDateOnly(ghurubSunJd + timezone / 24.0)} / JD ${String.format(Locale.US, "%.5f", ghurubSunJd).replace('.', ',')}"
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
        val gastMoonset = AstroDataUtils.calculateGAST(ghurubMoonJd)
        val haMoonMoonset = AstroMath.mod(gastMoonset + longitude - moonTopoMoonset.first, 360.0)
        val arahTerbenamBulanStr = formatDms3AngleWithSign(calculateAzimuth(haMoonMoonset, moonTopoMoonset.second, latitude))

        val hpBulanStr = formatDms3AngleWithSign(moonGeoSunset.horizontalParallax)

        val bestTimeParams = visibilityParametersAt(bestTimeJd, latitude, longitude, elevation)
        val wYallop = bestTimeParams.crescentWidthYallopArcMin.coerceAtLeast(0.0)
        val wOdeh = bestTimeParams.crescentWidthOdehArcMin.coerceAtLeast(0.0)
        val qYallop = (
                bestTimeParams.arcVYallop -
                        (11.8371 - 6.3226 * wYallop + 0.7319 * wYallop * wYallop - 0.1018 * wYallop * wYallop * wYallop)
                ) / 10.0
        val qOdeh = bestTimeParams.arcVOdeh -
                (7.1651 - 6.3226 * wOdeh + 0.7319 * wOdeh * wOdeh - 0.1018 * wOdeh * wOdeh * wOdeh)

        val rangeQOdehStr = String.format(Locale.US, "%.3f", qOdeh).replace('.', ',')
        val jarakBumiBulanStr = String.format(Locale.US, "%.2f km", moonGeoSunset.distanceAU * 149597870.7).replace('.', ',')

        val hiMonthNamesPersis = listOf(
            "Al-Muharram", "Shafar", "Rabi‘ul Awwal", "Rabi‘ul Akhir",
            "Jumadal Ula", "Jumadal Akhirah", "Rajab", "Sya‘ban",
            "Ramadhan", "Syawwal", "Dzul Qa‘dah", "Dzul Hijjah"
        )

        val visMabimsBaru  = altMariTopo >= 3.0 && elongGeo >= 6.4
        val visMabimsLama  = altMariTopo >= 2.0 && elongGeo >= 3.0 && (ghurubSunJd - (ijtimaGeoJde - dtGeo / 86400.0)) * 24.0 >= 8.0
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
