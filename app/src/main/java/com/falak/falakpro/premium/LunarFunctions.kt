package com.falak.falakpro.premium

import kotlin.math.*

/** Fungsi Bulan lengkap: Fase, Illuminasi, Perigee, Apogee, Node, Librasi (Meeus Ch.47-52) */
object LunarFunctions {

    // ── Ch.49: Fase Bulan ─────────────────────────────────────────────────────

    enum class LunarPhase(val k: Double) {
        NEW_MOON(0.0), FIRST_QUARTER(0.25), FULL_MOON(0.5), LAST_QUARTER(0.75)
    }

    /** Perkiraan k (nomor siklus) untuk fase terdekat sebelum/pada tahun+bulan */
    fun approximateK(year: Int, month: Int, phase: LunarPhase): Double {
        val k0 = (year + (month - 1) / 12.0 - 2000.0) * 12.3685
        return floor(k0) + phase.k
    }

    /**
     * JDE fase Bulan presisi tinggi (Meeus Ch.49)
     * k = bilangan bulat + fraksi fase (0=NM, 0.25=FQ, 0.5=FM, 0.75=LQ)
     */
    fun lunarPhaseJde(k: Double): Double {
        val T   = k / 1236.85
        val T2  = T * T; val T3 = T2 * T; val T4 = T3 * T

        var JDE = 2451550.09766 + 29.530588861 * k +
                T2 * (0.00015437 - T * (0.000000150 - T * 0.00000000073))

        val M   = Math.toRadians((2.5534 + 29.10535670 * k - T2 * (0.0000014 - T * 0.00000011)).mod(360.0))
        val Mp  = Math.toRadians((201.5643 + 385.81693528 * k + T2 * (0.0107582 + T * (0.00001238 - T * 0.000000058))).mod(360.0))
        val F   = Math.toRadians((160.7108 + 390.67050284 * k - T2 * (0.0016118 + T * (0.00000227 - T * 0.000000011))).mod(360.0))
        val Om  = Math.toRadians((124.7746 - 1.56375588 * k + T2 * (0.0020672 + T * 0.00000215)).mod(360.0))
        val E   = 1.0 - T * (0.002516 + T * 0.0000074)

        val frac = k - floor(k)
        return if (frac < 0.01 || frac > 0.99) {  // New Moon
            JDE + (-0.40720 * sin(Mp) + 0.17241 * E * sin(M) + 0.01608 * sin(2*Mp)
                    + 0.01039 * sin(2*F) + 0.00739 * E * sin(Mp - M) - 0.00514 * E * sin(Mp + M)
                    + 0.00208 * E * E * sin(2*M) - 0.00111 * sin(Mp - 2*F)
                    - 0.00057 * sin(Mp + 2*F) + 0.00056 * E * sin(2*Mp + M)
                    - 0.00042 * sin(3*Mp) + 0.00042 * E * sin(M + 2*F)
                    + 0.00038 * E * sin(M - 2*F) - 0.00024 * E * sin(2*Mp - M)
                    - 0.00017 * sin(Om) - 0.00007 * sin(Mp + 2*M) + 0.00004 * sin(2*Mp - 2*F)
                    + 0.00004 * sin(3*M) + 0.00003 * sin(Mp + M - 2*F)
                    + 0.00003 * sin(2*Mp + 2*F) - 0.00003 * sin(Mp + M + 2*F)
                    + 0.00003 * sin(Mp - M + 2*F) - 0.00002 * sin(Mp - M - 2*F)
                    - 0.00002 * sin(3*Mp + M) + 0.00002 * sin(4*Mp))
        } else if (abs(frac - 0.5) < 0.01) {  // Full Moon
            JDE + (-0.40614 * sin(Mp) + 0.17302 * E * sin(M) + 0.01614 * sin(2*Mp)
                    + 0.01043 * sin(2*F) + 0.00734 * E * sin(Mp - M) - 0.00515 * E * sin(Mp + M)
                    + 0.00209 * E * E * sin(2*M) - 0.00111 * sin(Mp - 2*F)
                    - 0.00057 * sin(Mp + 2*F) + 0.00056 * E * sin(2*Mp + M)
                    - 0.00042 * sin(3*Mp) + 0.00042 * E * sin(M + 2*F)
                    + 0.00038 * E * sin(M - 2*F) - 0.00024 * E * sin(2*Mp - M)
                    - 0.00017 * sin(Om) - 0.00007 * sin(Mp + 2*M) + 0.00004 * sin(2*Mp - 2*F)
                    + 0.00004 * sin(3*M) + 0.00003 * sin(Mp + M - 2*F)
                    + 0.00003 * sin(2*Mp + 2*F) - 0.00003 * sin(Mp + M + 2*F)
                    + 0.00003 * sin(Mp - M + 2*F) - 0.00002 * sin(Mp - M - 2*F)
                    - 0.00002 * sin(3*Mp + M) + 0.00002 * sin(4*Mp))
        } else {  // Quarters
            JDE + (-0.62801 * sin(Mp) + 0.17172 * E * sin(M) - 0.01183 * E * sin(Mp + M)
                    + 0.00862 * sin(2*Mp) + 0.00804 * sin(2*F) + 0.00454 * E * sin(Mp - M)
                    + 0.00204 * E * E * sin(2*M) - 0.00180 * sin(Mp - 2*F)
                    - 0.00070 * sin(Mp + 2*F) - 0.00040 * sin(3*Mp)
                    - 0.00034 * E * sin(2*Mp - M) + 0.00032 * E * sin(M + 2*F)
                    + 0.00032 * E * sin(M - 2*F) - 0.00028 * E * E * sin(Mp + 2*M)
                    + 0.00027 * E * sin(2*Mp + M) - 0.00017 * sin(Om) - 0.00005 * sin(Mp - M - 2*F)
                    + 0.00004 * sin(2*Mp + 2*F) - 0.00004 * sin(Mp + M + 2*F)
                    + 0.00004 * sin(Mp - 2*M) + 0.00003 * sin(Mp + M - 2*F)
                    + 0.00003 * sin(3*M) + 0.00002 * sin(2*Mp - 2*F)
                    + 0.00002 * sin(Mp - M + 2*F) - 0.00002 * sin(3*Mp + M))
        }
    }

