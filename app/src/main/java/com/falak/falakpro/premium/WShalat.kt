package com.falak.falakpro.premium

import kotlin.math.*

/**
 * AstroSolarEngine — Adapts the high-precision VSOP87 binary engine 
 * to the official PERSIS interface and algorithm flow.
 */
object AstroSolarEngine {
    
    fun getDeclination(jde: Double): Double {
        return Vsop87SolarEngine.compute(jde).dec
    }

    fun getEquationOfTime(jde: Double): Double {
        val s = Vsop87SolarEngine.compute(jde)
        val deltaT = DynamicalTimeEngine.deltaT(jde)
        val jdeTD = jde + deltaT / 86400.0
        val obliquity = AstroDataUtils.calculateTrueObliquity(jdeTD)
        val nutation = AstroDataUtils.calculateNutation(jdeTD)
        val t = (jde - 2451545.0) / 36525.0
        
        // Adopted PERSIS EoT formula
        var l0 = 280.4664567 + 36000.7698277 * t + 0.00030322 * t * t
        l0 = AstroMath.mod(l0, 360.0)
        return (l0 - s.ra + nutation.first * cos(AstroMath.rad(obliquity))) / 15.0
    }

    fun getSemidiameter(jde: Double): Double {
        val s = Vsop87SolarEngine.compute(jde)
        return 0.2666 / s.distanceAU
    }
}

/**
 * PersisWaktuShalat — Adopted 100% from PERSIS WShalat.kt.
 */
object AstroPrayerEngine {
    
    fun calculateZuhur(d: Int, m: Int, y: Int, lon: Double, tz: Double): Double {
        var zhr = 12.0
        repeat(3) {
            val jd = AstroTime.kmjd(d, m, y, zhr, tz)
            val jde = jd + DynamicalTimeEngine.deltaT(jd) / 86400.0
            val eot = AstroSolarEngine.getEquationOfTime(jde)
            val kwd = (lon - (tz * 15.0)) / 15.0
            zhr = 12.0 - eot - kwd
        }
        return zhr
    }

    fun calculateAshar(d: Int, m: Int, y: Int, lon: Double, lat: Double, tz: Double): Double {
        var asr = 15.0
        repeat(3) {
            val jd = AstroTime.kmjd(d, m, y, asr, tz)
            val jde = jd + DynamicalTimeEngine.deltaT(jd) / 86400.0
            val eot = AstroSolarEngine.getEquationOfTime(jde)
            val dec = AstroSolarEngine.getDeclination(jde)
            val kwd = (lon - (tz * 15.0)) / 15.0
            val zm = abs(lat - dec)
            val h = AstroMath.deg(atan(1.0 / (tan(AstroMath.rad(zm)) + 1.0)))
            val t = AstroMath.deg(acos(-tan(AstroMath.rad(lat)) * tan(AstroMath.rad(dec)) + sin(AstroMath.rad(h)) / (cos(AstroMath.rad(lat)) * cos(AstroMath.rad(dec)))))
            asr = 12.0 - eot + t / 15.0 - kwd
        }
        return asr
    }

    fun calculateMaghrib(d: Int, m: Int, y: Int, lon: Double, lat: Double, elev: Double, tz: Double): Double {
        var mgr = 18.0
        repeat(3) {
            val jd = AstroTime.kmjd(d, m, y, mgr, tz)
            val jde = jd + DynamicalTimeEngine.deltaT(jd) / 86400.0
            val eot = AstroSolarEngine.getEquationOfTime(jde)
            val dec = AstroSolarEngine.getDeclination(jde)
            val sd = AstroSolarEngine.getSemidiameter(jde)
            val dip = 1.76 / 60.0 * sqrt(elev)
            val h = -(sd + 34.5 / 60.0 + dip)
            val t = AstroMath.deg(acos(-tan(AstroMath.rad(lat)) * tan(AstroMath.rad(dec)) + sin(AstroMath.rad(h)) / (cos(AstroMath.rad(lat)) * cos(AstroMath.rad(dec)))))
            mgr = 12.0 - eot + t / 15.0 - kwd(lon, tz)
        }
        return mgr
    }

    fun calculateSubuh(d: Int, m: Int, y: Int, lon: Double, lat: Double, tz: Double): Double {
        var sbh = 4.0
        repeat(3) {
            val jd = AstroTime.kmjd(d, m, y, sbh, tz)
            val jde = jd + DynamicalTimeEngine.deltaT(jd) / 86400.0
            val eot = AstroSolarEngine.getEquationOfTime(jde)
            val dec = AstroSolarEngine.getDeclination(jde)
            val h = -20.0
            val t = AstroMath.deg(acos(-tan(AstroMath.rad(lat)) * tan(AstroMath.rad(dec)) + sin(AstroMath.rad(h)) / (cos(AstroMath.rad(lat)) * cos(AstroMath.rad(dec)))))
            sbh = 12.0 - eot - t / 15.0 - kwd(lon, tz)
        }
        return sbh
    }

    private fun kwd(lon: Double, tz: Double) = (lon - (tz * 15.0)) / 15.0
}
