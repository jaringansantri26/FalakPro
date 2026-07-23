package com.falak.falakpro.premium

import java.util.Locale
import java.util.Calendar
import kotlin.math.*

object MesinWaktuShalat {

    enum class ModePembulatan { KE_ATAS, NORMAL, KE_BAWAH }

    data class KriteriaWaktuShalat(
        val nama: String,
        val sudutSubuh: Double, // dalam derajat (negatif)
        val sudutIsya: Double,  // dalam derajat (negatif)
        val faktorBayanganAshar: Double = 1.0,
        val sudutDhuha: Double = 4.5
    )

    val KRITERIA_MANUAL = KriteriaWaktuShalat("Sesuaikan Sudut Manual", -20.0, -18.0, 1.0, 4.5)
    val KRITERIA_LFNU = KriteriaWaktuShalat("Lembaga Falakiyah NU, Indonesia", -20.0, -18.0, 1.0, 4.5)
    val KRITERIA_UMM_AL_QURA = KriteriaWaktuShalat("Umm al-Qura University, Makkah", -18.5, 0.0, 1.0, 4.5)
    val KRITERIA_JAKIM = KriteriaWaktuShalat("Jabatan Kemajuan Islam Malaysia (JAKIM)", -18.0, -18.0, 1.0, 4.5)
    val KRITERIA_MUIS = KriteriaWaktuShalat("Majlis Ugama Islam Singapura (MUIS)", -20.0, -18.0, 1.0, 4.5)
    val KRITERIA_EGYPT = KriteriaWaktuShalat("Egyptian General Authority", -19.5, -17.5, 1.0, 4.5)
    val KRITERIA_EGYPT_BIS = KriteriaWaktuShalat("Egyptian General Authority (Bis)", -20.0, -18.0, 1.0, 4.5)
    val KRITERIA_MWL = KriteriaWaktuShalat("Muslim World League", -18.0, -17.0, 1.0, 4.5)
    val KRITERIA_KUWAIT = KriteriaWaktuShalat("Kuwait", -18.0, -17.5, 1.0, 4.5)
    val KRITERIA_QATAR = KriteriaWaktuShalat("Qatar", -18.0, 0.0, 1.0, 4.5)
    val KRITERIA_BAHRAIN = KriteriaWaktuShalat("Bahrain", -19.5, 0.0, 1.0, 4.5)
    val KRITERIA_ISNA = KriteriaWaktuShalat("Islamic Society of North America (ISNA)", -15.0, -15.0, 1.0, 4.5)
    val KRITERIA_TURKEY = KriteriaWaktuShalat("Diyanet İşleri Başkanlığı, Turkey", -18.0, -17.0, 1.0, 4.5)
    val KRITERIA_KARACHI = KriteriaWaktuShalat("University of Islamic Sciences, Karachi", -18.0, -18.0, 1.0, 4.5)
    val KRITERIA_UK = KriteriaWaktuShalat("Muslim Prayer Times UK", -15.0, -15.0, 1.0, 4.5)
    val KRITERIA_DUBAI = KriteriaWaktuShalat("Dubai", -18.2, -18.2, 1.0, 4.5)
    val KRITERIA_THAILAND = KriteriaWaktuShalat("Central Islamic Council of Thailand", -19.0, -18.0, 1.0, 4.5)
    val KRITERIA_MOONSIGHTING = KriteriaWaktuShalat("Moon sighting Committee", -18.0, -18.0, 1.0, 4.5)
    val KRITERIA_TEHRAN = KriteriaWaktuShalat("Institute of Geophysics, University of Tehran", -17.7, -14.0, 1.0, 4.5)

    val DAFTAR_KRITERIA = listOf(
        KRITERIA_MANUAL,
        KRITERIA_LFNU,
        KRITERIA_UMM_AL_QURA,
        KRITERIA_JAKIM,
        KRITERIA_MUIS,
        KRITERIA_EGYPT,
        KRITERIA_EGYPT_BIS,
        KRITERIA_MWL,
        KRITERIA_KUWAIT,
        KRITERIA_QATAR,
        KRITERIA_BAHRAIN,
        KRITERIA_ISNA,
        KRITERIA_TURKEY,
        KRITERIA_KARACHI,
        KRITERIA_UK,
        KRITERIA_DUBAI,
        KRITERIA_THAILAND,
        KRITERIA_MOONSIGHTING,
        KRITERIA_TEHRAN
    )

    data class HasilWaktuShalat(
        val nama: String,
        val teksWaktu: String,
        val teksWaktuMurni: String,
        val jd: Double
    )

