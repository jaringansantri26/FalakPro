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
        val n = Nutation()
        // PERSIS v1.8 Nutation (105 terms)
        return Pair(n.nutationInLongitude(jde), n.nutationInObliquity(jde))
    }

    fun calculateTrueObliquity(jde: Double): Double {
        val t = (jde - 2451545.0) / 36525.0
        val u = t / 100.0
        val eps0 = 23.0 + 26.0/60.0 + 21.448/3600.0 + (-4680.93*u - 1.55*u*u + 1999.25*u.pow(3) - 51.38*u.pow(4) - 249.67*u.pow(5) - 39.05*u.pow(6) + 7.12*u.pow(7) + 27.87*u.pow(8) + 5.79*u.pow(9) + 2.45*u.pow(10)) / 3600.0
        return eps0 + calculateNutation(jde).second
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
        val t = (jde - 2451545.0) / 36525.0
        val u = t / 100.0
        val eps0 = 23.0 + 26.0/60.0 + 21.448/3600.0 + (-4680.93*u - 1.55*u*u + 1999.25*u.pow(3) - 51.38*u.pow(4) - 249.67*u.pow(5) - 39.05*u.pow(6) + 7.12*u.pow(7) + 27.87*u.pow(8) + 5.79*u.pow(9) + 2.45*u.pow(10)) / 3600.0
        val l = rad(lambdaTrue); val b = rad(beta)
        val ra = atan2(sin(l) * cos(rad(eps0)) - tan(b) * sin(rad(eps0)), cos(l))
        val dec = asin(sin(b) * cos(rad(eps0)) + cos(b) * sin(rad(eps0)) * sin(l))
        return Pair(deg(mod(ra, 2 * PI)), deg(dec))
    }
}
