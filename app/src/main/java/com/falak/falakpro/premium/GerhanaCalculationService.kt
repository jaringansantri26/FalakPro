package com.falak.falakpro.premium

import android.content.Context
import kotlin.math.floor
import kotlin.math.round

data class GerhanaDetailCalculation(
    val solarDetail: EclipseDetail?,
    val localSolarDetail: LocalEclipseDetail?,
    val lunarDetail: LunarEclipseDetail?
)

object GerhanaCalculationService {

    fun search(
        year: Int,
        context: Context,
        isSolar: Boolean,
        typology: String,
        lat: Double = 0.0,
        lon: Double = 0.0,
        elev: Double = 0.0,
        timezone: Double = 7.0,
        locName: String = ""
    ): List<EclipseResultItem> {
        AstroAssetPreloader.ensureCoreBlocking(context.applicationContext)

        val parityEngine = EclipseParityEngine()
        val searchDeltaT = DynamicalTimeEngine.deltaT2(AstroTime.kmjd(1, 1, year))
        val eclipseCandidates = parityEngine.searchYearly(year, searchDeltaT, isSolar)

        return eclipseCandidates.mapNotNull { jde ->
            if (isSolar) {
                buildSolarSearchItem(parityEngine, jde, typology, lat, lon, elev, timezone, locName)
            } else {
                buildLunarSearchItem(parityEngine, jde, typology, lat, lon, timezone)
            }
        }
    }

    fun detail(
        jdeApprox: Double,
        context: Context,
        isSolar: Boolean,
        typology: String = "Global",
        lat: Double = 0.0,
        lon: Double = 0.0,
        elev: Double = 0.0,
        timezone: Double = 7.0,
        locName: String = ""
    ): GerhanaDetailCalculation {
        AstroAssetPreloader.ensureCoreBlocking(context.applicationContext)

        val parityEngine = EclipseParityEngine()
        return if (isSolar) {
            val deltaTGlobal = DynamicalTimeEngine.deltaT2(jdeApprox)
            val global = parityEngine.calculateFullDetail(jdeApprox, deltaTGlobal, timezone)
            val local = if (typology == "Lokal") {
                val deltaTLocal = DynamicalTimeEngine.deltaT(jdeApprox)
                parityEngine.calculateLocalDetail(jdeApprox, deltaTLocal, lat, lon, elev, timezone, locName)
            } else {
                null
            }
            GerhanaDetailCalculation(
                solarDetail = global,
                localSolarDetail = local,
                lunarDetail = null
            )
        } else {
            val deltaT = DynamicalTimeEngine.deltaT(jdeApprox)
            GerhanaDetailCalculation(
                solarDetail = null,
                localSolarDetail = null,
                lunarDetail = parityEngine.calculateLunarDetail(jdeApprox, deltaT, timezone)
            )
        }
    }

    private fun buildSolarSearchItem(
        parityEngine: EclipseParityEngine,
        jde: Double,
        typology: String,
        lat: Double,
        lon: Double,
        elev: Double,
        timezone: Double,
        locName: String
    ): EclipseResultItem? {
        val globalDeltaT = DynamicalTimeEngine.deltaT2(jde)
        val global = parityEngine.calculateFullDetail(jde, globalDeltaT, timezone)
        val title = solarTypeTitle(global.type)

        if (typology == "Global") {
            return EclipseResultItem(
                title = title,
                dateString = jdeToDateString(parityEngine, jde),
                typeString = "Global | Mag: %.3f".format(global.magnitude),
                isSolar = true,
                jdeGreatest = jde,
                magnitude = global.magnitude,
                localTime = null
            )
        }

        val dynamicDeltaT = DynamicalTimeEngine.deltaT(jde)
        val local = parityEngine.calculateLocalDetail(jde, dynamicDeltaT, lat, lon, elev, timezone, locName)
        if (!isLocalSolarVisible(local)) return null

        val typeDesc = local.type.lowercase().replaceFirstChar { it.uppercase() }
        val obsNote = when {
            local.u1 != null && local.u1.latitude < 0.0 -> " (Terbit saat gerhana)"
            local.u4 != null && local.u4.latitude < 0.0 -> " (Terbenam saat gerhana)"
            else -> ""
        }
        val tzLabel = timezoneLabel(timezone)

        return EclipseResultItem(
            title = title,
            dateString = jdeToDateString(parityEngine, jde),
            typeString = "Terlihat Lokal ($typeDesc)$obsNote | Mag: %.3f".format(local.magnitude),
            isSolar = true,
            jdeGreatest = jde,
            magnitude = local.magnitude,
            localTime = formatJde(local.mx.jdeTD, dynamicDeltaT, timezone, tzLabel)
        )
    }

