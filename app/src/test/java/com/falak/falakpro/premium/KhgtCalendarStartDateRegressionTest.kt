package com.falak.falakpro.premium

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class KhgtCalendarStartDateRegressionTest {

    @Test
    fun ramadan1447CalendarStartDatesFollowKhgtCriteria() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }
        CalendarFunctions.clearStartJdeCache()

        val lat = -6.175392
        val lon = 106.827153
        val elev = 8.0
        val tz = 7.0

        val muhammadiyahStart = CalendarFunctions.getStartJdeOfHijriMonth(
            1447, 9, lat, lon, elev, tz, "KGHT Muhammadiyah"
        )
        val turkiStart = CalendarFunctions.getStartJdeOfHijriMonth(
            1447, 9, lat, lon, elev, tz, "KGHT Turki"
        )

        assertEquals(
            Triple(2026, 2, 18),
            localGregorianDate(muhammadiyahStart, tz)
        )
        assertEquals(
            Triple(2026, 2, 19),
            localGregorianDate(turkiStart, tz)
        )
    }

    @Test
    fun ramadan1447CalendarCellsDoNotShowTurkeyOnFebruary18() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }
        CalendarFunctions.clearStartJdeCache()

        val lat = -6.175392
        val lon = 106.827153
        val elev = 8.0
        val tz = 7.0
        val turkiStart = CalendarFunctions.getStartJdeOfHijriMonth(
            1447, 9, lat, lon, elev, tz, "KGHT Turki"
        )
        val muhammadiyahStart = CalendarFunctions.getStartJdeOfHijriMonth(
            1447, 9, lat, lon, elev, tz, "KGHT Muhammadiyah"
        )
        val turkiPrevStart = CalendarFunctions.getStartJdeOfHijriMonth(
            1447, 8, lat, lon, elev, tz, "KGHT Turki"
        )
        val muhammadiyahPrevStart = CalendarFunctions.getStartJdeOfHijriMonth(
            1447, 8, lat, lon, elev, tz, "KGHT Muhammadiyah"
        )

        val turkiAnchors = listOf(
            Triple(1447, 8, 1) to turkiPrevStart,
            Triple(1447, 9, 1) to turkiStart
        )
        val muhammadiyahAnchors = listOf(
            Triple(1447, 8, 1) to muhammadiyahPrevStart,
            Triple(1447, 9, 1) to muhammadiyahStart
        )

        assertEquals(Triple(1447, 9, 1), calendarScreenHijriDate(2026, 2, 18, tz, muhammadiyahAnchors))
        assertEquals(Triple(1447, 8, 29), calendarScreenHijriDate(2026, 2, 18, tz, turkiAnchors))
        assertEquals(Triple(1447, 9, 1), calendarScreenHijriDate(2026, 2, 19, tz, turkiAnchors))
    }

    private fun localGregorianDate(jdeUt: Double, timezone: Double): Triple<Int, Int, Int> {
        val greg = CalendarFunctions.jdeToGregorian(jdeUt + timezone / 24.0)
        return Triple(greg.first, greg.second, greg.third.toInt())
    }

    private fun calendarScreenHijriDate(
        year: Int,
        month: Int,
        day: Int,
        timezone: Double,
        anchors: List<Pair<Triple<Int, Int, Int>, Double>>
    ): Triple<Int, Int, Int> {
        val cellJde = CalendarFunctions.gregorianToJde(year, month, day.toDouble())
        return CalendarFunctions.getHijriDateFromMonthAnchors(cellJde, timezone, anchors)
            ?: error("No anchor found")
    }
}
