package com.falak.falakpro.premium

import kotlin.math.*

/**
 * Eclipse engine for global and local eclipse circumstances.
 */
class EclipseParityEngine {

    enum class EclipseType { TOTAL, ANNULAR, HYBRID, PARTIAL, NONE }
    private data class SolarContactSolution(
        val td: Double,
        val t: Double,
        val contactX: Double,
        val contactY: Double
    )

    private data class EllipseContact(
        val distance: Double,
        val x: Double,
        val y: Double
    )

    fun searchYearly(year: Int, deltaT: Double, isSolar: Boolean): List<Double> {
        val results = mutableListOf<Double>()
        for (i in -1..13) {
            val k = floor((year - 2000.0) * 12.3685).toInt() + i
            val phase = if (isSolar) k.toDouble() else k + 0.5
            val jde = LunarFunctions.lunarPhaseJde(phase)
            val t = phase / 1236.85
            val f = Math.toRadians((160.7108 + 390.67050284 * phase - (t * t) * 0.0016118).mod(360.0))
            val absF = AstroMath.deg(f).mod(360.0).let { minOf(it, 360 - it, abs(180 - it)) }
            if (isSolar && absF < 21.0) results.add(jde) else if (!isSolar && absF < 13.9) results.add(jde)
        }
        return results.filter { jdeToCalendar(it)[0] == year }
    }

    fun calculateFullDetail(jdeApprox: Double, deltaT: Double, timezone: Double): EclipseDetail {
        val roundedDeltaT = AstroMath.roundTo(deltaT, 2)
        val engine = BesselianEngine()
        val t0Hour = round((jdeApprox + 0.5 - floor(jdeApprox + 0.5)) * 24.0)
        val jdeT0 = floor(jdeApprox + 0.5) - 0.5 + t0Hour / 24.0
        
        val polyRes = engine.calculatePolynomials(jdeT0)
        val poly = polyRes.polynomials
        
        var tMx = 0.0
        var xMx = 0.0; var yMx = 0.0; var dMx = 0.0; var muMx = 0.0
        var xpMx = 0.0; var ypMx = 0.0
        var l1Mx = 0.0; var l2Mx = 0.0; var tanf1 = 0.0; var tanf2 = 0.0
        
        for (i in 1..7) {
            xMx = eval(poly, tMx, 0); yMx = eval(poly, tMx, 1); dMx = eval(poly, tMx, 2); muMx = eval(poly, tMx, 5)
            xpMx = deriv(poly, tMx, 0); ypMx = deriv(poly, tMx, 1)
            l1Mx = eval(poly, tMx, 3); l2Mx = eval(poly, tMx, 4)
            tanf1 = eval(poly, tMx, 6); tanf2 = eval(poly, tMx, 7)
            
            val mbMx = AstroMath.mod(AstroMath.deg(atan2(yMx, xMx)), 360.0)
            val msMx = sqrt(xMx * xMx + yMx * yMx)
            val nbMx = AstroMath.mod(AstroMath.deg(atan2(ypMx, xpMx)), 360.0)
            val nsMx = sqrt(xpMx * xpMx + ypMx * ypMx)
            val tuMx = -(msMx * cos(AstroMath.rad(mbMx - nbMx)) / nsMx)
            tMx += tuMx
        }
        
        val peakTD = jdeT0 + tMx / 24.0
        
        val e2 = AstroMath.EARTH_ECCENTRICITY_SQUARED
        val rho0 = sqrt(1.0 - e2 * cos(AstroMath.rad(dMx)).pow(2.0))
        val y1Mx = yMx / rho0; val m1Mx = sqrt(xMx * xMx + y1Mx * y1Mx)
        val msMx = sqrt(xMx * xMx + yMx * yMx); val bBig = sqrt(max(0.0, 1.0 - xMx * xMx - y1Mx * y1Mx))
        val L1pMx = l1Mx - bBig * tanf1; val L2pMx = l2Mx - bBig * tanf2
        val rho = msMx / m1Mx; val DDD = msMx - rho
        val magnitude = if (msMx < 0.9972) (L1pMx - L2pMx) / (L1pMx + L2pMx) else (l1Mx - DDD) / (l1Mx + l2Mx)

        val contacts = mutableListOf<ContactPoint>()
        contacts.add(formatContact("Mx", peakTD, poly, tMx, roundedDeltaT))
        solveSolarContact(poly, jdeT0, tMx - 2.6, isUmbra = false)?.let { contacts.add(formatContact("P1", it.td, poly, it.t, roundedDeltaT)) }
        solveSolarContact(poly, jdeT0, tMx - 1.7, isUmbra = true)?.let { contacts.add(formatContact("U1", it.td, poly, it.t, roundedDeltaT, it.contactX, it.contactY)) }
        solveSolarContact(poly, jdeT0, tMx - 1.6, isUmbra = true)?.let { contacts.add(formatContact("U2", it.td, poly, it.t, roundedDeltaT, it.contactX, it.contactY)) }
        solveSolarContact(poly, jdeT0, tMx + 1.6, isUmbra = true)?.let { contacts.add(formatContact("U3", it.td, poly, it.t, roundedDeltaT, it.contactX, it.contactY)) }
        solveSolarContact(poly, jdeT0, tMx + 1.7, isUmbra = true)?.let { contacts.add(formatContact("U4", it.td, poly, it.t, roundedDeltaT, it.contactX, it.contactY)) }
        solveSolarContact(poly, jdeT0, tMx + 2.6, isUmbra = false)?.let { contacts.add(formatContact("P4", it.td, poly, it.t, roundedDeltaT)) }
        contacts.sortBy { it.jdeTD }

        val sun = Vsop87SolarEngine.compute(peakTD, 0.0)
        val moon = ElpMpp02LunarEngine.computeGeometric(peakTD)

        val gamma = msMx * if (yMx < 0) -1.0 else 1.0

        return EclipseDetail(
            t0 = t0Hour, deltaT = roundedDeltaT,
            besselianTable = (0..4).map { i -> BesselianRow(i, poly[i][0], poly[i][1], poly[i][2], poly[i][3], poly[i][4], poly[i][5]) },
            contacts = contacts, magnitude = magnitude, gamma = gamma,
            sunRA = sun.ra, sunDec = sun.dec,
            sunSD = SolarFunctions.solarSemidiameter(sun.distanceAU),
            sunHP = SolarFunctions.solarEquatorialHorizontalParallax(sun.distanceAU),
            moonRA = moon.ra, moonDec = moon.dec,
            moonSD = moon.semidiameter,
            moonHP = moon.horizontalParallax,
            type = when {
                l2Mx < 0 -> EclipseType.TOTAL
                l2Mx < 0.0047 -> EclipseType.HYBRID
                l2Mx < 0.54 -> EclipseType.ANNULAR
                else -> EclipseType.PARTIAL
            }
        )
    }

