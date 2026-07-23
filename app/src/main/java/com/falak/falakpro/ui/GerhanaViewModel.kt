package com.falak.falakpro.ui

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.falak.falakpro.premium.EclipseResultItem
import com.falak.falakpro.premium.GerhanaCalculationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
                val results = withContext(Dispatchers.Default) {
                    GerhanaCalculationService.search(
                        year = year,
                        context = context,
                        isSolar = isSolar,
                        typology = typology,
                        lat = lat,
                        lon = lon,
                        elev = elev,
                        timezone = timezone,
                        locName = locName
                    )
                }
                searchResults.value = results
                if (results.isEmpty()) {
                    Toast.makeText(context, "Pencarian selesai: 0 hasil.", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                isLoading.value = false
            }
        }
    }
}
