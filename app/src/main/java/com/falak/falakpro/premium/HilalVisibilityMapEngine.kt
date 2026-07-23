package com.falak.falakpro.premium

import java.util.Locale
import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.tan

enum class HilalVisibilityMapMode(val label: String) {
    YALLOP("Yallop"),
    ODEH("Odeh"),
    MABIMS_BARU("MABIMS Baru"),
    MABIMS_LAMA("MABIMS Lama"),
    WUJUDUL_HILAL("Wujudul Hilal"),
    LAPAN("LAPAN"),
    DANJON("Danjon"),
    KGHT_TURKI("KGHT Turki"),
    KGHT_MUHAMMADIYAH("KGHT Muhammadiyah")
}

enum class HilalVisibilityZone(val label: String) {
    EASY_NAKED_EYE("Mudah terlihat dengan mata telanjang"),
    POSSIBLE_NAKED_EYE("Terlihat dengan mata telanjang dalam kondisi sempurna"),
    OPTICAL_AID_TO_FIND("Mungkin perlu alat optik untuk menemukan hilal"),
    OPTICAL_AID("Perlu bantuan alat optik"),
    TELESCOPE_ONLY("Tidak terlihat dengan teleskop konvensional"),
    NOT_VISIBLE("Tidak terlihat"),
    BEFORE_CONJUNCTION("Bulan masih sebelum ijtimak"),
    MOON_SET_BEFORE_SUN("Bulan tenggelam sebelum Matahari"),
    NO_EVENT("Tidak ada terbit/terbenam"),
    GLOBAL_ACCEPTED("Diterima secara global (KGHT)")
}

data class HilalVisibilityPoint(
    val latitude: Double,
    val longitude: Double,
    val zone: HilalVisibilityZone,
    val score: Double,
    val arcV: Double,
    val arcL: Double,
    val daz: Double,
    val ageHours: Double,
    val crescentWidthArcMin: Double,
    val moonAltTopo: Double = 0.0,
    val moonAltGeo: Double = 0.0,
    val moonLagHours: Double = Double.NaN,
    val sunsetAgeHours: Double = Double.NaN
)

data class HilalVisibilityMapResult(
    val mode: HilalVisibilityMapMode,
    val points: List<HilalVisibilityPoint>,
    val bestPoint: HilalVisibilityPoint?,
    val latMin: Double,
    val latMax: Double,
    val lonMin: Double,
    val lonMax: Double,
    val latStep: Double,
    val lonStep: Double,
    val dayOffset: Int,
    val baseDateJdUt: Double
) {
    val summaryText: String
        get() {
            val p = bestPoint ?: return "Belum ada titik peta yang dapat dievaluasi."
            return String.format(
                Locale.US,
                "%s terbaik di %.1f, %.1f | ARCV %.2f deg, ARCL %.2f deg, DAZ %.2f deg, umur %.1f jam.",
                p.zone.label,
                p.latitude,
                p.longitude,
                p.arcV,
                p.arcL,
                p.daz,
                p.ageHours
            )
    }
}

class HilalVisibilityRasterMapResult(
    val cacheKey: String,
    val mode: HilalVisibilityMapMode,
    val scores: FloatArray,
    val grayMargins: FloatArray,
    val noEventMasks: ByteArray,
    val bestPoint: HilalVisibilityPoint?,
    val latMin: Double,
    val latMax: Double,
    val lonMin: Double,
    val lonMax: Double,
    val scanLatMin: Double,
    val scanLatMax: Double,
    val latStep: Double,
    val lonStep: Double,
    val latCount: Int,
    val lonCount: Int,
    val dayOffset: Int,
    val baseDateJdUt: Double
)

data class VisibilityMapRequest(
    val hijriYear: Int,
    val hijriMonth: Int,
    val ijtimaGeoJde: Double,
    val ijtimaLocalJd: Double,
    val timezone: Double,
    val mode: HilalVisibilityMapMode,
    val magribLocalTimeText: String = "",
    val locationName: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val elevation: Double = 0.0
)

object HilalVisibilityMapEngine {
    private const val KM_PER_AU = 149597870.7
    private const val EARTH_RADIUS_KM = AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_KM
    private const val MOON_RADIUS_KM = 1737.4
    private const val SUN_RADIUS_KM = 695700.0
    private const val SYNODIC_MONTH_DAYS = 29.530588861
    private const val REFRACTION_NEAR_HORIZON_DEG = AstroTransform.AA_HORIZON_REFRACTION_DEG
    private val cache = mutableMapOf<String, HilalVisibilityMapResult>()
    private val rasterCache = mutableMapOf<String, HilalVisibilityRasterMapResult>()

    private data class NewMoonWindow(
        val previousUt: Double,
        val currentUt: Double,
        val nextUt: Double
    )

    private data class EphData(
        val jdUt: Double,
        val moonRa: Double,
        val moonDec: Double,
        val moonDistanceKm: Double,
        val sunRa: Double,
        val sunDec: Double,
        val sunDistanceKm: Double
    )

    private data class HorizontalPosition(
        val altitude: Double,
        val azimuth: Double
    )

    private enum class Body { SUN, MOON }

