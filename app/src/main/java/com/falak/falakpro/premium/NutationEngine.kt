package com.falak.falakpro.premium

import kotlin.math.*

/**
 * NutationEngine — IAU 2000B Model
 * Menghitung Nutasi Longitude (ΔΨ) dan Nutasi Obliquitas (Δε)
 * berdasarkan 77 suku periodik utama IAU 2000B
 */
object NutationEngine {

    // Koefisien argumen fundamental IAU 2000B (77 suku)
    // Format: [l, l', F, D, Ω, S0, S1, C0, C1]
    // S = S0 + S1*T (dalam 0.1 mikroarcsecond), ΔΨ = Σ(S0+S1*T)*sin(arg)
    // C = C0 + C1*T (dalam 0.1 mikroarcsecond), Δε = Σ(C0+C1*T)*cos(arg)
    private val IAU2000B_COEFFS = arrayOf(
        //  l   l'  F   D   Ω     S0       S1      C0      C1
        intArrayOf( 0,  0,  0,  0,  1), doubleArrayOf(-172064161.0, -174666.0,  92052331.0,  9086.0),
        intArrayOf( 0,  0,  2, -2,  2), doubleArrayOf( -13170906.0,  -13696.0,   5730336.0, -3015.0),
        intArrayOf( 0,  0,  2,  0,  2), doubleArrayOf(  -2276413.0,   -2353.0,    978459.0,  -485.0),
        intArrayOf( 0,  0,  0,  0,  2), doubleArrayOf(   2074554.0,     2070.0,   -897492.0,   470.0),
        intArrayOf( 0,  1,  0,  0,  0), doubleArrayOf(   1475877.0,    -3633.0,     73871.0,  -184.0),
        intArrayOf( 0,  1,  2, -2,  2), doubleArrayOf(  -516821.0,     1226.0,    224386.0,  -677.0),
        intArrayOf( 1,  0,  0,  0,  0), doubleArrayOf(   711159.0,       73.0,     -6750.0,     0.0),
        intArrayOf( 0,  0,  2,  0,  1), doubleArrayOf(  -387298.0,     -367.0,    200728.0,    18.0),
        intArrayOf( 1,  0,  2,  0,  2), doubleArrayOf(  -301461.0,     -36.0,    129025.0,   -63.0),
        intArrayOf( 0, -1,  2, -2,  2), doubleArrayOf(   215829.0,     -494.0,    -95929.0,   299.0),
        intArrayOf( 0,  0,  2, -2,  1), doubleArrayOf(   128227.0,      137.0,    -68982.0,    -9.0),
        intArrayOf(-1,  0,  2,  0,  2), doubleArrayOf(   123457.0,       11.0,    -53311.0,    32.0),
        intArrayOf(-1,  0,  0,  2,  0), doubleArrayOf(   156994.0,       10.0,     -1235.0,     0.0),
        intArrayOf( 1,  0,  0,  0,  1), doubleArrayOf(    63110.0,       63.0,    -33228.0,     0.0),
        intArrayOf(-1,  0,  0,  0,  1), doubleArrayOf(   -57976.0,      -63.0,     31429.0,     0.0),
        intArrayOf(-1,  0,  2,  2,  2), doubleArrayOf(   -59641.0,      -11.0,     25543.0,   -11.0),
        intArrayOf( 1,  0,  2,  0,  1), doubleArrayOf(   -51613.0,      -42.0,     26366.0,     0.0),
        intArrayOf(-2,  0,  2,  0,  1), doubleArrayOf(    45893.0,       50.0,    -24236.0,   -10.0),
        intArrayOf( 0,  0,  0,  2,  0), doubleArrayOf(    63384.0,       11.0,     -1220.0,     0.0),
        intArrayOf( 0,  0,  2,  2,  2), doubleArrayOf(   -38571.0,       -1.0,     16452.0,   -11.0),
        intArrayOf( 0, -2,  2, -2,  2), doubleArrayOf(    32481.0,        0.0,    -13870.0,     0.0),
        intArrayOf(-2,  0,  0,  2,  0), doubleArrayOf(   -47722.0,        0.0,       477.0,     0.0),
        intArrayOf( 2,  0,  2,  0,  2), doubleArrayOf(   -31046.0,       -1.0,     13238.0,   -11.0),
        intArrayOf( 1,  0,  2, -2,  2), doubleArrayOf(    28593.0,        0.0,    -12338.0,    10.0),
        intArrayOf(-1,  0,  2,  0,  1), doubleArrayOf(    20441.0,       21.0,    -10758.0,     0.0),
        intArrayOf( 2,  0,  0,  0,  0), doubleArrayOf(    29243.0,        0.0,      -609.0,     0.0),
        intArrayOf( 0,  0,  2,  0,  0), doubleArrayOf(    25887.0,        0.0,      -550.0,     0.0),
        intArrayOf( 0,  1,  0,  0,  1), doubleArrayOf(   -14053.0,      -25.0,      8551.0,    -2.0),
        intArrayOf(-1,  0,  0,  2,  1), doubleArrayOf(    15164.0,       10.0,     -8001.0,     0.0),
        intArrayOf( 0,  2,  2, -2,  2), doubleArrayOf(   -15794.0,       72.0,      6850.0,   -42.0),
        intArrayOf( 0,  0, -2,  2,  0), doubleArrayOf(    21783.0,        0.0,      -167.0,     0.0),
        intArrayOf( 1,  0,  0, -2,  1), doubleArrayOf(   -12873.0,      -10.0,      6953.0,     0.0),
        intArrayOf( 0, -1,  0,  0,  1), doubleArrayOf(   -12654.0,       11.0,      6415.0,     0.0),
        intArrayOf(-1,  0,  2,  2,  1), doubleArrayOf(   -10204.0,        0.0,      5222.0,     0.0),
        intArrayOf( 0,  2,  0,  0,  0), doubleArrayOf(    16707.0,      -85.0,       168.0,    -1.0),
        intArrayOf( 1,  0,  2,  2,  2), doubleArrayOf(    -7691.0,        0.0,      3268.0,     0.0),
        intArrayOf(-2,  0,  2,  0,  0), doubleArrayOf(   -11024.0,        0.0,       104.0,     0.0),
        intArrayOf( 0,  1,  2,  0,  2), doubleArrayOf(     7566.0,      -21.0,     -3250.0,     0.0),
        intArrayOf( 0,  0,  2,  2,  1), doubleArrayOf(    -6637.0,      -11.0,      3353.0,     0.0),
        intArrayOf( 0, -1,  2,  0,  2), doubleArrayOf(    -7141.0,       21.0,      3070.0,     0.0),
        intArrayOf( 0,  0,  0,  2,  1), doubleArrayOf(    -6302.0,      -11.0,      3272.0,     0.0),
        intArrayOf( 1,  0,  2, -2,  1), doubleArrayOf(     5800.0,       10.0,     -3045.0,     0.0),
        intArrayOf( 2,  0,  2, -2,  2), doubleArrayOf(     6443.0,        0.0,     -2768.0,     0.0),
        intArrayOf(-2,  0,  0,  2,  1), doubleArrayOf(    -5774.0,      -11.0,      3041.0,     0.0),
        intArrayOf( 2,  0,  2,  0,  1), doubleArrayOf(    -5350.0,        0.0,      2695.0,     0.0),
        intArrayOf( 0, -1,  2, -2,  1), doubleArrayOf(    -4752.0,      -11.0,      2719.0,     0.0),
        intArrayOf( 0,  0,  0, -2,  1), doubleArrayOf(    -4940.0,      -11.0,      2720.0,     0.0),
        intArrayOf(-1, -1,  0,  2,  0), doubleArrayOf(     7350.0,        0.0,       -51.0,     0.0),
        intArrayOf( 2,  0,  0, -2,  1), doubleArrayOf(     4065.0,        0.0,     -2206.0,     0.0),
        intArrayOf( 1,  0,  0,  2,  0), doubleArrayOf(     6579.0,        0.0,      -199.0,     0.0),
        intArrayOf( 0,  1,  2, -2,  1), doubleArrayOf(     3579.0,        0.0,     -1900.0,     0.0),
        intArrayOf( 1, -1,  0,  0,  0), doubleArrayOf(     4725.0,        0.0,       -41.0,     0.0),
        intArrayOf(-2,  0,  2,  0,  2), doubleArrayOf(    -3075.0,        0.0,      1313.0,     0.0),
        intArrayOf( 3,  0,  2,  0,  2), doubleArrayOf(    -2904.0,        0.0,      1233.0,     0.0),
        intArrayOf( 0, -1,  0,  2,  0), doubleArrayOf(     4348.0,        0.0,       -81.0,     0.0),
        intArrayOf( 1, -1,  2,  0,  2), doubleArrayOf(    -2878.0,        0.0,      1232.0,     0.0),
        intArrayOf( 0,  0,  0,  1,  0), doubleArrayOf(    -4230.0,        0.0,        -20.0,    0.0),
        intArrayOf(-1, -1,  2,  2,  2), doubleArrayOf(    -2819.0,        0.0,      1207.0,     0.0),
        intArrayOf(-1,  0,  2,  0,  0), doubleArrayOf(    -4056.0,        0.0,        40.0,     0.0),
        intArrayOf( 0, -1,  2,  2,  2), doubleArrayOf(    -2647.0,        0.0,      1129.0,     0.0),
        intArrayOf(-2,  0,  0,  0,  1), doubleArrayOf(    -2294.0,        0.0,      1266.0,     0.0),
        intArrayOf( 1,  1,  2,  0,  2), doubleArrayOf(     2481.0,        0.0,     -1062.0,     0.0),
        intArrayOf( 2,  0,  0,  0,  1), doubleArrayOf(     2179.0,        0.0,     -1129.0,     0.0),
        intArrayOf(-1,  1,  0,  1,  0), doubleArrayOf(     3276.0,        0.0,        -9.0,     0.0),
        intArrayOf( 1,  1,  0,  0,  0), doubleArrayOf(    -3389.0,        0.0,        35.0,     0.0),
        intArrayOf( 1,  0,  2,  0,  0), doubleArrayOf(     3339.0,        0.0,      -107.0,     0.0),
        intArrayOf(-1,  0,  2, -2,  1), doubleArrayOf(    -1987.0,        0.0,      1073.0,     0.0),
        intArrayOf( 1,  0,  0,  0,  2), doubleArrayOf(    -1981.0,        0.0,       854.0,     0.0),
        intArrayOf(-1,  0,  0,  1,  0), doubleArrayOf(     4026.0,        0.0,      -553.0,     0.0),
        intArrayOf( 0,  0,  2,  1,  2), doubleArrayOf(     1660.0,        0.0,      -710.0,     0.0),
        intArrayOf(-1,  0,  2,  4,  2), doubleArrayOf(    -1521.0,        0.0,       647.0,     0.0),
        intArrayOf(-1,  1,  0,  1,  1), doubleArrayOf(     1314.0,        0.0,      -700.0,     0.0),
        intArrayOf( 0, -2,  2, -2,  1), doubleArrayOf(    -1283.0,        0.0,       672.0,     0.0),
        intArrayOf( 1,  0,  2,  2,  1), doubleArrayOf(    -1331.0,        0.0,       663.0,     0.0),
        intArrayOf(-2,  0,  2,  2,  2), doubleArrayOf(     1383.0,        0.0,      -594.0,     0.0),
        intArrayOf(-1,  0,  0,  0,  2), doubleArrayOf(     1405.0,        0.0,      -610.0,     0.0),
        intArrayOf( 1,  1,  2, -2,  2), doubleArrayOf(     1290.0,        0.0,      -556.0,     0.0)
    )

