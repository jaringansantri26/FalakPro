package com.falak.falakpro.premium

import kotlin.math.*

/**
 * PrayerTimeFunctions.kt
 * Hitung waktu shalat berdasarkan posisi Matahari presisi tinggi.
 * Mendukung berbagai kriteria/konvensi dunia Islam.
 * Referensi: Meeus Ch. 15 + ISNA/Kemenag/MWL/UISK/Karachi formulasi
 */
object PrayerTimeFunctions {

    // ─────────────────────────────────────────────────────────────────────────
    // Kriteria Waktu Shalat
    // ─────────────────────────────────────────────────────────────────────────

    enum class AsrMazhab {
        SHAFI,    // Bayangan = 1× tinggi objek (default)
        HANAFI    // Bayangan = 2× tinggi objek
    }

    data class PrayerCriteria(
        val name: String,
        val fajrAngle: Double,      // Sudut Matahari di bawah ufuk untuk Fajr (derajat)
        val ishaAngle: Double,      // Sudut untuk Isha (derajat), 0 jika pakai menit
        val ishaMinutes: Double,    // Menit setelah Maghrib untuk Isha (jika ishaAngle=0)
        val fajrMinutes: Double,    // Menit sebelum Subuh (jika ishaAngle=0)
        val mazhab: AsrMazhab = AsrMazhab.SHAFI
    )

    // Kriteria populer
    object Criteria {
        val KEMENAG       = PrayerCriteria("Kemenag RI",          20.0, 18.0, 0.0, 0.0)
        val MWLISL_KARACHI= PrayerCriteria("MWL / Karachi",       18.0, 18.0, 0.0, 0.0)
        val ISNA          = PrayerCriteria("ISNA",                 15.0, 15.0, 0.0, 0.0)
        val EGYPT         = PrayerCriteria("Egypt OAEE",           19.5, 17.5, 0.0, 0.0)
        val UISK          = PrayerCriteria("UISK Umm Al-Qura",    18.5,  0.0,90.0, 0.0)
        val DIYANET       = PrayerCriteria("Diyanet Turkey",       18.0, 17.0, 0.0, 0.0)
        val SINGAPORE     = PrayerCriteria("MUIS Singapore",       20.0, 18.0, 0.0, 0.0)
        val MUHAMMADIYAH  = PrayerCriteria("Muhammadiyah",         20.0, 18.0, 0.0, 0.0)
        val HANAFI_RUSSIA = PrayerCriteria("Russia Hanafi",        16.0, 15.0, 0.0, 0.0, AsrMazhab.HANAFI)
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hasil waktu shalat
    // ─────────────────────────────────────────────────────────────────────────

    data class PrayerTimes(
        val fajr: Double,        // Subuh  (jam desimal, waktu lokal)
        val sunrise: Double,     // Terbit  (jam)
        val dhuhr: Double,       // Dzuhur  (jam)
        val asr: Double,         // Ashar   (jam)
        val maghrib: Double,     // Maghrib (jam) = Terbenam
        val isha: Double,        // Isya    (jam)
        val midnight: Double,    // Tengah malam (jam)
        val lastThird: Double,   // Sepertiga malam terakhir (jam)
        val imsak: Double        // Imsak = Fajr - 10 menit (default)
    )

