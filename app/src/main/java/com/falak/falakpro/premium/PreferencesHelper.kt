package com.falak.falakpro.premium

import android.content.Context
import android.content.SharedPreferences
import kotlin.math.roundToInt

class PreferencesHelper(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("JadwalShalatPrefs", Context.MODE_PRIVATE)

    var ikhImsak: Int
        get() = prefs.getInt("ikhImsak", 2)
        set(value) = prefs.edit().putInt("ikhImsak", value).apply()

    var ikhSubuh: Int
        get() = prefs.getInt("ikhSubuh", 2)
        set(value) = prefs.edit().putInt("ikhSubuh", value).apply()

    var ikhTerbit: Int
        get() = prefs.getInt("ikhTerbit", 2)
        set(value) = prefs.edit().putInt("ikhTerbit", value).apply()

    var ikhDhuha: Int
        get() = prefs.getInt("ikhDhuha", 2)
        set(value) = prefs.edit().putInt("ikhDhuha", value).apply()

    var ikhDzuhur: Int
        get() = prefs.getInt("ikhDzuhur", 2)
        set(value) = prefs.edit().putInt("ikhDzuhur", value).apply()

    var ikhAshar: Int
        get() = prefs.getInt("ikhAshar", 2)
        set(value) = prefs.edit().putInt("ikhAshar", value).apply()

    var ikhMaghrib: Int
        get() = prefs.getInt("ikhMaghrib", 2)
        set(value) = prefs.edit().putInt("ikhMaghrib", value).apply()

    var ikhIsya: Int
        get() = prefs.getInt("ikhIsya", 2)
        set(value) = prefs.edit().putInt("ikhIsya", value).apply()

    var kriteriaIndex: Int
        get() = prefs.getInt("kriteriaIndex", 1) // Default to LFNU
        set(value) = prefs.edit().putInt("kriteriaIndex", value).apply()

    var kriteriaAwalBulan: String
        get() = prefs.getString("kriteriaAwalBulan", "Mabims Baru") ?: "Mabims Baru"
        set(value) = prefs.edit().putString("kriteriaAwalBulan", value).apply()

    var pengaturanOtomatis: Boolean
        get() = prefs.getBoolean("pengaturanOtomatis", false)
        set(value) = prefs.edit().putBoolean("pengaturanOtomatis", value).apply()

    var metodeAsharSyafii: Boolean
        get() = prefs.getBoolean("metodeAsharSyafii", true)
        set(value) = prefs.edit().putBoolean("metodeAsharSyafii", value).apply()

    var pembulatanIndex: Int
        get() = prefs.getInt("pembulatanIndex", 0)
        set(value) = prefs.edit().putInt("pembulatanIndex", value).apply()

    var sudutManualSubuh: Float
        get() = prefs.getFloat("sudutManualSubuh", -20.0f)
        set(value) = prefs.edit().putFloat("sudutManualSubuh", value).apply()

    var sudutManualIsya: Float
        get() = prefs.getFloat("sudutManualIsya", -18.0f)
        set(value) = prefs.edit().putFloat("sudutManualIsya", value).apply()

    var lokasiOtomatis: Boolean
        get() = prefs.getBoolean("lokasiOtomatis", true)
        set(value) = prefs.edit().putBoolean("lokasiOtomatis", value).apply()

    var manualLokasiNama: String
        get() = prefs.getString("manualLokasiNama", "Lokasi Manual") ?: "Lokasi Manual"
        set(value) = prefs.edit().putString("manualLokasiNama", value).apply()

    var manualLat: Double
        get() = prefs.getFloat("manualLat", -6.3133f).toDouble()
        set(value) = prefs.edit().putFloat("manualLat", value.toFloat()).apply()

    var manualLon: Double
        get() = prefs.getFloat("manualLon", 107.3191f).toDouble()
        set(value) = prefs.edit().putFloat("manualLon", value.toFloat()).apply()

    var manualTimezone: Double
        get() {
            val stored = prefs.getFloat("manualTimezone", Float.NaN)
            return if (stored.isNaN()) defaultTimezoneFromLongitude(manualLon) else stored.toDouble()
        }
        set(value) = prefs.edit().putFloat("manualTimezone", value.toFloat()).apply()

    var manualElev: Double
        get() = prefs.getFloat("manualElev", 38.0f).toDouble()
        set(value) = prefs.edit().putFloat("manualElev", value.toFloat()).apply()

    var ketinggianDataranTinggi: Double
        get() = prefs.getFloat("ketinggianDataranTinggi", 38.0f).toDouble()
        set(value) = prefs.edit().putFloat("ketinggianDataranTinggi", value.toFloat()).apply()

    // ─── Kiblat Settings ───

    var kiblatBeepSound: Boolean
        get() = prefs.getBoolean("kiblatBeepSound", true)
        set(value) = prefs.edit().putBoolean("kiblatBeepSound", value).apply()

    var kiblatShowSunMoon: Boolean
        get() = prefs.getBoolean("kiblatShowSunMoon", false)
        set(value) = prefs.edit().putBoolean("kiblatShowSunMoon", value).apply()

    // 0 = Default, 1 = Minimal, 2 = Neon
    var kiblatCrossfinderTheme: Int
        get() = prefs.getInt("kiblatCrossfinderTheme", 0)
        set(value) = prefs.edit().putInt("kiblatCrossfinderTheme", value).apply()

    // 0 = None, 1 = Posisi Matahari, 2 = Bayangan Matahari, 3 = Posisi Bulan
    var kiblatKalibrasiObjek: Int
        get() = prefs.getInt("kiblatKalibrasiObjek", 2)
        set(value) = prefs.edit().putInt("kiblatKalibrasiObjek", value).apply()

    var kiblatKoreksiAzimut: Boolean
        get() = prefs.getBoolean("kiblatKoreksiAzimut", true)
        set(value) = prefs.edit().putBoolean("kiblatKoreksiAzimut", value).apply()

    var kiblatMethod: Int
        get() = prefs.getInt("kiblatMethod", 2) // default Vincenty
        set(value) = prefs.edit().putInt("kiblatMethod", value).apply()

    var kiblatKoreksiNilai: Float
        get() = prefs.getFloat("kiblatKoreksiNilai", 0f)
        set(value) = prefs.edit().putFloat("kiblatKoreksiNilai", value).apply()

    // ─── App Global Settings ───

    // 0 = System, 1 = Light, 2 = Dark
    var appTheme: Int
        get() = prefs.getInt("appTheme", 0)
        set(value) = prefs.edit().putInt("appTheme", value).apply()

    // "system" = Mengikuti sistem, "id" = Indonesia, "en" = English, "ar" = Arabic
    var appLanguage: String
        get() = prefs.getString("appLanguage", "system") ?: "system"
        set(value) = prefs.edit().putString("appLanguage", value).apply()

    var is24HourFormat: Boolean
        get() = prefs.getBoolean("is24HourFormat", true)
        set(value) = prefs.edit().putBoolean("is24HourFormat", value).apply()
}

private fun defaultTimezoneFromLongitude(longitude: Double): Double = when {
    longitude >= 94.0 && longitude < 112.5 -> 7.0
    longitude >= 112.5 && longitude < 127.5 -> 8.0
    longitude >= 127.5 && longitude <= 141.5 -> 9.0
    else -> (longitude / 15.0).roundToInt().coerceIn(-12, 14).toDouble()
}