    fun buildMap(
        ijtimaGeoJde: Double,
        mode: HilalVisibilityMapMode,
        latStep: Double = 3.0,
        lonStep: Double = 3.0,
        dayOffset: Int = 0,
        baseDateJdUtOverride: Double? = null
    ): HilalVisibilityMapResult {
        val latMin = -90.0
        val latMax = 90.0
        val lonMin = -180.0
        val lonMax = 180.0
        val scanLatMin = -90.0
        val scanLatMax = 90.0

        val dtIjtima = DynamicalTimeEngine.deltaT(ijtimaGeoJde)
        val jdUtIjtima = ijtimaGeoJde - dtIjtima / 86400.0
        val defaultBaseDateJdUt = floor(jdUtIjtima + 0.5) - 0.5
        val baseDateJdUt = (baseDateJdUtOverride ?: defaultBaseDateJdUt) + dayOffset
        val newMoonWindow = buildNewMoonWindow(jdUtIjtima)
        val cacheKey = String.format(Locale.US, "%s:%.5f:%.5f:%.2f:%.2f:%d", mode.name, ijtimaGeoJde, baseDateJdUt, latStep, lonStep, dayOffset)
        cache[cacheKey]?.let { return it }

        val ephTable = buildEphTable(baseDateJdUt)
        val latCount = floor((scanLatMax - scanLatMin) / latStep + 0.5).toInt() + 1
        val lonCount = floor((lonMax - lonMin) / lonStep + 0.5).toInt() + 1
        val rawPoints = IntStream.range(0, latCount).parallel().mapToObj { latIndex ->
            val lat = scanLatMin + latIndex * latStep
            val row = ArrayList<HilalVisibilityPoint>(lonCount)
            for (lonIndex in 0 until lonCount) {
                val lon = lonMin + lonIndex * lonStep
                row.add(evaluatePoint(jdUtIjtima, baseDateJdUt, lat, lon, ephTable, mode, newMoonWindow))
            }
            row
        }.collect(Collectors.toList()).flatten()

        val points = rawPoints

        val bestPoint = points
            .filter { it.zone.ordinal <= HilalVisibilityZone.NOT_VISIBLE.ordinal || it.zone == HilalVisibilityZone.GLOBAL_ACCEPTED }
            .maxWithOrNull(compareBy<HilalVisibilityPoint> { it.score }.thenBy { it.arcV }.thenBy { it.arcL })

        return HilalVisibilityMapResult(
            mode = mode,
            points = points,
            bestPoint = bestPoint,
            latMin = latMin,
            latMax = latMax,
            lonMin = lonMin,
            lonMax = lonMax,
            latStep = latStep,
            lonStep = lonStep,
            dayOffset = dayOffset,
            baseDateJdUt = baseDateJdUt
        ).also { cache[cacheKey] = it }
    }

    fun buildFastMap(
        ijtimaGeoJde: Double,
        mode: HilalVisibilityMapMode,
        latStep: Double = 3.0,
        lonStep: Double = 3.0,
        dayOffset: Int = 0,
        baseDateJdUtOverride: Double? = null
    ): HilalVisibilityMapResult {
        // KGHT: gunakan jalur cepat khusus dengan early-exit
        if (mode == HilalVisibilityMapMode.KGHT_TURKI || mode == HilalVisibilityMapMode.KGHT_MUHAMMADIYAH) {
            return buildKghtFastMap(ijtimaGeoJde, mode, latStep, lonStep, dayOffset, baseDateJdUtOverride)
        }

        val latMin = -90.0
        val latMax = 90.0
        val lonMin = -180.0
        val lonMax = 180.0
        val scanLatMin = -90.0
        val scanLatMax = 90.0

        val dtIjtima = DynamicalTimeEngine.deltaT(ijtimaGeoJde)
        val jdUtIjtima = ijtimaGeoJde - dtIjtima / 86400.0
        val defaultBaseDateJdUt = floor(jdUtIjtima + 0.5) - 0.5
        val baseDateJdUt = (baseDateJdUtOverride ?: defaultBaseDateJdUt) + dayOffset
        val newMoonWindow = buildNewMoonWindow(jdUtIjtima)
        val cacheKey = String.format(Locale.US, "FAST:%s:%.5f:%.5f:%.2f:%.2f:%d", mode.name, ijtimaGeoJde, baseDateJdUt, latStep, lonStep, dayOffset)
        cache[cacheKey]?.let { return it }

        val ephTable = buildFastEphTable(baseDateJdUt)
        val latCount = floor((scanLatMax - scanLatMin) / latStep + 0.5).toInt() + 1
        val lonCount = floor((lonMax - lonMin) / lonStep + 0.5).toInt() + 1
        val rawPoints = IntStream.range(0, latCount).parallel().mapToObj { latIndex ->
            val lat = scanLatMin + latIndex * latStep
            val row = ArrayList<HilalVisibilityPoint>(lonCount)
            for (lonIndex in 0 until lonCount) {
                val lon = lonMin + lonIndex * lonStep
                row.add(evaluateFastPoint(jdUtIjtima, baseDateJdUt, lat, lon, ephTable, mode, newMoonWindow))
            }
            row
        }.collect(Collectors.toList()).flatten()

        val points = rawPoints

        val bestPoint = points
            .filter { it.zone.ordinal <= HilalVisibilityZone.NOT_VISIBLE.ordinal || it.zone == HilalVisibilityZone.GLOBAL_ACCEPTED }
            .maxWithOrNull(compareBy<HilalVisibilityPoint> { it.score }.thenBy { it.arcV }.thenBy { it.arcL })

        return HilalVisibilityMapResult(
            mode = mode,
            points = points,
            bestPoint = bestPoint,
            latMin = latMin,
            latMax = latMax,
            lonMin = lonMin,
            lonMax = lonMax,
            latStep = latStep,
            lonStep = lonStep,
            dayOffset = dayOffset,
            baseDateJdUt = baseDateJdUt
        ).also { cache[cacheKey] = it }
    }