    fun calculateLocalDetail(jdeApprox: Double, deltaT: Double, lat: Double, lon: Double, elev: Double, timezone: Double, obsName: String = ""): LocalEclipseDetail {
        val roundedDeltaT = AstroMath.roundTo(deltaT, 2)
        val engine = BesselianEngine()
        val t0Hour = round((jdeApprox + 0.5 - floor(jdeApprox + 0.5)) * 24.0)
        val jdeT0 = floor(jdeApprox + 0.5) - 0.5 + t0Hour / 24.0
        val poly = engine.calculatePolynomials(jdeT0).polynomials

        val earthBa = AstroMath.EARTH_POLAR_RADIUS_M / AstroMath.EARTH_EQUATORIAL_RADIUS_M
        val phi = atan(earthBa * tan(AstroMath.rad(lat)))
        val S = earthBa * sin(phi) + (elev / AstroMath.EARTH_EQUATORIAL_RADIUS_M) * sin(AstroMath.rad(lat))
        val C = cos(phi) + (elev / AstroMath.EARTH_EQUATORIAL_RADIUS_M) * cos(AstroMath.rad(lat))

        var tMx = 0.0
        var xMx = 0.0; var yMx = 0.0; var dMx = 0.0; var muMx = 0.0
        var xpMx = 0.0; var ypMx = 0.0; var PPMx = 0.0
        
        for (i in 1..5) {
            xMx = eval(poly, tMx, 0); yMx = eval(poly, tMx, 1); dMx = eval(poly, tMx, 2); muMx = eval(poly, tMx, 5)
            xpMx = deriv(poly, tMx, 0); ypMx = deriv(poly, tMx, 1)
            val d1 = deriv(poly, tMx, 2)
            
            val h = AstroMath.rad(muMx + lon - AstroMath.SIDEREAL_DEGREES_PER_UT_SECOND * deltaT)
            val xi = C * sin(h); val eta = S * cos(AstroMath.rad(dMx)) - C * cos(h) * sin(AstroMath.rad(dMx))
            val zeta = S * sin(AstroMath.rad(dMx)) + C * cos(h) * cos(AstroMath.rad(dMx))
            
            val prMx = 0.01745329 * deriv(poly, tMx, 5) * C * cos(h)
            val qpMx = 0.01745329 * (deriv(poly, tMx, 5) * xi * sin(AstroMath.rad(dMx)) - zeta * d1)
            
            val u = xMx - xi; val v = yMx - eta; val a = xpMx - prMx; val b = ypMx - qpMx
            PPMx = -(u * a + v * b) / (a * a + b * b)
            tMx += PPMx
        }

        val jdMx = floor(jdeT0 + 0.5) - 0.5 + (t0Hour + tMx - deltaT / 3600.0) / 24.0
        val mxContact = formatLocalContact("Mx", jdMx, poly, tMx, roundedDeltaT, lat, lon, elev, C, S)

        // Initial Tau (5GerhanaMatahariLokal.kt:477)
        val l1Mx = eval(poly, tMx, 3); val tf1Mx = eval(poly, tMx, 6)
        val hMx = AstroMath.rad(muMx + lon - AstroMath.SIDEREAL_DEGREES_PER_UT_SECOND * deltaT)
        val rMx = S * sin(AstroMath.rad(dMx)) + C * cos(hMx) * cos(AstroMath.rad(dMx))
        val l1pMx = l1Mx - rMx * tf1Mx
        val aMx = deriv(poly, tMx, 0) - (0.01745329 * deriv(poly, tMx, 5) * C * cos(hMx))
        val bMx = deriv(poly, tMx, 1) - (0.01745329 * (deriv(poly, tMx, 5) * C * sin(hMx) * sin(AstroMath.rad(dMx)) - rMx * deriv(poly, tMx, 2)))
        val nMx = sqrt(aMx * aMx + bMx * bMx)
        val tau = l1pMx / nMx
        
        val l2Mx = eval(poly, tMx, 4); val tf2Mx = eval(poly, tMx, 7)
        val l2pMx = l2Mx - rMx * tf2Mx
        val tau2 = abs(l2pMx) / nMx

        val u1 = solveLocalPersis(poly, jdeT0, deltaT, t0Hour, tMx, -tau, 1, lat, lon, elev, C, S, isUmbra = false)
        val u2 = solveLocalPersis(poly, jdeT0, deltaT, t0Hour, tMx, -tau2, 2, lat, lon, elev, C, S, isUmbra = true)
        val u3 = solveLocalPersis(poly, jdeT0, deltaT, t0Hour, tMx, tau2, 3, lat, lon, elev, C, S, isUmbra = true)
        val u4 = solveLocalPersis(poly, jdeT0, deltaT, t0Hour, tMx, tau, 4, lat, lon, elev, C, S, isUmbra = false)
        
        // Literal Magnitude & Obscuration (5GerhanaMatahariLokal.kt:472-496)
        val hFinal = AstroMath.rad(muMx + lon - AstroMath.SIDEREAL_DEGREES_PER_UT_SECOND * deltaT)
        val xiMx = C * sin(hFinal); val etaMx = S * cos(AstroMath.rad(dMx)) - C * cos(hFinal) * sin(AstroMath.rad(dMx))
        val zetaMx = S * sin(AstroMath.rad(dMx)) + C * cos(hFinal) * cos(AstroMath.rad(dMx))
        val uFinal = xMx - xiMx; val vFinal = yMx - etaMx; val mm = sqrt(uFinal * uFinal + vFinal * vFinal)
        val l1p = l1Mx - zetaMx * tf1Mx; val l2p = l2Mx - zetaMx * tf2Mx
        val magnitude = (l1p - mm) / (l1p + l2p)

        val rpMx = 2 * mm / (l1p + l2p); val spMx = (l1p - l2p) / (l1p + l2p)
        val yy = (spMx * spMx + rpMx * rpMx - 1) / (2 * rpMx * spMx)
        val zp = (rpMx * rpMx - spMx * spMx + 1) / (2 * rpMx)
        
        val BB = if (yy < -1) PI else if (yy > 1) 0.0 else acos(yy)
        val CC = if (zp < -1) PI else if (zp > 1) 0.0 else acos(zp)
        val obscuration = ((spMx * spMx * (BB - sin(2 * BB) / 2) + (CC - sin(2 * CC) / 2)) / PI) * 100

        val sun = Vsop87SolarEngine.compute(jdMx, 0.0)
        val moon = ElpMpp02LunarEngine.computeGeometric(jdMx)

        // Sunrise, Transit, Sunset using Meeus Most Accurate Interpolation
        val jde0 = floor(jdeT0 + 0.5) - 0.5
        val sun1 = Vsop87SolarEngine.compute(jde0 - 1.0 + deltaT / 86400.0, 0.0)
        val sun2 = Vsop87SolarEngine.compute(jde0 + deltaT / 86400.0, 0.0)
        val sun3 = Vsop87SolarEngine.compute(jde0 + 1.0 + deltaT / 86400.0, 0.0)
        
        val rts = SolarFunctions.riseTransitSet(
            jde0 = jde0, lat = lat, lon = lon, elev = elev,
            ra1 = sun1.ra, dec1 = sun1.dec,
            ra2 = sun2.ra, dec2 = sun2.dec,
            ra3 = sun3.ra, dec3 = sun3.dec
        )
        
        fun formatRts(hourUT: Double?): String {
            if (hourUT == null) return "--:--"
            val lt = (hourUT + timezone).let { if (it < 0) it + 24 else if (it >= 24) it - 24 else it }
            val h = floor(lt).toInt()
            val m = floor((lt - h) * 60.0).toInt()
            val s = round(((lt - h) * 60.0 - m) * 60.0).toInt()
            return "%02d:%02d:%02d".format(h, m, s)
        }

        return LocalEclipseDetail(
            type = if (magnitude <= 0) "TIDAK TERJADI GERHANA" else if (l2p < 0 && mm < abs(l2p)) "GERHANA MATAHARI TOTAL" else if (l2p > 0 && mm < abs(l2p)) "GERHANA MATAHARI CINCIN" else "GERHANA MATAHARI SEBAGIAN",
            magnitude = magnitude, obscuration = obscuration, tzLabel = "LT", timezone = timezone,
            t0 = t0Hour, deltaT = roundedDeltaT, tanf1 = eval(poly, 0.0, 6), tanf2 = eval(poly, 0.0, 7),
            besselianTable = (0..4).map { i -> BesselianRow(i, poly[i][0], poly[i][1], poly[i][2], poly[i][3], poly[i][4], poly[i][5]) },
            mx = mxContact, p1 = null, u1 = u1, u2 = u2, u3 = u3, u4 = u4, p4 = null,
            sunRA = sun.ra, sunDec = sun.dec, sunSD = SolarFunctions.solarSemidiameter(sun.distanceAU), sunHP = SolarFunctions.solarEquatorialHorizontalParallax(sun.distanceAU),
            moonRA = moon.ra, moonDec = moon.dec, moonSD = moon.semidiameter, moonHP = moon.horizontalParallax,
            sunrise = formatRts(rts.rise),
            transit = formatRts(rts.transit),
            sunset = formatRts(rts.set),
            obsLat = lat, obsLon = lon, obsElev = elev, obsName = obsName
        )
    }

