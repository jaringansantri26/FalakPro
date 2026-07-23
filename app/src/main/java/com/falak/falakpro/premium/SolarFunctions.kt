package com.falak.falakpro.premium

import kotlin.math.*

/**
 * SolarFunctions — Fungsi Lengkap Posisi Matahari
 * Referensi: Jean Meeus, "Astronomical Algorithms" 2nd Ed.
 * Ch. 7 (JDE), Ch. 10 (ΔT), Ch. 12 (ST), Ch. 22 (Nutation),
 * Ch. 25 (VSOP87), Ch. 26 (EoT), Ch. 27 (Physical Ephemeris)
 */
object SolarFunctions {

    // ── Ch. 7: Julian Day ─────────────────────────────────────────────────────

    /** Gregorian → JDE (jam dalam UT) */
    fun gregorianToJde(year: Int, month: Int, day: Int, hourUT: Double = 0.0): Double {
        var y = year; var m = month
        if (m <= 2) { y--; m += 12 }
        val A = y / 100
        val B = 2 - A + A / 4
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + B - 1524.5 + hourUT / 24.0
    }

    /** JDE → Gregorian */
    data class GregorianDate(val year: Int, val month: Int, val day: Int, val hourUT: Double)
    fun jdeToGregorian(jde: Double): GregorianDate {
        val z = floor(jde + 0.5).toInt()
        val f = (jde + 0.5) - z
        val alpha = floor((z - 1867216.25) / 36524.25).toInt()
        val a = z + 1 + alpha - alpha / 4
        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toInt()
        val d = floor(365.25 * c).toInt()
        val e = floor((b - d) / 30.6001).toInt()
        val dayFrac = b - d - floor(30.6001 * e) + f
        val day   = dayFrac.toInt()
        val hourUT = (dayFrac - day) * 24.0
        val month = if (e < 14) e - 1 else e - 13
        val year  = if (month > 2) c - 4716 else c - 4715
        return GregorianDate(year, month, day, hourUT)
    }

    /** Hari dalam seminggu: 0=Ahad,1=Senin,...,6=Sabtu */
    fun dayOfWeek(jde: Double): Int = ((jde + 1.5).toLong() % 7).toInt()

    // ── Ch. 10: Delta T ───────────────────────────────────────────────────────

    // ── Ch. 12: Sidereal Time ─────────────────────────────────────────────────

    /** Greenwich Mean Sidereal Time (derajat) */
    fun greenwichMeanSiderealTime(jde: Double): Double {
        val T = (jde - 2451545.0) / 36525.0
        val gmst = 280.46061837 + 360.98564736629 * (jde - 2451545.0) + 0.000387933 * T * T - T * T * T / 38710000.0
        return (gmst).mod(360.0)
    }

    /** Greenwich Apparent Sidereal Time (derajat) */
    fun greenwichApparentSiderealTime(jde: Double): Double {
        val gmst = greenwichMeanSiderealTime(jde)
        val nutation = AstroDataUtils.calculateNutation(jde).first
        val epsilon = AstroDataUtils.calculateTrueObliquity(jde)
        return (gmst + nutation * cos(Math.toRadians(epsilon))).mod(360.0)
    }

    // ── Ch. 26: Equation of Time ──────────────────────────────────────────────

    /** Persamaan Waktu (menit) */
    fun equationOfTime(jde: Double): Double {
        val T = (jde - 2451545.0) / 36525.0
        val L0 = (280.46607 + 36000.76908 * T).mod(360.0)
        val M = (357.52911 + 35999.05029 * T).mod(360.0)
        val e = 0.016708634 - 0.000042037 * T
        val epsilon = Iau2006Nutation.meanObliquityDeg(jde)
        val y = tan(Math.toRadians(epsilon / 2.0)).pow(2)
        
        val eot = y * sin(2 * Math.toRadians(L0)) - 
                  2 * e * sin(Math.toRadians(M)) + 
                  4 * e * y * sin(Math.toRadians(M)) * cos(2 * Math.toRadians(L0)) - 
                  0.5 * y * y * sin(4 * Math.toRadians(L0)) - 
                  1.25 * e * e * sin(2 * Math.toRadians(M))
                  
        return Math.toDegrees(eot) * 4.0 // derajat ke menit (1 deg = 4 min)
    }

    // ── Ch. 15 / 25: Rise, Transit, Set ───────────────────────────────────────

    data class RiseTransitSet(val rise: Double?, val transit: Double, val set: Double?)