    fun buildFastRasterMap(
        ijtimaGeoJde: Double,
        mode: HilalVisibilityMapMode,
        latStep: Double = 2.0,
        lonStep: Double = 2.0,
        dayOffset: Int = 0,
        baseDateJdUtOverride: Double? = null
    ): HilalVisibilityRasterMapResult {
        val latMin = -90.0
        val latMax = 90.0
        val lonMin = -180.0
        val lonMax = 180.0
        val scanLatMin = -90.0
        val scanLatMax = 90.0

        val dtIjtima = DynamicalTimeEngine.deltaT(ijtimaGeoJde)
        val jdUtIjtima = ijtimaGeoJde - dtIjtima / 86400.0
        val defaultBaseDateJdUt = floor(jdUtIjtima + 0.5) - 0.5
        val baseDateJdUt = (baseDateJdUtOverride ?: defaultBaseDateJdUt) + dayOffset
        val cacheKey = String.format(
            Locale.US,
            "RASTER_V7:%s:%.5f:%.5f:%.2f:%.2f:%d",
            mode.name,
            ijtimaGeoJde,
            baseDateJdUt,
            latStep,
            lonStep,
            dayOffset
        )
        rasterCache[cacheKey]?.let { return it }

        val newMoonWindow = buildNewMoonWindow(jdUtIjtima)
        val ephTable = buildFastEphTable(baseDateJdUt)
        val latCount = floor((scanLatMax - scanLatMin) / latStep + 0.5).toInt() + 1
        val lonCount = floor((lonMax - lonMin) / lonStep + 0.5).toInt() + 1
        val scores = FloatArray(latCount * lonCount) { -100.0f }
        val grayMargins = FloatArray(latCount * lonCount) { 1.0f }
        val noEventMasks = ByteArray(latCount * lonCount)
        val rowBestPoints = arrayOfNulls<HilalVisibilityPoint>(latCount)

        IntStream.range(0, latCount).parallel().forEach { latIndex ->
            val lat = scanLatMin + latIndex * latStep
            var rowBestPoint: HilalVisibilityPoint? = null
            for (lonIndex in 0 until lonCount) {
                val lon = lonMin + lonIndex * lonStep
                val point = evaluateFastPoint(jdUtIjtima, baseDateJdUt, lat, lon, ephTable, mode, newMoonWindow)
                val index = latIndex * lonCount + lonIndex
                when (point.zone) {
                    HilalVisibilityZone.BEFORE_CONJUNCTION,
                    HilalVisibilityZone.MOON_SET_BEFORE_SUN -> {
                        grayMargins[index] = grayMargin(point)
                        scores[index] = -100.0f
                    }
                    HilalVisibilityZone.NO_EVENT -> {
                        noEventMasks[index] = 1
                        scores[index] = -100.0f
                    }
                    else -> {
                        scores[index] = point.score.toFloat()
                        grayMargins[index] = grayMargin(point)
                        val currentBest = rowBestPoint
                        if (currentBest == null ||
                            point.score > currentBest.score ||
                            (point.score == currentBest.score && point.arcV > currentBest.arcV) ||
                            (point.score == currentBest.score && point.arcV == currentBest.arcV && point.arcL > currentBest.arcL)
                        ) {
                            rowBestPoint = point
                        }
                    }
                }
            }
            rowBestPoints[latIndex] = rowBestPoint
        }
        val bestPoint = rowBestPoints.filterNotNull()
            .maxWithOrNull(compareBy<HilalVisibilityPoint> { it.score }.thenBy { it.arcV }.thenBy { it.arcL })

        return HilalVisibilityRasterMapResult(
            cacheKey = cacheKey,
            mode = mode,
            scores = scores,
            grayMargins = grayMargins,
            noEventMasks = noEventMasks,
            bestPoint = bestPoint,
            latMin = latMin,
            latMax = latMax,
            lonMin = lonMin,
            lonMax = lonMax,
            scanLatMin = scanLatMin,
            scanLatMax = scanLatMax,
            latStep = latStep,
            lonStep = lonStep,
            latCount = latCount,
            lonCount = lonCount,
            dayOffset = dayOffset,
            baseDateJdUt = baseDateJdUt
        ).also { rasterCache[cacheKey] = it }
    }

    private fun grayMargin(point: HilalVisibilityPoint): Float {
        val lag = if (point.moonLagHours.isFinite()) point.moonLagHours else 24.0
        val age = if (point.sunsetAgeHours.isFinite()) point.sunsetAgeHours else 24.0
        val altitude = if (point.moonAltTopo.isFinite()) point.moonAltTopo else 24.0
        val margin = min(min(lag, age), altitude)
        return if (point.zone == HilalVisibilityZone.BEFORE_CONJUNCTION ||
            point.zone == HilalVisibilityZone.MOON_SET_BEFORE_SUN
        ) {
            margin.coerceAtMost(-1e-4).toFloat()
        } else {
            margin.toFloat()
        }
    }