    // ── Ch.48: Illuminasi Bulan ───────────────────────────────────────────────

    data class MoonIllumination(
        val phaseAngle: Double,      // Sudut fase i (derajat)
        val illuminatedFraction: Double, // Fraksi iluminasi k (0-1)
        val positionAngleBright: Double, // PA of bright limb χ (derajat)
        val positionAngleAxis: Double    // PA of rotation axis (derajat)
    )

    /**
     * Illuminasi Bulan (Meeus Ch.48)
     * @param sunRa, sunDec    RA/Dec Matahari (derajat)
     * @param moonRa, moonDec  RA/Dec Bulan (derajat)
     * @param sunDist          Jarak Bumi-Matahari (AU)
     * @param moonDist         Jarak Bumi-Bulan (km)
     */
    fun moonIllumination(
        sunRa: Double, sunDec: Double, sunDist: Double,
        moonRa: Double, moonDec: Double, moonDist: Double
    ): MoonIllumination {
        val sRa = Math.toRadians(sunRa); val sDec = Math.toRadians(sunDec)
        val mRa = Math.toRadians(moonRa); val mDec = Math.toRadians(moonDec)

        // Elongasi geosentrik
        val psi = acos(sin(sDec) * sin(mDec) + cos(sDec) * cos(mDec) * cos(sRa - mRa))

        // Sudut fase
        val distSunKm = sunDist * 149597870.7
        val i = atan2(distSunKm * sin(psi), moonDist - distSunKm * cos(psi))
        val k = (1 + cos(i)) / 2.0

        // Position angle of bright limb (χ)
        val chi = atan2(cos(sDec) * sin(sRa - mRa),
            sin(sDec) * cos(mDec) - cos(sDec) * sin(mDec) * cos(sRa - mRa))

        return MoonIllumination(Math.toDegrees(i), k, Math.toDegrees(chi), 0.0)
    }

