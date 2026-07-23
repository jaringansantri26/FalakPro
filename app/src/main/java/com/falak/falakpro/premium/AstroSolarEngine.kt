package com.falak.falakpro.premium

import kotlin.math.cos

/**
 * Helper Matahari presisi tinggi untuk data falak: deklinasi, EoT, dan semidiameter.
 */
object AstroSolarEngine {

    fun getDeclination(jde: Double): Double {
        return Vsop87SolarEngine.compute(jde).dec
    }

    fun getEquationOfTime(jde: Double): Double {
        val sun = Vsop87SolarEngine.compute(jde)
        val deltaT = DynamicalTimeEngine.deltaT(jde)
        val jdeTd = jde + deltaT / 86400.0
        val obliquity = AstroDataUtils.calculateTrueObliquity(jdeTd)
        val nutation = AstroDataUtils.calculateNutation(jdeTd)
        val t = (jde - 2451545.0) / 36525.0

        var meanLongitude = 280.4664567 + 36000.7698277 * t + 0.00030322 * t * t
        meanLongitude = AstroMath.mod(meanLongitude, 360.0)
        return (meanLongitude - sun.ra + nutation.first * cos(AstroMath.rad(obliquity))) / 15.0
    }

    fun getSemidiameter(jde: Double): Double {
        val sun = Vsop87SolarEngine.compute(jde)
        return 0.2666 / sun.distanceAU
    }
}
