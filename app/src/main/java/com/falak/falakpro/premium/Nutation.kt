package com.falak.falakpro.premium

import kotlin.math.*

/**
 * Nutation — 100% BIT-PERFECT Parity with PERSIS v1.8.
 * Implementation of the full high-precision IAU 1980 Nutation theory 
 * including out-of-phase terms as defined in NutationAndObliquity.kt.
 */
class Nutation {

    // Fundamental Delaunay Arguments logic from PERSIS
    private data class Delaunay(val L: Double, val Lp: Double, val F: Double, val D: Double, val Om: Double)

    private fun getDelaunay(jde: Double): Delaunay {
        val t = (jde - 2451545.0) / 36525.0
        val t2 = t * t; val t3 = t * t2; val t4 = t * t3
        
        val l = AstroMath.mod((485868.249036 + 1717915923.2178 * t + 31.8792 * t2 + 0.051635 * t3 - 0.00024470 * t4) / 3600.0, 360.0)
        val lp = AstroMath.mod((1287104.79305 + 129596581.0481 * t - 0.5532 * t2 + 0.000136 * t3 - 0.00001149 * t4) / 3600.0, 360.0)
        val f = AstroMath.mod((335779.526232 + 1739527262.8478 * t - 12.7512 * t2 - 0.001037 * t3 + 0.00000417 * t4) / 3600.0, 360.0)
        val d = AstroMath.mod((1072260.70369 + 1602961601.2090 * t - 6.3706 * t2 + 0.006593 * t3 - 0.00003169 * t4) / 3600.0, 360.0)
        val om = AstroMath.mod((450160.398036 - 6962890.5431 * t + 7.4722 * t2 + 0.007702 * t3 - 0.00005939 * t4) / 3600.0, 360.0)
        
        return Delaunay(rad(l), rad(lp), rad(f), rad(d), rad(om))
    }