    /**
     * Hitung Rise/Transit/Set Matahari
     * @param jde0  JDE pada 0h UT tanggal yang dihitung
     * @param lat   Lintang pengamat (derajat)
     * @param lon   Bujur pengamat (derajat, positif = Timur)
     * @param elev  Ketinggian pengamat (meter)
     */
    fun riseTransitSet(jde0: Double, lat: Double, lon: Double, elev: Double,
                       ra1: Double, dec1: Double,
                       ra2: Double, dec2: Double,
                       ra3: Double, dec3: Double): RiseTransitSet {
        // AA Ch. 15: standard altitude for apparent upper-limb setting.
        val sdS = 16.0 / 60.0
        val rfS = AstroTransform.AA_HORIZON_REFRACTION_DEG
        val dip = abs(AstroTransform.dipCorrection(elev))
        val h0 = 0.0 - sdS - rfS - dip
        
        val cosH0 = (sin(Math.toRadians(h0)) - sin(Math.toRadians(lat)) * sin(Math.toRadians(dec2))) /
                (cos(Math.toRadians(lat)) * cos(Math.toRadians(dec2)))

        if (cosH0 < -1) return RiseTransitSet(null, 0.0, null)
        if (cosH0 > 1)  return RiseTransitSet(null, 0.0, null)

        val H0 = Math.toDegrees(acos(cosH0))
        val gmst0 = greenwichMeanSiderealTime(jde0)

        // For East positive longitude, the mathematically correct formula is (ra - lon - gmst0) / 360
        var m0 = ((ra2 - lon - gmst0) / 360.0).mod(1.0)
        var m1 = (m0 - H0 / 360.0).mod(1.0)
        var m2 = (m0 + H0 / 360.0).mod(1.0)

        fun interp(y1: Double, y2: Double, y3: Double, n: Double): Double {
            val a = y2 - y1; val b = y3 - y2; val c = b - a
            return y2 + n * (a + b + n * c) / 2.0
        }

        fun correctM(m: Double): Double {
            val theta0 = (gmst0 + 360.985647 * m).mod(360.0)
            val jdeCurrent = jde0 + m
            val dltT = DynamicalTimeEngine.deltaT(jdeCurrent)
            val n    = m + dltT / 86400.0
            val raI  = interp(ra1, ra2, ra3, n)
            val decI = interp(dec1, dec2, dec3, n)
            val ha   = theta0 + lon - raI // East is positive
            val (alt, _) = AstroTransform.equatorialToHorizontal(ha, decI, lat)
            val dm = (alt - h0) / (360.0 * cos(Math.toRadians(decI)) * cos(Math.toRadians(lat)) * sin(Math.toRadians(ha)))
            return m + dm
        }

        fun correctMTransit(m: Double): Double {
            val theta0 = (gmst0 + 360.985647 * m).mod(360.0)
            val jdeCurrent = jde0 + m
            val dltT = DynamicalTimeEngine.deltaT(jdeCurrent)
            val n    = m + dltT / 86400.0
            val raI  = interp(ra1, ra2, ra3, n)
            var ha   = (theta0 + lon - raI).mod(360.0) // East is positive
            if (ha > 180.0) ha -= 360.0
            return m - ha / 360.0
        }

        m1 = m1.let { if (it < 0) it + 1.0 else if (it > 1) it - 1.0 else it }
        m2 = m2.let { if (it < 0) it + 1.0 else if (it > 1) it - 1.0 else it }

        m0 = correctMTransit(m0)
        m1 = correctM(m1)
        m2 = correctM(m2)

        fun finalizeM(m: Double): Double = (m).mod(1.0)
        return RiseTransitSet(finalizeM(m1) * 24.0, finalizeM(m0) * 24.0, finalizeM(m2) * 24.0)
    }

    // ── Ch. 16: Atmospheric Refraction ────────────────────────────────────────

    fun refractionBennett(altGeometric: Double): Double {
        if (altGeometric < -1.0) return 0.0
        return AstroTransform.atmosphericRefraction(altGeometric)
    }

    fun refractionSaemundsson(altApparent: Double): Double {
        if (altApparent < -0.5) return 0.0
        return (1.02 / tan(Math.toRadians(altApparent + 10.3 / (altApparent + 5.11))) + 0.0019279) / 60.0
    }

    fun refractionWithAtmosphere(altGeometric: Double, tempC: Double = 10.0, pressureMbar: Double = 1010.0): Double {
        val f = pressureMbar / 1010.0 * 283.0 / (273.0 + tempC)
        return AstroTransform.atmosphericRefraction(altGeometric) * f
    }