    // Helper Julian local untuk menghindari dependensi luar
    object PenolongJulian {
        fun dariKalender(tahun: Int, bulan: Int, hari: Double): Double {
            var y = tahun
            var m = bulan
            if (m <= 2) {
                y--
                m += 12
            }
            val a = y / 100
            val b = 2 - a + a / 4
            return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + hari + b - 1524.5
        }
    }

    private fun prepareSolarData(konteks: android.content.Context?) {
        AstroAssetPreloader.ensureSolarBlocking(konteks)
    }

    private fun cariTransit(jd0: Double, bujur: Double): Double {
        var m = 0.5 - bujur / 360.0
        for (i in 0..2) {
            val jd = jd0 + m
            val jde = jd + DynamicalTimeEngine.deltaT(jd) / 86400.0
            val sun = Vsop87SolarEngine.compute(jde)
            val gmst = AstroDataUtils.calculateGAST(jd)
            val lst = gmst + bujur
            var h = (lst - sun.ra).mod(360.0)
            if (h > 180) h -= 360.0
            m -= h / 360.9856
        }
        return jd0 + m
    }

    private fun cariKetinggian(
        transitJd: Double,
        ketinggianSasaran: Double,
        lintang: Double,
        bujur: Double,
        apakahTerbit: Boolean
    ): Double? {
        val jdeT = transitJd + DynamicalTimeEngine.deltaT(transitJd) / 86400.0
        val noonSun = Vsop87SolarEngine.compute(jdeT)
        
        val d = Math.toRadians(noonSun.dec)
        val l = Math.toRadians(lintang)
        val h0 = Math.toRadians(ketinggianSasaran)
        
        val cosH = (sin(h0) - sin(l) * sin(d)) / (cos(l) * cos(d))
        if (cosH > 1.0 || cosH < -1.0) return null
        
        val h0deg = Math.toDegrees(acos(cosH))
        var m = if (apakahTerbit) -h0deg / 360.0 else h0deg / 360.0
        
        for (i in 0..4) {
            val jd = transitJd + m
            val jde = jd + DynamicalTimeEngine.deltaT(jd) / 86400.0
            
            val sun = Vsop87SolarEngine.compute(jde)
            val lst = AstroDataUtils.calculateGAST(jd) + bujur
            val h = (lst - sun.ra).mod(360.0)
            
            val decR = Math.toRadians(sun.dec)
            val latR = Math.toRadians(lintang)
            val hourR = Math.toRadians(h)
            
            val sinAlt = sin(latR) * sin(decR) + cos(latR) * cos(decR) * cos(hourR)
            val dSinAlt_dH = -cos(latR) * cos(decR) * sin(hourR)
            val error = sinAlt - sin(h0)
            
            if (abs(dSinAlt_dH) < 1e-10) break
            
            val dm = error / (dSinAlt_dH * 2.0 * Math.PI * 1.0027379)
            m -= dm
        }
        return transitJd + m
    }

    private fun cariAshar(
        transitJd: Double,
        lintang: Double,
        bujur: Double,
        faktor: Double
    ): Double? {
        val jde = transitJd + DynamicalTimeEngine.deltaT(transitJd) / 86400.0
        val noonSun = Vsop87SolarEngine.compute(jde)
        val noonAlt = 90.0 - abs(lintang - noonSun.dec)
        val targetCot = faktor + 1.0 / tan(Math.toRadians(noonAlt))
        val targetAlt = Math.toDegrees(atan(1.0 / targetCot))
        
        return cariKetinggian(transitJd, targetAlt, lintang, bujur, apakahTerbit = false)
    }