    private fun solveLocalPersis(
        poly: Array<DoubleArray>,
        jdeT0: Double,
        deltaT: Double,
        t0: Double,
        tBase: Double,
        tau: Double,
        type: Int,
        lat: Double,
        lon: Double,
        elev: Double,
        C: Double,
        S: Double,
        isUmbra: Boolean
    ): ContactPoint? {
        var t = tBase + tau; var pp = 0.0
        val tanf = eval(poly, 0.0, if (isUmbra) 7 else 6)
        for (i in 1..5) {
            val x = eval(poly, t, 0); val y = eval(poly, t, 1); val d = eval(poly, t, 2); val mu = eval(poly, t, 5)
            val xp = deriv(poly, t, 0); val yp = deriv(poly, t, 1)
            val mu1 = deriv(poly, t, 5); val d1 = deriv(poly, t, 2)
            val l = eval(poly, t, if (isUmbra) 4 else 3)
            
            val h = AstroMath.rad(mu + lon - AstroMath.SIDEREAL_DEGREES_PER_UT_SECOND * deltaT)
            val xi = C * sin(h); val eta = S * cos(AstroMath.rad(d)) - C * cos(h) * sin(AstroMath.rad(d))
            val zeta = S * sin(AstroMath.rad(d)) + C * cos(h) * cos(AstroMath.rad(d))
            
            val pr = 0.01745329 * mu1 * C * cos(h)
            val qp = 0.01745329 * (mu1 * xi * sin(AstroMath.rad(d)) - zeta * d1)
            
            val u = x - xi; val v = y - eta; val a = xp - pr; val b = yp - qp
            val lp = if (isUmbra) abs(l) - zeta * tanf else l - zeta * tanf
            val n2 = a * a + b * b; val nn = sqrt(n2)
            val mm = (a * v - u * b) / (nn * lp)
            if (abs(mm) > 1.0) return null
            pp = -(u * a + v * b) / n2 + (lp / nn) * (if (type in listOf(1, 2)) -sqrt(1 - mm * mm) else sqrt(1 - mm * mm))
            t += pp
        }
        val au = t + pp - deltaT / 3600.0
        val jd = floor(jdeT0 + 0.5) - 0.5 + ((t0 + au) / 24.0)
        val name = when (type) {
            1 -> "U1"
            2 -> "U2"
            3 -> "U3"
            else -> "U4"
        }
        return formatLocalContact(name, jd, poly, t, deltaT, lat, lon, elev, C, S)
    }