    /**
     * Jalur cepat khusus KGHT:
     * 1. Scan grid kasar 15°x30° dengan early-exit saat pertama menemukan titik yang memenuhi
     * 2. Jika terpenuhi → buat grid visual dengan semua titik diwarnai (EASY + GLOBAL_ACCEPTED)
     * 3. Jika tidak → buat grid visual normal tanpa hijau
     */
    private fun buildKghtFastMap(
        ijtimaGeoJde: Double,
        mode: HilalVisibilityMapMode,
        latStep: Double,
        lonStep: Double,
        dayOffset: Int,
        baseDateJdUtOverride: Double?
    ): HilalVisibilityMapResult {
        val latMin = -90.0; val latMax = 90.0; val lonMin = -180.0; val lonMax = 180.0
        val dtIjtima = DynamicalTimeEngine.deltaT(ijtimaGeoJde)
        val jdUtIjtima = ijtimaGeoJde - dtIjtima / 86400.0
        val defaultBaseDateJdUt = floor(jdUtIjtima + 0.5) - 0.5
        val baseDateJdUt = (baseDateJdUtOverride ?: defaultBaseDateJdUt) + dayOffset
        val newMoonWindow = buildNewMoonWindow(jdUtIjtima)
        val cacheKey = String.format(Locale.US, "KGHT:%s:%.5f:%.5f:%.2f:%.2f:%d", mode.name, ijtimaGeoJde, baseDateJdUt, latStep, lonStep, dayOffset)
        cache[cacheKey]?.let { return it }

        val ephTable = buildFastEphTable(baseDateJdUt)

        // Pass 1: scan kasar 15°x30° dengan early exit untuk cek apakah KGHT terpenuhi global
        var globallyMet = false
        var bestLocalPoint: HilalVisibilityPoint? = null
        outer@ for (latI in -4..4) {
            for (lonI in -6..6) {
                val lat = latI * 15.0
                val lon = lonI * 30.0
                val pt = evaluateFastPoint(jdUtIjtima, baseDateJdUt, lat, lon, ephTable, mode, newMoonWindow)
                if (pt.zone == HilalVisibilityZone.EASY_NAKED_EYE) {
                    globallyMet = true
                    bestLocalPoint = pt
                    break@outer
                }
            }
        }

        // Pass 2: buat grid visual resolusi penuh (latStep x lonStep)
        val latCount = floor((latMax - latMin) / latStep + 0.5).toInt() + 1
        val lonCount = floor((lonMax - lonMin) / lonStep + 0.5).toInt() + 1
        val points: List<HilalVisibilityPoint>
        if (globallyMet) {
            // Jika terpenuhi, kita tetap perlu menampilkan kurva batas lokal
            // Gunakan grid resolusi normal untuk tampilan
            val rawPoints = IntStream.range(0, latCount).parallel().mapToObj { latIndex ->
                val lat = latMin + latIndex * latStep
                val row = ArrayList<HilalVisibilityPoint>(lonCount)
                for (lonIndex in 0 until lonCount) {
                    val lon = lonMin + lonIndex * lonStep
                    row.add(evaluateFastPoint(jdUtIjtima, baseDateJdUt, lat, lon, ephTable, mode, newMoonWindow))
                }
                row
            }.collect(Collectors.toList()).flatten()
            points = rawPoints
        } else {
            // Tidak terpenuhi: buat grid transparan saja (cepat, tanpa hitung lengkap)
            points = ArrayList<HilalVisibilityPoint>(latCount * lonCount).apply {
                for (latIndex in 0 until latCount) {
                    val lat = latMin + latIndex * latStep
                    for (lonIndex in 0 until lonCount) {
                        val lon = lonMin + lonIndex * lonStep
                        val pt = evaluateFastPoint(jdUtIjtima, baseDateJdUt, lat, lon, ephTable, mode, newMoonWindow)
                        add(pt)
                    }
                }
            }
        }

        val bestPoint = bestLocalPoint ?: points
            .filter { it.zone.ordinal <= HilalVisibilityZone.NOT_VISIBLE.ordinal || it.zone == HilalVisibilityZone.GLOBAL_ACCEPTED }
            .maxWithOrNull(compareBy<HilalVisibilityPoint> { it.score }.thenBy { it.arcV }.thenBy { it.arcL })

        return HilalVisibilityMapResult(
            mode = mode, points = points, bestPoint = bestPoint,
            latMin = latMin, latMax = latMax, lonMin = lonMin, lonMax = lonMax,
            latStep = latStep, lonStep = lonStep,
            dayOffset = dayOffset, baseDateJdUt = baseDateJdUt
        ).also { cache[cacheKey] = it }
    }

    private fun buildEphTable(baseDateJdUt: Double): List<EphData> {
        return (-36..84).map { h ->
            val jdUt = baseDateJdUt + h / 24.0
            val jde = jdUt + DynamicalTimeEngine.deltaT(jdUt) / 86400.0
            val moon = ElpMpp02LunarEngine.computeGeometric(jde)
            val sun = Vsop87SolarEngine.compute(jde)
            EphData(
                jdUt = jdUt,
                moonRa = moon.ra,
                moonDec = moon.dec,
                moonDistanceKm = moon.distanceAU * KM_PER_AU,
                sunRa = sun.ra,
                sunDec = sun.dec,
                sunDistanceKm = sun.distanceAU * KM_PER_AU
            )
        }
    }

    private fun buildFastEphTable(baseDateJdUt: Double): List<EphData> {
        return (-6..42).map { h ->
            val jdUt = baseDateJdUt + h / 24.0
            val jde = jdUt + DynamicalTimeEngine.deltaT(jdUt) / 86400.0
            val moon = ElpMpp02LunarEngine.computeGeometric(jde)
            val sun = Vsop87SolarEngine.compute(jde)
            EphData(
                jdUt = jdUt,
                moonRa = moon.ra,
                moonDec = moon.dec,
                moonDistanceKm = moon.distanceAU * KM_PER_AU,
                sunRa = sun.ra,
                sunDec = sun.dec,
                sunDistanceKm = sun.distanceAU * KM_PER_AU
            )
        }
    }

