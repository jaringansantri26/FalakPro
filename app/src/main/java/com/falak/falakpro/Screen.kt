package com.falak.falakpro

import com.falak.falakpro.premium.VisibilityMapRequest

sealed class Screen {
    data object Splash : Screen()
    data object Home : Screen()
    data object AwalBulan : Screen()
    data class VisibilityMap(val request: VisibilityMapRequest? = null) : Screen()
    data object KalenderAstronomis : Screen()
    data object Gerhana : Screen()
    data class GerhanaDetail(
        val jde: Double,
        val isSolar: Boolean,
        val typology: String,
        val lat: Double,
        val lon: Double,
        val elev: Double,
        val timezone: Double,
        val locName: String = ""
    ) : Screen()
    data object JadwalShalat : Screen()
    data object Kiblat : Screen()
    data object KiblatKamera : Screen()
    data object KiblatSettings : Screen()
    data object DataFalak : Screen()
    data object ScientificCalculator : Screen()
    data object Settings : Screen()
}