    private fun formatLocalContact(name: String, jd: Double, poly: Array<DoubleArray>, t: Double, deltaT: Double, lat: Double, lon: Double, elev: Double, C: Double, S: Double): ContactPoint {
        // Full topocentric recalculation for local circumstances.
        val sun = Vsop87SolarEngine.compute(jd, 0.0)
        val gast = AstroDataUtils.calculateGAST(jd)
        val ra = sun.ra
        val dec = sun.dec
        val lha = AstroMath.mod(gast + lon - ra, 360.0)
        
        val phi = SolarFunctions.solarEquatorialHorizontalParallax(sun.distanceAU)
        val earthBa = AstroMath.EARTH_POLAR_RADIUS_M / AstroMath.EARTH_EQUATORIAL_RADIUS_M
        val geocentricLat = atan(earthBa * tan(AstroMath.rad(lat)))
        val x = cos(geocentricLat) + (elev / AstroMath.EARTH_EQUATORIAL_RADIUS_M) * cos(AstroMath.rad(lat))
        val y = earthBa * sin(geocentricLat) + (elev / AstroMath.EARTH_EQUATORIAL_RADIUS_M) * sin(AstroMath.rad(lat))
        
        val dAlpha = AstroMath.deg(atan2(-x * sin(AstroMath.rad(phi)) * sin(AstroMath.rad(lha)), cos(AstroMath.rad(dec)) - x * sin(AstroMath.rad(phi)) * cos(AstroMath.rad(lha))))
        val dltP = AstroMath.deg(atan2((sin(AstroMath.rad(dec)) - y * sin(AstroMath.rad(phi))) * cos(AstroMath.rad(dAlpha)), cos(AstroMath.rad(dec)) - x * sin(AstroMath.rad(phi)) * cos(AstroMath.rad(lha))))
        val lhaP = lha - dAlpha
        
        val azmP = AstroMath.mod(AstroMath.deg(atan2(sin(AstroMath.rad(lhaP)), cos(AstroMath.rad(lhaP)) * sin(AstroMath.rad(lat)) - tan(AstroMath.rad(dltP)) * cos(AstroMath.rad(lat)))) + 180.0, 360.0)
        val ht = AstroMath.deg(asin(sin(AstroMath.rad(lat)) * sin(AstroMath.rad(dltP)) + cos(AstroMath.rad(lat)) * cos(AstroMath.rad(dltP)) * cos(AstroMath.rad(lhaP))))
        
        val hGeocentric = AstroMath.deg(asin(sin(AstroMath.rad(lat)) * sin(AstroMath.rad(dec)) + cos(AstroMath.rad(lat)) * cos(AstroMath.rad(dec)) * cos(AstroMath.rad(lha))))
        val Rfr = (1.02 / tan(AstroMath.rad(hGeocentric + 10.3 / (hGeocentric + 5.11))) * 1010.0 / 1010.0 * 283.0 / (273.0 + 10.0) + 0.0019279204034639303) / 60.0
        val Dip = 1.75 / 60.0 * sqrt(elev)
        
        val altP = ht + Rfr + Dip
        
        // Calculate Position Angle & Axis Distance on Besselian Plane for topocentric observer
        val xBesselian = eval(poly, t, 0); val yBesselian = eval(poly, t, 1); val d = eval(poly, t, 2); val mu = eval(poly, t, 5)
        val h = AstroMath.rad(mu + lon - AstroMath.SIDEREAL_DEGREES_PER_UT_SECOND * deltaT)
        val xi = C * sin(h); val eta = S * cos(AstroMath.rad(d)) - C * cos(h) * sin(AstroMath.rad(d))
        val u = xBesselian - xi; val v = yBesselian - eta
        val dist = sqrt(u * u + v * v)
        var pAngle = Math.toDegrees(atan2(u, v))
        if (pAngle < 0) pAngle += 360.0

        return ContactPoint(
            name = name,
            jdeTD = jd,
            latitude = altP,
            longitude = azmP,
            zenithLat = null,
            zenithLon = null,
            positionAngle = pAngle,
            axisDistance = dist
        )
    }

