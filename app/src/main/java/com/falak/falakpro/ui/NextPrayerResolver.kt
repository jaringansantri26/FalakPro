package com.falak.falakpro.ui

import com.falak.falakpro.premium.MesinWaktuShalat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

internal data class NextPrayerInfo(
    val name: String,
    val timeText: String,
    val timeMillis: Long
)

private val nextPrayerOrder = listOf(
    "Imsak",
    "Subuh",
    "Terbit",
    "Dhuha",
    "Dzuhur",
    "Ashar",
    "Maghrib",
    "Isya"
)

internal fun findNextPrayer(
    schedule: List<MesinWaktuShalat.HasilWaktuShalat>,
    prayerDate: Calendar,
    currentTimeMillis: Long,
    uppercaseName: Boolean = false
): NextPrayerInfo? {
    for (name in nextPrayerOrder) {
        val item = schedule.firstOrNull { it.nama == name } ?: continue
        val (hour, minute) = parsePrayerTime(item.teksWaktu) ?: continue
        val prayerCal = (prayerDate.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (prayerCal.timeInMillis > currentTimeMillis) {
            return NextPrayerInfo(
                name = if (uppercaseName) name.uppercase(Locale.US) else name,
                timeText = item.teksWaktu,
                timeMillis = prayerCal.timeInMillis
            )
        }
    }
    return null
}

private fun parsePrayerTime(timeText: String): Pair<Int, Int>? {
    val isPm = timeText.contains("PM", ignoreCase = true)
    val isAm = timeText.contains("AM", ignoreCase = true)
    val cleanText = timeText.replace(" AM", "").replace(" PM", "").replace(" am", "").replace(" pm", "").trim()
    val parts = cleanText.split(":")
    if (parts.size < 2) return null
    var hour = parts[0].toIntOrNull() ?: return null
    val minute = parts[1].toIntOrNull() ?: return null
    
    if (isPm && hour < 12) hour += 12
    if (isAm && hour == 12) hour = 0
    
    return hour to minute
}

internal fun timezoneFromLongitude(longitude: Double): Double = when {
    longitude >= 94.0 && longitude < 112.5 -> 7.0
    longitude >= 112.5 && longitude < 127.5 -> 8.0
    longitude >= 127.5 && longitude <= 141.5 -> 9.0
    else -> (longitude / 15.0).roundToInt().coerceIn(-12, 14).toDouble()
}

internal fun prayerTimezoneLabel(timezone: Double, longitude: Double): String {
    val zone = timezone.roundToInt()
    return when {
        zone == 7 && longitude >= 94.0 && longitude < 112.5 -> "WIB"
        zone == 8 && longitude >= 112.5 && longitude < 127.5 -> "WITA"
        zone == 9 && longitude >= 127.5 && longitude <= 141.5 -> "WIT"
        else -> "LT"
    }
}

internal fun formatTimezoneValue(timezone: Double): String {
    val rounded = timezone.roundToInt()
    return if (abs(timezone - rounded) < 0.001) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.2f", timezone)
    }
}

