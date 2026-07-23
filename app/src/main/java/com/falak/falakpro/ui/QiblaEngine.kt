package com.falak.falakpro.ui

import android.content.Context
import com.falak.falakpro.premium.*
import kotlin.math.*

object QiblaEngine {
    const val KAABA_LAT = 21.422487
    const val KAABA_LON = 39.826206

    private data class HorizontalPosition(
        val azimuth: Double,
        val altitude: Double
    )

    data class RashdulQiblaResult(
        val localHour: Double,
        val target: Target
    ) {
        enum class Target {
            SHADOW,
            SUN
        }
    }
    
    /**
     * Vincenty inverse solution – returns azimuth from north clockwise and distance in km.
     */
    fun calculateVincenty(lat: Double, lon: Double): Pair<Double, Double> {
        val a = 6378.137; val b = 6356.752314245; val f = 1.0 / 298.257223563
        val L = Math.toRadians(KAABA_LON - lon)
        val U1 = atan((1 - f) * tan(Math.toRadians(lat)))
        val U2 = atan((1 - f) * tan(Math.toRadians(KAABA_LAT)))
        val sinU1 = sin(U1); val cosU1 = cos(U1); val sinU2 = sin(U2); val cosU2 = cos(U2)
        var lambda = L; var lambdaP = 2 * PI; var iterLimit = 100
        var sinLambda = 0.0; var cosLambda = 0.0; var sinSigma = 0.0; var cosSigma = 0.0
        var sigma = 0.0; var sinAlpha = 0.0; var cosSqAlpha = 0.0; var cos2SigmaM = 0.0
        while (abs(lambda - lambdaP) > 1e-12 && iterLimit-- > 0) {
            sinLambda = sin(lambda); cosLambda = cos(lambda)
            sinSigma = sqrt((cosU2 * sinLambda).pow(2) + (cosU1 * sinU2 - sinU1 * cosU2 * cosLambda).pow(2))
            if (sinSigma == 0.0) return Pair(0.0, 0.0)
            cosSigma = sinU1 * sinU2 + cosU1 * cosU2 * cosLambda
            sigma = atan2(sinSigma, cosSigma)
            sinAlpha = cosU1 * cosU2 * sinLambda / sinSigma
            cosSqAlpha = 1.0 - sinAlpha * sinAlpha
            cos2SigmaM = if (cosSqAlpha == 0.0) 0.0 else cosSigma - 2.0 * sinU1 * sinU2 / cosSqAlpha
            val C = f / 16.0 * cosSqAlpha * (4.0 + f * (4.0 - 3.0 * cosSqAlpha))
            lambdaP = lambda
            lambda = L + (1.0 - C) * f * sinAlpha * (sigma + C * sinSigma * (cos2SigmaM + C * cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM)))
        }
        val uSq = cosSqAlpha * (a * a - b * b) / (b * b)
        val A = 1.0 + uSq / 16384.0 * (4096.0 + uSq * (-768.0 + uSq * (320.0 - 175.0 * uSq)))
        val B = uSq / 1024.0 * (256.0 + uSq * (-128.0 + uSq * (74.0 - 47.0 * uSq)))
        val deltaSigma = B * sinSigma * (cos2SigmaM + B / 4.0 * (cosSigma * (-1.0 + 2.0 * cos2SigmaM * cos2SigmaM) - B / 6.0 * cos2SigmaM * (-3.0 + 4.0 * sinSigma * sinSigma) * (-3.0 + 4.0 * cos2SigmaM * cos2SigmaM)))
        val s = b * A * (sigma - deltaSigma)
        var alpha1 = Math.toDegrees(atan2(cosU2 * sinLambda, cosU1 * sinU2 - sinU1 * cosU2 * cosLambda))
        if (alpha1 < 0) alpha1 += 360.0
        return Pair(alpha1, s)
    }

    /**
     * Simple spherical trigonometry (great‑circle) method.
     * Returns azimuth from north clockwise.
     */
    fun calculateSpherical(lat: Double, lon: Double): Double {
        val phi1 = Math.toRadians(lat)
        val lambda1 = Math.toRadians(lon)
        val phi2 = Math.toRadians(KAABA_LAT)
        val lambda2 = Math.toRadians(KAABA_LON)
        val deltaLambda = lambda2 - lambda1
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        var az = Math.toDegrees(atan2(y, x))
        if (az < 0) az += 360.0
        return az
    }

    fun calculateRashdulQibla(
        lat: Double,
        lon: Double,
        timezone: Double,
        year: Int,
        month: Int,
        day: Int,
        method: Int,
        context: Context
    ): RashdulQiblaResult? {
        ensureVsopInitialized(context)

        val qibla = when (method) {
            0 -> calculateSpherical(lat, lon)
            1 -> calculateEllipsoid(lat, lon)
            else -> calculateVincenty(lat, lon).first
        }
        val dayStart = day.toDouble()
        val latRad = Math.toRadians(lat)
        if (abs(tan(latRad)) < 1.0e-12 || abs(sin(latRad)) < 1.0e-12) {
            return calculateRashdulQiblaBySearch(lat, lon, timezone, year, month, day, qibla, context)
        }

        var mDay = dayStart
        var rashdul = Double.NaN
        var iteration = 0
        do {
            val old = rashdul
            val jd = julianForDay(year, month, mDay)
            val jde = jd + DynamicalTimeEngine.deltaT(jd) / 86400.0
            val sun = SunEngine.compute(jde, null, context)
            val declinationRad = Math.toRadians(sun.declination)
            val equationOfTimeHours = sun.equationOfTime / 60.0

            val qiblaForFormula = if (qibla > 180.0) 360.0 - qibla else qibla
            val u = atan(1.0 / (tan(Math.toRadians(qiblaForFormula)) * sin(latRad)))
            val acosArg = tan(declinationRad) * cos(u) / tan(latRad)
            if (acosArg !in -1.0..1.0 || acosArg.isNaN()) {
                return calculateRashdulQiblaBySearch(lat, lon, timezone, year, month, day, qibla, context)
            }

            val t = Math.toDegrees(acos(acosArg) + u) / 15.0
            val longitudeCorrection = timezone - lon / 15.0
            rashdul = if (qibla > 180.0) {
                12.0 - equationOfTimeHours + t + longitudeCorrection
            } else {
                12.0 - equationOfTimeHours - t + longitudeCorrection
            }
            mDay = floor(dayStart) + (rashdul - timezone) / 24.0
            iteration++
        } while (
            iteration < 10 &&
            (rashdul.isNaN() || abs(rashdul - old) >= 1.0 / 3600.0)
        )

        if (rashdul.isNaN()) {
            return calculateRashdulQiblaBySearch(lat, lon, timezone, year, month, day, qibla, context)
        }

        val refined = refineRashdulByShadowDirection(
            lat = lat,
            lon = lon,
            timezone = timezone,
            year = year,
            month = month,
            mDay = mDay,
            qibla = qibla,
            context = context
        )
        return refined ?: calculateRashdulQiblaBySearch(lat, lon, timezone, year, month, day, qibla, context)
    }

    /**
     * Ellipsoid‑corrected spherical method – same formula as the VB version.
     */
    fun calculateEllipsoid(lat: Double, lon: Double): Double {
        // WGS‑84 eccentricity
        val e = 0.0066943800229
        // Convert geographic latitudes to geocentric (geocentric latitude)
        val latGeoc = Math.toDegrees(atan((1 - e) * tan(Math.toRadians(lat))))
        val kabaGeoc = Math.toDegrees(atan((1 - e) * tan(Math.toRadians(KAABA_LAT))))
        val phi1 = Math.toRadians(latGeoc)
        val phi2 = Math.toRadians(kabaGeoc)
        val lambda1 = Math.toRadians(lon)
        val lambda2 = Math.toRadians(KAABA_LON)
        val deltaLambda = lambda2 - lambda1
        val y = sin(deltaLambda) * cos(phi2)
        val x = cos(phi1) * sin(phi2) - sin(phi1) * cos(phi2) * cos(deltaLambda)
        var az = Math.toDegrees(atan2(y, x))
        if (az < 0) az += 360.0
        return az
    }

    /**
     * Topocentric Sun azimuth – unchanged.
     */
    fun calculateSunAzimuth(jd: Double, lat: Double, lon: Double, context: Context): Double {
        return calculateTopocentricSun(jd, lat, lon, context).azimuth
    }

    /**
     * Topocentric Sun Position (Azimuth, Altitude)
     */
    fun calculateSunPosition(jd: Double, lat: Double, lon: Double, context: Context): Pair<Double, Double> {
        val position = calculateTopocentricSun(jd, lat, lon, context)
        return Pair(position.azimuth, position.altitude)
    }

    private fun calculateTopocentricSun(
        jd: Double,
        lat: Double,
        lon: Double,
        context: Context
    ): HorizontalPosition {
        val y = Julian.toCalendar(jd).year.toDouble()
        val dt = DeltaT.estimate(y)
        val jde = jd + dt / 86400.0
        val earth = VsopFactory.createEarth(context)
        val sun = SunEngine.compute(jde, earth, context)
        val nut = NutationIAU2000A(context).compute(jde)
        
        // Greenwich Apparent Sidereal Time in degrees
        val gst = SiderealTime.apparentGreenwich(jd, Math.toDegrees(nut.deltaPsi) * 3600.0, sun.trueObliquity)
        
        // Local Hour Angle H = LST - RA
        val lha = Angle.normalizeDegrees(gst + lon - sun.rightAscension)
        
        // Diurnal Parallax Correction (Meeus Chapter 40)
        val pi0 = Math.toRadians(AstroMath.SOLAR_PARALLAX_ARCSEC / 3600.0 / sun.distanceAU)
        val latR = Math.toRadians(lat)
        val hR = Math.toRadians(lha)
        val decR = Math.toRadians(sun.declination)
        
        val u = atan(0.99664719 * tan(latR))
        val rsP = 0.99664719 * sin(u) // rho sin phi'
        val rcP = cos(u)             // rho cos phi'
        
        val deltaH = atan2(-rcP * sin(pi0) * sin(hR), cos(decR) - rcP * sin(pi0) * cos(hR))
        val hT = hR - deltaH
        val decT = atan2((sin(decR) - rsP * sin(pi0)) * cos(deltaH), cos(decR) - rcP * sin(pi0) * cos(hR))
        
        // Azimuth dari Utara searah jarum jam (N=0°, E=90°, S=180°, W=270°)
        // Meeus Ch.13: A = atan2(-sin(H)·cos(δ), sin(δ)·cos(φ) - cos(δ)·sin(φ)·cos(H))
        // Sesuai standar NASA JPL Horizons — tanpa offset +180°
        val yAz = -sin(hT) * cos(decT)
        val xAz = sin(decT) * cos(latR) - cos(decT) * sin(latR) * cos(hT)
        val azDeg = Math.toDegrees(atan2(yAz, xAz))

        val sinAlt = sin(latR) * sin(decT) + cos(latR) * cos(decT) * cos(hT)
        val altDeg = Math.toDegrees(asin(sinAlt))

        return HorizontalPosition(
            azimuth = Angle.normalizeDegrees(azDeg),
            altitude = altDeg
        )
    }

    /**
     * Topocentric Moon Position (Azimuth, Altitude)
     */
    fun calculateMoonPosition(jd: Double, lat: Double, lon: Double, context: Context): Pair<Double, Double> {
        AstroAssetPreloader.ensureCoreBlocking(context)
        val y = Julian.toCalendar(jd).year.toDouble()
        val dt = DeltaT.estimate(y)
        val jde = jd + dt / 86400.0
        
        val moon = ElpMpp02LunarEngine.computeGeometric(jde)
        val earth = VsopFactory.createEarth(context)
        val sun = SunEngine.compute(jde, earth, context)
        val nut = NutationIAU2000A(context).compute(jde)
        
        val gst = SiderealTime.apparentGreenwich(jd, Math.toDegrees(nut.deltaPsi) * 3600.0, sun.trueObliquity)
        val lha = Angle.normalizeDegrees(gst + lon - moon.ra)
        
        val pi0 = Math.toRadians(moon.horizontalParallax)
        val latR = Math.toRadians(lat)
        val hR = Math.toRadians(lha)
        val decR = Math.toRadians(moon.dec)
        
        val u = atan(0.99664719 * tan(latR))
        val rsP = 0.99664719 * sin(u)
        val rcP = cos(u)
        
        val deltaH = atan2(-rcP * sin(pi0) * sin(hR), cos(decR) - rcP * sin(pi0) * cos(hR))
        val hT = hR - deltaH
        val decT = atan2((sin(decR) - rsP * sin(pi0)) * cos(deltaH), cos(decR) - rcP * sin(pi0) * cos(hR))
        
        val yAz = -sin(hT) * cos(decT)
        val xAz = sin(decT) * cos(latR) - cos(decT) * sin(latR) * cos(hT)
        val azDeg = Angle.normalizeDegrees(Math.toDegrees(atan2(yAz, xAz)))
        
        val sinAlt = sin(latR) * sin(decT) + cos(latR) * cos(decT) * cos(hT)
        val altDeg = Math.toDegrees(asin(sinAlt))
        
        return Pair(azDeg, altDeg)
    }

    private fun ensureVsopInitialized(context: Context) {
        AstroAssetPreloader.ensureSolarBlocking(context)
    }

    private fun julianForDay(year: Int, month: Int, day: Double): Double =
        Julian.fromCalendar(year, month, day)

    private fun localHourToDay(day: Int, localHour: Double, timezone: Double): Double =
        day + (localHour - timezone) / 24.0

    private fun sunAzAltForDay(
        lat: Double,
        lon: Double,
        year: Int,
        month: Int,
        dayDecimalUt: Double,
        context: Context
    ): Pair<Double, Double> {
        val jd = julianForDay(year, month, dayDecimalUt)
        return calculateSunPosition(jd, lat, lon, context)
    }

    private fun shadowDirectionForDay(
        lat: Double,
        lon: Double,
        year: Int,
        month: Int,
        dayDecimalUt: Double,
        context: Context
    ): Double {
        val sunAzimuth = sunAzAltForDay(lat, lon, year, month, dayDecimalUt, context).first
        return Angle.normalizeDegrees(sunAzimuth + 180.0)
    }

    private fun signedAngleDiff(a: Double, b: Double): Double {
        var diff = (a - b + 540.0) % 360.0 - 180.0
        if (diff < -180.0) diff += 360.0
        return diff
    }

    private fun refineRashdulByShadowDirection(
        lat: Double,
        lon: Double,
        timezone: Double,
        year: Int,
        month: Int,
        mDay: Double,
        qibla: Double,
        context: Context
    ): RashdulQiblaResult? {
        var direction = 0
        var delta = signedAngleDiff(
            shadowDirectionForDay(lat, lon, year, month, mDay, context),
            qibla
        ) * 3600.0

        if (abs(delta) > 5.0) {
            direction = 1
            delta = signedAngleDiff(
                shadowDirectionForDay(lat, lon, year, month, mDay, context) + 180.0,
                qibla
            ) * 3600.0
        }

        var refinedDay = mDay
        var iteration = 0
        while (abs(delta) > 1.0 && iteration < 20) {
            val inv = 20.0 / 86400.0
            val nextDelta = signedAngleDiff(
                shadowDirectionForDay(lat, lon, year, month, refinedDay + inv, context) + 180.0 * direction,
                qibla
            ) * 3600.0
            val denominator = delta - nextDelta
            if (abs(denominator) < 1.0e-9) return null
            refinedDay += inv * delta / denominator
            delta = signedAngleDiff(
                shadowDirectionForDay(lat, lon, year, month, refinedDay, context) + 180.0 * direction,
                qibla
            ) * 3600.0
            iteration++
        }

        val (sunAzimuth, sunAltitude) = sunAzAltForDay(lat, lon, year, month, refinedDay, context)
        if (sunAltitude < 0.0) return null

        val localHour = (refinedDay * 24.0 + timezone).mod(24.0)
        val target = if (direction == 0) {
            RashdulQiblaResult.Target.SHADOW
        } else {
            RashdulQiblaResult.Target.SUN
        }
        val targetAzimuth = if (target == RashdulQiblaResult.Target.SHADOW) {
            Angle.normalizeDegrees(sunAzimuth + 180.0)
        } else {
            sunAzimuth
        }
        return if (abs(signedAngleDiff(targetAzimuth, qibla)) <= 1.0 / 3600.0) {
            RashdulQiblaResult(localHour, target)
        } else {
            null
        }
    }

    private fun calculateRashdulQiblaBySearch(
        lat: Double,
        lon: Double,
        timezone: Double,
        year: Int,
        month: Int,
        day: Int,
        qibla: Double,
        context: Context
    ): RashdulQiblaResult? {
        val targets = listOf(
            RashdulQiblaResult.Target.SHADOW,
            RashdulQiblaResult.Target.SUN
        )

        return targets.asSequence().mapNotNull { target ->
            var previousHour: Double? = null
            var previousDelta: Double? = null
            var best: RashdulQiblaResult? = null

            var hour = 0.0
            while (hour <= 24.0) {
                val delta = rashdulDeltaAtLocalHour(lat, lon, timezone, year, month, day, hour, qibla, target, context)
                if (delta != null) {
                    val ph = previousHour
                    val pd = previousDelta
                    if (ph != null && pd != null && pd * delta <= 0.0) {
                        best = refineRashdulBracket(lat, lon, timezone, year, month, day, ph, hour, qibla, target, context)
                        break
                    }
                    previousHour = hour
                    previousDelta = delta
                }
                hour += 10.0 / 60.0
            }
            best
        }.minByOrNull { it.localHour }
    }

    private fun refineRashdulBracket(
        lat: Double,
        lon: Double,
        timezone: Double,
        year: Int,
        month: Int,
        day: Int,
        startHour: Double,
        endHour: Double,
        qibla: Double,
        target: RashdulQiblaResult.Target,
        context: Context
    ): RashdulQiblaResult? {
        var lo = startHour
        var hi = endHour
        var fLo = rashdulDeltaAtLocalHour(lat, lon, timezone, year, month, day, lo, qibla, target, context) ?: return null
        repeat(40) {
            val mid = (lo + hi) / 2.0
            val fMid = rashdulDeltaAtLocalHour(lat, lon, timezone, year, month, day, mid, qibla, target, context) ?: return null
            if (fLo * fMid <= 0.0) {
                hi = mid
            } else {
                lo = mid
                fLo = fMid
            }
        }
        val localHour = (lo + hi) / 2.0
        val delta = rashdulDeltaAtLocalHour(lat, lon, timezone, year, month, day, localHour, qibla, target, context)
            ?: return null
        return if (abs(delta) <= 1.0 / 3600.0) {
            RashdulQiblaResult(localHour.mod(24.0), target)
        } else {
            null
        }
    }

    private fun rashdulDeltaAtLocalHour(
        lat: Double,
        lon: Double,
        timezone: Double,
        year: Int,
        month: Int,
        day: Int,
        localHour: Double,
        qibla: Double,
        target: RashdulQiblaResult.Target,
        context: Context
    ): Double? {
        val dayDecimalUt = localHourToDay(day, localHour, timezone)
        val (sunAzimuth, sunAltitude) = sunAzAltForDay(lat, lon, year, month, dayDecimalUt, context)
        if (sunAltitude < 0.0) return null
        val azimuth = if (target == RashdulQiblaResult.Target.SHADOW) {
            Angle.normalizeDegrees(sunAzimuth + 180.0)
        } else {
            sunAzimuth
        }
        return signedAngleDiff(azimuth, qibla)
    }
}

