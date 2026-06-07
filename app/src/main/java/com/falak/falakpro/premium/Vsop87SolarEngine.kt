package com.falak.falakpro.premium

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.*

/**
 * Vsop87SolarEngine — 100% PERSIS v1.8 Parity Edition.
 * Adopts the 21-term sunAberration polynomial and exact FK5 flow.
 */
object Vsop87SolarEngine {

    data class SunPosition(
        val ra: Double, val dec: Double, val distanceAU: Double,
        val longitudeEcliptic: Double, val latitudeEcliptic: Double
    )

    private var data: Vsop87Data? = null

    val isInitialized: Boolean get() = data != null

    fun initialize(inputStream: InputStream) {
        if (data != null) return
        try {
            val bytes = inputStream.readBytes()
            val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
            val firstInt = buffer.int
            if (firstInt > 10 || firstInt < 0) buffer.position(17) else buffer.position(0)

            fun readVar(): Array<Array<DoubleArray>> {
                if (buffer.remaining() < 4) return emptyArray()
                val nOrders = buffer.int.coerceIn(0, 10)
                return Array(nOrders) {
                    val nTerms = buffer.int.coerceIn(0, 2000)
                    Array(nTerms) { doubleArrayOf(buffer.double, buffer.double, buffer.double) }
                }
            }
            data = Vsop87Data(readVar(), readVar(), readVar())
        } catch (e: Exception) { e.printStackTrace() }
    }

    fun initializeWithFile(path: String) {
        val file = java.io.File(path)
        if (file.exists()) initialize(file.inputStream())
    }

    fun compute(jdeTD: Double, deltaT: Double = 0.0, withAberration: Boolean = true): SunPosition {
        val d = data ?: return computeAnalytical(jdeTD, deltaT)
        val tCenturies = (jdeTD - 2451545.0) / 36525.0
        val tau = tCenturies / 10.0
        
        val lRad = evalSeries(d.L, tau); val bRad = evalSeries(d.B, tau); val rAU = evalSeries(d.R, tau)
        val theta = Math.toDegrees(lRad) + 180.0
        val beta = -Math.toDegrees(bRad)
        
        // PERSIS v1.8 FK5 Correction (SunDatas.kt:271-274, 300-302)
        val tFK5 = tCenturies
        val lp = theta - 1.397 * tFK5 - 0.00031 * tFK5 * tFK5
        val deltaL = (-0.09033 + 0.03916 * (cos(rad(lp)) + sin(rad(lp))) * tan(rad(beta))) / 3600.0
        val deltaB = (0.03916 * (cos(rad(lp)) - sin(rad(lp)))) / 3600.0
        
        val thetaFK5 = theta + deltaL
        val betaFK5 = beta + deltaB

        // PERSIS v1.8 Sun Aberration (SunDatas.kt:229-250)
        // Ref: Meeus Ch.25; for Besselian elements (solar eclipse), aberration is omitted
        val aberration = if (!withAberration) 0.0 else {
            val dL = 3548.330 +
                    118.568 * sin(rad(87.5287 + 359993.7286 * tau)) +
                    2.476 * sin(rad(85.0561 + 719987.4571 * tau)) +
                    1.376 * sin(rad(27.8502 + 4452671.1152 * tau)) +
                    0.119 * sin(rad(73.1375 + 450368.8564 * tau)) +
                    0.114 * sin(rad(337.2264 + 329644.6718 * tau)) +
                    0.086 * sin(rad(222.5400 + 659289.3436 * tau)) +
                    0.078 * sin(rad(162.8136 + 9224659.7915 * tau)) +
                    0.054 * sin(rad(82.5823 + 1079981.1857 * tau)) +
                    0.052 * sin(rad(171.5189 + 225184.4282 * tau)) +
                    0.034 * sin(rad(30.3214 + 4092677.3866 * tau)) +
                    0.033 * sin(rad(119.8105 + 337181.4711 * tau)) +
                    0.023 * sin(rad(247.5418 + 299295.6151 * tau)) +
                    0.023 * sin(rad(325.1526 + 315559.5560 * tau)) +
                    0.021 * sin(rad(155.1241 + 675553.2846 * tau)) +
                    7.311 * tau * sin(rad(333.4515 + 359993.7286 * tau)) +
                    0.305 * tau * sin(rad(330.9814 + 719987.4571 * tau)) +
                    0.010 * tau * sin(rad(328.5170 + 1079981.1857 * tau)) +
                    0.309 * tau * tau * sin(rad(241.4518 + 359993.7286 * tau)) +
                    0.021 * tau * tau * sin(rad(205.0482 + 719987.4571 * tau)) +
                    0.004 * tau * tau * sin(rad(297.8610 + 4452671.1152 * tau)) +
                    0.010 * tau * tau * tau * sin(rad(154.7066 + 359993.7286 * tau))
            (-0.005775518 * rAU * dL) / 3600.0
        }
        val nutation = AstroDataUtils.calculateNutation(jdeTD).first

        // Match PERSIS exactly: Join components before transformation
        val finalLon = AstroDataUtils.mod(thetaFK5 + nutation + aberration, 360.0)

        // Use AstroDataUtils for final RA/Dec (Matches SunDatas.kt:310-342)
        val (ra, dec) = AstroDataUtils.eclipticToEquatorialApparent(finalLon, betaFK5, jdeTD)
        return SunPosition(ra, dec, rAU, finalLon, betaFK5)
    }

