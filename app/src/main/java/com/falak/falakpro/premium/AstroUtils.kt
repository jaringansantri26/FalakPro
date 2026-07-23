package com.falak.falakpro.premium

import kotlin.math.*

/**
 * AstroDataUtils — 100% PERSIS v1.8 Parity Edition.
 * Implementation of the full IAU 1980 Nutation theory and exact Obliquity polynomials.
 * Updated with all PERSIS v1.8 nutation terms.
 */
object AstroDataUtils {

    fun rad(deg: Double): Double = deg * PI / 180.0
    fun deg(rad: Double): Double = rad * 180.0 / PI

    fun mod(a: Double, b: Double): Double {
        val r = a % b
        return if (r < 0) r + b else r
    }

    fun calculateNutation(jde: Double): Pair<Double, Double> {
        val nutation = Iau2006Nutation.compute(jde)
        return Pair(nutation.deltaPsiDeg, nutation.deltaEpsilonDeg)
    }

    fun calculateTrueObliquity(jde: Double): Double {
        return Iau2006Nutation.trueObliquityDeg(jde)
    }

    fun calculateGAST(jdUT: Double): Double {
        val deltaT = DynamicalTimeEngine.deltaT(jdUT)
        val jdTD = jdUT + deltaT / 86400.0
        
        // 1. GMST menggunakan JD UT1 (estimasi JD UT)
        val tUT = (jdUT - 2451545.0) / 36525.0
        val gmst = mod(280.46061837 + 360.98564736629 * (jdUT - 2451545.0) + 0.000387933 * tUT * tUT - tUT * tUT * tUT / 38710000.0, 360.0)
        
        // 2. Persamaan Ekuinoks (Equation of Equinoxes) menggunakan JD TD untuk Nutasi
        val nutation = calculateNutation(jdTD)
        val epsilon = calculateTrueObliquity(jdTD)
        
        return mod(gmst + nutation.first * cos(rad(epsilon)), 360.0)
    }

    fun eclipticToEquatorialApparent(lambdaAppa: Double, beta: Double, jde: Double): Pair<Double, Double> {
        val eps = rad(calculateTrueObliquity(jde))
        val l = rad(lambdaAppa); val b = rad(beta)
        val ra = atan2(sin(l) * cos(eps) - tan(b) * sin(eps), cos(l))
        val dec = asin(sin(b) * cos(eps) + cos(b) * sin(eps) * sin(l))
        return Pair(deg(mod(ra, 2 * PI)), deg(dec))
    }

    fun eclipticToEquatorial(lambdaTrue: Double, beta: Double, jde: Double): Pair<Double, Double> {
        val eps0 = Iau2006Nutation.meanObliquityDeg(jde)
        val l = rad(lambdaTrue); val b = rad(beta)
        val ra = atan2(sin(l) * cos(rad(eps0)) - tan(b) * sin(rad(eps0)), cos(l))
        val dec = asin(sin(b) * cos(rad(eps0)) + cos(b) * sin(rad(eps0)) * sin(l))
        return Pair(deg(mod(ra, 2 * PI)), deg(dec))
    }
}
