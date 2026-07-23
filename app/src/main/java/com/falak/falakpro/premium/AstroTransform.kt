package com.falak.falakpro.premium

import kotlin.math.*

/**
 * AstroTransform — Transformasi Koordinat Astronomi
 * Ekliptik ↔ Ekuatorial ↔ Horizontal
 */
object AstroTransform {
    const val AA_EARTH_EQUATORIAL_RADIUS_KM = 6378.14
    const val AA_EARTH_EQUATORIAL_RADIUS_M = 6378140.0
    const val AA_HORIZON_REFRACTION_DEG = 34.0 / 60.0
    const val AA_MOON_HORIZON_REFRACTION_DEG = 34.5 / 60.0
    const val AA_DIP_DEG_PER_SQRT_M = 0.0293

    /**
     * Konversi Koordinat Ekliptik → Ekuatorial
     * @param lambda Longitude Ekliptik (derajat)
     * @param beta   Latitude Ekliptik (derajat)
     * @param epsilon Obliquitas Sejati (derajat)
     * @return Pair(RA_degrees, Dec_degrees)
     */
    fun eclipticToEquatorial(lambda: Double, beta: Double, epsilon: Double): Pair<Double, Double> {
        val lRad = Math.toRadians(lambda)
        val bRad = Math.toRadians(beta)
        val eRad = Math.toRadians(epsilon)

        val sinDec = sin(bRad) * cos(eRad) + cos(bRad) * sin(eRad) * sin(lRad)
        val dec = Math.toDegrees(asin(sinDec))

        val y = sin(lRad) * cos(eRad) - tan(bRad) * sin(eRad)
        val x = cos(lRad)
        var ra = Math.toDegrees(atan2(y, x))
        if (ra < 0) ra += 360.0

        return Pair(ra, dec)
    }

    /**
     * Hitung Greenwich Apparent Sidereal Time (GAST) dalam derajat
     * @param jde Julian Day Efemeris
     * @param deltaPsi_deg Nutasi Longitude (derajat)
     * @param epsilon_deg  Obliquitas Sejati (derajat)
     */
    fun greenwichApparentSiderealTime(jde: Double, deltaPsi_deg: Double, epsilon_deg: Double): Double {
        val T = (jde - 2451545.0) / 36525.0
        // Greenwich Mean Sidereal Time (derajat)
        var theta0 = 280.46061837 + 360.98564736629 * (jde - 2451545.0) +
                0.000387933 * T * T - T * T * T / 38710000.0
        theta0 = theta0.mod(360.0)
        // Equation of Equinoxes koreksi
        val eqEq = deltaPsi_deg * cos(Math.toRadians(epsilon_deg))
        return (theta0 + eqEq).mod(360.0)
    }

    /**
     * Hitung Hour Angle (HA) dari RA dan GAST
     */
    fun hourAngle(gast_deg: Double, ra_deg: Double, lon_deg: Double): Double {
        return (gast_deg + lon_deg - ra_deg).mod(360.0)
    }

    /**
     * Konversi Ekuatorial → Horizontal (Altitude & Azimuth)
     * @param ha_deg  Hour Angle (derajat)
     * @param dec_deg Deklinasi (derajat)
     * @param lat_deg Lintang tempat (derajat, positif = Utara)
     * @return Pair(altitude_deg, azimuth_deg)
     */
    fun equatorialToHorizontal(ha_deg: Double, dec_deg: Double, lat_deg: Double): Pair<Double, Double> {
        val haRad  = Math.toRadians(ha_deg)
        val decRad = Math.toRadians(dec_deg)
        val latRad = Math.toRadians(lat_deg)

        val sinAlt = sin(decRad) * sin(latRad) + cos(decRad) * cos(latRad) * cos(haRad)
        val alt = Math.toDegrees(asin(sinAlt))

        val y = -sin(haRad) * cos(decRad)
        val x = sin(decRad) * cos(latRad) - cos(decRad) * sin(latRad) * cos(haRad)
        var az = Math.toDegrees(atan2(y, x))
        if (az < 0) az += 360.0

        return Pair(alt, az)
    }