    fun computeGeometric(jdeTD: Double, deltaT: Double = 0.0): SunPosition {
        val d = data ?: return computeAnalytical(jdeTD, deltaT)
        val tCenturies = (jdeTD - 2451545.0) / 36525.0
        val tau = tCenturies / 10.0
        
        val lRad = evalSeries(d.L, tau); val bRad = evalSeries(d.B, tau); val rAU = evalSeries(d.R, tau)
        var theta = Math.toDegrees(lRad) + 180.0
        val beta = -Math.toDegrees(bRad)
        theta = (theta % 360.0 + 360.0) % 360.0

        val lambdaP = (theta - 1.397 * tCenturies - 0.00031 * tCenturies * tCenturies) % 360.0
        val deltaTheta = (-0.09033 + 0.03916 * (cos(rad(lambdaP)) + sin(rad(lambdaP))) * tan(rad(beta))) / 3600.0
        val thetaFK5 = theta + deltaTheta
        val deltaBeta = (0.03916 * (cos(rad(lambdaP)) - sin(rad(lambdaP)))) / 3600.0
        val betaFK5 = beta + deltaBeta

        val pos = AstroDataUtils.eclipticToEquatorial(thetaFK5, betaFK5, jdeTD)
        return SunPosition(pos.first, pos.second, rAU, thetaFK5, betaFK5)
    }

    fun computeGHA(jdeTD: Double): Double {
        val sun = compute(jdeTD)
        val gast = AstroDataUtils.calculateGAST(jdeTD)
        return AstroMath.mod(gast - sun.ra, 360.0)
    }

    private fun computeAnalytical(jdeTD: Double, deltaT: Double): SunPosition {
        val t = (jdeTD - 2451545.0) / 36525.0
        val l0 = 280.46646 + 36000.76983 * t
        val lon = l0 % 360.0
        val (ra, dec) = AstroDataUtils.eclipticToEquatorialApparent(lon, 0.0, jdeTD)
        return SunPosition(ra, dec, 1.0, lon, 0.0)
    }

    private fun evalSeries(series: Array<Array<DoubleArray>>, tau: Double): Double {
        var res = 0.0; var p = 1.0
        for (order in series) {
            var s = 0.0
            for (term in order) s += term[0] * cos(term[1] + term[2] * tau)
            res += s * p; p *= tau
        }
        return res
    }

    private fun rad(d: Double) = AstroMath.rad(d)
    class Vsop87Data(val L: Array<Array<DoubleArray>>, val B: Array<Array<DoubleArray>>, val R: Array<Array<DoubleArray>>)
}
