package com.falak.falakpro.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.MesinWaktuShalat
import com.falak.falakpro.premium.PreferencesHelper
import java.util.Locale

object PencetakJadwalShalat {

    private val NAMA_BULAN_MASEHI = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    private fun formatDms(valDecimal: Double, isLat: Boolean): String {
        val absolute = kotlin.math.abs(valDecimal)
        val degrees = kotlin.math.floor(absolute).toInt()
        val minutesDecimal = (absolute - degrees) * 60.0
        val minutes = kotlin.math.floor(minutesDecimal).toInt()
        val seconds = kotlin.math.round((minutesDecimal - minutes) * 60.0).toInt()
        
        val direction = if (isLat) {
            if (valDecimal < 0) "LS" else "LU"
        } else {
            if (valDecimal < 0) "BB" else "BT"
        }
        return String.format(Locale.US, "%02d° %02d' %02d\" %s", degrees, minutes, seconds, direction)
    }

    private fun getLogoBase64(context: Context): String {
        return try {
            val bitmap = android.graphics.BitmapFactory.decodeResource(context.resources, com.falak.falakpro.R.drawable.logo_nu)
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            bitmap.recycle()
            "data:image/png;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            ""
        }
    }

    fun cetakJadwalBulanan(
        context: Context,
        tahun: Int,
        bulan: Int, // 1-indexed (Jan = 1, Des = 12)
        lintang: Double,
        bujur: Double,
        elevasi: Double,
        zonaWaktu: Double,
        namaLokasi: String,
        prefs: PreferencesHelper
    ) {
        val webView = WebView(context)
        val sb = StringBuilder()
        val logoBase64 = getLogoBase64(context)
        
        // Resolve kriteria
        val kriteria = if (prefs.pengaturanOtomatis) {
            MesinWaktuShalat.DAFTAR_KRITERIA[1] // LF-NU
        } else if (prefs.kriteriaIndex == 0) {
            MesinWaktuShalat.KriteriaWaktuShalat(
                "Sesuaikan Sudut Manual",
                prefs.sudutManualSubuh.toDouble(),
                prefs.sudutManualIsya.toDouble(),
                1.0,
                4.5
            )
        } else {
            MesinWaktuShalat.DAFTAR_KRITERIA.getOrElse(prefs.kriteriaIndex) { MesinWaktuShalat.DAFTAR_KRITERIA[1] }
        }

        val asharFactor = if (prefs.metodeAsharSyafii) 1.0 else 2.0
        val modePembulatan = when (prefs.pembulatanIndex) {
            1 -> MesinWaktuShalat.ModePembulatan.KE_ATAS
            2 -> MesinWaktuShalat.ModePembulatan.KE_BAWAH
            else -> MesinWaktuShalat.ModePembulatan.NORMAL
        }

        // Get Hijri month name range
        val cal = java.util.Calendar.getInstance()
        cal.set(tahun, bulan - 1, 1)
        val jumlahHari = cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
                val jdStart = CalendarFunctions.gregorianToJde(tahun, bulan, 1.0)
        val hijriStart = CalendarFunctions.getCorrectedHijri(jdStart, lintang, bujur, elevasi, zonaWaktu)
        
        val jdEnd = CalendarFunctions.gregorianToJde(tahun, bulan, jumlahHari.toDouble())
        val hijriEnd = CalendarFunctions.getCorrectedHijri(jdEnd, lintang, bujur, elevasi, zonaWaktu)
        
        val labelHijriRange = if (hijriStart.second == hijriEnd.second) {
            "${CalendarFunctions.HIJRI_MONTH_NAMES[hijriStart.second - 1].uppercase()} ${hijriStart.first} H"
        } else {
            "${CalendarFunctions.HIJRI_MONTH_NAMES[hijriStart.second - 1].uppercase()} ${hijriStart.first} H / ${CalendarFunctions.HIJRI_MONTH_NAMES[hijriEnd.second - 1].uppercase()} ${hijriEnd.first} H"
        }
        
        sb.append("""
            <html>
            <head>
                <style>
                    @page {
                        size: A4 portrait;
                        margin: 8mm 10mm;
                    }
                    body {
                        font-family: 'Outfit', 'Inter', sans-serif;
                        padding: 0;
                        color: #333;
                        margin: 0;
                    }
                    .header-card {
                        background-color: #004D32;
                        color: white;
                        padding: 12px;
                        border-radius: 12px;
                        text-align: center;
                        margin-bottom: 10px;
                    }
                    .logo-img {
                        height: 40px;
                        width: auto;
                        filter: brightness(0) invert(1);
                        margin-bottom: 4px;
                    }
                    .org-name {
                        font-size: 10px;
                        font-weight: 700;
                        letter-spacing: 1.5px;
                        margin-bottom: 2px;
                        text-transform: uppercase;
                    }
                    .title {
                        font-size: 18px;
                        font-weight: 900;
                        letter-spacing: 0.5px;
                        margin-bottom: 2px;
                        text-transform: uppercase;
                    }
                    .subtitle {
                        font-size: 11px;
                        font-weight: 600;
                        letter-spacing: 0.5px;
                        margin-bottom: 6px;
                        text-transform: uppercase;
                    }
                    .badge {
                        display: inline-block;
                        background-color: #007D53;
                        color: white;
                        font-size: 9px;
                        font-weight: 700;
                        padding: 3px 12px;
                        border-radius: 20px;
                        border: 1px solid #009664;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 6px;
                    }
                    .coords {
                        font-size: 8px;
                        color: #C8E6C9;
                        letter-spacing: 0.5px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 10px;
                    }
                    th {
                        background-color: #004D32;
                        color: white;
                        font-size: 9px;
                        font-weight: bold;
                        padding: 5px 3px;
                        border: 1px solid #E0E0E0;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    th.highlight {
                        color: #FFEB3B !important;
                    }
                    td {
                        padding: 4.5px 3px;
                        font-size: 9px;
                        text-align: center;
                        border: 1px solid #E0E0E0;
                        color: #333333;
                    }
                    tr:nth-child(even) {
                        background-color: #F4F9F6;
                    }
                    td.time-val {
                        font-weight: bold;
                        color: #004D32;
                    }
                    .note-card {
                        background-color: #F0F7F4;
                        border: 1px solid #C2E0D4;
                        border-radius: 8px;
                        padding: 8px 12px;
                        margin-bottom: 6px;
                    }
                    .note-list {
                        margin: 0;
                        padding-left: 14px;
                        font-size: 8.5px;
                        color: #004D32;
                        line-height: 1.4;
                    }
                    .footer-text {
                        text-align: right;
                        font-size: 8px;
                        color: #888888;
                        margin-top: 2px;
                    }
                    @media print {
                        body {
                            padding: 0;
                            margin: 0;
                        }
                        .header-card {
                            border-radius: 12px;
                            -webkit-print-color-adjust: exact;
                            print-color-adjust: exact;
                        }
                        th {
                            -webkit-print-color-adjust: exact;
                            print-color-adjust: exact;
                        }
                        tr:nth-child(even) {
                            background-color: #F4F9F6 !important;
                            -webkit-print-color-adjust: exact;
                            print-color-adjust: exact;
                        }
                        .note-card {
                            -webkit-print-color-adjust: exact;
                            print-color-adjust: exact;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="header-card">
                    <img class="logo-img" src="$logoBase64" alt="logo NU" />
                    <div class="org-name">Lembaga Falakiyah PWNU Jawa Barat</div>
                    <div class="title">Jadwal Waktu Shalat</div>
                    <div class="subtitle">${NAMA_BULAN_MASEHI[bulan - 1].uppercase()} $tahun M / $labelHijriRange</div>
                    <div class="badge">Wilayah $namaLokasi</div>
                    <div class="coords">Koordinat ${formatDms(lintang, true)}, ${formatDms(bujur, false)} | Ketinggian ${elevasi.toInt()} mdpl</div>
                </div>
                <table>
                    <tr>
                        <th style="width: 11%">Masehi</th>
                        <th style="width: 7%">Hari</th>
                        <th style="width: 14%">Hijriyah</th>
                        <th class="highlight" style="width: 8.5%">Imsak</th>
                        <th style="width: 8.5%">Subuh</th>
                        <th style="width: 8.5%">Terbit</th>
                        <th style="width: 8.5%">Dhuha</th>
                        <th style="width: 8.5%">Dzuhur</th>
                        <th style="width: 8.5%">Ashar</th>
                        <th class="highlight" style="width: 8.5%">Maghrib</th>
                        <th style="width: 8.5%">Isya</th>
                    </tr>
        """)

        for (hari in 1..jumlahHari) {
            val jd = CalendarFunctions.gregorianToJde(tahun, bulan, hari.toDouble())
            val hijri = CalendarFunctions.getCorrectedHijri(jd, lintang, bujur, elevasi, zonaWaktu)
            val nomorHariSeminggu = CalendarFunctions.dayOfWeek(jd)
            val namaHari = CalendarFunctions.DAYS_ARABIC[nomorHariSeminggu]

            val hasil = MesinWaktuShalat.hitung(
                konteks = context,
                tahun = tahun,
                bulan = bulan,
                hari = hari,
                lintang = lintang,
                bujur = bujur,
                elevasi = elevasi,
                zonaWaktu = zonaWaktu,
                kriteria = kriteria,
                ikhImsak = prefs.ikhImsak,
                ikhSubuh = prefs.ikhSubuh,
                ikhTerbit = prefs.ikhTerbit,
                ikhDhuha = prefs.ikhDhuha,
                ikhDzuhur = prefs.ikhDzuhur,
                ikhAshar = prefs.ikhAshar,
                ikhMaghrib = prefs.ikhMaghrib,
                ikhIsya = prefs.ikhIsya,
                pembulatan = modePembulatan,
                gunakanElevasi = true,
                faktorAshar = asharFactor,
                is24HourFormat = prefs.is24HourFormat
            )

            sb.append("<tr>")
            sb.append("<td>$hari ${NAMA_BULAN_MASEHI[bulan - 1].uppercase()}</td>")
            sb.append("<td>${namaHari.uppercase()}</td>")
            sb.append("<td>${hijri.third} ${CalendarFunctions.HIJRI_MONTH_NAMES[hijri.second - 1].uppercase()}</td>")
            
            hasil.forEach { h ->
                sb.append("<td class='time-val'>${h.teksWaktu}</td>")
            }
            sb.append("</tr>")
        }

        sb.append("""
                </table>
                <div class="note-card">
                    <ul class="note-list">
                        <li>Jadwal ini disusun oleh Lembaga Falakiyah dengan Kriteria: ${kriteria.nama}</li>
                        <li>Penggunaan jadwal ini memerlukan penyelarasan penanda waktu aplikasi/gadget dengan jam atom BMKG (http://jam.bmkg.go.id).</li>
                        <li>Penetapan awal bulan Hijriyah menunggu Ikhbar Pengurus Besar Nahdlatul Ulama / Pengumuman Pemerintah.</li>
                    </ul>
                </div>
                <div class="footer-text">
                    &copy; $tahun FalakPro by LF PWNU JABAR
                </div>
            </body>
            </html>
        """)

        val htmlContent = sb.toString()
        val jobName = "JadwalShalat_${NAMA_BULAN_MASEHI[bulan - 1]}_$tahun"
        
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }

