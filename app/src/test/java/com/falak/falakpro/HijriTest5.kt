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

class HijriTest5 {
    @Test
    fun testHijri() {
        val lat = -6.2
        val lon = 106.8
        val ghurubJde = 2461177.944946659
        
        val sun = Vsop87SolarEngine.compute(ghurubJde)
        val moonGeo = ElpMpp02LunarEngine.computeGeometric(ghurubJde)
        
        val dt = DynamicalTimeEngine.deltaT(ghurubJde)
        val ghurubSunUt = ghurubJde - dt / 86400.0
        val gast = AstroDataUtils.calculateGAST(ghurubSunUt)
        
        val ha = AstroMath.mod(gast + lon - moonGeo.ra, 360.0)
        val haRad = AstroMath.rad(ha)
        val latRad = AstroMath.rad(lat)
        val decRad = AstroMath.rad(moonGeo.dec)
        
        val sinLat = sin(latRad)
        val sinDec = sin(decRad)
        val cosLat = cos(latRad)
        val cosDec = cos(decRad)
        val cosHa = cos(haRad)
        
        val sinAlt = sinLat * sinDec + cosLat * cosDec * cosHa
        val coerced = sinAlt.coerceIn(-1.0, 1.0)
        val asinVal = asin(coerced)
        val moonGeoAlt = AstroMath.deg(asinVal)
        val hp = moonGeo.horizontalParallax
        val moonTopoAlt = moonGeoAlt - hp * cos(AstroMath.rad(moonGeoAlt))
        
        println("gast: $gast")
        println("moonGeo.ra: ${moonGeo.ra}")
        println("ha: $ha")
        println("sinAlt: $sinAlt")
        println("moonGeoAlt: $moonGeoAlt")
        println("hp: $hp")
        println("moonTopoAlt: $moonTopoAlt")
    }
}