    fun calculateLunarDetail(jdeApprox: Double, deltaT: Double, timezone: Double): LunarEclipseDetail {
        var jdeMxTD = jdeApprox
        val stepMx = 1.0 / 1440.0
        for (iter in 0..4) {
            val m0 = getLunarDist(jdeMxTD - stepMx)
            val m2 = getLunarDist(jdeMxTD + stepMx)
            val dm = (m2 - m0) / (2.0 * stepMx)
            val d2m = (m2 - 2 * getLunarDist(jdeMxTD) + m0) / (stepMx * stepMx)
            if (abs(d2m) < 1e-10) break
            jdeMxTD -= dm / d2m
        }

        val sunMx  = Vsop87SolarEngine.compute(jdeMxTD, 0.0)
        val moonMx = ElpMpp02LunarEngine.computeGeometric(jdeMxTD)
        val raSh   = AstroMath.mod(sunMx.ra + 180.0, 360.0)
        val decSh  = -sunMx.dec
        val dif    = AstroMath.mod(moonMx.ra - raSh, 360.0)
        val dRaMx  = if (dif > 180.0) dif - 360.0 else dif
        val xMx    = dRaMx * cos(AstroMath.rad(moonMx.dec))
        val yMx    = moonMx.dec - decSh
        val mMx    = sqrt(xMx * xMx + yMx * yMx)

        val hpMoon = moonMx.horizontalParallax
        val hpSun  = SolarFunctions.solarEquatorialHorizontalParallax(sunMx.distanceAU)
        val sdSun  = SolarFunctions.solarSemidiameter(sunMx.distanceAU)
        val sdMoon = moonMx.semidiameter

        // Herald/Sinnott atmospheric enlargement for lunar shadow radii.
        val rUmbra    = (hpMoon + hpSun - sdSun) + (46.3 / 3600.0)
        val rPenumbra = (hpMoon + hpSun + sdSun) + (46.3 / 3600.0)

        // NASA Magnitude uses Polar Radius because greatest eclipse is near the Y-axis
        val rUmbraPolar = rUmbra * 0.996647
        val rPenumbraPolar = rPenumbra * 0.996647

        val gamma       = (mMx / hpMoon) * (if (yMx < 0) -1.0 else 1.0)
        val magUmbra    = (rUmbraPolar + sdMoon - mMx) / (2.0 * sdMoon)
        val magPenumbra = (rPenumbraPolar + sdMoon - mMx) / (2.0 * sdMoon)
        val type = when {
            magUmbra >= 1.0   -> "TOTAL"
            magUmbra >  0.0   -> "SEBAGIAN"
            magPenumbra > 0.0 -> "PENUMBRAL"
            else              -> "TIDAK TERJADI GERHANA"
        }

        // Polynomial Besselian Elements for Fast Contact Solving
        val t0H   = kotlin.math.round((jdeMxTD + 0.5 - kotlin.math.floor(jdeMxTD + 0.5)) * 24.0)
        val jdeT0 = kotlin.math.floor(jdeMxTD + 0.5) - 0.5 + t0H / 24.0
        val tX = DoubleArray(5); val tY = DoubleArray(5); val tD = DoubleArray(5)
        val tF1 = DoubleArray(5); val tF2 = DoubleArray(5); val tF3 = DoubleArray(5); val tMu = DoubleArray(5)
        for (i in 0..4) {
            val jdS = jdeT0 + (i - 2).toDouble() / 24.0
            val s = Vsop87SolarEngine.compute(jdS, 0.0)
            val m = ElpMpp02LunarEngine.computeGeometric(jdS)
            val rs = AstroMath.mod(s.ra + 180.0, 360.0); val ds = -s.dec
            val df = AstroMath.mod(m.ra - rs, 360.0); val dr = if (df > 180.0) df - 360.0 else df
            
            // Earth flattening factor for Lunar Eclipses
            val flattenScale = 1.003364
            
            tX[i] = dr * kotlin.math.cos(AstroMath.rad(m.dec)); tY[i] = (m.dec - ds) * flattenScale; tD[i] = s.dec
            val hM = m.horizontalParallax; val hS = SolarFunctions.solarEquatorialHorizontalParallax(s.distanceAU)
            val sS = SolarFunctions.solarSemidiameter(s.distanceAU)
            tF1[i] = (hM + hS + sS) + (46.3/3600.0)
            tF2[i] = (hM + hS - sS) + (46.3/3600.0)
            tF3[i] = m.semidiameter
            
            // Apparent greenwich hour angle of the shadow
            val jdUT = jdS - deltaT / 86400.0
            val jd0 = kotlin.math.floor(jdUT + 0.5) - 0.5
            val tJC = (jd0 - 2451545.0) / 36525.0
            val gmst = 24110.54841 + 8640184.812866 * tJC + 0.093104 * tJC.pow(2.0) - 0.0000062 * tJC.pow(3.0)
            val H = (jdUT - jd0) * 24.0
            val gha0 = AstroMath.mod(gmst / 3600.0 * 15.0, 360.0)
            val gha = AstroMath.mod(gha0 + H * 15.024601, 360.0)
            tMu[i] = AstroMath.mod(gha - rs, 360.0)
        }
        val poly = Array(5) { DoubleArray(7) } // rows: 0 to 4 (powers of t), cols: x, y, d, L1, L2, mu, F3
        for (o in 0..4) {
            poly[o][0] = AstroMath.interpolationFromFiveTabularValues(tX[0], tX[1], tX[2], tX[3], tX[4], o)
            poly[o][1] = AstroMath.interpolationFromFiveTabularValues(tY[0], tY[1], tY[2], tY[3], tY[4], o)
            poly[o][2] = AstroMath.interpolationFromFiveTabularValues(tD[0], tD[1], tD[2], tD[3], tD[4], o)
            poly[o][3] = AstroMath.interpolationFromFiveTabularValues(tF1[0],tF1[1],tF1[2],tF1[3],tF1[4], o)
            poly[o][4] = AstroMath.interpolationFromFiveTabularValues(tF2[0],tF2[1],tF2[2],tF2[3],tF2[4], o)
            poly[o][5] = AstroMath.interpolationFromFiveTabularValues(tMu[0],tMu[1],tMu[2],tMu[3],tMu[4], o)
            poly[o][6] = AstroMath.interpolationFromFiveTabularValues(tF3[0],tF3[1],tF3[2],tF3[3],tF3[4], o)
        }
        val bTable = (0..4).map { o -> BesselianRow(o, poly[o][0], poly[o][1], poly[o][2], poly[o][3], poly[o][4], poly[o][6]) }

        // Fast polynomial solve for contacts!
        val tMx = (jdeMxTD - jdeT0) * 24.0
        val p1 = solveLunarContactPoint("P1", poly, jdeT0, deltaT, tMx - 3.0, 1, 1)
        val u1 = solveLunarContactPoint("U1", poly, jdeT0, deltaT, tMx - 2.0, 2, 1)
        val u2 = if (magUmbra >= 1.0) solveLunarContactPoint("U2", poly, jdeT0, deltaT, tMx - 1.0, 2, 2) else null
        val u3 = if (magUmbra >= 1.0) solveLunarContactPoint("U3", poly, jdeT0, deltaT, tMx + 1.0, 2, 3) else null
        val u4 = solveLunarContactPoint("U4", poly, jdeT0, deltaT, tMx + 2.0, 2, 4)
        val p4 = solveLunarContactPoint("P4", poly, jdeT0, deltaT, tMx + 3.0, 1, 4)

        // Mx contact point
        val xAtMx = eval(poly, tMx, 0)
        val yAtMx = eval(poly, tMx, 1)
        val mMxDist = sqrt(xAtMx * xAtMx + yAtMx * yAtMx)
        var pAngleMx = Math.toDegrees(atan2(xAtMx, yAtMx))
        if (pAngleMx < 0) pAngleMx += 360.0
        val moonMxGeo = ElpMpp02LunarEngine.computeGeometric(jdeMxTD)
        val jdeMxUT = jdeMxTD - deltaT / 86400.0
        val gastMx = AstroDataUtils.calculateGAST(jdeMxUT)
        val ghaMx = AstroMath.mod(gastMx - moonMxGeo.ra, 360.0)
        var lonMx = if (ghaMx > 180.0) ghaMx - 360.0 else ghaMx
        lonMx = -lonMx
        
        val mxPoint = ContactPoint(
            name = "Greatest",
            jdeTD = jdeMxTD,
            latitude = 0.0,
            longitude = 0.0,
            zenithLat = moonMxGeo.dec,
            zenithLon = lonMx,
            positionAngle = pAngleMx,
            axisDistance = mMxDist
        )

        val dtD = deltaT / 86400.0
        val tzLabel = when(timezone) {
            7.0 -> "WIB"
            8.0 -> "WITA"
            9.0 -> "WIT"
            else -> "LT"
        }
        
        return LunarEclipseDetail(
            type = type, magUmbra = magUmbra, magPenumbra = magPenumbra,
            gamma = gamma, epsilon = mMx,
            p1 = p1, u1 = u1, u2 = u2, mx = mxPoint, u3 = u3, u4 = u4, p4 = p4,
            sunRA = sunMx.ra, sunDec = sunMx.dec, sunSD = sdSun, sunHP = hpSun,
            moonRA = moonMx.ra, moonDec = moonMx.dec, moonSD = sdMoon, moonHP = hpMoon,
            rUmbra = rUmbra, rPenumbra = rPenumbra, deltaT = deltaT,
            besselianTable = bTable,
            shadowRule = "Herald/Sinnott", shadowEnlargement = "1.000",
            timezone = timezone, tzLabel = tzLabel
        )
    }

