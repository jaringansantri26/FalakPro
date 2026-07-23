package com.falak.falakpro.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.falak.falakpro.premium.EclipseDetail
import com.falak.falakpro.premium.GerhanaCalculationService
import com.falak.falakpro.premium.LocalEclipseDetail
import com.falak.falakpro.premium.LunarEclipseDetail
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
        context: Context,
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
                val result = GerhanaCalculationService.detail(
                    jdeApprox = jdeApprox,
                    context = context,
                    isSolar = isSolar,
                    typology = typology,
                    lat = lat,
                    lon = lon,
                    elev = elev,
                    timezone = timezone,
                    locName = locName
                )
                solarDetail.value = result.solarDetail
                localSolarDetail.value = result.localSolarDetail
                lunarDetail.value = result.lunarDetail
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading.value = false
            }
        }
    }
}
