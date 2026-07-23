package com.falak.falakpro.premium

import org.junit.Test
import java.io.File
import java.util.Locale
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class KanzulFalakLojiComparisonTest {
    @Test
    fun compareMuharram1448LojiWithKanzulFalakScreenshot() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }

        val lat = -(6.0 + 32.0 / 60.0 + 27.0 / 3600.0)
        val lon = 107.0 + 15.0 / 60.0 + 16.0 / 3600.0
        val elev = 3.0
        val tz = 7.0

        val res = HilalEngine.calculateHilalStart(
            hijriYear = 1448,
            hijriMonth = 1,
            latitude = lat,
            longitude = lon,
            elevation = elev,
            timezone = tz
        )

        println("=== FalakPro VSOP87D/ELP-MPP02 - Loji Muharram 1448 H ===")
        println("Lat/Lon/Elev/TZ      : $lat, $lon, $elev, $tz")
        println("DeltaT               : ${res.deltaT}")
        println("JD Ijtima Geo JDE    : ${res.julianDay}")
        println("Ijtima Geo           : ${res.ijtimaGeoStr.replace("\n", " | ")}")
        println("Ijtima Topo          : ${res.ijtimaTopoStr.replace("\n", " | ")}")
        println("Saat Perhitungan     : ${res.saatPerhitunganStr}")
        println("Sunset Sun           : ${res.ghurubSun}")
        println("Moonset              : ${res.ghurubMoon}")
        println("Moon Lon/Lat Topo    : ${res.bujurBulanStr} / ${res.lintangBulanStr}")
        println("Sun Lon/Lat Topo     : ${res.bujurMatahariStr} / ${res.lintangMatahariStr}")
        println("Moon RA/Dec Topo     : ${res.raBulanStr} / ${res.decBulanStr}")
        println("Sun RA/Dec Topo      : ${res.raMatahariStr} / ${res.decMatahariStr}")
        println("Altitude Geo Moon    : ${res.altGeoBulanStr}")
        println("Altitude Topo Sun    : ${res.altTopoMatahariStr}")
        println("Altitude Topo Moon U : ${res.altTopoBulanAtasStr}")
        println("Altitude Topo Moon C : ${res.altTopoBulanTengahStr}")
        println("Altitude Topo Moon L : ${res.altTopoBulanBawahStr}")
        println("Altitude Appar Moon U: ${res.altMariBulanAtasStr}")
        println("Altitude Appar Moon C: ${res.altMariBulanTengahStr}")
        println("Altitude Appar Moon L: ${res.altMariBulanBawahStr}")
        println("Az Sun/Moon          : ${res.azMatahariStr} / ${res.azBulanStr}")
        println("Elong Geo/Topo       : ${res.elongasiGeoStr} / ${res.elongasiTopoStr}")
        println("Best Time            : ${res.bestTimeStr}")
        println("Lag/Umur             : ${res.ghurubMoon} - ${res.ghurubSun}")
        println("Semidiameter Moon    : ${res.semidiameter}")
        println("Horizontal Parallax  : ${res.hpBulanStr}")
        println("Crescent Width       : ${res.lebarSabitStr}")
        println("Illumination         : ${res.illumination}")
        println("Range Q Odeh         : ${res.rangeQOdehStr}")
        println("Earth-Moon Distance  : ${res.jarakBumiBulanStr}")
        println("Conclusions          : ${res.summary.replace("\n", " | ")}")

        val sunsetUt = findMoonEventUt(
            guessUt = 2461206.94722,
            lat = lat,
            lon = lon,
            elev = elev,
            target = BodyEventTarget.SUN_UPPER_OBSERVED
        )
        println("--- Event variants around moonset ---")
        BodyEventTarget.entries.forEach { target ->
            val ut = findMoonEventUt(
                guessUt = 2461206.9578,
                lat = lat,
                lon = lon,
                elev = elev,
                target = target
            )
            println("${target.name.padEnd(22)}: ${formatLocalTimeFromJd(ut, tz)}")
        }
        println("Sunset helper         : ${formatLocalTimeFromJd(sunsetUt, tz)}")
        println("--- Topocentric conjunction variants ---")
        TopoConjunctionTarget.entries.forEach { target ->
            val jde = findTopoConjunctionJde(
                guessJde = res.julianDay - 1.3 / 24.0,
                lat = lat,
                lon = lon,
                elev = elev,
                target = target
            )
            println("${target.name.padEnd(30)}: ${formatLocalTimeFromJd(jde - DynamicalTimeEngine.deltaT(jde) / 86400.0, tz)} JDE=$jde")
        }
    }

    private enum class BodyEventTarget {
        MOON_UPPER_OBSERVED,
        MOON_CENTER_OBSERVED,
        MOON_UPPER_APPARENT,
        MOON_CENTER_APPARENT,
        MOON_UPPER_AIRLESS,
        MOON_CENTER_AIRLESS,
        SUN_UPPER_OBSERVED
    }

    private enum class TopoConjunctionTarget {
        MOON_TOPO_APP_SUN_TOPO_APP,
        MOON_TOPO_APP_SUN_GEO_APP,
        MOON_TOPO_TRUE_SUN_TOPO_APP,
        MOON_TOPO_TRUE_SUN_GEO_APP,
        MOON_GEO_APP_SUN_GEO_APP
    }

    private fun findTopoConjunctionJde(
        guessJde: Double,
        lat: Double,
        lon: Double,
        elev: Double,
        target: TopoConjunctionTarget
    ): Double {
        var lo = guessJde - 0.04
        var hi = guessJde + 0.04
        repeat(60) {
            val mid = (lo + hi) / 2.0
            if (topoConjunctionDiff(lo, lat, lon, elev, target) * topoConjunctionDiff(mid, lat, lon, elev, target) <= 0.0) {
                hi = mid
            } else {
                lo = mid
            }
        }
        return (lo + hi) / 2.0
    }

    private fun topoConjunctionDiff(
        jde: Double,
        lat: Double,
        lon: Double,
        elev: Double,
        target: TopoConjunctionTarget
    ): Double {
        val jdUt = jde - DynamicalTimeEngine.deltaT(jde) / 86400.0
        val eps = AstroDataUtils.calculateTrueObliquity(jde)
        val moonGeo = ElpMpp02LunarEngine.computeGeometric(
            jde,
            withAberration = target != TopoConjunctionTarget.MOON_TOPO_TRUE_SUN_TOPO_APP &&
                target != TopoConjunctionTarget.MOON_TOPO_TRUE_SUN_GEO_APP
        )
        val moonRaDec = when (target) {
            TopoConjunctionTarget.MOON_GEO_APP_SUN_GEO_APP -> moonGeo.ra to moonGeo.dec
            else -> {
                val moonTopo = AstroMoonEngine.getTopocentricPosition(jdUt, lon, lat, elev)
                moonTopo.first to moonTopo.second
            }
        }
        val moonLon = equatorialToEclipticLocal(moonRaDec.first, moonRaDec.second, eps).first

        val sunGeo = Vsop87SolarEngine.compute(jde)
        val sunRaDec = when (target) {
            TopoConjunctionTarget.MOON_TOPO_APP_SUN_TOPO_APP,
            TopoConjunctionTarget.MOON_TOPO_TRUE_SUN_TOPO_APP -> {
                topocentricEquatorial(
                    sunGeo.ra,
                    sunGeo.dec,
                    AstroMath.deg(kotlin.math.asin(6378.14 / (sunGeo.distanceAU * 149597870.7))),
                    jdUt,
                    lat,
                    lon,
                    elev
                )
            }
            else -> sunGeo.ra to sunGeo.dec
        }
        val sunLon = equatorialToEclipticLocal(sunRaDec.first, sunRaDec.second, eps).first
        return AstroMath.mod(moonLon - sunLon + 180.0, 360.0) - 180.0
    }

    private fun equatorialToEclipticLocal(ra: Double, dec: Double, epsDeg: Double): Pair<Double, Double> {
        val rRA = AstroMath.rad(ra)
        val rDec = AstroMath.rad(dec)
        val rEps = AstroMath.rad(epsDeg)
        val sinBeta = sin(rDec) * cos(rEps) - cos(rDec) * sin(rEps) * sin(rRA)
        val beta = AstroMath.deg(asin(sinBeta.coerceIn(-1.0, 1.0)))
        val y = sin(rRA) * cos(rEps) + tan(rDec) * sin(rEps)
        val x = cos(rRA)
        val lambda = AstroMath.mod(AstroMath.deg(kotlin.math.atan2(y, x)), 360.0)
        return lambda to beta
    }

    private fun findMoonEventUt(
        guessUt: Double,
        lat: Double,
        lon: Double,
        elev: Double,
        target: BodyEventTarget
    ): Double {
        var t = guessUt
        val dt = 20.0 / 86400.0
        repeat(12) {
            val f = eventAltitude(t, lat, lon, elev, target)
            val fp = eventAltitude(t + dt, lat, lon, elev, target)
            val fm = eventAltitude(t - dt, lat, lon, elev, target)
            val deriv = (fp - fm) / (2.0 * dt)
            if (abs(deriv) < 1e-10) return@repeat
            t -= (f / deriv).coerceIn(-0.06, 0.06)
        }
        return t
    }

    private fun eventAltitude(
        jdUt: Double,
        lat: Double,
        lon: Double,
        elev: Double,
        target: BodyEventTarget
    ): Double {
        val dt = DynamicalTimeEngine.deltaT(jdUt)
        val jde = jdUt + dt / 86400.0
        val gast = AstroDataUtils.calculateGAST(jdUt)
        val (ra, dec, sd) = if (target == BodyEventTarget.SUN_UPPER_OBSERVED) {
            val sun = Vsop87SolarEngine.compute(jde)
            val topo = topocentricEquatorial(
                sun.ra,
                sun.dec,
                AstroMath.deg(kotlin.math.asin(6378.14 / (sun.distanceAU * 149597870.7))),
                jdUt,
                lat,
                lon,
                elev
            )
            Triple(topo.first, topo.second, (959.63 / 3600.0) / sun.distanceAU)
        } else {
            val moonGeo = ElpMpp02LunarEngine.computeGeometric(jde)
            val moonTopo = AstroMoonEngine.getTopocentricPosition(jdUt, lon, lat, elev)
            Triple(moonTopo.first, moonTopo.second, moonGeo.semidiameter)
        }
        val ha = AstroMath.mod(gast + lon - ra, 360.0)
        val alt = AstroMath.deg(
            asin(
                sin(AstroMath.rad(lat)) * sin(AstroMath.rad(dec)) +
                    cos(AstroMath.rad(lat)) * cos(AstroMath.rad(dec)) * cos(AstroMath.rad(ha))
            )
        )
        val ref = when (target) {
            BodyEventTarget.MOON_UPPER_OBSERVED,
            BodyEventTarget.MOON_CENTER_OBSERVED,
            BodyEventTarget.SUN_UPPER_OBSERVED -> if (target == BodyEventTarget.SUN_UPPER_OBSERVED) 34.0 / 60.0 else 34.5 / 60.0
            BodyEventTarget.MOON_UPPER_APPARENT,
            BodyEventTarget.MOON_CENTER_APPARENT -> 34.5 / 60.0
            BodyEventTarget.MOON_UPPER_AIRLESS,
            BodyEventTarget.MOON_CENTER_AIRLESS -> 0.0
        }
        val dip = when (target) {
            BodyEventTarget.MOON_UPPER_OBSERVED,
            BodyEventTarget.MOON_CENTER_OBSERVED,
            BodyEventTarget.SUN_UPPER_OBSERVED -> 1.76 / 60.0 * sqrt(max(0.0, elev))
            else -> 0.0
        }
        val limb = when (target) {
            BodyEventTarget.MOON_UPPER_OBSERVED,
            BodyEventTarget.MOON_UPPER_APPARENT,
            BodyEventTarget.MOON_UPPER_AIRLESS,
            BodyEventTarget.SUN_UPPER_OBSERVED -> sd
            else -> 0.0
        }
        return alt + limb + ref + dip
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
        val u = kotlin.math.atan(0.99664719 * kotlin.math.tan(AstroMath.rad(lat)))
        val x = kotlin.math.cos(u) + (elevation / 6378140.0) * kotlin.math.cos(AstroMath.rad(lat))
        val y = 0.99664719 * kotlin.math.sin(u) + (elevation / 6378140.0) * kotlin.math.sin(AstroMath.rad(lat))
        val hpRad = AstroMath.rad(horizontalParallax)
        val haRad = AstroMath.rad(ha)
        val decRad = AstroMath.rad(dec)
        val deltaRa = AstroMath.deg(
            kotlin.math.atan2(
                -x * kotlin.math.sin(hpRad) * kotlin.math.sin(haRad),
                kotlin.math.cos(decRad) - x * kotlin.math.sin(hpRad) * kotlin.math.cos(haRad)
            )
        )
        val raTopo = AstroMath.mod(ra + deltaRa, 360.0)
        val decTopo = AstroMath.deg(
            kotlin.math.atan2(
                (kotlin.math.sin(decRad) - y * kotlin.math.sin(hpRad)) * kotlin.math.cos(AstroMath.rad(deltaRa)),
                kotlin.math.cos(decRad) - x * kotlin.math.sin(hpRad) * kotlin.math.cos(haRad)
            )
        )
        return raTopo to decTopo
    }

    private fun formatLocalTimeFromJd(jdUt: Double, tz: Double): String {
        val jdLocal = jdUt + tz / 24.0
        val jdMidnight = kotlin.math.floor(jdLocal + 0.5) - 0.5
        val totalSeconds = (jdLocal - jdMidnight) * 86400.0
        val h = kotlin.math.floor(totalSeconds / 3600.0).toInt()
        val m = kotlin.math.floor((totalSeconds - h * 3600.0) / 60.0).toInt()
        val s = totalSeconds - h * 3600.0 - m * 60.0
        return String.format(Locale.US, "%02d:%02d:%05.2f", h, m, s)
    }
}