    private fun getLunarDist(jd: Double): Double {
        val sun = Vsop87SolarEngine.compute(jd, 0.0)
        val moon = ElpMpp02LunarEngine.computeGeometric(jd)
        val raShadow = AstroMath.mod(sun.ra + 180.0, 360.0)
        val decShadow = -sun.dec
        val diff = AstroMath.mod(moon.ra - raShadow, 360.0)
        val dRa = if (diff > 180.0) diff - 360.0 else diff
        val x = dRa * cos(AstroMath.rad(moon.dec))
        val y = moon.dec - decShadow
        return sqrt(x * x + y * y)
    }

    fun jdeToCalendar(jde: Double): IntArray {
        val date = SolarFunctions.jdeToGregorian(jde)
        return intArrayOf(date.year, date.month, date.day)
    }

    private fun solveContact(poly: Array<DoubleArray>, jdeT0: Double, deltaT: Double, tStart: Double, type: Int, point: Int): Triple<Double, Double, Double>? {
        var t = tStart
        for (i in 1..20) {
            val x = eval(poly, t, 0)
            val y = eval(poly, t, 1)
            val xp = deriv(poly, t, 0)
            val yp = deriv(poly, t, 1)
            
            // F1 = col 3, F2 = col 4, F3 = col 6
            val fIndex = if (type == 1) 3 else 4
            val fBase = eval(poly, t, fIndex)
            val f3 = eval(poly, t, 6)
            
            val fBaseP = deriv(poly, t, fIndex)
            val f3P = deriv(poly, t, 6)
            
            // For Outer contacts (P1, P4, U1, U4) -> radius sum (fBase + f3)
            // For Inner contacts (U2, U3) -> radius difference (fBase - f3)
            val isOuter = point == 1 || point == 4
            val L = if (isOuter) fBase + f3 else fBase - f3
            val Lp = if (isOuter) fBaseP + f3P else fBaseP - f3P
            
            val m = sqrt(x * x + y * y)
            val mp = (x * xp + y * yp) / m
            
            // f(t) = m - L = 0
            val f = m - L
            val df = mp - Lp
            
            val dt = -f / df
            
            t += dt
            if (abs(dt) < 1e-6) {
                val td = jdeT0 + t / 24.0
                return Triple(td, td - deltaT / 86400.0, t)
            }
        }
        return null
    }

