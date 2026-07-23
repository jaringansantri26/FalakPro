package com.falak.falakpro.premium

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlin.math.floor

class EclipseNasaRegressionTest {

    @Test
    fun solarEclipse2027Aug02MatchesEspenakPrimeData() {
        val detail = solarDetail(year = 2027, month = 8, day = 2, deltaT = 72.8)
        val mx = detail.contacts.first { it.name == "Mx" }
        val mxUtJd = mx.jdeTD - detail.deltaT / 86400.0

        assertEquals(10.0, detail.t0, 0.0)
        assertEquals(72.8, detail.deltaT, 0.0)
        assertEquals(1.07903, detail.magnitude, 2.0e-5)
        assertEquals(0.14209, detail.gamma, 3.0e-5)

        // The reference table's Julian Date column is UT1 JD, not TD JD.
        assertEquals(2461619.921266, mxUtJd, 2.0e-6)
        assertEquals(25.503333, mx.latitude, 2.0e-3)
        assertEquals(33.183333, mx.longitude, 2.0e-3)

        assertEquals(132.362083, detail.sunRA, 2.0e-4)
        assertEquals(17.761472, detail.sunDec, 2.0e-4)
        assertEquals(132.416667, detail.moonRA, 2.0e-3)
        assertEquals(17.896611, detail.moonDec, 2.0e-4)
        assertEquals(0.262639, detail.sunSD, 2.0e-4)
        assertEquals(0.002417, detail.sunHP, 2.0e-5)
        assertEquals(0.278639, detail.moonSD, 2.0e-4)
        assertEquals(1.022611, detail.moonHP, 2.0e-4)

        assertBesselian0(
            detail,
            x = -0.01977,
            y = 0.16007,
            d = 17.7625,
            l1 = 0.53062,
            l2 = -0.01546,
            mu = 328.4225
        )

        assertSolarContact(detail, "P1", 7.52275, 25.261667, -29.661667)
        assertSolarContact(detail, "U1", 8.41050, 27.840000, -44.021667)
        assertSolarContact(detail, "U2", 8.464194, 28.081667, -44.928333)
        assertSolarContact(detail, "U3", 11.798083, -12.361667, 90.885000)
        assertSolarContact(detail, "U4", 11.851639, -12.606667, 90.000000)
        assertSolarContact(detail, "P4", 12.739250, -15.211667, 75.798333)
    }

    @Test
    fun solarEclipse2016Mar09MatchesEspenakPrimeData() {
        val detail = solarDetail(year = 2016, month = 3, day = 9, deltaT = 68.1)
        val mx = detail.contacts.first { it.name == "Mx" }
        val mxUtJd = mx.jdeTD - detail.deltaT / 86400.0

        assertEquals(2.0, detail.t0, 0.0)
        assertEquals(68.1, detail.deltaT, 0.0)
        assertEquals(1.04499, detail.magnitude, 2.0e-5)
        assertEquals(0.26092, detail.gamma, 3.0e-5)
        assertEquals(2457456.581382, mxUtJd, 3.0e-6)
        assertEquals(10.121667, mx.latitude, 2.0e-3)
        assertEquals(148.793333, mx.longitude, 2.0e-3)

        assertBesselian0(
            detail,
            x = -0.06253,
            y = 0.25383,
            d = -4.3797,
            l1 = 0.53889,
            l2 = -0.00723,
            mu = 207.3722
        )

        assertSolarContact(detail, "P1", 23.341194, -7.631667, 102.211667)
        assertSolarContact(detail, "U1", 0.284806, -2.360000, 88.465000)
        assertSolarContact(detail, "U2", 0.310500, -2.138333, 88.096667)
        assertSolarContact(detail, "U3", 3.631389, 32.695000, -144.366667)
        assertSolarContact(detail, "U4", 3.657944, 32.465000, -144.740000)
        assertSolarContact(detail, "P4", 4.600917, 27.211667, -158.348333)
    }

    private fun solarDetail(year: Int, month: Int, day: Int, deltaT: Double): EclipseDetail {
        loadEphemerides()

        val engine = EclipseParityEngine()
        val jde = engine.searchYearly(year, deltaT, isSolar = true).first {
            val cal = engine.jdeToCalendar(it)
            cal[1] == month && cal[2] == day
        }
        return engine.calculateFullDetail(jde, deltaT, timezone = 0.0)
    }

    private fun loadEphemerides() {
        ElpDataProvider.initialize(File("src/main/assets/mpp02_core.bin").inputStream())
        Vsop87SolarEngine.initialize(File("src/main/assets/earth_vsop87d.bin").inputStream())
    }

    private fun assertBesselian0(
        detail: EclipseDetail,
        x: Double,
        y: Double,
        d: Double,
        l1: Double,
        l2: Double,
        mu: Double
    ) {
        val b0 = detail.besselianTable[0]
        assertEquals(x, b0.x, 5.0e-5)
        assertEquals(y, b0.y, 5.0e-5)
        assertEquals(d, b0.d, 5.0e-5)
        assertEquals(l1, b0.L1, 5.0e-5)
        assertEquals(l2, b0.L2, 5.0e-5)
        assertEquals(mu, b0.mu, 5.0e-5)
    }

    private fun assertSolarContact(
        detail: EclipseDetail,
        name: String,
        expectedTdHour: Double,
        expectedLat: Double,
        expectedLon: Double
    ) {
        val contact = detail.contacts.first { it.name == name }
        assertEquals(expectedTdHour, tdHour(contact.jdeTD), 0.5 / 3600.0)
        assertEquals(expectedLat, contact.latitude, 0.003)
        assertEquals(expectedLon, contact.longitude, 0.003)
    }

    private fun tdHour(jde: Double): Double {
        return (jde + 0.5 - floor(jde + 0.5)) * 24.0
    }
}
