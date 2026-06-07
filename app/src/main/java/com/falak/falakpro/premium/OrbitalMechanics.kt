package com.falak.falakpro.premium

import kotlin.math.*

/**
 * OrbitalMechanics.kt
 * Presesi, Aberasi, Sudut Parallaktik, Ekuinoks & Solstis
 * Referensi: Meeus Ch. 21 (Presesi), Ch. 23 (Aberasi),
 *            Ch. 13 (Parallaktik), Ch. 27 (Ekuinoks/Solstis)
 */
object OrbitalMechanics {

    // ─────────────────────────────────────────────────────────────────────────
    // A. PRESESI (Meeus Ch. 21 — IAU 1976 / Lieske)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Presesi koordinat ekuatorial dari epoch T0 ke epoch T
     * (Standard: J2000.0 = T0, target date = T dalam Julian Centuries)
     *
     * Menggunakan konstanta presesi Lieske (1979) — standar IAU 1976
     *
     * @param ra0    RA awal (derajat)
     * @param dec0   Dec awal (derajat)
     * @param T0     Epoch awal (Julian Centuries dari J2000.0), biasanya 0.0
     * @param T      Epoch target (Julian Centuries dari J2000.0)
     * @return Pair(ra_prec, dec_prec) dalam derajat
     */
    fun precessEquatorial(ra0: Double, dec0: Double, T0: Double, T: Double): Pair<Double, Double> {
        val t = T - T0  // interval dalam Julian Centuries

        // Konstanta presesi (arcsec)
        val zeta_A  = (2306.2181 + 1.39656 * T0 - 0.000139 * T0 * T0) * t +
                      (0.30188 - 0.000344 * T0) * t * t + 0.017998 * t * t * t
        val z_A     = (2306.2181 + 1.39656 * T0 - 0.000139 * T0 * T0) * t +
                      (1.09468 + 0.000066 * T0) * t * t + 0.018203 * t * t * t
        val theta_A = (2004.3109 - 0.85330 * T0 - 0.000217 * T0 * T0) * t -
                      (0.42665 + 0.000217 * T0) * t * t - 0.041775 * t * t * t

        // Konversi ke radian
        val zetaR  = Math.toRadians(zeta_A / 3600.0)
        val zA     = Math.toRadians(z_A / 3600.0)
        val thetaR = Math.toRadians(theta_A / 3600.0)

        val ra0R  = Math.toRadians(ra0 + zeta_A / 3600.0)
        val dec0R = Math.toRadians(dec0)

        val A = cos(dec0R) * sin(ra0R)
        val B = cos(thetaR) * cos(dec0R) * cos(ra0R) - sin(thetaR) * sin(dec0R)
        val C = sin(thetaR) * cos(dec0R) * cos(ra0R) + cos(thetaR) * sin(dec0R)

        val ra  = (Math.toDegrees(atan2(A, B)) + z_A / 3600.0).mod(360.0)
        val dec = Math.toDegrees(asin(C))

        return Pair(ra, dec)
    }

    /**
     * Presesi koordinat ekliptik dari J2000.0 ke epoch T
     * @return Pair(lambda_prec, beta_prec) dalam derajat
     */
    fun precessEcliptic(lambda0: Double, beta0: Double, T: Double): Pair<Double, Double> {
        val eta  = (47.0029 - 0.06603 * T + 0.000598 * T * T) * T / 3600.0
        val Pi0  = 174.876384 + (3289.4789 * T + 0.60622 * T * T) / 3600.0
        val p0   = (5029.0966 + 2.22226 * T - 0.000042 * T * T) * T / 3600.0

        val etaR = Math.toRadians(eta)
        val Pi0R = Math.toRadians(Pi0)
        val l0R  = Math.toRadians(lambda0)
        val b0R  = Math.toRadians(beta0)
        val pR   = Math.toRadians(Pi0 + p0)

        val A = cos(etaR) * cos(b0R) * sin(Pi0R - l0R) - sin(etaR) * sin(b0R)
        val B = cos(b0R) * cos(Pi0R - l0R)
        val C = cos(etaR) * sin(b0R) + sin(etaR) * cos(b0R) * sin(Pi0R - l0R)

        val lambda = (Math.toDegrees(atan2(A, B)) + pR).mod(360.0)
        val beta   = Math.toDegrees(asin(C))

        return Pair(lambda, beta)
    }

