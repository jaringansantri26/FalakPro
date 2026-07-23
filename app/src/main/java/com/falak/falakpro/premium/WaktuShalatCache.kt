package com.falak.falakpro.premium

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class WaktuShalatCacheKey(
    val year: Int,
    val month: Int,
    val day: Int,
    val latKey: Int,
    val lonKey: Int,
    val elevKey: Int,
    val timezoneKey: Int,
    val kriteriaName: String,
    val subuh: Int,
    val terbit: Int,
    val dhuha: Int,
    val dzuhur: Int,
    val ashar: Int,
    val maghrib: Int,
    val isya: Int,
    val pembulatan: MesinWaktuShalat.ModePembulatan,
    val faktorAsharKey: Int,
    val is24HourFormat: Boolean
)

object WaktuShalatCache {
    private val cache = ConcurrentHashMap<WaktuShalatCacheKey, List<MesinWaktuShalat.HasilWaktuShalat>>()

    fun key(
        year: Int,
        month: Int,
        day: Int,
        lat: Double,
        lon: Double,
        elev: Double,
        timezone: Double,
        kriteria: MesinWaktuShalat.KriteriaWaktuShalat,
        subuh: Int,
        terbit: Int,
        dhuha: Int,
        dzuhur: Int,
        ashar: Int,
        maghrib: Int,
        isya: Int,
        pembulatan: MesinWaktuShalat.ModePembulatan,
        faktorAshar: Double,
        is24HourFormat: Boolean
    ): WaktuShalatCacheKey = WaktuShalatCacheKey(
        year = year,
        month = month,
        day = day,
        latKey = (lat * 100000.0).toInt(),
        lonKey = (lon * 100000.0).toInt(),
        elevKey = (elev * 10.0).toInt(),
        timezoneKey = (timezone * 100.0).toInt(),
        kriteriaName = kriteria.nama,
        subuh = subuh,
        terbit = terbit,
        dhuha = dhuha,
        dzuhur = dzuhur,
        ashar = ashar,
        maghrib = maghrib,
        isya = isya,
        pembulatan = pembulatan,
        faktorAsharKey = (faktorAshar * 100.0).toInt(),
        is24HourFormat = is24HourFormat
    )

    fun peek(key: WaktuShalatCacheKey): List<MesinWaktuShalat.HasilWaktuShalat>? = cache[key]

    suspend fun getOrCompute(
        context: Context,
        key: WaktuShalatCacheKey,
        compute: () -> List<MesinWaktuShalat.HasilWaktuShalat>
    ): List<MesinWaktuShalat.HasilWaktuShalat> {
        cache[key]?.let { return it }
        return withContext(Dispatchers.Default) {
            cache[key] ?: run {
                AstroAssetPreloader.ensureSolarBlocking(context.applicationContext)
                compute().also { cache[key] = it }
            }
        }
    }
}

