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

class HijriTest4 {
    @Test
    fun testHijri() {
        val ghurubJde = 2461177.944946659
        val dt = DynamicalTimeEngine.deltaT(ghurubJde)
        val ghurubSunUt = ghurubJde - dt / 86400.0
        val gast = AstroDataUtils.calculateGAST(ghurubSunUt)
        
        println("ghurubJde: $ghurubJde, dt: $dt, ghurubSunUt: $ghurubSunUt, gast: $gast")
    }
}
