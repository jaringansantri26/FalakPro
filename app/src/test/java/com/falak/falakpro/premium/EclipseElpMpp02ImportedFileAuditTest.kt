package com.falak.falakpro.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

class EclipseElpMpp02ImportedFileAuditTest {

    @Test
    fun importedEclipseElpMpp02FilesAreReadable() {
        val coef = ImportedElpMpp02Reader.load(IMPORT_DIR)

        assertEquals(1023, coef.mainLong.size)
        assertEquals(918, coef.mainLat.size)
        assertEquals(704, coef.mainDist.size)
        assertEquals(11314, coef.pertLongT0.size)
        assertEquals(1199, coef.pertLongT1.size)
        assertEquals(219, coef.pertLongT2.size)
        assertEquals(2, coef.pertLongT3.size)
        assertEquals(6462, coef.pertLatT0.size)
        assertEquals(516, coef.pertLatT1.size)
        assertEquals(52, coef.pertLatT2.size)
        assertEquals(12115, coef.pertDistT0.size)
        assertEquals(1165, coef.pertDistT1.size)
        assertEquals(210, coef.pertDistT2.size)
        assertEquals(2, coef.pertDistT3.size)
    }

    @Test
    fun importedEclipseElpMpp02EngineIsNotYetSafeAsFalakProReplacement() {
        val coef = ImportedElpMpp02Reader.load(IMPORT_DIR)
        val errorsKm = NASA_MOON_VECTORS.map { expected ->
            val t = ImportedElpMpp02Engine.jdToT(expected.jd)
            val actual = ImportedElpMpp02Engine.getPosition(t, coef)
            distanceErrorKm(expected.xyzKm, actual)
        }

        val minError = errorsKm.minOrNull() ?: error("No imported ELP samples tested")
        val maxError = errorsKm.maxOrNull() ?: error("No imported ELP samples tested")

        assertTrue(
            "Imported C:\\eclipse ELPMPP02 engine/data unexpectedly looks safe. Errors=$errorsKm",
            minError > 50_000.0
        )
        assertTrue(
            "Imported C:\\eclipse ELPMPP02 audit should expose the known large mismatch. Errors=$errorsKm",
            maxError > 60_000.0
        )
    }