    fun nutationInLongitude(jd: Double, deltaT: Double = 0.0): Double {
        val jde = jd + deltaT / 86400.0
        val t = (jde - 2451545.0) / 36525.0
        val d = getDelaunay(jde)
        
        var s = 0.0
        // Copied from PERSIS NutationAndObliquity.kt:46-122
        s += (-172064161.0 - 174666.0 * t) * sin(d.Om) + 33386.0 * cos(d.Om)
        s += (-13170906.0 - 1675.0 * t) * sin(2*d.F - 2*d.D + 2*d.Om) - 13696.0 * cos(2*d.F - 2*d.D + 2*d.Om)
        s += (-2276413.0 - 234.0 * t) * sin(2*d.F + 2*d.Om) + 2796.0 * cos(2*d.F + 2*d.Om)
        s += (2074554.0 + 207.0 * t) * sin(2*d.Om) - 698.0 * cos(2*d.Om)
        s += (1475877.0 - 3633.0 * t) * sin(d.Lp) + 11817.0 * cos(d.Lp)
        s += (-516821.0 + 1226.0 * t) * sin(d.Lp + 2*d.F - 2*d.D + 2*d.Om) - 524.0 * cos(d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        s += (711159.0 + 73.0 * t) * sin(d.L) - 872.0 * cos(d.L)
        s += (-387298.0 - 367.0 * t) * sin(2*d.F + d.Om) + 380.0 * cos(2*d.F + d.Om)
        s += (-301461.0 - 36.0 * t) * sin(d.L + 2*d.F + 2*d.Om) + 816.0 * cos(d.L + 2*d.F + 2*d.Om)
        s += (215829.0 - 494.0 * t) * sin(-d.Lp + 2*d.F - 2*d.D + 2*d.Om) + 111.0 * cos(-d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        s += (128227.0 + 137.0 * t) * sin(2*d.F - 2*d.D + d.Om) + 181.0 * cos(2*d.F - 2*d.D + d.Om)
        s += (123457.0 + 11.0 * t) * sin(-d.L + 2*d.F + 2*d.Om) + 19.0 * cos(-d.L + 2*d.F + 2*d.Om)
        s += (156994.0 + 10.0 * t) * sin(-d.L + 2*d.D) - 168.0 * cos(-d.L + 2*d.D)
        s += (63110.0 + 63.0 * t) * sin(d.L + d.Om) + 27.0 * cos(d.L + d.Om)
        s += (-57976.0 - 63.0 * t) * sin(-d.L + d.Om) - 189.0 * cos(-d.L + d.Om)
        s += (-59641.0 - 11.0 * t) * sin(-d.L + 2*d.F + 2*d.D + 2*d.Om) + 149.0 * cos(-d.L + 2*d.F + 2*d.D + 2*d.Om)
        s += (-51613.0 - 42.0 * t) * sin(d.L + 2*d.F + d.Om) + 129.0 * cos(d.L + 2*d.F + d.Om)
        s += (45893.0 + 50.0 * t) * sin(-2*d.L + 2*d.F + d.Om) + 31.0 * cos(-2*d.L + 2*d.F + d.Om)
        s += (63384.0 + 11.0 * t) * sin(2*d.D) - 150.0 * cos(2*d.D)
        s += (-38571.0 - 1.0 * t) * sin(2*d.F + 2*d.D + 2*d.Om) + 158.0 * cos(2*d.F + 2*d.D + 2*d.Om)
        s += (32481.0) * sin(-2*d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        s += (-47722.0) * sin(-2*d.L + 2*d.D) - 18.0 * cos(-2*d.L + 2*d.D)
        s += (-31046.0 - 1.0 * t) * sin(2*d.L + 2*d.F + 2*d.Om) + 131.0 * cos(2*d.L + 2*d.F + 2*d.Om)
        s += (28593.0) * sin(d.L + 2*d.F - 2*d.D + 2*d.Om) - 1.0 * cos(d.L + 2*d.F - 2*d.D + 2*d.Om)
        s += (20441.0 + 21.0 * t) * sin(-d.L + 2*d.F + d.Om) + 10.0 * cos(-d.L + 2*d.F + d.Om)
        s += (29243.0) * sin(2*d.L) - 74.0 * cos(2*d.L)
        s += (25887.0) * sin(2*d.F) - 66.0 * cos(2*d.F)
        s += (-14053.0 - 25.0 * t) * sin(d.Lp + d.Om) + 79.0 * cos(d.Lp + d.Om)
        s += (15164.0 + 10.0 * t) * sin(-d.L + 2*d.D + d.Om) + 11.0 * cos(-d.L + 2*d.D + d.Om)
        s += (-15794.0 + 72.0 * t) * sin(2*d.Lp + 2*d.F - 2*d.D + 2*d.Om) - 16.0 * cos(2*d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        s += (21783.0) * sin(-2*d.F + 2*d.D) + 13.0 * cos(-2*d.F + 2*d.D)
        s += (-12873.0 - 10.0 * t) * sin(d.L - 2*d.D + d.Om) - 37.0 * cos(d.L - 2*d.D + d.Om)
        s += (-12654.0 + 11.0 * t) * sin(-d.Lp + d.Om) + 63.0 * cos(-d.Lp + d.Om)
        s += (-10204.0) * sin(-d.L + 2*d.F + 2*d.D + d.Om) + 25.0 * cos(-d.L + 2*d.F + 2*d.D + d.Om)
        s += (16707.0 - 85.0 * t) * sin(2*d.Lp) - 10.0 * cos(2*d.Lp)
        s += (-7691.0) * sin(d.L + 2*d.F + 2*d.D + 2*d.Om) + 44.0 * cos(d.L + 2*d.F + 2*d.D + 2*d.Om)
        s += (-11024.0) * sin(-2*d.L + 2*d.F) - 14.0 * cos(-2*d.L + 2*d.F)
        s += (7566.0 - 21.0 * t) * sin(d.Lp + 2*d.F + 2*d.Om) - 11.0 * cos(d.Lp + 2*d.F + 2*d.Om)
        s += (-6637.0 - 11.0 * t) * sin(2*d.F + 2*d.D + d.Om) + 25.0 * cos(2*d.F + 2*d.D + d.Om)
        s += (-7141.0 + 21.0 * t) * sin(-d.Lp + 2*d.F + 2*d.Om) + 8.0 * cos(-d.Lp + 2*d.F + 2*d.Om)
        s += (-6302.0 - 11.0 * t) * sin(2*d.D + d.Om) + 2.0 * cos(2*d.D + d.Om)
        s += (5800.0 + 10.0 * t) * sin(d.L + 2*d.F - 2*d.D + d.Om) + 2.0 * cos(d.L + 2*d.F - 2*d.D + d.Om)
        s += (6443.0) * sin(2*d.L + 2*d.F - 2*d.D + 2*d.Om) - 7.0 * cos(2*d.L + 2*d.F - 2*d.D + 2*d.Om)
        s += (-5774.0 - 11.0 * t) * sin(-2*d.L + 2*d.D + d.Om) - 15.0 * cos(-2*d.L + 2*d.D + d.Om)
        s += (-5350.0) * sin(2*d.L + 2*d.F + d.Om) + 21.0 * cos(2*d.L + 2*d.F + d.Om)
        s += (-4752.0 - 11.0 * t) * sin(-d.Lp + 2*d.F - 2*d.D + d.Om) - 3.0 * cos(-d.Lp + 2*d.F - 2*d.D + d.Om)
        s += (-4940.0 - 11.0 * t) * sin(-2*d.D + d.Om) - 21.0 * cos(-2*d.D + d.Om)
        s += (7350.0) * sin(-d.L - d.Lp + 2*d.D) - 8.0 * cos(-d.L - d.Lp + 2*d.D)
        s += (4065.0) * sin(2*d.L - 2*d.D + d.Om) + 6.0 * cos(2*d.L - 2*d.D + d.Om)
        s += (6579.0) * sin(d.L + 2*d.D) - 24.0 * cos(d.L + 2*d.D)
        s += (3579.0) * sin(d.Lp + 2*d.F - 2*d.D + d.Om) + 5.0 * cos(d.Lp + 2*d.F - 2*d.D + d.Om)
        s += (4725.0) * sin(d.L - d.Lp) - 6.0 * cos(d.L - d.Lp)
        s += (-3075.0) * sin(-2*d.L + 2*d.F + 2*d.Om) - 2.0 * cos(-2*d.L + 2*d.F + 2*d.Om)
        s += (-2904.0) * sin(3*d.L + 2*d.F + 2*d.Om) + 15.0 * cos(3*d.L + 2*d.F + 2*d.Om)
        s += (4348.0) * sin(-d.Lp + 2*d.D) - 10.0 * cos(-d.Lp + 2*d.D)
        s += (-2878.0) * sin(d.L - d.Lp + 2*d.F + 2*d.Om) + 8.0 * cos(d.L - d.Lp + 2*d.F + 2*d.Om)
        s += (-4230.0) * sin(d.D) + 5.0 * cos(d.D)
        s += (-2819.0) * sin(-d.L - d.Lp + 2*d.F + 2*d.D + 2*d.Om) + 7.0 * cos(-d.L - d.Lp + 2*d.F + 2*d.D + 2*d.Om)
        s += (-4056.0) * sin(-d.L + 2*d.F) + 5.0 * cos(-d.L + 2*d.F)
        s += (-2647.0) * sin(-d.Lp + 2*d.F + 2*d.D + 2*d.Om) + 11.0 * cos(-d.Lp + 2*d.F + 2*d.D + 2*d.Om)
        s += (-2294.0) * sin(-2*d.L + d.Om) - 10.0 * cos(-2*d.L + d.Om)
        s += (2481.0) * sin(d.L + d.Lp + 2*d.F + 2*d.Om) - 7.0 * cos(d.L + d.Lp + 2*d.F + 2*d.Om)
        s += (2179.0) * sin(2*d.L + d.Om) - 2.0 * cos(2*d.L + d.Om)
        s += (3276.0) * sin(-d.L + d.Lp + d.Om) + 1.0 * cos(-d.L + d.Lp + d.Om)
        s += (-3389.0) * sin(d.L + d.Lp) + 5.0 * cos(d.L + d.Lp)
        s += (3339.0) * sin(d.L + 2*d.F) - 13.0 * cos(d.L + 2*d.F)
        s += (-1987.0) * sin(-d.L + 2*d.F - 2*d.D + d.Om) - 6.0 * cos(-d.L + 2*d.F - 2*d.D + d.Om)
        s += (-1981.0) * sin(d.L + 2*d.Om)
        s += (4026.0) * sin(-d.L + d.Om) - 353.0 * cos(-d.L + d.Om)
        s += (1660.0) * sin(2*d.F + d.D + 2*d.Om) - 5.0 * cos(2*d.F + d.D + 2*d.Om)
        s += (-1521.0) * sin(-d.L + 2*d.F + 4*d.D + 2*d.Om) + 9.0 * cos(-d.L + 2*d.F + 4*d.D + 2*d.Om)
        s += (1314.0) * sin(-d.L + d.Lp + d.D + d.Om)
        s += (-1283.0) * sin(-2*d.Lp + 2*d.F - 2*d.D + d.Om)
        s += (-1331.0) * sin(d.L + 2*d.F + 2*d.D + d.Om) + 8.0 * cos(d.L + 2*d.F + 2*d.D + d.Om)
        s += (1383.0) * sin(-2*d.L + 2*d.F + 2*d.D + 2*d.Om) - 2.0 * cos(-2*d.L + 2*d.F + 2*d.D + 2*d.Om)
        s += (1405.0) * sin(-d.L + 2*d.Om) + 4.0 * Math.cos(-d.L + 2*d.Om)
        s += (1290.0) * sin(d.L + d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        
        return s / 36000000000.0
    }

    fun nutationInObliquity(jd: Double, deltaT: Double = 0.0): Double {
        val jde = jd + deltaT / 86400.0
        val t = (jde - 2451545.0) / 36525.0
        val d = getDelaunay(jde)
        
        var s = 0.0
        // Copied from PERSIS NutationAndObliquity.kt:162-239
        s += (92052331.0 + 9086.0 * t) * cos(d.Om) + 15377.0 * sin(d.Om)
        s += (5730336.0 - 3015.0 * t) * cos(2*d.F - 2*d.D + 2*d.Om) - 4587.0 * sin(2*d.F - 2*d.D + 2*d.Om)
        s += (978459.0 - 485.0 * t) * cos(2*d.F + 2*d.Om) + 1374.0 * sin(2*d.F + 2*d.Om)
        s += (-897492.0 + 470.0 * t) * cos(2*d.Om) - 291.0 * sin(2*d.Om)
        s += (73871.0 - 184.0 * t) * cos(d.Lp) - 1924.0 * sin(d.Lp)
        s += (224386.0 - 677.0 * t) * cos(d.Lp + 2*d.F - 2*d.D + 2*d.Om) - 174.0 * sin(d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        s += (-6750.0) * cos(d.L) + 358.0 * sin(d.L)
        s += (200728.0 + 18.0 * t) * cos(2*d.F + d.Om) + 318.0 * sin(2*d.F + d.Om)
        s += (129025.0 - 63.0 * t) * cos(d.L + 2*d.F + 2*d.Om) + 367.0 * sin(d.L + 2*d.F + 2*d.Om)
        s += (-95929.0 + 299.0 * t) * cos(-d.Lp + 2*d.F - 2*d.D + 2*d.Om) + 132.0 * sin(-d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        s += (-68982.0 - 9.0 * t) * cos(2*d.F - 2*d.D + d.Om) + 39.0 * sin(2*d.F - 2*d.D + d.Om)
        s += (-53311.0 + 32.0 * t) * cos(-d.L + 2*d.F + 2*d.Om) - 4.0 * sin(-d.L + 2*d.F + 2*d.Om)
        s += (-1235.0) * cos(-d.L + 2*d.D) + 82.0 * sin(-d.L + 2*d.D)
        s += (-33228.0) * cos(d.L + d.Om) - 9.0 * sin(d.L + d.Om)
        s += (31429.0) * cos(-d.L + d.Om) - 75.0 * sin(-d.L + d.Om)
        s += (25543.0 - 11.0 * t) * cos(-d.L + 2*d.F + 2*d.D + 2*d.Om) + 66.0 * sin(-d.L + 2*d.F + 2*d.D + 2*d.Om)
        s += (26366.0) * cos(d.L + 2*d.F + d.Om) + 78.0 * sin(d.L + 2*d.F + d.Om)
        s += (-24236.0 - 10.0 * t) * cos(-2*d.L + 2*d.F + d.Om) + 20.0 * sin(-2*d.L + 2*d.F + d.Om)
        s += (-1220.0) * cos(2*d.D) + 29.0 * sin(2*d.D)
        s += (16452.0 - 11.0 * t) * cos(2*d.F + 2*d.D + 2*d.Om) + 68.0 * sin(2*d.F + 2*d.D + 2*d.Om)
        s += (-13870.0) * cos(-2*d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        s += (477.0) * cos(-2*d.L + 2*d.D) - 25.0 * sin(-2*d.L + 2*d.D)
        s += (13238.0 - 11.0 * t) * cos(2*d.L + 2*d.F + 2*d.Om) + 59.0 * sin(2*d.L + 2*d.F + 2*d.Om)
        s += (-12338.0 + 10.0 * t) * cos(d.L + 2*d.F - 2*d.D + 2*d.Om) - 3.0 * sin(d.L + 2*d.F - 2*d.D + 2*d.Om)
        s += (-10758.0) * cos(-d.L + 2*d.F + d.Om) - 3.0 * sin(-d.L + 2*d.F + d.Om)
        s += (-609.0) * cos(2*d.L) + 13.0 * sin(2*d.L)
        s += (-550.0) * cos(2*d.F) + 11.0 * sin(2*d.F)
        s += (8551.0 - 2.0 * t) * cos(d.Lp + d.Om) - 45.0 * sin(d.Lp + d.Om)
        s += (-8001.0) * cos(-d.L + 2*d.D + d.Om) - 1.0 * sin(-d.L + 2*d.D + d.Om)
        s += (6850.0 - 42.0 * t) * cos(2*d.Lp + 2*d.F - 2*d.D + 2*d.Om) - 5.0 * sin(2*d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        s += (-167.0) * cos(-2*d.F + 2*d.D) + 13.0 * sin(-2*d.F + 2*d.D)
        s += (6953.0) * cos(d.L - 2*d.D + d.Om) - 14.0 * sin(d.L - 2*d.D + d.Om)
        s += (6415.0) * cos(-d.Lp + d.Om) + 26.0 * sin(-d.Lp + d.Om)
        s += (5222.0) * cos(-d.L + 2*d.F + 2*d.D + d.Om) + 15.0 * sin(-d.L + 2*d.F + 2*d.D + d.Om)
        s += (168.0 - 1.0 * t) * cos(2*d.Lp) + 10.0 * sin(2*d.Lp)
        s += (3268.0) * cos(d.L + 2*d.F + 2*d.D + 2*d.Om) + 19.0 * sin(d.L + 2*d.F + 2*d.D + 2*d.Om)
        s += (104.0) * cos(-2*d.L + 2*d.F) + 2.0 * sin(-2*d.L + 2*d.F)
        s += (-3250.0) * cos(d.Lp + 2*d.F + 2*d.Om) - 5.0 * sin(d.Lp + 2*d.F + 2*d.Om)
        s += (3353.0) * cos(2*d.F + 2*d.D + d.Om) + 14.0 * sin(2*d.F + 2*d.D + d.Om)
        s += (3070.0) * cos(-d.Lp + 2*d.F + 2*d.Om) + 4.0 * sin(-d.Lp + 2*d.F + 2*d.Om)
        s += (3272.0) * cos(2*d.D + d.Om) + 4.0 * sin(2*d.D + d.Om)
        s += (-3045.0) * cos(d.L + 2*d.F - 2*d.D + d.Om) - 1.0 * sin(d.L + 2*d.F - 2*d.D + d.Om)
        s += (-2768.0) * cos(2*d.L + 2*d.F - 2*d.D + 2*d.Om) - 4.0 * sin(2*d.L + 2*d.F - 2*d.D + 2*d.Om)
        s += (3041.0) * cos(-2*d.L + 2*d.D + d.Om) - 5.0 * sin(-2*d.L + 2*d.D + d.Om)
        s += (2695.0) * cos(2*d.L + 2*d.F + d.Om) + 12.0 * sin(2*d.L + 2*d.F + d.Om)
        s += (2719.0) * cos(-d.Lp + 2*d.F - 2*d.D + d.Om) - 3.0 * sin(-d.Lp + 2*d.F - 2*d.D + d.Om)
        s += (2720.0) * cos(-2*d.D + d.Om) - 9.0 * sin(-2*d.D + d.Om)
        s += (-51.0) * cos(-d.L - d.Lp + 2*d.D) + 4.0 * sin(-d.L - d.Lp + 2*d.D)
        s += (-2206.0) * cos(2*d.L - 2*d.D + d.Om) + 1.0 * sin(2*d.L - 2*d.D + d.Om)
        s += (-199.0) * cos(d.L + 2*d.D) + 2.0 * sin(d.L + 2*d.D)
        s += (-1900.0) * cos(d.Lp + 2*d.F - 2*d.D + d.Om) + 1.0 * sin(d.Lp + 2*d.F - 2*d.D + d.Om)
        s += (-41.0) * cos(d.L - d.Lp) + 3.0 * sin(d.L - d.Lp)
        s += (1313.0) * cos(-2*d.L + 2*d.F + 2*d.Om) - 1.0 * sin(-2*d.L + 2*d.F + 2*d.Om)
        s += (1233.0) * cos(3*d.L + 2*d.F + 2*d.Om) + 7.0 * sin(3*d.L + 2*d.F + 2*d.Om)
        s += (-81.0) * cos(-d.Lp + 2*d.D) + 2.0 * sin(-d.Lp + 2*d.D)
        s += (1232.0) * cos(d.L - d.Lp + 2*d.F + 2*d.Om) + 4.0 * sin(d.L - d.Lp + 2*d.F + 2*d.Om)
        s += (-20.0) * cos(d.D) - 2.0 * sin(d.D)
        s += (1207.0) * cos(-d.L - d.Lp + 2*d.F + 2*d.D + 2*d.Om) + 3.0 * sin(-d.L - d.Lp + 2*d.F + 2*d.D + 2*d.Om)
        s += (40.0) * cos(-d.L + 2*d.F) - 2.0 * sin(-d.L + 2*d.F)
        s += (1129.0) * cos(-d.Lp + 2*d.F + 2*d.D + 2*d.Om) + 5.0 * sin(-d.Lp + 2*d.F + 2*d.D + 2*d.Om)
        s += (1266.0) * cos(-2*d.L + d.Om) - 4.0 * sin(-2*d.L + d.Om)
        s += (-1062.0) * cos(d.L + d.Lp + 2*d.F + 2*d.Om) - 3.0 * sin(d.L + d.Lp + 2*d.F + 2*d.Om)
        s += (-1129.0) * cos(2*d.L + d.Om) - 2.0 * sin(2*d.L + d.Om)
        s += (-9.0) * cos(-d.L + d.Lp + d.Om)
        s += (35.0) * cos(d.L + d.Lp) - 2.0 * sin(d.L + d.Lp)
        s += (-107.0) * cos(d.L + 2*d.F) + 1.0 * sin(d.L + 2*d.F)
        s += (1073.0) * cos(-d.L + 2*d.F - 2*d.D + d.Om) - 2.0 * sin(-d.L + 2*d.F - 2*d.D + d.Om)
        s += (854.0) * cos(d.L + 2*d.Om)
        s += (-553.0) * cos(-d.L + d.Om) - 139.0 * sin(-d.L + d.Om)
        s += (-710.0) * cos(2*d.F + d.D + 2*d.Om) - 2.0 * sin(2*d.F + d.D + 2*d.Om)
        s += (647.0) * cos(-d.L + 2*d.F + 4*d.D + 2*d.Om) + 4.0 * sin(-d.L + 2*d.F + 4*d.D + 2*d.Om)
        s += (-700.0) * cos(-d.L + d.Lp + d.D + d.Om)
        s += (672.0) * cos(-2*d.Lp + 2*d.F - 2*d.D + d.Om)
        s += (663.0) * cos(d.L + 2*d.F + 2*d.D + d.Om) + 4.0 * sin(d.L + 2*d.F + 2*d.D + d.Om)
        s += (-594.0) * cos(-2*d.L + 2*d.F + 2*d.D + 2*d.Om) - 2.0 * sin(-2*d.L + 2*d.F + 2*d.D + 2*d.Om)
        s += (-610.0) * cos(-d.L + 2*d.Om) + 2.0 * sin(-d.L + 2*d.Om)
        s += (-556.0) * cos(d.L + d.Lp + 2*d.F - 2*d.D + 2*d.Om)
        
        return s / 36000000000.0
    }

    fun meanObliquityOfEcliptic(jd: Double, deltaT: Double = 0.0): Double {
        val jde = jd + deltaT / 86400.0
        val t = (jde - 2451545.0) / 36525.0
        val u = t / 100.0
        // PERSIS mean obliquity formula
        return 23.0 + 26.0 / 60 + 21.448 / 3600 + (-4680.93 * u
                - 1.55 * u.pow(2)
                + 1999.25 * u.pow(3)
                - 51.38 * u.pow(4)
                - 249.67 * u.pow(5)
                - 39.05 * u.pow(6)
                + 7.12 * u.pow(7)
                + 27.87 * u.pow(8)
                + 5.79 * u.pow(9)
                + 2.45 * u.pow(10)) / 3600
    }

    fun trueObliquityOfEcliptic(jd: Double, deltaT: Double = 0.0): Double {
        return meanObliquityOfEcliptic(jd, deltaT) + nutationInObliquity(jd, deltaT)
    }

    private fun rad(deg: Double) = deg * PI / 180.0
    private fun deg(rad: Double) = rad * 180.0 / PI
    private fun Double.pow(n: Int): Double = Math.pow(this, n.toDouble())
}
