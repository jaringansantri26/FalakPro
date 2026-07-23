package com.falak.falakpro

import org.junit.Test
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.HilalEngine
import com.falak.falakpro.premium.DynamicalTimeEngine
import com.falak.falakpro.premium.Vsop87SolarEngine
import com.falak.falakpro.premium.ElpMpp02LunarEngine
import com.falak.falakpro.premium.AstroDataUtils
import com.falak.falakpro.premium.AstroMath
import kotlin.math.*

class HijriTest3 {
    @Test
    fun testHijri() {
        val lat = -6.2
        val lon = 106.8
        val ghurubJde = 2461177.944946659
        
        val sun = Vsop87SolarEngine.compute(ghurubJde)
        val moonGeo = ElpMpp02LunarEngine.computeGeometric(ghurubJde)
        
        val elongGeo = AstroMath.deg(acos(sin(AstroMath.rad(sun.dec))*sin(AstroMath.rad(moonGeo.dec)) + cos(AstroMath.rad(sun.dec))*cos(AstroMath.rad(moonGeo.dec))*cos(AstroMath.rad(sun.ra - moonGeo.ra))))
        println("Elongation: $elongGeo")
        
        val ghurubSunUt = ghurubJde - DynamicalTimeEngine.deltaT(ghurubJde) / 86400.0
        val gast = AstroDataUtils.calculateGAST(ghurubSunUt)
        val ha = AstroMath.mod(gast + lon - moonGeo.ra, 360.0)
        val haRad = AstroMath.rad(ha)
        val latRad = AstroMath.rad(lat)
        val decRad = AstroMath.rad(moonGeo.dec)
        val sinAlt = sin(latRad) * sin(decRad) + cos(latRad) * cos(decRad) * cos(haRad)
        val moonGeoAlt = AstroMath.deg(asin(sinAlt.coerceIn(-1.0, 1.0)))
        val moonTopoAlt = moonGeoAlt - moonGeo.horizontalParallax * cos(AstroMath.rad(moonGeoAlt))
        println("Moon Topo Alt: $moonTopoAlt")
    }
}