    private fun solveLunarContactPoint(
        name: String,
        poly: Array<DoubleArray>,
        jdeT0: Double,
        deltaT: Double,
        tStart: Double,
        type: Int,
        point: Int
    ): ContactPoint? {
        val result = solveContact(poly, jdeT0, deltaT, tStart, type, point) ?: return null
        val td = result.first
        val t = result.third
        
        val x = eval(poly, t, 0)
        val y = eval(poly, t, 1)
        val m = sqrt(x * x + y * y)
        
        // P-Angle (dari Utara ke Timur)
        var pAngle = Math.toDegrees(atan2(x, y))
        if (pAngle < 0) pAngle += 360.0
        
        // Zenith Coordinates
        // Moon position computed at TD (ephemeris time) - correct
        val moon = ElpMpp02LunarEngine.computeGeometric(td)
        // GAST must be calculated at UT1, not TD!
        val jdeUT = td - deltaT / 86400.0
        val gast = AstroDataUtils.calculateGAST(jdeUT)
        // Sub-Lunar Longitude = RA_moon - GAST (East positive)
        // GHA_moon = GAST - RA_moon (West positive)
        // Sub-Lunar Longitude = -GHA (East positive, range -180 to +180)
        val gha = AstroMath.mod(gast - moon.ra, 360.0)
        var lon = if (gha > 180.0) gha - 360.0 else gha
        lon = -lon // Convert from West-positive GHA to East-positive Longitude
        
        return ContactPoint(
            name = name,
            jdeTD = td,
            latitude = 0.0,
            longitude = 0.0,
            zenithLat = moon.dec,
            zenithLon = lon,
            positionAngle = pAngle,
            axisDistance = m
        )
    }

    private fun solveSolarContact(
        poly: Array<DoubleArray>,
        jdeT0: Double,
        tStart: Double,
        isUmbra: Boolean
    ): SolarContactSolution? {
        var t = tStart
        for (i in 1..30) {
            val f = solarContactResidual(poly, t, isUmbra).first
            val h = 1e-4
            val fp = (
                solarContactResidual(poly, t + h, isUmbra).first -
                    solarContactResidual(poly, t - h, isUmbra).first
                ) / (2.0 * h)
            if (abs(fp) < 1e-12) return null
            val dt = -f / fp
            t += dt
            if (abs(dt) < 1e-8) {
                val contact = solarContactResidual(poly, t, isUmbra).second
                val contactPoint = if (isUmbra) {
                    projectCenterToEarthLimb(eval(poly, t, 0), eval(poly, t, 1))
                } else {
                    contact
                }
                val td = jdeT0 + t / 24.0
                return SolarContactSolution(
                    td = td,
                    t = t,
                    contactX = contactPoint.x,
                    contactY = contactPoint.y
                )
            }
        }
        return null
    }

