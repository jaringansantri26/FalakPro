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

class HijriTest2 {
    @Test
    fun testHijri() {
        val lat = -6.2
        val lon = 106.8
        val elev = 0.0
        val tz = 7.0
        val criteria = "Mabims Baru"
        
        val targetArith = CalendarFunctions.hijriToJde(1447, 12, 1)
        val k = kotlin.math.round((targetArith - 2451550.09766) / 29.530588861).toInt()

        val T = k / 1236.85
        val T2 = T * T; val T3 = T * T2; val T4 = T * T3
        val eCorr = 1.0 - 0.002516 * T - 0.0000074 * T2

        val mDeg = 2.5534 + 29.10535669 * k - 0.0000218 * T2 - 0.00000011 * T3
        val mpDeg = 201.5643 + 385.81693528 * k + 0.0107438 * T2 + 0.00001239 * T3 - 0.000000058 * T4
        val fDeg = 160.7108 + 390.67050274 * k - 0.0016341 * T2 - 0.00000227 * T3 + 0.000000011 * T4
        val omDeg = 124.7746 - 1.5637558 * k + 0.0020691 * T2 + 0.00000215 * T3

        val rad = kotlin.math.PI / 180.0
        val mR = mDeg * rad; val mpR = mpDeg * rad; val fR = fDeg * rad; val omR = omDeg * rad

        val a1 = (299.77 + 0.107408 * k - 0.009173 * T2) * rad
        val a2 = (251.88 + 0.016321 * k) * rad
        val a3 = (251.83 + 26.651886 * k) * rad
        val a4 = (349.42 + 36.412478 * k) * rad
        val a5 = (84.66 + 18.206239 * k) * rad
        val a6 = (141.74 + 53.303771 * k) * rad
        val a7 = (207.14 + 2.453732 * k) * rad
        val a8 = (154.84 + 7.30686 * k) * rad
        val a9 = (34.52 + 27.261239 * k) * rad
        val a10 = (207.19 + 0.121824 * k) * rad
        val a11 = (291.34 + 1.844379 * k) * rad
        val a12 = (161.72 + 24.198154 * k) * rad
        val a13 = (239.56 + 25.513099 * k) * rad
        val a14 = (331.55 + 3.592518 * k) * rad

        val planetaryCorr = 325 * sin(a1) + 165 * sin(a2) + 164 * sin(a3) + 126 * sin(a4) + 110 * sin(a5) + 62 * sin(a6) + 60 * sin(a7) + 56 * sin(a8) + 47 * sin(a9) + 42 * sin(a10) + 40 * sin(a11) + 37 * sin(a12) + 35 * sin(a13) + 23 * sin(a14)

        val phaseSum = -40720 * sin(mpR) + 17241 * eCorr * sin(mR) + 1608 * sin(2*mpR) + 1039 * sin(2*fR) + 739 * eCorr * sin(mpR-mR) + -514 * eCorr * sin(mpR+mR) + 208 * eCorr*eCorr * sin(2*mR) + -111 * sin(mpR - 2*fR) + -57 * sin(mpR+2*fR) + 56 * eCorr * sin(2*mpR+mR) + -42 * sin(3*mpR) + 42 * eCorr * sin(mR+2*fR) + 38 * eCorr * sin(mR-2*fR) + -24 * eCorr * sin(2*mpR-mR) + -17 * sin(omR) + -7 * sin(mpR+2*mR) + 4 * sin(2*(mpR-fR)) + 4 * sin(3*mR) + 3 * sin(mpR+mR-2*fR) + 3 * sin(2*(mpR+fR)) + -3 * sin(mpR+mR+2*fR) + 3 * sin(mpR-mR+2*fR) + -2 * sin(mpR-mR-2*fR) + -2 * sin(3*mpR+mR) + 2 * sin(4*mpR)

        var ijtimaGeoJde = 2451550.09766 + 29.530588861 * k + 0.00015437 * T2 - 0.00000015 * T3 + 0.00000000073 * T4
        ijtimaGeoJde += planetaryCorr * 0.0000001 + phaseSum * 0.00001
        
        val dt = DynamicalTimeEngine.deltaT(ijtimaGeoJde)
        val ijtimaLocal = ijtimaGeoJde - dt / 86400.0 + tz / 24.0
        val startOfDayLocal = kotlin.math.floor(ijtimaLocal - 0.5) + 0.5
        val approxSunsetUt = startOfDayLocal + 18.0 / 24.0 - tz / 24.0
        val ghurubSunUt = HilalEngine.findSunsetNear(approxSunsetUt, lat, lon) ?: approxSunsetUt
        val ghurubDt = DynamicalTimeEngine.deltaT(ghurubSunUt)
        val ghurubJde = ghurubSunUt + ghurubDt / 86400.0

        println("Ijtima JDE: $ijtimaGeoJde, Local Time: $ijtimaLocal")
        val gc = CalendarFunctions.jdeToGregorian(ijtimaLocal)
        println("Ijtima Local Gregorian: ${gc.first}-${gc.second}-${gc.third}")
        println("Sunset JDE: $ghurubJde")

        val isVis = HilalEngine.computeHilalVisibility(ijtimaGeoJde, lat, lon, elev, criteria, ghurubSunUt)
        println("Is Visible at Local Sunset: $isVis")
    }
}
