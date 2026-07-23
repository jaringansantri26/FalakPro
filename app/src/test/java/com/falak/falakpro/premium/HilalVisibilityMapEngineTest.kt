package com.falak.falakpro.premium

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class HilalVisibilityMapEngineTest {

    @Test
    fun yallopMapProducesGridPoints() {
        File("src/main/assets/mpp02_core.bin").inputStream().use {
            ElpDataProvider.initialize(it)
        }
        File("src/main/assets/earth_vsop87d.bin").inputStream().use {
            Vsop87SolarEngine.initialize(it)
        }

        val ijtimaGeoJde = HilalEngine.calculateMeeusIjtima(1447, 9)
        val yallop = HilalVisibilityMapEngine.buildMap(
            ijtimaGeoJde = ijtimaGeoJde,
            mode = HilalVisibilityMapMode.YALLOP,
            latStep = 20.0,
            lonStep = 20.0
        )

        assertFalse(yallop.points.isEmpty())
        assertNotNull(yallop.bestPoint)
    }
}