    // ── Twilight ──────────────────────────────────────────────────────────────

    enum class TwilightType(val altitude: Double) {
        CIVIL(-6.0), NAUTICAL(-12.0), ASTRONOMICAL(-18.0)
    }

    fun twilight(jde0: Double, lat: Double, lon: Double, type: TwilightType,
                 ra1: Double, dec1: Double, ra2: Double, dec2: Double,
                 ra3: Double, dec3: Double): Pair<Double?, Double?> {
        val rts = riseTransitSet(jde0, lat, lon, ra1, dec1, ra2, dec2, ra3, dec3, type.altitude)
        return Pair(rts.rise, rts.set)
    }

    // ── Kiblat ────────────────────────────────────────────────────────────────

    fun qiblaDirection(lat: Double, lon: Double): Double {
        val latMakkah = Math.toRadians(21.4225); val lonMakkah = Math.toRadians(39.8262)
        val latObs    = Math.toRadians(lat); val dLon      = lonMakkah - Math.toRadians(lon)
        val y = sin(dLon) * cos(latMakkah)
        val x = cos(latObs) * sin(latMakkah) - sin(latObs) * cos(latMakkah) * cos(dLon)
        return (Math.toDegrees(atan2(y, x)) + 360.0).mod(360.0)
    }

    fun rashdulQiblah(jde0: Double, lat: Double, lon: Double): Double? {
        val sol = computeLowAccuracy(jde0); val qib = qiblaDirection(lat, lon)
        val latR = Math.toRadians(lat); val decR = Math.toRadians(sol.dec); val qibR = Math.toRadians(qib)
        val cosT = (sin(qibR) * cos(decR) - cos(qibR) * sin(latR) * cos(decR)) / (cos(latR) * cos(decR) * sin(qibR))
        if (cosT < -1 || cosT > 1) return null
        val T_ha = Math.toDegrees(acos(cosT)); val eot = equationOfTime(jde0) / 60.0
        val transit = 12.0 - eot - lon / 15.0
        return transit - T_ha / 15.0
    }

    // ── Low Accuracy Solar Position (Meeus Ch. 22) ────────────────────────────

    data class SolarPos(val ra: Double, val dec: Double, val distanceAU: Double)
    fun computeLowAccuracy(jde: Double): SolarPos {
        val T = (jde - 2451545.0) / 36525.0
        val L0 = (280.46646 + 36000.76983 * T + 0.0003032 * T * T).mod(360.0)
        val M = (357.52911 + 35999.05029 * T - 0.0001537 * T * T).mod(360.0)
        val e = 0.016708634 - 0.000042037 * T
        val C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * sin(Math.toRadians(M)) + 
                (0.019993 - 0.000101 * T) * sin(Math.toRadians(2 * M)) + 
                0.000289 * sin(Math.toRadians(3 * M))
        val lambda = L0 + C
        val epsilon0 = 23.4392911 - (46.8150 * T + 0.00059 * T * T - 0.001813 * T * T * T) / 3600.0
        val epsilon = epsilon0 + 0.00256 * cos(Math.toRadians(125.04 - 1934.136 * T))
        val ra = Math.toDegrees(atan2(cos(Math.toRadians(epsilon)) * sin(Math.toRadians(lambda)), cos(Math.toRadians(lambda))))
        val dec = Math.toDegrees(asin(sin(Math.toRadians(epsilon)) * sin(Math.toRadians(lambda))))
        val r = (1.000001018 * (1 - e * e)) / (1 + e * cos(Math.toRadians(M + C)))
        return SolarPos((ra).mod(360.0), dec, r)
    }

    // ── Meeus Ch. 17: Angular Separation ──────────────────────────────────────

    fun angularSeparation(ra1: Double, dec1: Double, ra2: Double, dec2: Double): Double {
        val d1 = Math.toRadians(dec1); val d2 = Math.toRadians(dec2); val dr = Math.toRadians(ra2 - ra1)
        return Math.toDegrees(acos(sin(d1) * sin(d2) + cos(d1) * cos(d2) * cos(dr)))
    }

    // ── Meeus Ch. 40: Solar Parallax ──────────────────────────────────────────

    fun solarEquatorialHorizontalParallax(distAU: Double): Double = AstroTransform.solarParallax(distAU)

    fun solarTransit(jde0: Double, lon: Double): Double {
        val eot = equationOfTime(jde0) / 60.0
        return 12.0 - lon / 15.0 - eot
    }

    fun solarSemidiameter(distAU: Double): Double = (959.63 / 3600.0) / distAU
}