    fun cetakImsakiyahBulanan(
        context: Context,
        tahunHijriah: Int,
        bulanHijriah: Int, // 1-indexed (1 = Muharram, 12 = Dzulhijjah)
        lintang: Double,
        bujur: Double,
        elevasi: Double,
        zonaWaktu: Double,
        namaLokasi: String,
        prefs: PreferencesHelper
    ) {
        val webView = WebView(context)
        val sb = StringBuilder()
        val logoBase64 = getLogoBase64(context)
        val namaBulanHijriah = CalendarFunctions.HIJRI_MONTH_NAMES[bulanHijriah - 1]

        // Resolve kriteria
        val kriteria = if (prefs.pengaturanOtomatis) {
            MesinWaktuShalat.DAFTAR_KRITERIA[1] // LF-NU
        } else if (prefs.kriteriaIndex == 0) {
            MesinWaktuShalat.KriteriaWaktuShalat(
                "Sesuaikan Sudut Manual",
                prefs.sudutManualSubuh.toDouble(),
                prefs.sudutManualIsya.toDouble(),
                1.0,
                4.5
            )
        } else {
            MesinWaktuShalat.DAFTAR_KRITERIA.getOrElse(prefs.kriteriaIndex) { MesinWaktuShalat.DAFTAR_KRITERIA[1] }
        }

        val asharFactor = if (prefs.metodeAsharSyafii) 1.0 else 2.0
        val modePembulatan = when (prefs.pembulatanIndex) {
            1 -> MesinWaktuShalat.ModePembulatan.KE_ATAS
            2 -> MesinWaktuShalat.ModePembulatan.KE_BAWAH
            else -> MesinWaktuShalat.ModePembulatan.NORMAL
        }
        val kriteriaAwalBulan = prefs.kriteriaAwalBulan

        // Inisialisasi engine VSOP87 & ELP-MPP02 — sama persis seperti HisabAwalBulanScreen
        // Wajib dilakukan sebelum getStartJdeOfHijriMonth agar HilalEngine bekerja presisi penuh
        try {
            context.assets.open("earth_vsop87d.bin").use {
                com.falak.falakpro.premium.Vsop87SolarEngine.initialize(it)
            }
            context.assets.open("mpp02_core.bin").use {
                com.falak.falakpro.premium.ElpDataProvider.initialize(it)
            }
            // Invalidate cache lp (low-precision) agar tidak pakai hasil lama yang salah
            com.falak.falakpro.premium.CalendarFunctions.clearStartJdeCache()
        } catch (_: Exception) { }

        val jdStart = CalendarFunctions.getStartJdeOfHijriMonth(tahunHijriah, bulanHijriah, lintang, bujur, elevasi, zonaWaktu, kriteriaAwalBulan)
        val nextMonthH = if (bulanHijriah == 12) 1 else bulanHijriah + 1
        val nextYearH = if (bulanHijriah == 12) tahunHijriah + 1 else tahunHijriah
        val nextStartJde = CalendarFunctions.getStartJdeOfHijriMonth(nextYearH, nextMonthH, lintang, bujur, elevasi, zonaWaktu, kriteriaAwalBulan)
        val jdEnd = nextStartJde - 1.0
        
        android.util.Log.d("FALAK_DEBUG", "Cetak Imsakiyah: Tahun=$tahunHijriah, Bulan=$bulanHijriah, Kriteria=$kriteriaAwalBulan")
        val testGreg = CalendarFunctions.jdeToGregorian(jdStart)
        android.util.Log.d("FALAK_DEBUG", "jdStart=$jdStart -> ${testGreg.first}-${testGreg.second}-${testGreg.third}")

        // JDE yang dikembalikan adalah UTC. Untuk mendapatkan tanggal Masehi LOKAL,
        // kita harus menambahkan zonaWaktu / 24.0 sebelum diubah ke Gregorian.
        val gregStart = CalendarFunctions.jdeToGregorian(jdStart + zonaWaktu / 24.0)
        val startMonthName = NAMA_BULAN_MASEHI[gregStart.second - 1].uppercase()
        val startYear = gregStart.first
        
        val gregEnd = CalendarFunctions.jdeToGregorian(jdEnd + zonaWaktu / 24.0)
        val endMonthName = NAMA_BULAN_MASEHI[gregEnd.second - 1].uppercase()
        val endYear = gregEnd.first

        val masehiLabel = if (gregStart.second == gregEnd.second) {
            "$startMonthName $startYear M"
        } else {
            if (startYear == endYear) {
                "$startMonthName - $endMonthName $startYear M"
            } else {
                "$startMonthName $startYear M / $endMonthName $endYear M"
            }
        }

        sb.append("""
            <html>
            <head>
                <style>
                    @page {
                        size: A4 portrait;
                        margin: 8mm 10mm;
                    }
                    body {
                        font-family: 'Outfit', 'Inter', sans-serif;
                        padding: 0;
                        color: #333;
                        margin: 0;
                    }
                    .header-card {
                        background-color: #004D32;
                        color: white;
                        padding: 12px;
                        border-radius: 12px;
                        text-align: center;
                        margin-bottom: 10px;
                    }
                    .logo-img {
                        height: 40px;
                        width: auto;
                        filter: brightness(0) invert(1);
                        margin-bottom: 4px;
                    }
                    .org-name {
                        font-size: 10px;
                        font-weight: 700;
                        letter-spacing: 1.5px;
                        margin-bottom: 2px;
                        text-transform: uppercase;
                    }
                    .title {
                        font-size: 18px;
                        font-weight: 900;
                        letter-spacing: 0.5px;
                        margin-bottom: 2px;
                        text-transform: uppercase;
                    }
                    .subtitle {
                        font-size: 11px;
                        font-weight: 600;
                        letter-spacing: 0.5px;
                        margin-bottom: 6px;
                        text-transform: uppercase;
                    }
                    .badge {
                        display: inline-block;
                        background-color: #007D53;
                        color: white;
                        font-size: 9px;
                        font-weight: 700;
                        padding: 3px 12px;
                        border-radius: 20px;
                        border: 1px solid #009664;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                        margin-bottom: 6px;
                    }
                    .coords {
                        font-size: 8px;
                        color: #C8E6C9;
                        letter-spacing: 0.5px;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin-bottom: 10px;
                    }
                    th {
                        background-color: #004D32;
                        color: white;
                        font-size: 9px;
                        font-weight: bold;
                        padding: 5px 3px;
                        border: 1px solid #E0E0E0;
                        text-transform: uppercase;
                        letter-spacing: 0.5px;
                    }
                    th.highlight {
                        color: #FFEB3B !important;
                    }
                    td {
                        padding: 4.5px 3px;
                        font-size: 9px;
                        text-align: center;
                        border: 1px solid #E0E0E0;
                        color: #333333;
                    }
                    tr:nth-child(even) {
                        background-color: #F4F9F6;
                    }
                    td.time-val {
                        font-weight: bold;
                        color: #004D32;
                    }
                    .note-card {
                        background-color: #F0F7F4;
                        border: 1px solid #C2E0D4;
                        border-radius: 8px;
                        padding: 8px 12px;
                        margin-bottom: 6px;
                    }
                    .note-list {
                        margin: 0;
                        padding-left: 14px;
                        font-size: 8.5px;
                        color: #004D32;
                        line-height: 1.4;
                    }
                    .footer-text {
                        text-align: right;
                        font-size: 8px;
                        color: #888888;
                        margin-top: 2px;
                    }
                    @media print {
                        body {
                            padding: 0;
                            margin: 0;
                        }
                        .header-card {
                            border-radius: 12px;
                            -webkit-print-color-adjust: exact;
                            print-color-adjust: exact;
                        }
                        th {
                            -webkit-print-color-adjust: exact;
                            print-color-adjust: exact;
                        }
                        tr:nth-child(even) {
                            background-color: #F4F9F6 !important;
                            -webkit-print-color-adjust: exact;
                            print-color-adjust: exact;
                        }
                        .note-card {
                            -webkit-print-color-adjust: exact;
                            print-color-adjust: exact;
                        }
                    }
                </style>
            </head>
            <body>
                <div class="header-card">
                    <img class="logo-img" src="$logoBase64" alt="logo NU" />
                    <div class="org-name">Lembaga Falakiyah PWNU Jawa Barat</div>
                    <div class="title">Jadwal Imsakiyah</div>
                    <div class="subtitle">${namaBulanHijriah.uppercase()} $tahunHijriah H / $masehiLabel</div>
                    <div class="badge">Wilayah $namaLokasi</div>
                    <div class="coords">Koordinat ${formatDms(lintang, true)}, ${formatDms(bujur, false)} | Ketinggian ${elevasi.toInt()} mdpl</div>
                </div>
                <table>
                    <tr>
                        <th style="width: 6%">Hijriyah</th>
                        <th style="width: 7%">Hari</th>
                        <th style="width: 13%">Masehi</th>
                        <th class="highlight" style="width: 9.25%">Imsak</th>
                        <th style="width: 9.25%">Subuh</th>
                        <th style="width: 9.25%">Terbit</th>
                        <th style="width: 9.25%">Dhuha</th>
                        <th style="width: 9.25%">Dzuhur</th>
                        <th style="width: 9.25%">Ashar</th>
                        <th class="highlight" style="width: 9.25%">Maghrib</th>
                        <th style="width: 9.25%">Isya</th>
                    </tr>
        """)

        // Hitung jumlah hari: selisih antara awal bulan berikutnya - awal bulan ini
        // Sudah memperhitungkan kriteria hilal via getStartJdeOfHijriMonth di atas
        val jumlahHariHijriah = (nextStartJde - jdStart).toInt().coerceIn(29, 30)

        // Loop Hijriah 1 sampai jumlah hari
        for (d in 1..jumlahHariHijriah) {
            val jd = jdStart + (d - 1)

            val greg = CalendarFunctions.jdeToGregorian(jd + zonaWaktu / 24.0)
            val gregYear = greg.first
            val gMonth = greg.second
            val gDay = greg.third.toInt()

            val nomorHariSeminggu = CalendarFunctions.dayOfWeek(jd + zonaWaktu / 24.0)
            val namaHari = CalendarFunctions.DAYS_ARABIC[nomorHariSeminggu]

            val hasil = MesinWaktuShalat.hitung(
                konteks = context,
                tahun = gregYear,
                bulan = gMonth,
                hari = gDay,
                lintang = lintang,
                bujur = bujur,
                elevasi = elevasi,
                zonaWaktu = zonaWaktu,
                kriteria = kriteria,
                ikhImsak = prefs.ikhImsak,
                ikhSubuh = prefs.ikhSubuh,
                ikhTerbit = prefs.ikhTerbit,
                ikhDhuha = prefs.ikhDhuha,
                ikhDzuhur = prefs.ikhDzuhur,
                ikhAshar = prefs.ikhAshar,
                ikhMaghrib = prefs.ikhMaghrib,
                ikhIsya = prefs.ikhIsya,
                pembulatan = modePembulatan,
                gunakanElevasi = true,
                faktorAshar = asharFactor,
                is24HourFormat = prefs.is24HourFormat
            )

            sb.append("<tr>")
            sb.append("<td>$d</td>")
            sb.append("<td>${namaHari.uppercase()}</td>")
            sb.append("<td>$gDay ${NAMA_BULAN_MASEHI[gMonth - 1].uppercase()}</td>")

            hasil.forEach { h ->
                sb.append("<td class='time-val'>${h.teksWaktu}</td>")
            }
            sb.append("</tr>")
        }

        sb.append("""
                </table>
                <div class="note-card">
                    <ul class="note-list">
                        <li>Jadwal ini disusun oleh Lembaga Falakiyah dengan Kriteria: ${kriteria.nama}</li>
                        <li>Penggunaan jadwal ini memerlukan penyelarasan penanda waktu aplikasi/gadget dengan jam atom BMKG (http://jam.bmkg.go.id).</li>
                        <li>Penetapan awal ${namaBulanHijriah} menunggu Ikhbar Pengurus Besar Nahdlatul Ulama / Pengumuman Pemerintah.</li>
                    </ul>
                </div>
                <div class="footer-text">
                    &copy; $endYear FalakPro by LF PWNU JABAR
                </div>
            </body>
            </html>
        """)

        val htmlContent = sb.toString()
        val jobName = "Imsakiyah_${namaBulanHijriah}_${tahunHijriah}H"

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, htmlContent, "text/HTML", "UTF-8", null)
    }
}

