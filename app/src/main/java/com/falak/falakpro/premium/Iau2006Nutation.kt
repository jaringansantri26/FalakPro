package com.falak.falakpro.premium

import java.io.DataInputStream
import java.io.InputStream
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

data class NutationResult(
    val deltaPsiDeg: Double,
    val deltaEpsilonDeg: Double
)

object Iau2006Nutation {
    private const val MAGIC = "FPNUT2A\u0000"
    private const val ARCSEC_TO_RAD = PI / 648000.0
    private const val ARCSEC_TO_DEG = 1.0 / 3600.0
    private val lock = Any()

    @Volatile
    private var terms: Array<Term>? = null

    val isInitialized: Boolean
        get() = terms != null

    fun initialize(input: InputStream) {
        if (terms != null) return
        synchronized(lock) {
            if (terms != null) return
            terms = DataInputStream(input.buffered()).use { data ->
                val magic = ByteArray(8)
                data.readFully(magic)
                require(String(magic, Charsets.US_ASCII) == MAGIC) { "Invalid IAU 2000A nutation binary" }
                val count = data.readInt()
                Array(count) {
                    val multipliers = IntArray(14)
                    repeat(14) { index ->
                        multipliers[index] = data.readByte().toInt()
                    }
                    Term(
                        multipliers = multipliers,
                        aa = data.readDouble(),
                        bb = data.readDouble(),
                        cc = data.readDouble(),
                        dd = data.readDouble(),
                        ee = data.readDouble(),
                        ff = data.readDouble()
                    )
                }
            }
        }
    }

    fun compute(jde: Double): NutationResult {
        val activeTerms = terms
        if (activeTerms != null) {
            return computeIau2006(jde, activeTerms)
        }

        val t = julianCenturies(jde)
        val fallback = NutationEngine.calculate(t)
        val fj2 = -2.7774e-6 * t
        return NutationResult(
            deltaPsiDeg = fallback.first * (1.0 + 0.4697e-6 + fj2),
            deltaEpsilonDeg = fallback.second * (1.0 + fj2)
        )
    }

    fun meanObliquityDeg(jde: Double): Double {
        val t = julianCenturies(jde)
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        val t5 = t4 * t
        return (
            84381.406 -
                46.836769 * t -
                0.0001831 * t2 +
                0.00200340 * t3 -
                0.000000576 * t4 -
                0.0000000434 * t5
            ) * ARCSEC_TO_DEG
    }

    fun trueObliquityDeg(jde: Double): Double =
        meanObliquityDeg(jde) + compute(jde).deltaEpsilonDeg

    private fun computeIau2006(jde: Double, activeTerms: Array<Term>): NutationResult {
        val t = julianCenturies(jde)
        val args = fundamentalArguments(t)
        var dpsiArcsec = 0.0
        var depsArcsec = 0.0

        for (term in activeTerms) {
            var arg = 0.0
            for (i in 0 until 14) {
                arg += term.multipliers[i] * args[i]
            }
            dpsiArcsec += (term.aa + term.bb * t) * sin(arg) + term.cc * cos(arg)
            depsArcsec += (term.dd + term.ee * t) * cos(arg) + term.ff * sin(arg)
        }

        val fj2 = -2.7774e-6 * t
        return NutationResult(
            deltaPsiDeg = dpsiArcsec * ARCSEC_TO_DEG * (1.0 + 0.4697e-6 + fj2),
            deltaEpsilonDeg = depsArcsec * ARCSEC_TO_DEG * (1.0 + fj2)
        )
    }

    private fun fundamentalArguments(t: Double): DoubleArray {
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        return doubleArrayOf(
            normalizeRadians(4.402608842 + 2608.7903141574 * t),
            normalizeRadians(3.176146697 + 1021.3285546211 * t),
            normalizeRadians(1.753470314 + 628.3075849991 * t),
            normalizeRadians(6.203480913 + 334.0612426700 * t),
            normalizeRadians(0.599546497 + 52.9690962641 * t),
            normalizeRadians(0.874016757 + 21.3299104960 * t),
            normalizeRadians(5.481293872 + 7.4781598567 * t),
            normalizeRadians(5.311886287 + 3.8133035638 * t),
            (0.024381750 + 0.00000538691 * t) * t,
            arcsecToRadians(485868.249036 + 1717915923.2178 * t + 31.8792 * t2 + 0.051635 * t3 - 0.00024470 * t4),
            arcsecToRadians(1287104.79305 + 129596581.0481 * t - 0.5532 * t2 + 0.000136 * t3 - 0.00001149 * t4),
            arcsecToRadians(335779.526232 + 1739527262.8478 * t - 12.7512 * t2 - 0.001037 * t3 + 0.00000417 * t4),
            arcsecToRadians(1072260.70369 + 1602961601.2090 * t - 6.3706 * t2 + 0.006593 * t3 - 0.00003169 * t4),
            arcsecToRadians(450160.398036 - 6962890.5431 * t + 7.4722 * t2 + 0.007702 * t3 - 0.00005939 * t4)
        )
    }

    private fun julianCenturies(jde: Double): Double =
        (jde - 2451545.0) / 36525.0

    private fun arcsecToRadians(arcsec: Double): Double =
        normalizeRadians(arcsec * ARCSEC_TO_RAD)

    private fun normalizeRadians(value: Double): Double {
        val twoPi = 2.0 * PI
        val mod = value % twoPi
        return if (mod < 0.0) mod + twoPi else mod
    }

    private data class Term(
        val multipliers: IntArray,
        val aa: Double,
        val bb: Double,
        val cc: Double,
        val dd: Double,
        val ee: Double,
        val ff: Double
    )
}
