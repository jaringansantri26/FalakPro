package com.falak.falakpro.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.falak.falakpro.premium.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.floor
import kotlin.math.round

class GerhanaViewModel : ViewModel() {
    val searchResults = MutableStateFlow<List<EclipseResultItem>>(emptyList())
    val isLoading = MutableStateFlow(false)

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
    ) {
        viewModelScope.launch {
            isLoading.value = true
            try {
                withContext(Dispatchers.Default) {
                    context.assets.open("mpp02_core.bin").use { stream: java.io.InputStream ->
                        ElpDataProvider.initialize(stream)
                    }
                    context.assets.open("earth_vsop87d.bin").use { stream: java.io.InputStream ->
                        Vsop87SolarEngine.initialize(stream)
                    }

                    val parityEngine = EclipseParityEngine()
                    
                    val searchDeltaT = DynamicalTimeEngine.deltaT2(AstroTime.kmjd(1, 1, year))
                    val ijtimaDates = parityEngine.searchYearly(year, searchDeltaT, isSolar)
                    
                    val results = mutableListOf<EclipseResultItem>()
                    
                    ijtimaDates.forEach { jde ->
                        val tzLabel = getTimezoneLabel(timezone)
                        
                        if (isSolar) {
                            val globalDeltaT = DynamicalTimeEngine.deltaT2(jde)
                            val global = parityEngine.calculateFullDetail(jde, globalDeltaT, timezone)
                            
                            val typeStr = when (global.type) {
                                EclipseParityEngine.EclipseType.TOTAL -> "Gerhana Matahari Total"
                                EclipseParityEngine.EclipseType.ANNULAR -> "Gerhana Matahari Cincin"
                                EclipseParityEngine.EclipseType.HYBRID -> "Gerhana Matahari Hibrida"
                                EclipseParityEngine.EclipseType.PARTIAL -> "Gerhana Matahari Sebagian"
                                else -> "Gerhana Matahari"
                            }

                            if (typology == "Global") {
                                results.add(EclipseResultItem(
                                    title = typeStr,
                                    dateString = jdeToDateString(jde),
                                    typeString = "Global | Mag: %.3f".format(global.magnitude),
                                    isSolar = true,
                                    jdeGreatest = jde,
                                    magnitude = global.magnitude,
                                    localTime = null
                                ))
                                return@forEach
                            }

                            val dynamicDeltaT = DynamicalTimeEngine.deltaT(jde)
                            val local = parityEngine.calculateLocalDetail(jde, dynamicDeltaT, lat, lon, elev, timezone, locName)

                            val u1Alt = local.u1?.latitude ?: -999.0
                            val u2Alt = local.u2?.latitude ?: -999.0
                            val u3Alt = local.u3?.latitude ?: -999.0
                            val u4Alt = local.u4?.latitude ?: -999.0
                            val mxAlt = local.mx.latitude
                            val maxAlt = maxOf(u1Alt, u2Alt, u3Alt, u4Alt, mxAlt)
                            
                            val isVisible = local.type != "TIDAK TERJADI GERHANA" && maxAlt > 0.0
                            
                            if (!isVisible) {
                                return@forEach
                            }
                            
                            val typeDesc = local.type.lowercase().replaceFirstChar { it.uppercase() }
                            val obsNote = if (local.u1 != null && u1Alt < 0.0) {
                                " (Terbit saat gerhana)"
                            } else if (local.u4 != null && u4Alt < 0.0) {
                                " (Terbenam saat gerhana)"
                            } else ""
                            val localTypeStr = "Terlihat Lokal ($typeDesc)$obsNote"
                            val localTimeStr = formatJde(local.mx.jdeTD, dynamicDeltaT, timezone, tzLabel)
                            
                            results.add(EclipseResultItem(
                                title = typeStr,
                                dateString = jdeToDateString(jde),
                                typeString = "$localTypeStr | Mag: %.3f".format(local.magnitude),
                                isSolar = true,
                                jdeGreatest = jde,
                                magnitude = local.magnitude,
                                localTime = localTimeStr
                            ))
                        } else {
                            val dynamicDeltaT = DynamicalTimeEngine.deltaT(jde)
                            val detail = parityEngine.calculateLunarDetail(jde, dynamicDeltaT, timezone)
                            val typeStr = "Gerhana Bulan ${detail.type.lowercase().replaceFirstChar { it.uppercase() }}"

                            if (typology == "Global") {
                                results.add(EclipseResultItem(
                                    title = typeStr,
                                    dateString = jdeToDateString(jde),
                                    typeString = "Global | Mag Umbra: %.3f".format(detail.magUmbra),
                                    isSolar = false,
                                    jdeGreatest = jde,
                                    magnitude = detail.magUmbra,
                                    localTime = null
                                ))
                                return@forEach
                            }
                            
                            var isVisible = false
                            val checkPts = listOfNotNull(detail.p1?.jdeTD, detail.u1?.jdeTD, detail.mx.jdeTD, detail.u4?.jdeTD, detail.p4?.jdeTD)
                            for (pt in checkPts) {
                                val gast = com.falak.falakpro.premium.AstroDataUtils.calculateGAST(pt)
                                val moon = com.falak.falakpro.premium.ElpMpp02LunarEngine.computeGeometric(pt)
                                val ha = com.falak.falakpro.premium.AstroTransform.hourAngle(gast, moon.ra, lon)
                                val (alt, _) = com.falak.falakpro.premium.AstroTransform.equatorialToHorizontal(ha, moon.dec, lat)
                                if (alt > 0) {
                                    isVisible = true
                                    break
                                }
                            }
                            
                            if (!isVisible) {
                                return@forEach
                            }
                            
                            val localTypeStr = "Terlihat Lokal"
                            val localTimeStr = formatJde(detail.mx.jdeTD, dynamicDeltaT, timezone, tzLabel)
                            
                            results.add(EclipseResultItem(
                                title = typeStr,
                                dateString = jdeToDateString(jde),
                                typeString = "$localTypeStr | Mag Umbra: %.3f".format(detail.magUmbra),
                                isSolar = false,
                                jdeGreatest = jde,
                                magnitude = detail.magUmbra,
                                localTime = localTimeStr
                            ))
                        }
                    }
                    
                    if (results.isEmpty()) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Pencarian selesai: 0 hasil.", Toast.LENGTH_SHORT).show()
                        }
                    }

                    withContext(Dispatchers.Main) {
                        searchResults.value = results
                        isLoading.value = false
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isLoading.value = false
                    Toast.makeText(context, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun getTimezoneLabel(offset: Double): String {
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

    private fun jdeToDateString(jde: Double): String {
        val cal = EclipseParityEngine().jdeToCalendar(jde)
        val months = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Agu", "Sep", "Okt", "Nov", "Des")
        return "${cal[2]} ${months[cal[1] - 1]} ${cal[0]}"
    }
}