    /**
     * Koreksi Paralaks Toposentrik untuk Bulan
     * Menggeser posisi Bulan dari pusat Bumi ke lokasi pengamat
     * @param ra_deg   RA ekuatorial geosentrik (derajat)
     * @param dec_deg  Dec ekuatorial geosentrik (derajat)
     * @param hp_deg   Horizontal Parallax Bulan (derajat)
     * @param ha_deg   Hour Angle (derajat)
     * @param lat_deg  Lintang pengamat (derajat)
     * @param heightM  Tinggi pengamat di atas permukaan laut (meter)
     * @return Pair(deltaRa_deg, deltaDec_deg) — koreksi yang ditambahkan ke posisi geosentrik
     */
    fun lunarTopocentricCorrection(
        ra_deg: Double, dec_deg: Double, hp_deg: Double,
        ha_deg: Double, lat_deg: Double, heightM: Double
    ): Pair<Double, Double> {
        val hpRad  = Math.toRadians(hp_deg)
        val haRad  = Math.toRadians(ha_deg)
        val latRad = Math.toRadians(lat_deg)
        val decRad = Math.toRadians(dec_deg)

        // Koreksi bentuk Bumi (oblateness)
        val u = atan(0.99664719 * tan(latRad))
        val rhoSinPhi = 0.99664719 * sin(u) + (heightM / AA_EARTH_EQUATORIAL_RADIUS_M) * sin(latRad)
        val rhoCosPhi = cos(u) + (heightM / AA_EARTH_EQUATORIAL_RADIUS_M) * cos(latRad)

        val sinHp = sin(hpRad)

        // Koreksi RA
        val deltaRaNumerator = -rhoCosPhi * sinHp * sin(haRad)
        val deltaRaDenom = cos(decRad) - rhoCosPhi * sinHp * cos(haRad)
        val deltaRa = Math.toDegrees(atan2(deltaRaNumerator, deltaRaDenom))

        // Koreksi Deklinasi
        val decTopoRad = atan2(
            (sin(decRad) - rhoSinPhi * sinHp) * cos(Math.toRadians(deltaRa)),
            cos(decRad) - rhoCosPhi * sinHp * cos(haRad)
        )
        val deltaDec = Math.toDegrees(decTopoRad) - dec_deg

        return Pair(deltaRa, deltaDec)
    }

    /**
     * Koreksi Refraksi Atmosfer untuk tinggi sejati/geometris.
     *
     * Meeus, Astronomical Algorithms Ch. 16 gives this Bennett form to convert
     * true altitude to apparent altitude. The inverse-looking 7.31/(h+4.4)
     * form is for apparent-to-true reduction, so do not use it here.
     *
     * @param altGeometric_deg Ketinggian geometris (derajat)
     * @return koreksi refraksi (derajat)
     */
    fun atmosphericRefraction(altGeometric_deg: Double): Double {
        val h = max(altGeometric_deg, -1.0)
        val refArcMin = 1.02 / tan(Math.toRadians(h + 10.3 / (h + 5.11)))
        return refArcMin / 60.0
    }

    /**
     * Koreksi Dip Ufuk (Inkhifadh al-Ufuq) - Meeus Standar
     * @param heightM Tinggi tempat (meter)
     * @return Dip dalam derajat (negatif)
     */
    fun dipCorrection(heightM: Double, usePersis: Boolean = false): Double {
        val coeff = if (usePersis) 2.1 else AA_DIP_DEG_PER_SQRT_M // Degrees: 0.0293 deg = 1.758' (AA Ch. 15)
        return -coeff * sqrt(max(0.0, heightM))
    }

    const val MEEUS_GHURUB_REFRACTION = AA_HORIZON_REFRACTION_DEG // Standard 34'

    /**
     * Koreksi Paralaks Matahari ke Toposentrik
     */
    fun solarParallax(distanceAU: Double): Double {
        return Math.toDegrees(asin(AA_EARTH_EQUATORIAL_RADIUS_KM / (distanceAU * 149597870.7)))
    }
}
