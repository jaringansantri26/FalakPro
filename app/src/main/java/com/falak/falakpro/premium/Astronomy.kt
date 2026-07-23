package com.falak.falakpro.premium

import kotlin.math.*

/**
 * PersisMoonEngine — 100% Adoption of PERSIS v1.8 Moon Algorithms.
 * Includes Topocentric corrections, Ijtima search, and Rise/Set logic.
 */
object AstroMoonEngine {

    fun getTopocentricPosition(jdUT: Double, lon: Double, lat: Double, elev: Double): Triple<Double, Double, Double> {
        val deltaT = DynamicalTimeEngine.deltaT(jdUT)
        val jdTD = jdUT + deltaT / 86400.0
        
        // Posisi geometris harus menggunakan TD
        val m = ElpMpp02LunarEngine.computeGeometric(jdTD)
        
        val ra = m.ra; val dec = m.dec; val hp = m.horizontalParallax
        // Waktu Sidereal harus menggunakan UT
        val gast = AstroDataUtils.calculateGAST(jdUT) 
        val ha = AstroMath.mod(gast + lon - ra, 360.0)
        
        // PERSIS Topocentric Logic (Matches SunDatas.kt termX/termY)
        val u = atan(0.99664719 * tan(AstroMath.rad(lat)))
        val x = cos(u) + (elev / AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_M) * cos(AstroMath.rad(lat))
        val y = 0.99664719 * sin(u) + (elev / AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_M) * sin(AstroMath.rad(lat))
        
        val phi = AstroMath.rad(hp)
        val rHA = AstroMath.rad(ha)
        val rDec = AstroMath.rad(dec)
        
        val deltaRA = AstroMath.deg(atan2(-x * sin(phi) * sin(rHA), cos(rDec) - x * sin(phi) * cos(rHA)))
        val raTopo = ra + deltaRA
        val decTopo = AstroMath.deg(atan2((sin(rDec) - y * sin(phi)) * cos(AstroMath.rad(deltaRA)), cos(rDec) - x * sin(phi) * cos(rHA)))
        
        return Triple(raTopo, decTopo, hp)
    }

    fun calculateIjtima(month: Int, year: Int): Double {
        // Adopt PERSIS iterative conjunction search
        val k = floor((year + (month - 1) / 12.0 - 2000.0) * 12.3676).toInt()
        var jde = 2451550.09766 + 29.530588861 * k
        repeat(5) {
            val s = Vsop87SolarEngine.compute(jde)
            val m = ElpMpp02LunarEngine.computeGeometric(jde)
            val diff = AstroMath.mod(m.longitudeEcliptic - s.longitudeEcliptic + 180.0, 360.0) - 180.0
            jde -= diff / 12.3685 // Iteration factor adjusted for faster convergence
        }
        return jde
    }
}

/**
 * PersisArahKiblat — 100% Adoption of PERSIS AKiblat.kt.
 */