    // ── Ch.50: Perigee dan Apogee ────────────────────────────────────────────

    data class PerigeeApogee(
        val jde: Double,          // Waktu (JDE)
        val distanceKm: Double,   // Jarak (km)
        val horizontalParallax: Double  // HP (derajat)
    )

    /** Nomor k untuk perigee/apogee terdekat pada tahun+bulan */
    fun approximateKPerigee(year: Int, month: Int): Double {
        val k0 = (year + (month - 1) / 12.0 - 1999.97) * 13.2555
        return floor(k0)
    }

    /** JDE Perigee (Meeus Ch.50, presisi ±2 menit) */
    fun perigeeJde(k: Double): PerigeeApogee {
        val T   = k / 1325.55
        val T2  = T * T; val T3 = T2 * T; val T4 = T3 * T

        val JDE = periR(k, T, T2, T3, T4)

        // Koreksi tabel
        val D   = Math.toRadians((171.9179 + 335.9106046 * k - T2 * (0.010025 + T * (0.00001156 - T * 0.000000023))).mod(360.0))
        val M   = Math.toRadians((347.3477 + 27.1577721 * k - T2 * (0.000813 + T * 0.000100)).mod(360.0))
        val F   = Math.toRadians((316.6109 + 364.5287911 * k - T2 * (0.012325 + T * (0.0001500 - T * 0.000000013))).mod(360.0))

        val cor = 0.0 +
            -1.6769 * sin(2*D)       + 0.4589 * sin(4*D)     - 0.1856 * sin(6*D)  + 0.1143 * sin(8*D) +
            -0.0870 * sin(2*D-M)     - 0.0682 * sin(4*D-M)   + 0.0525 * sin(12*D) + 0.0215 * sin(10*D) +
            -0.0205 * sin(10*D-M)   + 0.0154 * sin(6*D+M)  + 0.0130 * sin(4*D+M) + 0.0128 * sin(8*D-M) +
            -0.0117 * sin(4*D+2*F)  - 0.0078 * sin(6*D+2*F) + 0.0073 * sin(2*D+2*M) - 0.0067 * sin(2*D-2*M)

        val parallax = (3629.215 +
            63.224 * cos(2*D)     - 6.990 * cos(4*D)     + 2.834 * cos(2*D-M) - 0.0071 * cos(2*D) +
            1.927 * cos(6*D)      - 1.263 * cos(8*D)     - 0.702 * cos(10*D)  + 1.188 * cos(M) +
            0.673 * cos(4*D+M)    + 0.027 * cos(8*D+M)  + 0.187 * cos(2*D+M)) / 3600.0

        val dist = 6378.14 / sin(Math.toRadians(parallax))
        return PerigeeApogee(JDE + cor, dist, parallax)
    }

    private fun periR(k: Double, T: Double, T2: Double, T3: Double, T4: Double): Double =
        periR2(k, T, T2, T3, T4)

    private fun periR2(k: Double, T: Double, T2: Double, T3: Double, T4: Double): Double =
        2451076.02 + 27.321582241 * k + T2 * 0.0001667 - T3 * 0.000146

    /** JDE Apogee (Meeus Ch.50, presisi ±2 menit) */
    fun apogeeJde(k: Double): PerigeeApogee {
        val T   = k / 1325.55
        val T2  = T * T

        val JDE = 2451711.28 + 27.321582241 * k + T2 * 0.0001667 - T2 * T * 0.000146

        val D   = Math.toRadians((171.9179 + 335.9106046 * k - T2 * 0.010025).mod(360.0))
        val M   = Math.toRadians((347.3477 + 27.1577721 * k - T2 * 0.000813).mod(360.0))
        val F   = Math.toRadians((316.6109 + 364.5287911 * k - T2 * 0.012325).mod(360.0))

        val cor = 0.4392 * sin(2*D) - 0.0684 * sin(4*D) +
                  0.0456 * sin(M) - 0.0172 * sin(2*D-M) + 0.0101 * sin(6*D)

        val parallax = (3245.251 - 9.147 * cos(2*D) - 0.841 * cos(M) +
                0.697 * cos(2*F) - 0.656 * cos(4*D) + 0.355 * cos(4*D-M)) / 3600.0

        val dist = 6378.14 / sin(Math.toRadians(parallax))
        return PerigeeApogee(JDE + cor, dist, parallax)
    }