    @Test
    fun printFalakFormulaUsingImportedEclipseBinaryFor2027Aug02() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }

        val jdeTd = 2461619.922100
        val coef = ImportedElpMpp02Reader.load(IMPORT_DIR)
        val falakOriginal = ElpMpp02LunarEngine.computeGeometric(jdeTd)
        val falakWithEclipseBinary = ImportedElpMpp02Engine.computeFalakStyle(jdeTd, coef)

        println("FalakPro original mpp02_core @ JDE(TD)=$jdeTd")
        println(formatPosition(falakOriginal))
        println("FalakPro formula + imported C:\\eclipse ELPMPP02 binaries @ JDE(TD)=$jdeTd")
        println(formatPosition(falakWithEclipseBinary))
        println(
            "Difference imported-original: " +
                    "dRA=${falakWithEclipseBinary.ra - falakOriginal.ra} deg, " +
                    "dDec=${falakWithEclipseBinary.dec - falakOriginal.dec} deg, " +
                    "dLon=${falakWithEclipseBinary.longitudeEcliptic - falakOriginal.longitudeEcliptic} deg, " +
                    "dLat=${falakWithEclipseBinary.latitudeEcliptic - falakOriginal.latitudeEcliptic} deg, " +
                    "dDist=${falakWithEclipseBinary.distanceAU - falakOriginal.distanceAU} AU"
        )
    }

    @Test
    fun printCompatibleGeneratedBinaryAgainstNasaFor2027Aug02() {
        resetElpDataProvider()
        File("src/main/assets/mpp02_core_from_elpmp02_de405_falakpro_compatible.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }

        val jdeTd = 2461619.922100
        val moon = ElpMpp02LunarEngine.computeGeometric(jdeTd)

        val nasaRaDeg = hmsToDeg(8, 49, 40.1)
        val nasaDecDeg = dmsToDeg(17, 53, 47.9)
        val nasaHpDeg = dmsToDeg(1, 1, 21.4)
        val nasaSdDeg = dmsToDeg(0, 16, 43.1)

        println("Compatible generated ELPMPP02 binary @ JDE(TD)=$jdeTd")
        println(formatPosition(moon))
        println("NASA target: RA=$nasaRaDeg, Dec=$nasaDecDeg, HP=$nasaHpDeg, SD=$nasaSdDeg")
        println(
            "Error compatible-NASA: " +
                    "dRA=${moon.ra - nasaRaDeg} deg, " +
                    "dDec=${moon.dec - nasaDecDeg} deg, " +
                    "dHP=${moon.horizontalParallax - nasaHpDeg} deg, " +
                    "dSD=${moon.semidiameter - nasaSdDeg} deg"
        )
    }

    private fun distanceErrorKm(expected: DoubleArray, actual: DoubleArray): Double {
        val dx = actual[0] - expected[0]
        val dy = actual[1] - expected[1]
        val dz = actual[2] - expected[2]
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    private fun formatPosition(position: ElpMpp02LunarEngine.LunarPosition): String {
        return "RA=${position.ra}, Dec=${position.dec}, " +
                "Lon=${position.longitudeEcliptic}, Lat=${position.latitudeEcliptic}, " +
                "DistAU=${position.distanceAU}, HP=${position.horizontalParallax}, SD=${position.semidiameter}"
    }

    private fun resetElpDataProvider() {
        val field = ElpDataProvider::class.java.getDeclaredField("flatData")
        field.isAccessible = true
        field.set(null, null)
    }

    private fun hmsToDeg(h: Int, m: Int, s: Double): Double = (h + m / 60.0 + s / 3600.0) * 15.0

    private fun dmsToDeg(d: Int, m: Int, s: Double): Double = d + m / 60.0 + s / 3600.0

    private data class NasaVector(val jd: Double, val xyzKm: DoubleArray)

    private companion object {
        private val IMPORT_DIR = File("src/main/assets/elpmpp02")

        private val NASA_MOON_VECTORS = listOf(
            NasaVector(2444239.5, doubleArrayOf(43890.2966088, 381188.7212318406, -31633.44485493895)),
            NasaVector(2446239.5, doubleArrayOf(-313664.5940101508, 212007.2839720472, 33744.68098025756)),
            NasaVector(2448239.5, doubleArrayOf(-273220.0672071042, -296859.7667683215, -34604.33005922842)),
            NasaVector(2450239.5, doubleArrayOf(171613.1321333133, -318097.3368940933, 31293.62250632574)),
            NasaVector(2452239.5, doubleArrayOf(396530.0117222191, 47487.89986876472, -36085.27889970440))
        )
    }
}

private data class ImportedElpMainTerm(
    val i1: Short,
    val i2: Short,
    val i3: Short,
    val i4: Short,
    val a: Double,
    val b1: Double,
    val b2: Double,
    val b3: Double,
    val b4: Double,
    val b5: Double,
    val b6: Double
)

private data class ImportedElpPertTerm(
    val i: ShortArray,
    val a: Double,
    val phi0: Double
)

private data class ImportedElpCoefficients(
    val mainLong: List<ImportedElpMainTerm>,
    val mainLat: List<ImportedElpMainTerm>,
    val mainDist: List<ImportedElpMainTerm>,
    val pertLongT0: List<ImportedElpPertTerm>,
    val pertLongT1: List<ImportedElpPertTerm>,
    val pertLongT2: List<ImportedElpPertTerm>,
    val pertLongT3: List<ImportedElpPertTerm>,
    val pertLatT0: List<ImportedElpPertTerm>,
    val pertLatT1: List<ImportedElpPertTerm>,
    val pertLatT2: List<ImportedElpPertTerm>,
    val pertDistT0: List<ImportedElpPertTerm>,
    val pertDistT1: List<ImportedElpPertTerm>,
    val pertDistT2: List<ImportedElpPertTerm>,
    val pertDistT3: List<ImportedElpPertTerm>
)

private object ImportedElpMpp02Reader {
    private const val MAIN_BYTES_PER_TERM = 64
    private const val PERT_BYTES_PER_TERM = 42

    fun load(dir: File): ImportedElpCoefficients {
        require(dir.isDirectory) { "Imported ELPMPP02 directory not found: ${dir.absolutePath}" }

        return ImportedElpCoefficients(
            mainLong = mainTerms(dir, "elp_main_long.bin"),
            mainLat = mainTerms(dir, "elp_main_lat.bin"),
            mainDist = mainTerms(dir, "elp_main_dist.bin"),
            pertLongT0 = pertTerms(dir, "elp_pert_longT0.bin"),
            pertLongT1 = pertTerms(dir, "elp_pert_longT1.bin"),
            pertLongT2 = pertTerms(dir, "elp_pert_longT2.bin"),
            pertLongT3 = pertTerms(dir, "elp_pert_longT3.bin"),
            pertLatT0 = pertTerms(dir, "elp_pert_latT0.bin"),
            pertLatT1 = pertTerms(dir, "elp_pert_latT1.bin"),
            pertLatT2 = pertTerms(dir, "elp_pert_latT2.bin"),
            pertDistT0 = pertTerms(dir, "elp_pert_distT0.bin"),
            pertDistT1 = pertTerms(dir, "elp_pert_distT1.bin"),
            pertDistT2 = pertTerms(dir, "elp_pert_distT2.bin"),
            pertDistT3 = pertTerms(dir, "elp_pert_distT3.bin")
        )
    }

    private fun mainTerms(dir: File, name: String): List<ImportedElpMainTerm> {
        val buf = readBuffer(File(dir, name))
        val n = buf.int
        require(buf.remaining() == n * MAIN_BYTES_PER_TERM) {
            "Invalid imported ELP main file size for $name: n=$n remaining=${buf.remaining()}"
        }

        return List(n) {
            ImportedElpMainTerm(
                i1 = buf.short,
                i2 = buf.short,
                i3 = buf.short,
                i4 = buf.short,
                a = buf.double,
                b1 = buf.double,
                b2 = buf.double,
                b3 = buf.double,
                b4 = buf.double,
                b5 = buf.double,
                b6 = buf.double
            )
        }
    }

    private fun pertTerms(dir: File, name: String): List<ImportedElpPertTerm> {
        val buf = readBuffer(File(dir, name))
        val n = buf.int
        require(buf.remaining() == n * PERT_BYTES_PER_TERM) {
            "Invalid imported ELP perturbation file size for $name: n=$n remaining=${buf.remaining()}"
        }

        return List(n) {
            val ix = ShortArray(13) { buf.short }
            ImportedElpPertTerm(i = ix, a = buf.double, phi0 = buf.double)
        }
    }

    private fun readBuffer(file: File): ByteBuffer {
        require(file.isFile) { "Imported ELPMPP02 file not found: ${file.absolutePath}" }
        return ByteBuffer.wrap(file.readBytes()).order(ByteOrder.LITTLE_ENDIAN)
    }
}

private object ImportedElpMpp02Engine {
    private const val DW1_0 = -0.07008
    private const val DW2_0 = 0.20794
    private const val DW3_0 = -0.07215
    private const val DW1_1 = -0.35106
    private const val DW2_1 = 0.08017
    private const val DW3_1 = -0.04317
    private const val DW1_2 = -0.03743
    private const val DGAM = 0.00085
    private const val DE = -0.00006
    private const val DEART_0 = -0.00033
    private const val DEART_1 = 0.00732
    private const val DPERI = -0.00749
    private const val DEP = 0.00224
    private const val DW1_3 = -0.00018865
    private const val DW1_4 = -0.00001024
    private const val DW2_2 = 0.00470602
    private const val DW2_3 = -0.00025213
    private const val DW3_2 = -0.00261070
    private const val DW3_3 = -0.00010712

    private const val M_CONST = 0.074801329
    private const val ALPHA = 0.002571881
    private const val RA0 = 384747.961370173 / 384747.980674318

    private val b0 = arrayOf(
        doubleArrayOf(0.311079095, -0.103837907),
        doubleArrayOf(-0.004482398, 0.000668287),
        doubleArrayOf(-0.001102485, -0.001298072),
        doubleArrayOf(0.001056062, -0.000178028),
        doubleArrayOf(0.000050928, -0.000037342)
    )

    private val w1_1 = 1732559343.73604 + DW1_1
    private val w2_1 = 14643420.3171 + DW2_1
    private val w3_1 = -6967919.5383 + DW3_1
    private val deltaNu = 0.55604 + DW1_1
    private val deltaGam = -0.08066 + DGAM
    private val deltaE = 0.01789 + DE
    private val deltaE0 = -0.12879 + DEP
    private val deltaN0 = -0.0642 + DEART_1

    private val dW2_1: Double
    private val dW3_1: Double
    private val fA: Double
    private val fB1: Double
    private val fB2: Double
    private val fB3: Double
    private val fB4: Double
    private val fB5: Double

    init {
        val sum25 = b0[0][0] + (2.0 * ALPHA / (3.0 * M_CONST)) * b0[4][0]
        val sum35 = b0[0][1] + (2.0 * ALPHA / (3.0 * M_CONST)) * b0[4][1]

        dW2_1 = ((w2_1 / w1_1) - M_CONST) * sum25 * DW1_1 +
                sum25 * DEART_1 +
                w1_1 * (b0[1][0] * DGAM + b0[2][0] * DE + b0[3][0] * DEP)
        dW3_1 = ((w3_1 / w1_1) - M_CONST) * sum35 * DW1_1 +
                sum35 * DEART_1 +
                w1_1 * (b0[1][1] * DGAM + b0[2][1] * DE + b0[3][1] * DEP)

        fA = 1.0 - (2.0 * deltaNu) / (3.0 * w1_1)
        fB1 = (deltaN0 - M_CONST * deltaNu) / w1_1
        fB2 = deltaGam
        fB3 = deltaE
        fB4 = deltaE0
        fB5 = (2.0 * ALPHA / (3.0 * M_CONST * w1_1)) * (deltaN0 - M_CONST * deltaNu)
    }

    fun getPosition(t: Double, coef: ImportedElpCoefficients): DoubleArray {
        val args = computeArguments(t)
        val t2 = t * t
        val t3 = t2 * t
        val v = args[13] +
                evalMainSeries(coef.mainLong, args, false) +
                evalPertSeries(coef.pertLongT0, args) +
                evalPertSeries(coef.pertLongT1, args) * t +
                evalPertSeries(coef.pertLongT2, args) * t2 +
                evalPertSeries(coef.pertLongT3, args) * t3

        val u = evalMainSeries(coef.mainLat, args, false) +
                evalPertSeries(coef.pertLatT0, args) +
                evalPertSeries(coef.pertLatT1, args) * t +
                evalPertSeries(coef.pertLatT2, args) * t2

        val r = RA0 * (
                evalMainSeries(coef.mainDist, args, true) +
                        evalPertSeries(coef.pertDistT0, args) +
                        evalPertSeries(coef.pertDistT1, args) * t +
                        evalPertSeries(coef.pertDistT2, args) * t2 +
                        evalPertSeries(coef.pertDistT3, args) * t3
                )

        return eclipticToJ2000(v, u, r, t)
    }

    fun computeFalakStyle(jdeTd: Double, coef: ImportedElpCoefficients): ElpMpp02LunarEngine.LunarPosition {
        val t = jdToT(jdeTd)
        val sums = getFalakCompatibleSums(t, coef)

        val t2 = t * t
        val t3 = t * t2
        val t4 = t * t3
        val w = 3.81034409083088 +
                8399.68473007193 * t +
                -0.0000331895204255009 * t2 +
                3.11024944910606E-08 * t3 +
                -2.03282376489228E-10 * t4
        val p = (5029.0966 - 0.29965) * t +
                1.112 * t2 +
                0.000077 * t3 +
                -0.00002353 * t4

        val moonTrueLon = AstroMath.mod(Math.toDegrees(w) + sums.longitudeArcsec / 3600.0 + p / 3600.0, 360.0)
        val nutation = AstroDataUtils.calculateNutation(jdeTd).first
        val aberrLon = -0.00019524 - 0.00001059 * sin(Math.toRadians(225.0 + 477198.9 * t))
        val moonAppaLon = moonTrueLon + nutation + aberrLon

        val aberrLat = -0.00001754 * sin(Math.toRadians(183.3 + 483202.0 * t))
        val moonAppaLat = sums.latitudeArcsec / 3600.0 + aberrLat

        val distAbr = 0.0708 * cos(Math.toRadians(225.0 + 477198.9 * t))
        val distKm = sums.distanceKm + distAbr
        val coords = AstroDataUtils.eclipticToEquatorialApparent(moonAppaLon, moonAppaLat, jdeTd)
        val hpDeg = Math.toDegrees(kotlin.math.asin(6378.14 / distKm))
        val sdDeg = Math.toDegrees(kotlin.math.asin(0.272481 * sin(Math.toRadians(hpDeg))))

        return ElpMpp02LunarEngine.LunarPosition(
            ra = coords.first,
            dec = coords.second,
            longitudeEcliptic = moonAppaLon,
            latitudeEcliptic = moonAppaLat,
            distanceAU = distKm / 149597870.7,
            horizontalParallax = hpDeg,
            semidiameter = sdDeg
        )
    }

    fun jdToT(jd: Double): Double = (jd - 2451545.0) / 36525.0

    private data class FalakSums(
        val longitudeArcsec: Double,
        val latitudeArcsec: Double,
        val distanceKm: Double
    )

    private fun getFalakCompatibleSums(t: Double, coef: ImportedElpCoefficients): FalakSums {
        val args = computeArguments(t)
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        val radToArcsec = 648000.0 / PI

        val eclipseLongitudeArcsec = (
                evalMainSeries(coef.mainLong, args, false) +
                        evalPertSeries(coef.pertLongT0, args) +
                        evalPertSeries(coef.pertLongT1, args) * t +
                        evalPertSeries(coef.pertLongT2, args) * t2 +
                        evalPertSeries(coef.pertLongT3, args) * t3
                ) * radToArcsec

        val longitudeConventionCorrection =
            -6.41478999971878 +
                    -5028.79695010185 * t +
                    -1.11200000000001 * t2 +
                    -0.000077 * t3 +
                    0.00002353 * t4

        val latitudeArcsec = (
                evalMainSeries(coef.mainLat, args, false) +
                        evalPertSeries(coef.pertLatT0, args) +
                        evalPertSeries(coef.pertLatT1, args) * t +
                        evalPertSeries(coef.pertLatT2, args) * t2
                ) * radToArcsec

        val distanceKm = RA0 * (
                evalMainSeries(coef.mainDist, args, true) +
                        evalPertSeries(coef.pertDistT0, args) +
                        evalPertSeries(coef.pertDistT1, args) * t +
                        evalPertSeries(coef.pertDistT2, args) * t2 +
                        evalPertSeries(coef.pertDistT3, args) * t3
                )

        return FalakSums(
            longitudeArcsec = eclipseLongitudeArcsec + longitudeConventionCorrection,
            latitudeArcsec = latitudeArcsec,
            distanceKm = distanceKm
        )
    }

    private fun computeArguments(t: Double): DoubleArray {
        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t

        val w1 = (218.3148724777778 * 3600.0 + DW1_0) +
                (1732559343.73604 + DW1_1) * t +
                (-6.8084 + DW1_2) * t2 +
                (0.006604 + DW1_3) * t3 +
                (-0.00003169 + DW1_4) * t4
        val w2 = (83.3530741944444 * 3600.0 + DW2_0) +
                (14643420.3171 + DW2_1 + dW2_1) * t +
                (-38.2631 + DW2_2) * t2 +
                (-0.045047 + DW2_3) * t3 +
                0.00021301 * t4
        val w3 = (125.0444814444444 * 3600.0 + DW3_0) +
                (-6967919.5383 + DW3_1 + dW3_1) * t +
                (6.359 + DW3_2) * t2 +
                (0.007625 + DW3_3) * t3 +
                (-0.00003586) * t4
        val ea = (100.4664499722222 * 3600.0 + DEART_0) +
                (129597742.293 + DEART_1) * t +
                (-0.0202) * t2 +
                9e-4 * t3 +
                1.5e-7 * t4
        val peri = (102.9373481666667 * 3600.0 + DPERI) +
                1161.24342 * t +
                0.529265 * t2 +
                (-1.1814e-4) * t3 +
                1.1379e-5 * t4

        val arcsecToRad = PI / 648000.0
        return doubleArrayOf(
            (w1 - ea + 648000.0) * arcsecToRad,
            (w1 - w3) * arcsecToRad,
            (w1 - w2) * arcsecToRad,
            (ea - peri) * arcsecToRad,
            (252.2503991388889 * 3600.0 + 538101628.6689 * t) * arcsecToRad,
            (181.9797883333333 * 3600.0 + 210664136.4578 * t) * arcsecToRad,
            (100.4664499722222 * 3600.0 + 129597742.293 * t) * arcsecToRad,
            (355.4332961111111 * 3600.0 + 68905077.6594 * t) * arcsecToRad,
            (34.3514845555556 * 3600.0 + 10925660.5734 * t) * arcsecToRad,
            (50.0774744722222 * 3600.0 + 4399609.3363 * t) * arcsecToRad,
            (314.0556509722222 * 3600.0 + 1542482.5785 * t) * arcsecToRad,
            (304.3488800277778 * 3600.0 + 786547.897 * t) * arcsecToRad,
            (w1 + 5028.79695 * t) * arcsecToRad,
            w1 * arcsecToRad
        )
    }

    private fun evalMainSeries(
        terms: List<ImportedElpMainTerm>,
        args: DoubleArray,
        isDist: Boolean
    ): Double {
        var sum = 0.0
        val d = args[0]
        val f = args[1]
        val l = args[2]
        val lp = args[3]
        for (term in terms) {
            val phase = term.i1.toDouble() * d +
                    term.i2.toDouble() * f +
                    term.i3.toDouble() * l +
                    term.i4.toDouble() * lp
            val aTilde = if (isDist) {
                fA * term.a + fB1 * term.b1 + fB2 * term.b2 + fB3 * term.b3 + fB4 * term.b4 + fB5 * term.b5
            } else {
                term.a + fB1 * term.b1 + fB2 * term.b2 + fB3 * term.b3 + fB4 * term.b4 + fB5 * term.b5
            }
            sum += if (isDist) aTilde * cos(phase) else aTilde * sin(phase)
        }
        return sum
    }

    private fun evalPertSeries(terms: List<ImportedElpPertTerm>, args: DoubleArray): Double {
        var sum = 0.0
        for (term in terms) {
            var phase = term.phi0
            for (k in 0 until 13) {
                phase += term.i[k].toDouble() * args[k]
            }
            sum += term.a * sin(phase)
        }
        return sum
    }

    private fun eclipticToJ2000(v: Double, u: Double, r: Double, t: Double): DoubleArray {
        val cosV = cos(v)
        val sinV = sin(v)
        val cosU = cos(u)
        val sinU = sin(u)

        val x0 = r * cosV * cosU
        val y0 = r * sinV * cosU
        val z0 = r * sinU

        val t2 = t * t
        val t3 = t2 * t
        val t4 = t3 * t
        val t5 = t4 * t
        val p = 0.10180391e-4 * t + 0.47020439e-6 * t2 - 0.5417367e-9 * t3 -
                0.2507948e-11 * t4 + 0.463486e-14 * t5
        val q = -0.113469002e-3 * t + 0.12372674e-6 * t2 + 0.1265417e-8 * t3 -
                0.1371808e-11 * t4 - 0.320334e-14 * t5

        val p2 = p * p
        val q2 = q * q
        val sqr = sqrt(max(0.0, 1.0 - p2 - q2))

        return doubleArrayOf(
            (1.0 - 2.0 * p2) * x0 + 2.0 * p * q * y0 + 2.0 * p * sqr * z0,
            2.0 * p * q * x0 + (1.0 - 2.0 * q2) * y0 - 2.0 * q * sqr * z0,
            -2.0 * p * sqr * x0 + 2.0 * q * sqr * y0 + (1.0 - 2.0 * p2 - 2.0 * q2) * z0
        )
    }
}
