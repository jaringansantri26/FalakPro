package com.falak.falakpro.premium

import kotlin.math.*

/**
 * ElpMpp02LunarEngine — 100% PERSIS v1.8 Parity Edition.
 * Adopts the exact manual Precession and Aberration formulas from MoonLongitude.kt and MoonLatitude.kt.
 */
object ElpMpp02LunarEngine {

    data class LunarPosition(
        val ra: Double,
        val dec: Double,
        val longitudeEcliptic: Double,
        val latitudeEcliptic: Double,
        val distanceAU: Double,
        val horizontalParallax: Double, // Degrees
        val semidiameter: Double        // Degrees
    )

    fun computeGeometric(jdeTD: Double, withAberration: Boolean = true): LunarPosition {
        val t = (jdeTD - 2451545.0) / 36525.0
        val t2 = t * t; val t3 = t * t2; val t4 = t * t3

        // 1. Get ELP series sums (reading from binary data)
        val lSum = ElpDataProvider.getLongitudeSum(jdeTD) // arcseconds
        val bSum = ElpDataProvider.getLatitudeSum(jdeTD)  // arcseconds
        val rSum = ElpDataProvider.getDistanceSum(jdeTD)  // km

        // 2. Add Mean Longitude (W) from MoonLongitude.kt:271
        val w0 = 3.81034409083088
        val w1 = 8399.68473007193
        val w2 = -0.0000331895204255009
        val w3 = 3.11024944910606E-08
        val w4 = -2.03282376489228E-10
        val w = w0 + w1 * t + w2 * t2 + w3 * t3 + w4 * t4 // radians

        // 3. Add Precession (P) from MoonLongitude.kt:279
        val p1 = 5029.0966 - 0.29965
        val p2 = 1.112
        val p3 = 0.000077
        val p4 = -0.00002353
        val p = p1 * t + p2 * t2 + p3 * t3 + p4 * t4 // arcseconds

        // 4. Calculate True Geocentric Ecliptic Longitude (MoonLongitude.kt:287)
        val moonTrueLon = AstroMath.mod(deg(w) + lSum / 3600.0 + p / 3600.0, 360.0)

        // 5. Add Nutation and (optionally) Aberration (MoonLongitude.kt:294)
        // Ref: Explanatory Supplement §11.3 — Besselian elements use apparent (nutation included)
        // but WITHOUT aberration correction
        val nutation = AstroDataUtils.calculateNutation(jdeTD).first
        val aberrLon = if (withAberration) -0.00019524 - 0.00001059 * sin(rad(225.0 + 477198.9 * t)) else 0.0
        val moonAppaLon = moonTrueLon + nutation + aberrLon

        // 6. Calculate Apparent Latitude (MoonLatitude.kt:178)
        val aberrLat = if (withAberration) -0.00001754 * sin(rad(183.3 + 483202.0 * t)) else 0.0
        val moonAppaLat = bSum / 3600.0 + aberrLat

        // 7. Calculate Distance (MoonDistance.kt:267)
        val distAbr = if (withAberration) 0.0708 * cos(rad(225.0 + 477198.9 * t)) else 0.0
        val distKm = rSum + distAbr

        // 8. Transform to Equatorial
        val coords = AstroDataUtils.eclipticToEquatorialApparent(moonAppaLon, moonAppaLat, jdeTD)
        
        // 9. HP and SD (MoonOtherFunc.kt:109, 119)
        val hpDeg = deg(asin(6378.14 / distKm))
        val sdDeg = deg(asin(0.272481 * sin(rad(hpDeg))))

        return LunarPosition(
            ra = coords.first,
            dec = coords.second,
            longitudeEcliptic = moonAppaLon,
            latitudeEcliptic = moonAppaLat,
            distanceAU = distKm / 149597870.7,
            horizontalParallax = hpDeg,
            semidiameter = sdDeg
        )
    }

    private fun rad(deg: Double) = deg * PI / 180.0
    private fun deg(rad: Double) = rad * 180.0 / PI
}