    /**
     * Presesi sederhana: Koreksi RA & Dec untuk proper motion bintang
     * @param ra0, dec0  Posisi epoch J2000.0
     * @param muRa   Proper motion RA (arcsec/tahun)
     * @param muDec  Proper motion Dec (arcsec/tahun)
     * @param years  Jumlah tahun dari J2000.0
     */
    fun applyProperMotion(ra0: Double, dec0: Double, muRa: Double, muDec: Double, years: Double): Pair<Double, Double> {
        val ra  = ra0  + (muRa  / 3600.0) * years / cos(Math.toRadians(dec0))
        val dec = dec0 + (muDec / 3600.0) * years
        return Pair(ra.mod(360.0), dec)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // B. ABERASI TAHUNAN (Meeus Ch. 23)
    // ─────────────────────────────────────────────────────────────────────────

    data class AberrationCorrection(
        val deltaRa: Double,   // Koreksi RA (derajat)
        val deltaDec: Double   // Koreksi Dec (derajat)
    )

    /**
     * Aberasi Tahunan untuk Matahari/Planet (Meeus Ch. 23)
     * Menggunakan metode tepat (rigorous method)
     *
     * @param ra    RA geosentrik (derajat)
     * @param dec   Deklinasi geosentrik (derajat)
     * @param jde   Julian Day Efemeris
     * @param eps   Obliquitas sejati (derajat)
     * @param sunLon Longitude Matahari (derajat) — dari VSOP87
     * @param sunDist Jarak Bumi-Matahari (AU)
     * @return AberrationCorrection dalam derajat
     */
    fun aberrationEquatorial(
        ra: Double, dec: Double, jde: Double,
        eps: Double, sunLon: Double, sunDist: Double
    ): AberrationCorrection {
        val T = (jde - 2451545.0) / 36525.0
        val kappa = 20.49552 / 3600.0  // konstanta aberasi (derajat)
        val e = 0.016708617 - T * (0.000042037 + T * 0.0000001236)
        val pi0 = Math.toRadians(102.93735 + T * (1.71946 + T * 0.00046))  // perihelion

        val raR  = Math.toRadians(ra)
        val decR = Math.toRadians(dec)
        val epsR = Math.toRadians(eps)
        val sunR = Math.toRadians(sunLon)

        val dRa  = (-kappa * (cos(raR) * cos(sunR) * cos(epsR) + sin(raR) * sin(sunR)) / cos(decR) +
                    e * kappa * (cos(raR) * cos(pi0) * cos(epsR) + sin(raR) * sin(pi0)) / cos(decR))
        val dDec = (-kappa * (cos(sunR) * cos(epsR) * (tan(epsR) * cos(decR) - sin(raR) * sin(decR)) + cos(raR) * sin(decR) * sin(sunR)) +
                    e * kappa * (cos(pi0) * cos(epsR) * (tan(epsR) * cos(decR) - sin(raR) * sin(decR)) + cos(raR) * sin(decR) * sin(pi0)))

        return AberrationCorrection(dRa, dDec)
    }

    /**
     * Aberasi Tahunan dalam Koordinat Ekliptik (Meeus Eq. 23.2)
     * @return Pair(deltaLambda_deg, deltaBeta_deg)
     */
    fun aberrationEcliptic(
        lambda: Double, beta: Double, sunLon: Double,
        sunDist: Double, T: Double
    ): Pair<Double, Double> {
        val kappa = 20.49552 / 3600.0  // derajat
        val e  = 0.016708617 - T * (0.000042037 + T * 0.0000001236)
        val pi0 = 102.93735 + T * (1.71946 + T * 0.00046)

        val lR   = Math.toRadians(lambda)
        val bR   = Math.toRadians(beta)
        val sunR = Math.toRadians(sunLon)
        val pi0R = Math.toRadians(pi0)

        val dL = (-kappa * cos(sunR - lR) + e * kappa * cos(pi0R - lR)) / cos(bR)
        val dB = -kappa * sin(bR) * (sin(sunR - lR) - e * sin(pi0R - lR))

        return Pair(dL, dB)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // C. SUDUT PARALLAKTIK (Meeus Ch. 13)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sudut Parallaktik (q)
     * Sudut dari zenit ke north celestial pole, diukur di posisi benda
     * Positif = benda condong ke barat
     *
     * @param ha    Hour Angle (derajat)
     * @param dec   Deklinasi (derajat)
     * @param lat   Lintang pengamat (derajat)
     * @return Sudut parallaktik (derajat)
     */
    fun parallacticAngle(ha: Double, dec: Double, lat: Double): Double {
        val haR  = Math.toRadians(ha)
        val decR = Math.toRadians(dec)
        val latR = Math.toRadians(lat)
        return Math.toDegrees(
            atan2(sin(haR), cos(decR) * tan(latR) - sin(decR) * cos(haR))
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // D. EKUINOKS & SOLSTIS (Meeus Ch. 27)
    // ─────────────────────────────────────────────────────────────────────────

    enum class Season { SPRING_EQUINOX, SUMMER_SOLSTICE, AUTUMN_EQUINOX, WINTER_SOLSTICE }

    /**
     * Hitung JDE Ekuinoks / Solstis pada tahun tertentu
     * Presisi ~1 menit untuk tahun 1000-3000
     *
     * @param year   Tahun Masehi
     * @param season Musim/Ekuinoks/Solstis yang dicari
     * @return JDE
     */
    fun equinoxSolstice(year: Int, season: Season): Double {
        val JDE0 = if (year < 1000) {
            val Y = year / 1000.0
            when (season) {
                Season.SPRING_EQUINOX  -> 1721139.2855 + Y * (365242.1376 + Y * (0.06679 + Y * (-0.00150 + Y * 0.00000)))
                Season.SUMMER_SOLSTICE -> 1721233.2486 + Y * (365241.7436 + Y * (-0.05933 + Y * (-0.00892 + Y * 0.00025)))
                Season.AUTUMN_EQUINOX  -> 1721325.6978 + Y * (365242.4900 + Y * (-0.11767 + Y * (-0.00377 + Y * 0.00025)))
                Season.WINTER_SOLSTICE -> 1721414.3920 + Y * (365242.8739 + Y * (-0.06697 + Y * (-0.00844 + Y * 0.00000)))
            }
        } else {
            val Y = (year - 2000) / 1000.0
            when (season) {
                Season.SPRING_EQUINOX  -> 2451623.80984 + Y * (365242.37404 + Y * (0.05169 + Y * (-0.00411 + Y * (-0.00057))))
                Season.SUMMER_SOLSTICE -> 2451716.56767 + Y * (365241.62603 + Y * (0.00325 + Y * (0.00888 + Y * (-0.00030))))
                Season.AUTUMN_EQUINOX  -> 2451810.21715 + Y * (365242.01767 + Y * (-0.11575 + Y * (0.00337 + Y * (0.00078))))
                Season.WINTER_SOLSTICE -> 2451900.05952 + Y * (365242.74049 + Y * (-0.06223 + Y * (-0.00823 + Y * (0.00032))))
            }
        }

        // Koreksi periodik (Tabel 27.c Meeus)
        val T = (JDE0 - 2451545.0) / 36525.0
        val W = 35999.373 * T - 2.47
        val dLambda = 1 + 0.0334 * cos(Math.toRadians(W)) + 0.0007 * cos(Math.toRadians(2 * W))

        val S = periodicTermsEquinox(T)
        return JDE0 + 0.00001 * S / dLambda
    }

    private fun periodicTermsEquinox(T: Double): Double {
        val data = arrayOf(
            doubleArrayOf(485.0, 324.96,   1934.136),
            doubleArrayOf(203.0, 337.23,  32964.467),
            doubleArrayOf(199.0, 342.08,     20.186),
            doubleArrayOf(182.0,  27.85, 445267.112),
            doubleArrayOf(156.0,  73.14,  45036.886),
            doubleArrayOf(136.0, 171.52,  22518.443),
            doubleArrayOf( 77.0, 222.54,  65928.934),
            doubleArrayOf( 74.0, 296.72,   3034.906),
            doubleArrayOf( 70.0, 243.58,   9037.513),
            doubleArrayOf( 58.0, 119.81,  33718.147),
            doubleArrayOf( 52.0, 297.17,    150.678),
            doubleArrayOf( 50.0,  21.02,   2281.226),
            doubleArrayOf( 45.0, 247.54,  29929.562),
            doubleArrayOf( 44.0, 325.15,  31555.956),
            doubleArrayOf( 29.0,  60.93,   4443.417),
            doubleArrayOf( 18.0, 155.12,  67555.328),
            doubleArrayOf( 17.0, 288.79,   4562.452),
            doubleArrayOf( 16.0, 198.04,  62894.029),
            doubleArrayOf( 14.0, 199.76,  31436.921),
            doubleArrayOf( 12.0,  95.39,  14577.848),
            doubleArrayOf( 10.0, 287.11,  31931.756),
            doubleArrayOf( 10.0, 320.81,  34777.259),
            doubleArrayOf(  8.0, 227.73,   1222.114),
            doubleArrayOf(  6.0,  15.45,  16859.074)
        )
        return data.sumOf { it[0] * cos(Math.toRadians(it[1] + it[2] * T)) }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // E. FK5 KOREKSI (Meeus Ch. 25, Eq. 25.9)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Koreksi FK5 untuk longitude ekliptik geosentrik Matahari
     * Diterapkan setelah konversi dari heliocentric VSOP87
     *
     * @param theta Longitude geosentrik (derajat) sebelum FK5
     * @param beta  Latitude geosentrik (derajat) sebelum FK5
     * @param T     Julian Centuries
     * @return Pair(theta_corrected, beta_corrected)
     */
    fun applyFK5(theta: Double, beta: Double, T: Double): Pair<Double, Double> {
        val lPrime = Math.toRadians(theta - 1.397 * T - 0.00031 * T * T)
        val dTheta = (-0.09033 + 0.03916 * (cos(lPrime) - sin(lPrime))) / 3600.0
        val dBeta  = (0.03916 * (cos(lPrime) - sin(lPrime))) / 3600.0
        return Pair(theta + dTheta, beta + dBeta)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // F. PERSAMAAN KEPLER (Meeus Ch. 30)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Selesaikan Persamaan Kepler: M = E - e*sin(E)
     * Iterasi Newton-Raphson
     *
     * @param M Mean Anomaly (radian)
     * @param e Eksentrisitas orbit (0 ≤ e < 1)
     * @return E Eccentric Anomaly (radian)
     */
    fun solveKepler(M: Double, e: Double): Double {
        var E = M  // tebakan awal
        repeat(100) {
            val dE = (M - E + e * sin(E)) / (1 - e * cos(E))
            E += dE
            if (abs(dE) < 1e-12) return E
        }
        return E
    }

    /**
     * Selesaikan Persamaan Kepler eksentrisitas tinggi (e ≥ 0.98)
     * Metode Laguerre (Meeus Ch. 30)
     */
    fun solveKeplerHighEcc(M: Double, e: Double): Double {
        var E = M
        repeat(100) {
            val f0 = E - e * sin(E) - M
            val f1 = 1 - e * cos(E)
            val f2 = e * sin(E)
            val d1 = -f0 / f1
            val d2 = -f0 / (f1 + d1 * f2 / 2)
            val d3 = -f0 / (f1 + d2 * f2 / 2 + d2 * d2 * (-f1) / 6)
            E += d3
            if (abs(d3) < 1e-12) return E
        }
        return E
    }

    /**
     * Hitung posisi dari elemen orbit eliptik
     * @param a  Semi-major axis (AU)
     * @param e  Eksentrisitas
     * @param M  Mean anomaly (derajat)
     * @return Pair(trueAnomaly_deg, radius_AU)
     */
    fun ellipticPosition(a: Double, e: Double, M: Double): Pair<Double, Double> {
        val Mrad = Math.toRadians(M.mod(360.0))
        val E    = solveKepler(Mrad, e)
        val v    = 2.0 * atan2(sqrt(1 + e) * sin(E / 2), sqrt(1 - e) * cos(E / 2))
        val r    = a * (1 - e * cos(E))
        return Pair(Math.toDegrees(v).mod(360.0), r)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // G. PEMISAHAN SUDUT & POSISI SUDUT (Meeus Ch. 17, 18)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Position Angle dari benda 1 ke benda 2
     * 0° = ke Utara, 90° = ke Timur
     */
    fun positionAngle(ra1: Double, dec1: Double, ra2: Double, dec2: Double): Double {
        val dRa  = Math.toRadians(ra2 - ra1)
        val dec1R = Math.toRadians(dec1)
        val dec2R = Math.toRadians(dec2)
        return Math.toDegrees(atan2(sin(dRa), cos(dec1R) * tan(dec2R) - sin(dec1R) * cos(dRa))).mod(360.0)
    }

    /**
     * Koreksi parallaks geodetik Bumi (Meeus Ch. 11)
     * ρ sin φ' dan ρ cos φ' untuk pengamat di permukaan
     *
     * @param latGeographic Lintang geografis (derajat)
     * @param heightM       Tinggi (meter)
     * @return Pair(rho_sin_phi_prime, rho_cos_phi_prime)
     */
    fun earthParallaxFactors(latGeographic: Double, heightM: Double): Pair<Double, Double> {
        val latR = Math.toRadians(latGeographic)
        val u    = atan(0.99664719 * tan(latR))
        val rhoSin = 0.99664719 * sin(u) + (heightM / 6378140.0) * sin(latR)
        val rhoCos = cos(u) + (heightM / 6378140.0) * cos(latR)
        return Pair(rhoSin, rhoCos)
    }
}