    private fun buildLunarSearchItem(
        parityEngine: EclipseParityEngine,
        jde: Double,
        typology: String,
        lat: Double,
        lon: Double,
        timezone: Double
    ): EclipseResultItem? {
        val dynamicDeltaT = DynamicalTimeEngine.deltaT(jde)
        val detail = parityEngine.calculateLunarDetail(jde, dynamicDeltaT, timezone)
        val title = "Gerhana Bulan ${detail.type.lowercase().replaceFirstChar { it.uppercase() }}"

        if (typology == "Global") {
            return EclipseResultItem(
                title = title,
                dateString = jdeToDateString(parityEngine, jde),
                typeString = "Global | Mag Umbra: %.3f".format(detail.magUmbra),
                isSolar = false,
                jdeGreatest = jde,
                magnitude = detail.magUmbra,
                localTime = null
            )
        }

        if (!isLocalLunarVisible(detail, lat, lon)) return null

        val tzLabel = timezoneLabel(timezone)
        return EclipseResultItem(
            title = title,
            dateString = jdeToDateString(parityEngine, jde),
            typeString = "Terlihat Lokal | Mag Umbra: %.3f".format(detail.magUmbra),
            isSolar = false,
            jdeGreatest = jde,
            magnitude = detail.magUmbra,
            localTime = formatJde(detail.mx.jdeTD, dynamicDeltaT, timezone, tzLabel)
        )
    }

    private fun isLocalSolarVisible(local: LocalEclipseDetail): Boolean {
        val contactAltitudes = listOfNotNull(
            local.u1?.latitude,
            local.u2?.latitude,
            local.u3?.latitude,
            local.u4?.latitude,
            local.mx.latitude
        )
        return local.type != "TIDAK TERJADI GERHANA" && contactAltitudes.any { it > 0.0 }
    }

    private fun isLocalLunarVisible(detail: LunarEclipseDetail, lat: Double, lon: Double): Boolean {
        val checkPoints = listOfNotNull(
            detail.p1?.jdeTD,
            detail.u1?.jdeTD,
            detail.mx.jdeTD,
            detail.u4?.jdeTD,
            detail.p4?.jdeTD
        )
        return checkPoints.any { jde ->
            val gast = AstroDataUtils.calculateGAST(jde)
            val moon = ElpMpp02LunarEngine.computeGeometric(jde)
            val ha = AstroTransform.hourAngle(gast, moon.ra, lon)
            val (alt, _) = AstroTransform.equatorialToHorizontal(ha, moon.dec, lat)
            alt > 0.0
        }
    }

    private fun solarTypeTitle(type: EclipseParityEngine.EclipseType): String {
        return when (type) {
            EclipseParityEngine.EclipseType.TOTAL -> "Gerhana Matahari Total"
            EclipseParityEngine.EclipseType.ANNULAR -> "Gerhana Matahari Cincin"
            EclipseParityEngine.EclipseType.HYBRID -> "Gerhana Matahari Hibrida"
            EclipseParityEngine.EclipseType.PARTIAL -> "Gerhana Matahari Sebagian"
            else -> "Gerhana Matahari"
        }
    }

    private fun timezoneLabel(offset: Double): String {
        return when (offset) {
            7.0 -> "WIB"
            8.0 -> "WITA"
            9.0 -> "WIT"
            else -> "LT"
        }
    }

    private fun formatJde(jdeTD: Double, deltaT: Double, timezone: Double, tzLabel: String): String {
        val jdeUT = jdeTD - deltaT / 86400.0
        val jdeLT = if (tzLabel == "UT") jdeUT else jdeUT + timezone / 24.0
        val h = (jdeLT + 0.5 - floor(jdeLT + 0.5)) * 24.0
        val hh = h.toInt()
        val mm = ((h - hh) * 60.0).toInt()
        val ss = round(((h - hh) * 60.0 - mm) * 60.0).toInt()
        return "%02d:%02d:%02d $tzLabel".format(hh, mm, ss)
    }

    private fun jdeToDateString(parityEngine: EclipseParityEngine, jde: Double): String {
        val cal = parityEngine.jdeToCalendar(jde)
        val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
        return "${cal[2]} ${months[cal[1] - 1]} ${cal[0]}"
    }
}