    // ─────────────────────────────────────────────────────────────────────────
    // Kalkulasi Utama
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Hitung waktu shalat untuk satu hari.
     *
     * @param jde       Julian Day Efemeris (tengah hari UT)
     * @param lat       Lintang (derajat, + = Utara)
     * @param lon       Bujur (derajat, + = Timur)
     * @param heightM   Tinggi tempat (meter, untuk dip)
     * @param timezone  Zona waktu (jam)
     * @param criteria  Kriteria/konvensi waktu shalat
     * @param sunRa     RA Matahari (derajat) — dari Vsop87SolarEngine
     * @param sunDec    Deklinasi Matahari (derajat)
     * @param eot       Equation of Time (jam)
     */
    fun calculate(
        jde: Double, lat: Double, lon: Double,
        heightM: Double, timezone: Double,
        criteria: PrayerCriteria,
        sunRa: Double, sunDec: Double, eot: Double
    ): PrayerTimes {
        val latR = Math.toRadians(lat)
        val decR = Math.toRadians(sunDec)
        val dip  = AstroTransform.dipCorrection(heightM)

        // Transit (Dzuhur) = 12 + eot + bujur koreksi
        val dhuhrUT = 12.0 - eot - lon / 15.0
        val dhuhrLocal = dhuhrUT + timezone

        // Fungsi hitung hour angle untuk altitude target h (derajat)
        fun hourAngleFor(h: Double): Double? {
            val hR = Math.toRadians(h)
            val cosHA = (sin(hR) - sin(latR) * sin(decR)) / (cos(latR) * cos(decR))
            if (cosHA < -1.0 || cosHA > 1.0) return null
            return Math.toDegrees(acos(cosHA)) / 15.0  // dalam jam
        }

        // Sunrise & Sunset — altitude = -0.8333° + dip
        val hSunset = -0.8333 + dip
        val halfDaySun = hourAngleFor(hSunset)

        val sunriseLocal  = halfDaySun?.let { dhuhrLocal - it } ?: Double.NaN
        val maghribLocal  = halfDaySun?.let { dhuhrLocal + it } ?: Double.NaN

        // Fajr — Matahari di bawah ufuk sebesar fajrAngle
        val hFajr = -(criteria.fajrAngle) + dip
        val halfDayFajr = hourAngleFor(hFajr)
        val fajrLocal = halfDayFajr?.let { dhuhrLocal - it } ?: (sunriseLocal - criteria.fajrMinutes / 60.0)

        // Isha
        val ishaLocal = if (criteria.ishaAngle > 0) {
            val hIsha = -(criteria.ishaAngle) + dip
            val halfDayIsha = hourAngleFor(hIsha)
            halfDayIsha?.let { dhuhrLocal + it } ?: (maghribLocal + criteria.ishaMinutes / 60.0)
        } else {
            maghribLocal + criteria.ishaMinutes / 60.0
        }

        // Ashar — shadow factor berdasarkan mazhab
        val shadowFactor = if (criteria.mazhab == AsrMazhab.HANAFI) 2.0 else 1.0
        val hAsr = Math.toDegrees(atan(1.0 / (shadowFactor + tan(abs(latR - decR)))))
        val halfDayAsr = hourAngleFor(hAsr)
        val asrLocal = halfDayAsr?.let { dhuhrLocal + it } ?: Double.NaN

        // Tengah malam & sepertiga malam
        val nightDuration = fajrLocal + 24.0 - maghribLocal
        val midnight   = maghribLocal + nightDuration / 2.0
        val lastThird  = maghribLocal + 2.0 * nightDuration / 3.0

        // Imsak = 10 menit sebelum Fajr
        val imsak = fajrLocal - 10.0 / 60.0

        return PrayerTimes(
            fajr      = fajrLocal,
            sunrise   = sunriseLocal,
            dhuhr     = dhuhrLocal,
            asr       = asrLocal,
            maghrib   = maghribLocal,
            isha      = ishaLocal,
            midnight  = midnight,
            lastThird = lastThird,
            imsak     = imsak
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Utilitas Format
    // ─────────────────────────────────────────────────────────────────────────

    /** Konversi jam desimal ke string HH:MM */
    fun formatHHMM(hours: Double): String {
        if (hours.isNaN() || hours.isInfinite()) return "--:--"
        val h = hours.mod(24.0)
        val hh = h.toInt()
        val mm = ((h - hh) * 60).roundToInt()
        return "%02d:%02d".format(hh, mm.coerceIn(0, 59))
    }

    /** Konversi jam desimal ke string HH:MM:SS */
    fun formatHHMMSS(hours: Double): String {
        if (hours.isNaN() || hours.isInfinite()) return "--:--:--"
        val h = hours.mod(24.0)
        val hh = h.toInt()
        val mm = ((h - hh) * 60).toInt()
        val ss = (((h - hh) * 60 - mm) * 60).roundToInt()
        return "%02d:%02d:%02d".format(hh, mm, ss.coerceIn(0, 59))
    }

    private fun Double.roundToInt() = Math.round(this).toInt()
}