    private fun solarContactResidual(poly: Array<DoubleArray>, t: Double, isUmbra: Boolean): Pair<Double, EllipseContact> {
        val x = eval(poly, t, 0)
        val y = eval(poly, t, 1)
        val d = eval(poly, t, 2)
        val e2 = AstroMath.EARTH_ECCENTRICITY_SQUARED
        val rho = sqrt(1.0 - e2 * cos(AstroMath.rad(d)).pow(2.0))
        val contact = closestPointOnEarthEllipse(x, y, rho)
        val lVal = eval(poly, t, if (isUmbra) 4 else 3)
        val shadowRadius = if (isUmbra) abs(lVal) else lVal
        return Pair(contact.distance - shadowRadius, contact)
    }

    private fun closestPointOnEarthEllipse(x: Double, y: Double, rho: Double): EllipseContact {
        var bestDistance = Double.POSITIVE_INFINITY
        var bestX = 0.0
        var bestY = 0.0
        val initialAngles = doubleArrayOf(atan2(y / rho, x), 0.0, PI / 2.0, PI, -PI / 2.0)

        for (initial in initialAngles) {
            var theta = initial
            for (i in 1..30) {
                val c = cos(theta)
                val s = sin(theta)
                val rx = c - x
                val ry = rho * s - y
                val f = rx * -s + ry * rho * c
                val fp = s * s - rx * c + rho * rho * c * c - ry * rho * s
                if (abs(fp) < 1e-14) break
                val step = f / fp
                theta -= step
                if (abs(step) < 1e-14) break
            }

            val cx = cos(theta)
            val cy = rho * sin(theta)
            val distance = hypot(cx - x, cy - y)
            if (distance < bestDistance) {
                bestDistance = distance
                bestX = cx
                bestY = cy
            }
        }

        return EllipseContact(bestDistance, bestX, bestY)
    }

    private fun projectCenterToEarthLimb(x: Double, y: Double): EllipseContact {
        val scale = 1.0 / hypot(x, y)
        val px = x * scale
        val py = y * scale
        return EllipseContact(hypot(px - x, py - y), px, py)
    }

    private fun formatContact(name: String, td: Double, poly: Array<DoubleArray>, t: Double, deltaT: Double, contactX: Double? = null, contactY: Double? = null): ContactPoint {
        val shadowX = eval(poly, t, 0); val shadowY = eval(poly, t, 1); val d = eval(poly, t, 2); val mu = eval(poly, t, 5)
        val x = contactX ?: shadowX
        val y = contactY ?: shadowY
        val ba = AstroMath.EARTH_POLAR_RADIUS_M / AstroMath.EARTH_EQUATORIAL_RADIUS_M
        val ab = AstroMath.EARTH_EQUATORIAL_RADIUS_M / AstroMath.EARTH_POLAR_RADIUS_M
        val ms = sqrt(x * x + y * y); val e2 = 1.0 - ba * ba
        val rho0 = sqrt(1.0 - e2 * cos(AstroMath.rad(d)).pow(2.0))
        val d1 = AstroMath.deg(atan(sin(AstroMath.rad(d)) / (cos(AstroMath.rad(d)) * ba)))
        val y1 = y / rho0; val m1 = sqrt(x * x + y1 * y1); val y2 = y1 / m1
        val bBig = sqrt(max(0.0, 1.0 - x * x - y1 * y1))
        val pi1 = if (ms < 0.9972) AstroMath.deg(asin(y1 * cos(AstroMath.rad(d1)) + bBig * sin(AstroMath.rad(d1)))) else AstroMath.deg(asin(y2 * cos(AstroMath.rad(d1))))
        val lat = AstroMath.deg(atan(ab * tan(AstroMath.rad(pi1))))
        val x2 = if (ms < 0.9972) -y1 * sin(AstroMath.rad(d1)) + bBig * cos(AstroMath.rad(d1)) else -y1 * sin(AstroMath.rad(d1))
        val ha = AstroMath.mod(AstroMath.deg(atan2(x, x2)), 360.0)
        val lonRaw = ha - mu + (AstroMath.SIDEREAL_DEGREES_PER_UT_SECOND * deltaT)
        val lon = if (lonRaw > 180) lonRaw - 360 else if (lonRaw < -180) lonRaw + 360 else lonRaw
        
        val dist = sqrt(shadowX * shadowX + shadowY * shadowY)
        var pAngle = Math.toDegrees(atan2(shadowX, shadowY))
        if (pAngle < 0) pAngle += 360.0

        return ContactPoint(
            name = name,
            jdeTD = td,
            latitude = lat,
            longitude = lon,
            zenithLat = null,
            zenithLon = null,
            positionAngle = pAngle,
            axisDistance = dist
        )
    }

    private fun eval(poly: Array<DoubleArray>, t: Double, col: Int): Double {
        return poly[0][col] + poly[1][col] * t + poly[2][col] * t * t + poly[3][col] * t.pow(3) + poly[4][col] * t.pow(4)
    }

    private fun deriv(poly: Array<DoubleArray>, t: Double, col: Int): Double {
        return poly[1][col] + 2 * poly[2][col] * t + 3 * poly[3][col] * t * t + 4 * poly[4][col] * t.pow(3)
    }
}