    // ── Ch.51: Node Bulan ─────────────────────────────────────────────────────

    enum class LunarNode { ASCENDING, DESCENDING }

    /** JDE saat Bulan melewati node (Meeus Ch.51) */
    fun lunarNodeJde(k: Double, node: LunarNode): Double {
        val T  = k / 1342.23
        val T2 = T * T

        var JDE = 2451565.2 + 27.212220817 * k + T2 * (0.0002762 + T * (0.000000021 - T * 0.000000000088))

        val om = Math.toRadians((124.7746 - 1.56375580 * k + T2 * (0.0020672 + T * 0.00000215)).mod(360.0))
        val D  = Math.toRadians((183.638 + 331.73735682 * k + T2 * (0.0014781 + T * 0.00000015)).mod(360.0))
        val M  = Math.toRadians((17.4006 + 26.82037250 * k + T2 * (0.0001236 + T * 0.00000006)).mod(360.0))
        val Mp = Math.toRadians((38.3776 + 355.52747313 * k + T2 * (0.0123499 + T * (0.000014627 - T * 0.000000069))).mod(360.0))
        val F  = Math.toRadians((7.4121 + 383.25075646 * k - T2 * (0.0040756 + T * (0.00001765 - T * 0.000000012))).mod(360.0))

        JDE += (-0.4721 * sin(Mp) - 0.1649 * sin(2*D) - 0.0868 * sin(2*D - Mp)
                + 0.0410 * sin(2*D + Mp) - 0.0348 * sin(2*D - M)
                + 0.0305 * sin(2*D + M) + 0.0153 * sin(4*D - Mp)
                + 0.0128 * sin(D) + 0.0125 * sin(4*D) - 0.0063 * sin(Mp + M)
                - 0.0060 * sin(2*D - Mp - M) - 0.0048 * sin(2*D + 2*Mp))

        return if (node == LunarNode.DESCENDING) JDE + 13.606 else JDE
    }

    // ── Ch.52: Deklinasi Maksimum Bulan ──────────────────────────────────────

    /** JDE saat Bulan mencapai deklinasi maksimum utara/selatan (Meeus Ch.52) */
    fun lunarMaxDeclinationJde(k: Double, northern: Boolean): Double {
        val T  = k / 1336.86
        val T2 = T * T

        val JDE = if (northern)
            2451562.5897 + 27.321582247 * k + T2 * (0.000100695 - T * 0.000000141)
        else
            2451548.9289 + 27.321582247 * k + T2 * (0.000100695 - T * 0.000000141)

        val D  = Math.toRadians((152.2029 + 333.0705546 * k - T2 * (0.0004025 + T * 0.00000001)).mod(360.0))
        val M  = Math.toRadians((14.8591 + 26.9281592 * k - T2 * (0.000100 - T * 0.000000016)).mod(360.0))
        val Mp = Math.toRadians((4.6676 + 477.2561117 * k + T2 * (0.0003 + T * 0.000000036)).mod(360.0))
        val F  = Math.toRadians((325.8867 + 364.5410748 * k - T2 * (0.0125071 + T * (0.0000529 - T * 0.000000059))).mod(360.0))

        val cor = 0.8975 * cos(F) - 0.4726 * sin(Mp) - 0.1030 * sin(2*F) -
                0.0976 * sin(2*D - Mp) - 0.0462 * cos(Mp - F) - 0.0461 * cos(Mp + F) -
                0.0438 * sin(2*D) + 0.0422 * cos(Mp) - 0.0256 * cos(Mp - 2*F)

        return JDE + cor
    }