    /**
     * Menghitung Nutasi Longitude (ΔΨ) dan Obliquitas (Δε)
     * @param T Julian Centuries dari J2000.0
     * @return Pair(deltaPsi_deg, deltaEps_deg)
     */
    fun calculate(T: Double): Pair<Double, Double> {
        // Argumen Fundamental (dalam derajat)
        val l  = (485868.249036 + 1717915923.2178 * T) % 1296000.0  // anomali rata2 Bulan
        val lp = (1287104.79305 +  129596581.0481 * T) % 1296000.0  // anomali rata2 Matahari
        val F  = (335779.526232 + 1739527262.8478 * T) % 1296000.0  // arg lintang Bulan
        val D  = (1072260.70369 + 1602961601.2090 * T) % 1296000.0  // elongasi rata2 Bulan
        val Om = (450160.398036 -    6962890.5431 * T) % 1296000.0  // long ascending node

        // Koreksi ke radian
        val toRad = PI / 648000.0 // arcsec to rad

        var dpsi = 0.0
        var deps = 0.0
        var i = 0
        while (i < IAU2000B_COEFFS.size) {
            val ni = IAU2000B_COEFFS[i] as IntArray
            val ci = IAU2000B_COEFFS[i + 1] as DoubleArray
            val arg = (ni[0] * l + ni[1] * lp + ni[2] * F + ni[3] * D + ni[4] * Om) * toRad
            dpsi += (ci[0] + ci[1] * T) * sin(arg)
            deps += (ci[2] + ci[3] * T) * cos(arg)
            i += 2
        }

        // Konversi dari 0.1 mikroarcsec ke derajat
        val factor = 1e-7 / 3600.0
        return Pair(dpsi * factor, deps * factor)
    }

    /**
     * Obliquitas rata-rata ekliptik (dalam derajat)
     * Formula IAU
     */
    fun meanObliquity(T: Double): Double {
        val T2 = T * T
        val T3 = T2 * T
        // Formula IAU (Meeus Ch.22)
        // epsilon0 = 23d 26' 21.448" - 46.8150" T - 0.00059" T^2 + 0.001813" T^3
        return (84381.448 - 46.8150 * T - 0.00059 * T2 + 0.001813 * T3) / 3600.0
    }
}
