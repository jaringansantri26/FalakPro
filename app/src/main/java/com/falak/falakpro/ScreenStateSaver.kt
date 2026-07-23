package com.falak.falakpro

import androidx.compose.runtime.saveable.Saver
import com.falak.falakpro.premium.HilalVisibilityMapMode
import com.falak.falakpro.premium.VisibilityMapRequest
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

val screenStateSaver = Saver<Screen, String>(
    save = { screen ->
        when (screen) {
            is Screen.Splash -> "splash"
            is Screen.Home -> "home"
            is Screen.AwalBulan -> "awal_bulan"
            is Screen.VisibilityMap -> saveVisibilityMapScreen(screen.request)
            is Screen.KalenderAstronomis -> "kalender_astronomis"
            is Screen.Gerhana -> "gerhana"
            is Screen.GerhanaDetail -> saveGerhanaDetailScreen(screen)
            is Screen.JadwalShalat -> "jadwal_shalat"
            is Screen.Kiblat -> "kiblat"
            is Screen.KiblatKamera -> "kiblat_kamera"
            is Screen.KiblatSettings -> "kiblat_settings"
            is Screen.DataFalak -> "data_falak"
            is Screen.ScientificCalculator -> "scientific_calculator"
            is Screen.Settings -> "settings"
        }
    },
    restore = { raw ->
        val pipeParts = raw.split('|')
        val colonParts = raw.split(":")
        when {
            pipeParts.firstOrNull() == "visibility_map" -> {
                Screen.VisibilityMap(restoreVisibilityMapRequest(pipeParts))
            }
            colonParts.firstOrNull() == "gerhana_detail" -> restoreGerhanaDetailScreen(colonParts)
            colonParts.firstOrNull() == "splash" -> Screen.Splash
            colonParts.firstOrNull() == "home" -> Screen.Home
            colonParts.firstOrNull() == "awal_bulan" -> Screen.AwalBulan
            colonParts.firstOrNull() == "kalender_astronomis" -> Screen.KalenderAstronomis
            colonParts.firstOrNull() == "gerhana" -> Screen.Gerhana
            colonParts.firstOrNull() == "jadwal_shalat" -> Screen.JadwalShalat
            colonParts.firstOrNull() == "kiblat" -> Screen.Kiblat
            colonParts.firstOrNull() == "kiblat_kamera" -> Screen.KiblatKamera
            colonParts.firstOrNull() == "kiblat_settings" -> Screen.KiblatSettings
            colonParts.firstOrNull() == "data_falak" -> Screen.DataFalak
            colonParts.firstOrNull() == "scientific_calculator" -> Screen.ScientificCalculator
            colonParts.firstOrNull() == "settings" -> Screen.Settings
            else -> Screen.Home
        }
    }
)

private fun saveGerhanaDetailScreen(screen: Screen.GerhanaDetail): String {
    return listOf(
        "gerhana_detail",
        screen.jde.toString(),
        screen.isSolar.toString(),
        encodeScreenText(screen.typology),
        screen.lat.toString(),
        screen.lon.toString(),
        screen.elev.toString(),
        screen.timezone.toString(),
        encodeScreenText(screen.locName)
    ).joinToString(":")
}

private fun restoreGerhanaDetailScreen(parts: List<String>): Screen {
    if (parts.size < 9) return Screen.Gerhana
    return Screen.GerhanaDetail(
        jde = parts[1].toDoubleOrNull() ?: return Screen.Gerhana,
        isSolar = parts[2].toBooleanStrictOrNull() ?: return Screen.Gerhana,
        typology = decodeScreenText(parts[3]),
        lat = parts[4].toDoubleOrNull() ?: return Screen.Gerhana,
        lon = parts[5].toDoubleOrNull() ?: return Screen.Gerhana,
        elev = parts[6].toDoubleOrNull() ?: return Screen.Gerhana,
        timezone = parts[7].toDoubleOrNull() ?: return Screen.Gerhana,
        locName = decodeScreenText(parts[8])
    )
}

private fun saveVisibilityMapScreen(request: VisibilityMapRequest?): String {
    request ?: return "visibility_map"
    return listOf(
        "visibility_map",
        request.hijriYear.toString(),
        request.hijriMonth.toString(),
        request.ijtimaGeoJde.toString(),
        request.ijtimaLocalJd.toString(),
        request.timezone.toString(),
        request.mode.name,
        encodeScreenText(request.magribLocalTimeText),
        encodeScreenText(request.locationName),
        request.latitude.toString(),
        request.longitude.toString(),
        request.elevation.toString()
    ).joinToString("|")
}

private fun restoreVisibilityMapRequest(parts: List<String>): VisibilityMapRequest? {
    if (parts.size < 12) return null
    return VisibilityMapRequest(
        hijriYear = parts[1].toIntOrNull() ?: return null,
        hijriMonth = parts[2].toIntOrNull() ?: return null,
        ijtimaGeoJde = parts[3].toDoubleOrNull() ?: return null,
        ijtimaLocalJd = parts[4].toDoubleOrNull() ?: return null,
        timezone = parts[5].toDoubleOrNull() ?: 7.0,
        mode = runCatching {
            HilalVisibilityMapMode.valueOf(parts[6])
        }.getOrDefault(HilalVisibilityMapMode.MABIMS_BARU),
        magribLocalTimeText = decodeScreenText(parts[7]),
        locationName = decodeScreenText(parts[8]),
        latitude = parts[9].toDoubleOrNull() ?: 0.0,
        longitude = parts[10].toDoubleOrNull() ?: 0.0,
        elevation = parts[11].toDoubleOrNull() ?: 0.0
    )
}

private fun encodeScreenText(value: String): String {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.name())
}

private fun decodeScreenText(value: String): String {
    return URLDecoder.decode(value, StandardCharsets.UTF_8.name())
}
