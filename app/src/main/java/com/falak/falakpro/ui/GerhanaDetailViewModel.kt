package com.falak.falakpro.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.falak.falakpro.premium.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class GerhanaDetailViewModel : ViewModel() {
    val isLoading = MutableStateFlow(false)
    val solarDetail = MutableStateFlow<EclipseDetail?>(null)
    val localSolarDetail = MutableStateFlow<LocalEclipseDetail?>(null)
    val lunarDetail = MutableStateFlow<LunarEclipseDetail?>(null)

    fun calculate(
        jdeApprox: Double, 
        context: android.content.Context, 
        isSolar: Boolean,
        typology: String = "Global",
        lat: Double = 0.0,
        lon: Double = 0.0,
        elev: Double = 0.0,
        timezone: Double = 7.0,
        locName: String = ""
    ) {
        viewModelScope.launch(Dispatchers.Default) {
            isLoading.value = true
            try {
                context.assets.open("mpp02_core.bin").use { ElpDataProvider.initialize(it) }
                context.assets.open("earth_vsop87d.bin").use { Vsop87SolarEngine.initialize(it) }

                val parityEngine = EclipseParityEngine()
                
                if (isSolar) {
                    val deltaTGlobal = DynamicalTimeEngine.deltaT2(jdeApprox)
                    solarDetail.value = parityEngine.calculateFullDetail(jdeApprox, deltaTGlobal, timezone)
                    localSolarDetail.value = if (typology == "Lokal") {
                        val deltaTLocal = DynamicalTimeEngine.deltaT(jdeApprox)
                        parityEngine.calculateLocalDetail(jdeApprox, deltaTLocal, lat, lon, elev, timezone, locName)
                    } else {
                        null
                    }
                    
                    lunarDetail.value = null
                } else {
                    val deltaT = DynamicalTimeEngine.deltaT(jdeApprox)
                    lunarDetail.value = parityEngine.calculateLunarDetail(jdeApprox, deltaT, timezone)
                    solarDetail.value = null
                    localSolarDetail.value = null
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading.value = false
            }
        }
    }
}