    // ── Ch.48: Librasi Bulan ─────────────────────────────────────────────────

    data class LunarLibration(
        val lOptical: Double,   // Librasi longitude optik (derajat)
        val bOptical: Double,   // Librasi latitude optik (derajat)
        val lPhysical: Double,  // Librasi longitude fisik (derajat)
        val bPhysical: Double,  // Librasi latitude fisik (derajat)
        val lTotal: Double,     // Librasi longitude total (derajat)
        val bTotal: Double,     // Librasi latitude total (derajat)
        val positionAngle: Double // PA sumbu rotasi Bulan (derajat)
    )

    /**
     * Librasi Bulan (Meeus Ch.53)
     * @param jde     Julian Day Efemeris
     * @param moonLon Longitude ekliptik Bulan (derajat)
     * @param moonLat Latitude ekliptik Bulan (derajat)
     * @param moonDist Jarak Bulan (km)
     * @param eps     Obliquitas sejati (derajat)
     */
    fun lunarLibration(
        jde: Double, moonLon: Double, moonLat: Double,
        moonDist: Double, eps: Double
    ): LunarLibration {
        val T  = (jde - 2451545.0) / 36525.0

        val F   = Math.toRadians((93.2720993 + 483202.0175273 * T).mod(360.0))
        val Om  = Math.toRadians((125.0445550 - 1934.1361849 * T).mod(360.0))
        val I   = Math.toRadians(1.5424)  // inklinasi orbit Bulan terhadap ekliptik

        val W   = Math.toRadians(moonLon) - Om
        val lonR = Math.toRadians(moonLon)
        val latR = Math.toRadians(moonLat)
        val epsR = Math.toRadians(eps)

        // Librasi longitude optik
        val sinA = sin(W) * cos(latR) * cos(I) - sin(latR) * sin(I)
        val cosA = cos(W) * cos(latR)
        val A    = atan2(sinA, cosA)
        val lPrime = Math.toDegrees(A) - Math.toDegrees(F)
        val lOpt = lPrime.mod(360.0).let { if (it > 180) it - 360 else it }

        // Librasi latitude optik
        val bOpt = Math.toDegrees(asin(-sin(W) * cos(latR) * sin(I) - sin(latR) * cos(I)))

        // Librasi fisik (Meeus Eq. 53.5)
        val k1  = 119.75 + 131.849 * T
        val k2  = 72.56 + 20.186 * T
        val rho = (-0.02752 * cos(Math.toRadians(moonLon - 269.926)) -
                   0.02245 * sin(F) + 0.00684 * cos(Math.toRadians(moonLon - F)))
        val sig = (-0.02816 * sin(Math.toRadians(moonLon - 0.0)) +
                   0.02244 * cos(F) - 0.00682 * cos(Math.toRadians(moonLon - F)))
        val tau = (0.02520 * sin(Math.toRadians(moonLon - 0.0)) * cos(Math.toRadians(epsR)) / cos(latR))

        val lPhys = -tau + (rho * cos(A) + sig * sin(A)) * tan(Math.toRadians(bOpt))
        val bPhys = sig * cos(A) - rho * sin(A)

        // Sudut posisi sumbu
        val V  = Om + Math.toRadians(rho / sin(I))
        val X  = sin(I + Math.toRadians(rho)) * sin(V)
        val Y  = sin(I + Math.toRadians(rho)) * cos(V) * cos(epsR) - cos(I + Math.toRadians(rho)) * sin(epsR)
        val om = Math.toDegrees(atan2(X, Y))
        val sinPA = sin(om - moonLon) * cos(Math.toRadians(moonLat)) / cos(Math.toRadians(lOpt + lPhys))

        return LunarLibration(
            lOptical  = lOpt,    bOptical  = bOpt,
            lPhysical = lPhys,   bPhysical = bPhys,
            lTotal    = lOpt + lPhys, bTotal = bOpt + bPhys,
            positionAngle = Math.toDegrees(asin(sinPA))
        )
    }
}
