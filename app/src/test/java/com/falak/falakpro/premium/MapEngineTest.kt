package com.falak.falakpro.premium

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MapEngineTest {
    @Test
    fun australiaHPlusOneHasEvaluatedVisibilityPoints() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }

        val hYear = 1447
        val hMonth = 12
        val ijtimaGeoJde = HilalEngine.calculateMeeusIjtima(hYear, hMonth)

        val result = HilalVisibilityMapEngine.buildMap(
            ijtimaGeoJde = ijtimaGeoJde,
            dayOffset = 1,
            mode = HilalVisibilityMapMode.YALLOP,
            latStep = 10.0,
            lonStep = 20.0
        )

        val lon = 140.0
        for (lat in listOf(-10.0, -20.0, -30.0, -40.0)) {
            val point = result.points.firstOrNull {
                kotlin.math.abs(it.latitude - lat) < 0.1 && kotlin.math.abs(it.longitude - lon) < 0.1
            }
            assertNotNull("Point $lat, $lon should be present in the generated map", point)
        }
    }

    @Test
    fun australiaSouthIsMoonsetZoneForYallopReferenceDate() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }

        val ijtimaGeoJde = HilalEngine.calculateMeeusIjtima(1443, 12)
        val result = HilalVisibilityMapEngine.buildMap(
            ijtimaGeoJde = ijtimaGeoJde,
            dayOffset = 0,
            mode = HilalVisibilityMapMode.YALLOP,
            latStep = 5.0,
            lonStep = 10.0
        )

        val southAustralia = result.points.first {
            kotlin.math.abs(it.latitude - -35.0) < 0.1 && kotlin.math.abs(it.longitude - 140.0) < 0.1
        }
        val northAustralia = result.points.first {
            kotlin.math.abs(it.latitude - -15.0) < 0.1 && kotlin.math.abs(it.longitude - 140.0) < 0.1
        }

        assertEquals(HilalVisibilityZone.MOON_SET_BEFORE_SUN, southAustralia.zone)
        assertNotEquals(HilalVisibilityZone.MOON_SET_BEFORE_SUN, northAustralia.zone)
    }

    @Test
    fun odehMuharram1448MatchesReferenceRegions() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }

        val ijtimaGeoJde = HilalEngine.calculateMeeusIjtima(1448, 1)
        val result = HilalVisibilityMapEngine.buildFastMap(
            ijtimaGeoJde = ijtimaGeoJde,
            mode = HilalVisibilityMapMode.ODEH,
            latStep = 5.0,
            lonStep = 10.0
        )

        val centralUs = result.pointAt(40.0, -100.0)
        val centralEurope = result.pointAt(50.0, 10.0)
        val australia = result.pointAt(-25.0, 130.0)

        assertEquals(HilalVisibilityZone.EASY_NAKED_EYE, centralUs.zone)
        assertNotEquals(HilalVisibilityZone.EASY_NAKED_EYE, centralEurope.zone)
        assertTrue(
            australia.zone == HilalVisibilityZone.MOON_SET_BEFORE_SUN ||
                australia.zone == HilalVisibilityZone.BEFORE_CONJUNCTION
        )
    }

    @Test
    fun allVisibilityCriteriaProduceEvaluatedMapPoints() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }

        val ijtimaGeoJde = HilalEngine.calculateMeeusIjtima(1448, 1)
        HilalVisibilityMapMode.entries.forEach { mode ->
            val result = HilalVisibilityMapEngine.buildFastMap(
                ijtimaGeoJde = ijtimaGeoJde,
                mode = mode,
                latStep = 15.0,
                lonStep = 30.0
            )

            assertTrue("$mode harus menghasilkan titik peta", result.points.isNotEmpty())
            assertNotNull("$mode harus punya titik terbaik atau titik evaluasi", result.bestPoint ?: result.points.firstOrNull())
        }
    }

    private fun HilalVisibilityMapResult.pointAt(lat: Double, lon: Double): HilalVisibilityPoint {
        return points.first {
            kotlin.math.abs(it.latitude - lat) < 0.1 && kotlin.math.abs(it.longitude - lon) < 0.1
        }
    }
}