    private fun evaluateFastPoint(
        jdUtIjtima: Double,
        baseDateJdUt: Double,
        lat: Double,
        lon: Double,
        table: List<EphData>,
        mode: HilalVisibilityMapMode,
        newMoonWindow: NewMoonWindow
    ): HilalVisibilityPoint {
        val startUt = baseDateJdUt - lon / 360.0
        val sunsetUt = searchSet(Body.SUN, startUt, lat, lon, table)
            ?: return specialPoint(lat, lon, HilalVisibilityZone.NO_EVENT)
        val nearestNewMoonUt = nearestNewMoonUt(sunsetUt, newMoonWindow)
        val beforeConjunction = sunsetUt < nearestNewMoonUt
        val ageAtSunsetHours = (sunsetUt - nearestNewMoonUt) * 24.0
        val usesBestTime = mode == HilalVisibilityMapMode.YALLOP || mode == HilalVisibilityMapMode.ODEH
        val moonsetUt = if (usesBestTime) searchSet(Body.MOON, startUt, lat, lon, table) else null
        if (usesBestTime && moonsetUt == null) {
            return specialPoint(
                lat,
                lon,
                HilalVisibilityZone.NO_EVENT,
                ageHours = ageAtSunsetHours
            )
        }
        val lagDays = moonsetUt?.let { it - sunsetUt }

        val zoneOverride = when {
            beforeConjunction -> HilalVisibilityZone.BEFORE_CONJUNCTION
            usesBestTime && lagDays != null && lagDays < 0.0 -> HilalVisibilityZone.MOON_SET_BEFORE_SUN
            else -> null
        }

        val evaluationUt = if (usesBestTime && lagDays != null) sunsetUt + lagDays * 4.0 / 9.0 else sunsetUt
        val eph = interpolateEph(evaluationUt, table)
        val gast = AstroDataUtils.calculateGAST(evaluationUt)
        val sunHorizontal = horizontal(eph.sunRa, eph.sunDec, lat, lon, gast)
        val moonTopo = topocentricMoon(eph.moonRa, eph.moonDec, eph.moonDistanceKm, lat, lon, gast)
        val moonHorizontal = horizontal(moonTopo.first, moonTopo.second, lat, lon, gast)
        val moonGeoHorizontal = horizontal(eph.moonRa, eph.moonDec, lat, lon, gast)

        val geoArcL = elongation(eph.sunRa, eph.sunDec, eph.moonRa, eph.moonDec)
        val topoArcL = elongation(eph.sunRa, eph.sunDec, moonTopo.first, moonTopo.second)
        val daz = angularDistanceAbs(sunHorizontal.azimuth, moonHorizontal.azimuth)
        val arcL = if (mode == HilalVisibilityMapMode.ODEH) topoArcL else geoArcL
        val arcV = if (mode == HilalVisibilityMapMode.ODEH) {
            signedOdehArcV(topoArcL, daz, moonHorizontal.altitude - sunHorizontal.altitude)
        } else {
            moonGeoHorizontal.altitude - sunHorizontal.altitude
        }
        val moonSemiDiameterArcMin = AstroMath.deg(asin((MOON_RADIUS_KM / eph.moonDistanceKm).coerceIn(-1.0, 1.0))) * 60.0
        val lunarParallaxArcMin = moonSemiDiameterArcMin / 0.27245
        val sdTopoArcMin = moonSemiDiameterArcMin *
            (1.0 + sin(AstroMath.rad(moonHorizontal.altitude)) * sin(AstroMath.rad(lunarParallaxArcMin / 60.0)))
        val crescentWidthArcMin = sdTopoArcMin * (1.0 - cos(AstroMath.rad(arcL)))
        val ageHours = (evaluationUt - nearestNewMoonUt(evaluationUt, newMoonWindow)) * 24.0
        val refBulan = AstroTransform.atmosphericRefraction(moonHorizontal.altitude)
        val moonAltTopoApparent = moonHorizontal.altitude + refBulan

        val (zone, score) = classifyByMode(mode, arcV, arcL, ageHours, crescentWidthArcMin, moonAltTopoApparent, moonGeoHorizontal.altitude, jdUtIjtima, sunsetUt)
        val finalZone = zoneOverride ?: if (!usesBestTime && moonAltTopoApparent < 0.0) {
            HilalVisibilityZone.MOON_SET_BEFORE_SUN
        } else {
            zone
        }
        val moonLagHours = when {
            lagDays != null -> lagDays * 24.0
            else -> Double.NaN
        }
        return HilalVisibilityPoint(
            latitude = lat,
            longitude = lon,
            zone = finalZone,
            score = score,
            arcV = arcV,
            arcL = arcL,
            daz = daz,
            ageHours = ageHours,
            crescentWidthArcMin = crescentWidthArcMin,
            moonAltTopo = moonAltTopoApparent,
            moonAltGeo = moonGeoHorizontal.altitude,
            moonLagHours = moonLagHours,
            sunsetAgeHours = ageAtSunsetHours
        )
    }

