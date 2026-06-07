package com.falak.falakpro.premium

import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

/**
 * Builds solar-eclipse Besselian elements from apparent Sun and Moon positions.
 */
class BesselianEngine {

    data class Elements(
        val x: Double,
        val y: Double,
        val d: Double,
        val L1: Double,
        val L2: Double,
        val mu: Double,
        val tanf1: Double,
        val tanf2: Double
    )

    data class PolynomialResponse(val polynomials: Array<DoubleArray>, val t0Jde: Double)

    fun calculatePolynomials(jdeT0: Double): PolynomialResponse {
        val table = Array(5) { DoubleArray(8) }

        for (i in 0..4) {
            val hourOffset = (i - 2).toDouble()
            val jdeSample = jdeT0 + hourOffset / 24.0
            val elements = calculateElements(jdeSample)

            table[i][0] = elements.x
            table[i][1] = elements.y
            table[i][2] = elements.d
            table[i][3] = elements.L1
            table[i][4] = elements.L2
            table[i][5] = elements.mu
            table[i][6] = elements.tanf1
            table[i][7] = elements.tanf2
        }

        val polynomials = Array(5) { DoubleArray(8) }
        for (col in 0..7) {
            for (order in 0..4) {
                polynomials[order][col] = AstroMath.interpolationFromFiveTabularValues(
                    table[0][col],
                    table[1][col],
                    table[2][col],
                    table[3][col],
                    table[4][col],
                    order
                )
            }
        }

        return PolynomialResponse(polynomials, jdeT0)
    }

    internal fun calculateElements(jdeTD: Double): Elements {
        val sun = Vsop87SolarEngine.compute(jdeTD, 0.0)
        val moon = ElpMpp02LunarEngine.computeGeometric(jdeTD)
        val gast = AstroDataUtils.calculateGAST(jdeTD)

        val parallaxRatio = sin(AstroMath.rad(AstroMath.SOLAR_PARALLAX_ARCSEC / 3600.0)) /
            sun.distanceAU /
            sin(AstroMath.rad(moon.horizontalParallax))

        val g1 = cos(AstroMath.rad(sun.dec)) * cos(AstroMath.rad(sun.ra)) -
            parallaxRatio * cos(AstroMath.rad(moon.dec)) * cos(AstroMath.rad(moon.ra))
        val g2 = cos(AstroMath.rad(sun.dec)) * sin(AstroMath.rad(sun.ra)) -
            parallaxRatio * cos(AstroMath.rad(moon.dec)) * sin(AstroMath.rad(moon.ra))
        val g3 = sin(AstroMath.rad(sun.dec)) -
            parallaxRatio * sin(AstroMath.rad(moon.dec))

        val a = AstroMath.mod(AstroMath.deg(atan2(g2, g1)), 360.0)
        val d = AstroMath.deg(atan(g3 / sqrt(g1 * g1 + g2 * g2)))
        val g = sqrt(g1 * g1 + g2 * g2 + g3 * g3)

        val x = cos(AstroMath.rad(moon.dec)) *
            sin(AstroMath.rad(moon.ra - a)) /
            sin(AstroMath.rad(moon.horizontalParallax))
        val y = (
            sin(AstroMath.rad(moon.dec)) * cos(AstroMath.rad(d)) -
                cos(AstroMath.rad(moon.dec)) * sin(AstroMath.rad(d)) *
                cos(AstroMath.rad(moon.ra - a))
            ) / sin(AstroMath.rad(moon.horizontalParallax))
        val z = (
            sin(AstroMath.rad(moon.dec)) * sin(AstroMath.rad(d)) +
                cos(AstroMath.rad(moon.dec)) * cos(AstroMath.rad(d)) *
                cos(AstroMath.rad(moon.ra - a))
            ) / sin(AstroMath.rad(moon.horizontalParallax))

        val sinf1 = AstroMath.BESSELIAN_PENUMBRAL_CONE_SINE / (g * sun.distanceAU)
        val sinf2 = AstroMath.BESSELIAN_UMBRAL_CONE_SINE / (g * sun.distanceAU)
        val tanf1 = tan(asin(sinf1))
        val tanf2 = tan(asin(sinf2))

        val L1 = (z + AstroMath.SOLAR_ECLIPSE_PENUMBRAL_K / sinf1) * tanf1
        val L2 = (z - AstroMath.SOLAR_ECLIPSE_UMBRAL_K / sinf2) * tanf2
        val mu = AstroMath.mod(gast - a, 360.0)

        return Elements(x, y, d, L1, L2, mu, tanf1, tanf2)
    }
}