    fun hitung(
        konteks: android.content.Context?,
        tahun: Int, bulan: Int, hari: Int,
        lintang: Double, bujur: Double, elevasi: Double,
        zonaWaktu: Double,
        kriteria: KriteriaWaktuShalat,
        ikhSubuh: Int = 2,
        ikhTerbit: Int = 2,
        ikhDhuha: Int = 2,
        ikhDzuhur: Int = 2,
        ikhAshar: Int = 2,
        ikhMaghrib: Int = 2,
        ikhIsya: Int = 2,
        pembulatan: ModePembulatan = ModePembulatan.NORMAL,
        gunakanElevasi: Boolean = false,
        faktorAshar: Double = 1.0,
        is24HourFormat: Boolean = true
    ): List<HasilWaktuShalat> {
        prepareSolarData(konteks)

        val jd0 = PenolongJulian.dariKalender(tahun, bulan, hari.toDouble())
        val transitJd = cariTransit(jd0, bujur)
        
        // Subuh
        val fajrJd = cariKetinggian(transitJd, kriteria.sudutSubuh, lintang, bujur, apakahTerbit = true)
        
        // Terbit (Syuruk)
        // Dip matahari & Refraksi
        val dip = if (gunakanElevasi) AstroTransform.dipCorrection(elevasi) else 0.0
        val targetAltTerbit = -0.8333 + dip
        val syurukJd = cariKetinggian(transitJd, targetAltTerbit, lintang, bujur, apakahTerbit = true)
        
        // Dhuha
        val dhuhaJd = cariKetinggian(transitJd, kriteria.sudutDhuha, lintang, bujur, apakahTerbit = true)
        
        // Ashar
        val asrJd = cariAshar(transitJd, lintang, bujur, faktorAshar)
        
        // Maghrib
        val maghribJd = cariKetinggian(transitJd, targetAltTerbit, lintang, bujur, apakahTerbit = false)
        
        // Isya
        val isUmmAlQura = kriteria.nama.contains("Umm al-Qura", ignoreCase = true)
        val ishaJd = if (isUmmAlQura) {
            // 90 menit setelah Maghrib, atau 120 menit saat Ramadhan
            if (maghribJd != null) {
                val hijri = CalendarFunctions.jdeToHijri(maghribJd)
                val menit = if (hijri.second == 9) 120.0 else 90.0
                maghribJd + (menit / 60.0) / 24.0
            } else null
        } else {
            cariKetinggian(transitJd, kriteria.sudutIsya, lintang, bujur, apakahTerbit = false)
        }
        
        // Imsak mengikuti waktu Subuh final: Subuh setelah ikhtiyat dikurangi 10 menit.
        val imsakJd = if (fajrJd != null) fajrJd + ((ikhSubuh - 10.0) / 60.0) / 24.0 else null

        val hasil = mutableListOf<HasilWaktuShalat>()
        
        fun tambahHasil(nama: String, jd: Double?, ikhtiyatMenit: Int) {
            if (jd == null) {
                hasil.add(HasilWaktuShalat(nama, "--:--", "--:--:--", 0.0))
                return
            }
            // Konversi ke waktu lokal dalam jam desimal
            val localHours = (jd - jd0) * 24.0 + zonaWaktu
            val localMinutes = localHours * 60.0
            
            // Format waktu murni (tanpa pembulatan dan tanpa ikhtiyat) dalam HH:MM:SS
            val rawSeconds = localHours * 3600.0
            var totalRawSeconds = Math.round(rawSeconds).toLong()
            if (totalRawSeconds < 0) totalRawSeconds += 24 * 3600
            val rawH = ((totalRawSeconds / 3600) % 24).toInt()
            val rawM = ((totalRawSeconds / 60) % 60).toInt()
            val rawS = (totalRawSeconds % 60).toInt()
            val formattedMurni = String.format(Locale.US, "%02d:%02d:%02d", rawH, rawM, rawS)

            val roundedMinutes = if (nama == "Terbit") {
                // Terbit menjadi batas akhir Subuh, maka ikhtiyat mengurangi waktu.
                floor(localMinutes - ikhtiyatMenit)
            } else {
                // Untuk selain terbit, tambahkan ikhtiyat lalu lakukan pembulatan pada level MENIT
                val finalMinutes = localMinutes + ikhtiyatMenit
                when (pembulatan) {
                    ModePembulatan.KE_ATAS -> ceil(finalMinutes)
                    ModePembulatan.KE_BAWAH -> floor(finalMinutes)
                    ModePembulatan.NORMAL -> round(finalMinutes)
                }
            }
            
            var totalRoundedMinutes = roundedMinutes.toLong()
            if (totalRoundedMinutes < 0) totalRoundedMinutes += 24 * 60
            
            val hour24 = ((totalRoundedMinutes / 60) % 24).toInt()
            val min = (totalRoundedMinutes % 60).toInt()
            
            val formatted = if (is24HourFormat) {
                String.format(Locale.US, "%02d:%02d", hour24, min)
            } else {
                val amPm = if (hour24 >= 12) "PM" else "AM"
                val hour12 = if (hour24 % 12 == 0) 12 else hour24 % 12
                String.format(Locale.US, "%02d:%02d %s", hour12, min, amPm)
            }
            hasil.add(HasilWaktuShalat(nama, formatted, formattedMurni, jd))
        }

        tambahHasil("Imsak", imsakJd, 0)
        tambahHasil("Subuh", fajrJd, ikhSubuh)
        tambahHasil("Terbit", syurukJd, ikhTerbit)
        tambahHasil("Dhuha", dhuhaJd, ikhDhuha)
        tambahHasil("Dzuhur", transitJd, ikhDzuhur)
        tambahHasil("Ashar", asrJd, ikhAshar)
        tambahHasil("Maghrib", maghribJd, ikhMaghrib)
        tambahHasil("Isya", ishaJd, ikhIsya)

        return hasil
    }
}