    private fun evaluatePoint(
        jdUtIjtima: Double,
        baseDateJdUt: Double,
        lat: Double,
        lon: Double,
        table: List<EphData>,
        mode: HilalVisibilityMapMode,
        newMoonWindow: NewMoonWindow
    ): HilalVisibilityPoint {
        val startUt = baseDateJdUt - lon / 360.0
        val sunsetUt = searchSet(Body.SUN, startUt, lat, lon, table)
            ?: return specialPoint(lat, lon, HilalVisibilityZone.NO_EVENT)
        val moonsetUt = searchSet(Body.MOON, startUt, lat, lon, table)
            ?: return specialPoint(lat, lon, HilalVisibilityZone.NO_EVENT)

        val lagDays = moonsetUt - sunsetUt
        val lagHours = lagDays * 24.0
        val nearestNewMoonUt = nearestNewMoonUt(sunsetUt, newMoonWindow)
        val beforeConjunction = sunsetUt < nearestNewMoonUt
        val ageAtSunsetHours = (sunsetUt - nearestNewMoonUt) * 24.0
        val zoneOverride = when {
            lagDays < 0.0 && beforeConjunction -> HilalVisibilityZone.BEFORE_CONJUNCTION
            lagDays < 0.0 -> HilalVisibilityZone.MOON_SET_BEFORE_SUN
            beforeConjunction -> HilalVisibilityZone.BEFORE_CONJUNCTION
            else -> null
        }

        val usesBestTime = mode == HilalVisibilityMapMode.YALLOP || mode == HilalVisibilityMapMode.ODEH
        val evaluationUt = if (usesBestTime) sunsetUt + lagDays * 4.0 / 9.0 else sunsetUt
        val eph = interpolateEph(evaluationUt, table)
        val gast = AstroDataUtils.calculateGAST(evaluationUt)
        val sunHorizontal = horizontal(eph.sunRa, eph.sunDec, lat, lon, gast)
        val moonTopo = topocentricMoon(eph.moonRa, eph.moonDec, eph.moonDistanceKm, lat, lon, gast)
        val moonHorizontal = horizontal(moonTopo.first, moonTopo.second, lat, lon, gast)
        val moonGeoHorizontal = horizontal(eph.moonRa, eph.moonDec, lat, lon, gast)

        val geoArcL = elongation(eph.sunRa, eph.sunDec, eph.moonRa, eph.moonDec)
        val topoArcL = elongation(eph.sunRa, eph.sunDec, moonTopo.first, moonTopo.second)
        val daz = angularDistanceAbs(sunHorizontal.azimuth, moonHorizontal.azimuth)
        val arcL = if (mode == HilalVisibilityMapMode.ODEH) topoArcL else geoArcL
        val arcV = if (mode == HilalVisibilityMapMode.ODEH) {
            signedOdehArcV(topoArcL, daz, moonHorizontal.altitude - sunHorizontal.altitude)
        } else {
            moonGeoHorizontal.altitude - sunHorizontal.altitude
        }

        val moonSemiDiameterArcMin = AstroMath.deg(asin((MOON_RADIUS_KM / eph.moonDistanceKm).coerceIn(-1.0, 1.0))) * 60.0
        val lunarParallaxArcMin = moonSemiDiameterArcMin / 0.27245
        val sdTopoArcMin = moonSemiDiameterArcMin *
            (1.0 + sin(AstroMath.rad(moonHorizontal.altitude)) * sin(AstroMath.rad(lunarParallaxArcMin / 60.0)))
        val crescentWidthArcMin = sdTopoArcMin * (1.0 - cos(AstroMath.rad(arcL)))
        val ageHours = (evaluationUt - nearestNewMoonUt(evaluationUt, newMoonWindow)) * 24.0
        val refBulan = AstroTransform.atmosphericRefraction(moonHorizontal.altitude)
        val moonAltTopoApparent = moonHorizontal.altitude + refBulan

        val (zone, score) = classifyByMode(mode, arcV, arcL, ageHours, crescentWidthArcMin, moonAltTopoApparent, moonGeoHorizontal.altitude, jdUtIjtima, sunsetUt)
        return HilalVisibilityPoint(
            latitude = lat,
            longitude = lon,
            zone = zoneOverride ?: zone,
            score = score,
            arcV = arcV,
            arcL = arcL,
            daz = daz,
            ageHours = ageHours,
            crescentWidthArcMin = crescentWidthArcMin,
            moonAltTopo = moonAltTopoApparent,
            moonAltGeo = moonGeoHorizontal.altitude,
            moonLagHours = lagHours,
            sunsetAgeHours = ageAtSunsetHours
        )
    }

    private fun specialPoint(
        lat: Double,
        lon: Double,
        zone: HilalVisibilityZone,
        ageHours: Double = 0.0,
        moonLagHours: Double = Double.NaN
    ) = HilalVisibilityPoint(
        latitude = lat,
        longitude = lon,
        zone = zone,
        score = -100.0,
        arcV = 0.0,
        arcL = 0.0,
        daz = 0.0,
        ageHours = ageHours,
        crescentWidthArcMin = 0.0,
        moonLagHours = moonLagHours,
        sunsetAgeHours = ageHours
    )

    private fun searchSet(
        body: Body,
        startUt: Double,
        lat: Double,
        lon: Double,
        table: List<EphData>
    ): Double? {
        return estimateSet(body, startUt, lat, lon, table)
    }

    private fun estimateSet(
        body: Body,
        startUt: Double,
        lat: Double,
        lon: Double,
        table: List<EphData>
    ): Double? {
        var t = startUt + 0.75
        repeat(3) {
            val eph = interpolateEph(t, table)
            val gast = AstroDataUtils.calculateGAST(t)
            val (ra, dec, distanceKm, radiusKm) = when (body) {
                Body.SUN -> Quad(eph.sunRa, eph.sunDec, eph.sunDistanceKm, SUN_RADIUS_KM)
                Body.MOON -> {
                    val moonTopo = topocentricMoon(eph.moonRa, eph.moonDec, eph.moonDistanceKm, lat, lon, gast)
                    Quad(moonTopo.first, moonTopo.second, eph.moonDistanceKm, MOON_RADIUS_KM)
                }
            }
            val radiusDeg = AstroMath.deg(asin((radiusKm / distanceKm).coerceIn(-1.0, 1.0)))
            val h0 = AstroMath.rad(-(radiusDeg + REFRACTION_NEAR_HORIZON_DEG))
            val latRad = AstroMath.rad(lat)
            val decRad = AstroMath.rad(dec)
            val cosH = (sin(h0) - sin(latRad) * sin(decRad)) / (cos(latRad) * cos(decRad))
            if (cosH !in -1.0..1.0) return null
            val setHourAngle = AstroMath.deg(acos(cosH.coerceIn(-1.0, 1.0)))
            val targetGast = AstroMath.mod(ra + setHourAngle - lon, 360.0)
            val delta = signedAngle(targetGast - gast)
            t += delta / 360.98564736629
            while (t < startUt) t += 1.0
            while (t >= startUt + 1.0) t -= 1.0
        }
        return t
    }

