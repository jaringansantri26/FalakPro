package com.falak.falakpro.ui

import android.content.Context
import com.falak.falakpro.premium.AstroAssetPreloader
import kotlin.math.asin

object DataFalakCalculationService {

    suspend fun compute(
        context: Context,
        mode: Int,
        year: Int,
        month: Int,
        day: Int
    ): DataFalakResult {
        AstroAssetPreloader.ensureCore(context)
        return if (mode == 0) {
            DataFalakResult.Ephemeris(
                EphemerisGenerator.computeDay(year, month, day, context)
            )
        } else {
            // Screen: hitung hanya 1 hari agar cepat
            DataFalakResult.Almanac(
                days = listOf(EphemerisGenerator.computeDay(year, month, day, context)),
                moonHiRes = emptyList()
            )
        }
    }

    /**
     * Hitung 3 hari untuk keperluan cetak PDF.
     * Dipanggil hanya saat user menekan tombol CETAK PDF.
     */
    suspend fun compute3DaysForPrint(
        context: Context,
        year: Int,
        month: Int,
        day: Int
    ): DataFalakResult.Almanac {
        AstroAssetPreloader.ensureCore(context)
        val baseJd = Julian.fromCalendar(year, month, day.toDouble())
        val almanacDays = (0..2).map { dayOffset ->
            val date = Julian.toCalendar(baseJd + dayOffset.toDouble())
            EphemerisGenerator.computeDay(date.year, date.month, date.day, context)
        }
        return DataFalakResult.Almanac(
            days = almanacDays,
            moonHiRes = computeMoonHiRes(context, year, baseJd)
        )
    }

    private fun computeMoonHiRes(
        context: Context,
        year: Int,
        baseJd: Double
    ): List<List<AlmanacGenerator.MoonPoint>> {
        val elpMoon = ElpFactory.createMoon(context)
        val earth = VsopFactory.createEarth(context)
        val nutEngine = NutationIAU2000A(context)
        val deltaT = DeltaT.estimate(year.toDouble())

        return (0..2).map { dayOffset ->
            (0..48).map { step ->
                val hourFrac = step * 0.5
                val jdStep = baseJd + dayOffset.toDouble() + hourFrac / 24.0
                val jdeStep = jdStep + deltaT / 86400.0
                val moon = MoonEngine.compute(jdeStep, elpMoon, context)
                val sun = SunEngine.compute(jdeStep, earth, context)
                val nut = nutEngine.compute(jdeStep)
                val gst = SiderealTime.apparentGreenwich(
                    jdStep,
                    Math.toDegrees(nut.deltaPsi) * 3600.0,
                    sun.trueObliquity
                )
                AlmanacGenerator.MoonPoint(
                    hour = hourFrac,
                    dec = moon.declination,
                    gha = Angle.normalizeDegrees(gst - moon.rightAscension),
                    hp = Math.toDegrees(asin(6378.14 / moon.distanceKm))
                )
            }
        }
    }
}
