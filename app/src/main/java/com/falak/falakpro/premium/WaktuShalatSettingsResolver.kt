package com.falak.falakpro.premium

data class WaktuShalatResolvedSettings(
    val kriteria: MesinWaktuShalat.KriteriaWaktuShalat,
    val faktorAshar: Double,
    val pembulatan: MesinWaktuShalat.ModePembulatan
)

object WaktuShalatSettingsResolver {
    fun resolve(prefs: PreferencesHelper): WaktuShalatResolvedSettings {
        val kriteria = if (prefs.pengaturanOtomatis) {
            MesinWaktuShalat.KRITERIA_LFNU
        } else if (prefs.kriteriaIndex == 0) {
            MesinWaktuShalat.KriteriaWaktuShalat(
                "Sesuaikan Sudut Manual",
                prefs.sudutManualSubuh.toDouble(),
                prefs.sudutManualIsya.toDouble(),
                1.0,
                4.5
            )
        } else {
            MesinWaktuShalat.DAFTAR_KRITERIA.getOrElse(prefs.kriteriaIndex) {
                MesinWaktuShalat.KRITERIA_LFNU
            }
        }

        val faktorAshar = if (prefs.metodeAsharSyafii) 1.0 else 2.0
        val pembulatan = when (prefs.pembulatanIndex) {
            1 -> MesinWaktuShalat.ModePembulatan.KE_ATAS
            2 -> MesinWaktuShalat.ModePembulatan.KE_BAWAH
            else -> MesinWaktuShalat.ModePembulatan.NORMAL
        }

        return WaktuShalatResolvedSettings(
            kriteria = kriteria,
            faktorAshar = faktorAshar,
            pembulatan = pembulatan
        )
    }
}