    private data class Quad(
        val first: Double,
        val second: Double,
        val third: Double,
        val fourth: Double
    )

    private fun refineSet(
        body: Body,
        leftUt: Double,
        rightUt: Double,
        lat: Double,
        lon: Double,
        table: List<EphData>
    ): Double {
        var lo = leftUt
        var hi = rightUt
        repeat(24) {
            val mid = (lo + hi) / 2.0
            if (riseSetFunction(body, mid, lat, lon, table) > 0.0) lo = mid else hi = mid
        }
        return (lo + hi) / 2.0
    }

    private fun riseSetFunction(
        body: Body,
        jdUt: Double,
        lat: Double,
        lon: Double,
        table: List<EphData>
    ): Double {
        val eph = interpolateEph(jdUt, table)
        val gast = AstroDataUtils.calculateGAST(jdUt)
        return when (body) {
            Body.SUN -> {
                val alt = horizontal(eph.sunRa, eph.sunDec, lat, lon, gast).altitude
                val radiusDeg = AstroMath.deg(asin((SUN_RADIUS_KM / eph.sunDistanceKm).coerceIn(-1.0, 1.0)))
                alt + radiusDeg + REFRACTION_NEAR_HORIZON_DEG
            }
            Body.MOON -> {
                val moonTopo = topocentricMoon(eph.moonRa, eph.moonDec, eph.moonDistanceKm, lat, lon, gast)
                val alt = horizontal(moonTopo.first, moonTopo.second, lat, lon, gast).altitude
                val radiusDeg = AstroMath.deg(asin((MOON_RADIUS_KM / eph.moonDistanceKm).coerceIn(-1.0, 1.0)))
                alt + radiusDeg + REFRACTION_NEAR_HORIZON_DEG
            }
        }
    }

    private fun interpolateEph(jdUt: Double, table: List<EphData>): EphData {
        val raw = (jdUt - table.first().jdUt) * 24.0
        val i0 = floor(raw).toInt().coerceIn(0, table.size - 2)
        val f = (raw - i0).coerceIn(0.0, 1.0)
        val p0 = table[i0]
        val p1 = table[i0 + 1]
        return EphData(
            jdUt = jdUt,
            moonRa = interpolateAngle(p0.moonRa, p1.moonRa, f),
            moonDec = p0.moonDec + f * (p1.moonDec - p0.moonDec),
            moonDistanceKm = p0.moonDistanceKm + f * (p1.moonDistanceKm - p0.moonDistanceKm),
            sunRa = interpolateAngle(p0.sunRa, p1.sunRa, f),
            sunDec = p0.sunDec + f * (p1.sunDec - p0.sunDec),
            sunDistanceKm = p0.sunDistanceKm + f * (p1.sunDistanceKm - p0.sunDistanceKm)
        )
    }

    private fun interpolateAngle(a0: Double, a1: Double, f: Double): Double {
        var d = (a1 - a0) % 360.0
        if (d > 180.0) d -= 360.0
        if (d < -180.0) d += 360.0
        return AstroMath.mod(a0 + f * d, 360.0)
    }

    private fun classifyByMode(
        mode: HilalVisibilityMapMode,
        arcV: Double,
        arcL: Double,
        ageHours: Double,
        crescentWidthArcMin: Double,
        moonAltTopo: Double,
        moonAltGeo: Double,
        jdUtIjtima: Double,
        sunsetUt: Double
    ): Pair<HilalVisibilityZone, Double> {
        return when (mode) {
            HilalVisibilityMapMode.YALLOP -> classifyYallop(arcV, crescentWidthArcMin)
            HilalVisibilityMapMode.ODEH -> classifyOdeh(arcV, crescentWidthArcMin)
            HilalVisibilityMapMode.MABIMS_BARU -> classifyContinuous(min(moonAltTopo - 3.0, arcL - 6.4))
            HilalVisibilityMapMode.MABIMS_LAMA -> classifyContinuous(min(moonAltTopo - 2.0, min(arcL - 3.0, ageHours - 8.0)))
            HilalVisibilityMapMode.WUJUDUL_HILAL -> classifyContinuous(min(moonAltGeo, (sunsetUt - jdUtIjtima) * 24.0))
            HilalVisibilityMapMode.LAPAN -> classifyContinuous(min(moonAltTopo - 2.0, arcL - 3.0))
            HilalVisibilityMapMode.DANJON -> classifyContinuous(arcL - 7.0)
            HilalVisibilityMapMode.KGHT_TURKI -> classifyContinuous(min(moonAltTopo - 5.0, arcL - 8.0))
            HilalVisibilityMapMode.KGHT_MUHAMMADIYAH -> classifyContinuous(min(moonAltGeo - 5.0, arcL - 8.0))
        }
    }

    private fun classifyYallop(arcV: Double, widthArcMin: Double): Pair<HilalVisibilityZone, Double> {
        val w = widthArcMin.coerceAtLeast(0.0)
        val q = (arcV - (11.8371 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w * w * w)) / 10.0
        val zone = when {
            q > 0.216 -> HilalVisibilityZone.EASY_NAKED_EYE
            q > -0.014 -> HilalVisibilityZone.POSSIBLE_NAKED_EYE
            q > -0.160 -> HilalVisibilityZone.OPTICAL_AID_TO_FIND
            q > -0.232 -> HilalVisibilityZone.OPTICAL_AID
            q > -0.293 -> HilalVisibilityZone.TELESCOPE_ONLY
            else -> HilalVisibilityZone.NOT_VISIBLE
        }
        return zone to q
    }

    private fun classifyOdeh(arcV: Double, widthArcMin: Double): Pair<HilalVisibilityZone, Double> {
        val w = widthArcMin.coerceAtLeast(0.0)
        val v = arcV - (7.1651 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w * w * w)
        val zone = when {
            v >= 5.65 -> HilalVisibilityZone.EASY_NAKED_EYE
            v >= 2.0 -> HilalVisibilityZone.OPTICAL_AID_TO_FIND
            v >= -0.96 -> HilalVisibilityZone.TELESCOPE_ONLY
            else -> HilalVisibilityZone.NOT_VISIBLE
        }
        return zone to v
    }

    private fun classifyContinuous(margin: Double): Pair<HilalVisibilityZone, Double> {
        // margin >= 0.0 berarti memenuhi syarat.
        // Konversi margin agar saat margin == 0.0, score == 8.0 (batas AL_HABIB_EASY)
        // Jika margin < 0.0 (misal -0.1), score = 7.9 (tidak memenuhi AL_HABIB_EASY)
        // Dan jika sangat jauh (misal margin < -8.0), score akan < 0.0 menjadi TRANSPARENT.
        val score = 8.0 + margin
        return (if (margin >= 0.0) HilalVisibilityZone.EASY_NAKED_EYE else HilalVisibilityZone.NOT_VISIBLE) to score
    }

    private fun topocentricMoon(
        ra: Double,
        dec: Double,
        distanceKm: Double,
        lat: Double,
        lon: Double,
        gast: Double
    ): Pair<Double, Double> {
        val hp = AstroMath.deg(asin((EARTH_RADIUS_KM / distanceKm).coerceIn(-1.0, 1.0)))
        val ha = AstroMath.mod(gast + lon - ra, 360.0)
        val u = atan(0.99664719 * tan(AstroMath.rad(lat)))
        val x = cos(u)
        val y = 0.99664719 * sin(u)
        val hpRad = AstroMath.rad(hp)
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

    private fun horizontal(ra: Double, dec: Double, lat: Double, lon: Double, gast: Double): HorizontalPosition {
        val ha = AstroMath.mod(gast + lon - ra, 360.0)
        val haRad = AstroMath.rad(ha)
        val decRad = AstroMath.rad(dec)
        val latRad = AstroMath.rad(lat)
        val sinAlt = sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(haRad)
        val alt = AstroMath.deg(asin(sinAlt.coerceIn(-1.0, 1.0)))
        val y = -sin(haRad) * cos(decRad)
        val x = sin(decRad) * cos(latRad) - cos(decRad) * sin(latRad) * cos(haRad)
        val az = AstroMath.mod(AstroMath.deg(atan2(y, x)), 360.0)
        return HorizontalPosition(alt, az)
    }

    private fun elongation(sunRa: Double, sunDec: Double, moonRa: Double, moonDec: Double): Double {
        val c = sin(AstroMath.rad(sunDec)) * sin(AstroMath.rad(moonDec)) +
            cos(AstroMath.rad(sunDec)) * cos(AstroMath.rad(moonDec)) * cos(AstroMath.rad(sunRa - moonRa))
        return AstroMath.deg(acos(c.coerceIn(-1.0, 1.0)))
    }

    private fun odehArcV(arcL: Double, daz: Double): Double {
        val cosDaz = cos(AstroMath.rad(daz))
        if (abs(cosDaz) < 1e-12) return 90.0
        return AstroMath.deg(acos((cos(AstroMath.rad(arcL)) / cosDaz).coerceIn(-1.0, 1.0)))
    }

    private fun signedOdehArcV(arcL: Double, daz: Double, altitudeDifference: Double): Double {
        val unsigned = odehArcV(arcL, daz)
        return if (altitudeDifference < 0.0) -unsigned else unsigned
    }

    private fun angularDistanceAbs(a: Double, b: Double): Double {
        var d = abs(a - b) % 360.0
        if (d > 180.0) d = 360.0 - d
        return d
    }

    private fun signedAngle(angle: Double): Double {
        var d = angle % 360.0
        if (d > 180.0) d -= 360.0
        if (d < -180.0) d += 360.0
        return d
    }

    private fun buildNewMoonWindow(currentUt: Double): NewMoonWindow {
        val prev = refineNewMoonUt(currentUt - SYNODIC_MONTH_DAYS)
        val curr = refineNewMoonUt(currentUt)
        val next = refineNewMoonUt(currentUt + SYNODIC_MONTH_DAYS)
        return NewMoonWindow(previousUt = prev, currentUt = curr, nextUt = next)
    }

    private fun refineNewMoonUt(seedUt: Double): Double {
        var t = seedUt
        repeat(8) {
            val f0 = moonSunLongitudeDiff(t)
            val f1 = moonSunLongitudeDiff(t + 0.01)
            var df = signedAngle(f1 - f0) / 0.01
            if (abs(df) < 1e-8) df = if (df < 0.0) -1e-8 else 1e-8
            val step = (f0 / df).coerceIn(-2.0, 2.0)
            t -= step
            if (abs(step) < 1e-8) return t
        }
        return t
    }

    private fun moonSunLongitudeDiff(jdUt: Double): Double {
        val jde = jdUt + DynamicalTimeEngine.deltaT(jdUt) / 86400.0
        val sun = Vsop87SolarEngine.compute(jde)
        val moon = ElpMpp02LunarEngine.computeGeometric(jde)
        return signedAngle(moon.longitudeEcliptic - sun.longitudeEcliptic)
    }

    private fun nearestNewMoonUt(jdUt: Double, window: NewMoonWindow): Double {
        val dPrev = abs(jdUt - window.previousUt)
        val dCurr = abs(jdUt - window.currentUt)
        val dNext = abs(jdUt - window.nextUt)
        return when {
            dPrev <= dCurr && dPrev <= dNext -> window.previousUt
            dNext < dCurr && dNext < dPrev -> window.nextUt
            else -> window.currentUt
        }
    }
}
