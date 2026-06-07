package com.falak.falakpro.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.webkit.WebView
import android.webkit.WebViewClient
import com.falak.falakpro.premium.HilalResult
import com.falak.falakpro.AddurulAniq.IjtimaResult
import java.util.Locale

object PencetakHisabAwalBulan {

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

    fun cetakVsop(
        context: Context, 
        res: HilalResult, 
        markazName: String, 
        koordinat: String, 
        elevasi: String, 
        bulanTarget: String
    ) {
        val webView = WebView(context)
        val logoBase64 = getLogoBase64(context)

        val html = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 11px; margin: 15px; color: #333; }
  .header { text-align: center; border-bottom: 2px solid #2e7d32; padding-bottom: 5px; margin-bottom: 10px; }
  .logo { max-width: 50px; margin-bottom: 5px; }
  .title { font-size: 16px; font-weight: bold; color: #2e7d32; margin: 0; }
  .subtitle { font-size: 11px; color: #555; margin-top: 3px; }
  h3 { font-size: 13px; color: #1565c0; border-bottom: 1px solid #1565c0; padding-bottom: 2px; margin-top: 15px; margin-bottom: 5px; }
  table { width: 100%; border-collapse: collapse; margin-bottom: 10px; }
  th, td { border: 1px solid #bbb; padding: 4px; text-align: left; }
  th { background-color: #f0f0f0; font-weight: bold; }
  .val { font-family: 'Courier New', Courier, monospace; font-size:11px; }
  .note { font-size:10px; color:#666; font-style:italic; }
  .concl { margin-top:15px; padding:10px; border:1px solid #1565c0; background:#f5f9ff; border-radius:5px;}
</style>
</head>
<body>
<div class="header">
  <img class="logo" src="$logoBase64" alt="Logo">
  <h1 class="title">LEMBAGA FALAKIYAH PWNU JAWA BARAT</h1>
  <div class="title" style="margin-top:4px; font-size:14px;">LAPORAN HISAB AWAL BULAN ${bulanTarget.uppercase()}</div>
  <div class="subtitle"><b>Markaz Perhitungan:</b> $markazName</div>
  <div class="subtitle"><b>Koordinat:</b> $koordinat &nbsp;|&nbsp; <b>Ketinggian:</b> $elevasi</div>
  <div class="subtitle" style="margin-top:4px; font-style:italic;">Metode: VSOP87D / ELP-MPP02 (Toposentrik / Mar'i)</div>
</div>

<h3>&#9670; 1. IJTIMA' (Geosentrik)</h3>
<table>
  <tr><td>Julian Day Ephemeris (JDE)</td><td class="val">${String.format(Locale.US, "%.6f", res.julianDay)}</td></tr>
  <tr><td>Delta T (ΔT)</td><td class="val">${String.format(Locale.US, "%.2f detik", res.deltaT)}</td></tr>
  <tr><td>Waktu Ijtima' (WD/WIB)</td><td class="val" style="font-weight:bold;">${res.ijtimaGeoStr}</td></tr>
</table>

<h3>&#9670; 2. GHURUB MATAHARI & BULAN</h3>
<table>
  <tr><td>Julian Day (JDE) saat Ghurub</td><td class="val">${res.saatPerhitunganStr}</td></tr>
  <tr><td>Ghurub Matahari</td><td class="val">${res.ghurubSun}</td></tr>
  <tr><td>Ghurub Bulan</td><td class="val">${res.ghurubMoon}</td></tr>
  <tr><td>Waktu Matahari Terbenam s.d Bulan Terbenam</td><td class="val" style="font-weight:bold;">${res.bestTimeStr}</td></tr>
  <tr><td>Umur Hilal</td><td class="val">${res.dayOfIjtimaInPrevMonth} hari</td></tr>
</table>

<h3>&#9670; 3. POSISI MATAHARI SAAT GHURUB</h3>
<table>
  <tr><th>Properti</th><th>Geosentris (Apparent)</th><th>Toposentrik</th></tr>
  <tr><td>Bujur Ekliptika</td><td class="val">${res.bujurMatahariStr}</td><td>-</td></tr>
  <tr><td>Asensio Rekta (RA)</td><td class="val">${res.raMatahariStr}</td><td class="val">${res.raMatahariStr}</td></tr>
  <tr><td>Deklinasi</td><td class="val">${res.decMatahariStr}</td><td class="val">${res.decMatahariStr}</td></tr>
  <tr><td>Azimut Matahari</td><td>-</td><td class="val">${res.azMatahariStr}</td></tr>
</table>

<h3>&#9670; 4. POSISI BULAN SAAT GHURUB</h3>
<table>
  <tr><th>Tinggi Piringan</th><th>Tinggi Toposentrik (Hk)</th><th>Tinggi Mar'i (App)</th></tr>
  <tr><td>Atas (Upper Limb)</td><td class="val">${res.altTopoBulanAtasStr}</td><td class="val" style="font-weight:bold;">${res.altMariBulanAtasStr}</td></tr>
  <tr><td>Tengah (Center)</td><td class="val">${res.altTopoBulanTengahStr}</td><td class="val">${res.altMariBulanTengahStr}</td></tr>
  <tr><td>Bawah (Lower Limb)</td><td class="val">${res.altTopoBulanBawahStr}</td><td class="val">${res.altMariBulanBawahStr}</td></tr>
</table>
<div class="note">* Tinggi Mar'i Atas mempertimbangkan refraksi, semi-diameter, dan paralaks.</div>

<h3>&#9670; 5. ELONGASI & PARAMETER FISIK BULAN</h3>
<table>
  <tr><td>Elongasi (Geosentrik / True)</td><td class="val">${res.elongasiGeoStr}</td></tr>
  <tr><td>Elongasi Toposentrik (Hakiki)</td><td class="val" style="font-weight:bold;">${res.elongasiTopoStr}</td></tr>
  <tr><td>Fraksi Iluminasi (Cahaya)</td><td class="val">${res.illumination}</td></tr>
  <tr><td>Lebar Sabit Hilal (W)</td><td class="val">${res.lebarSabitStr}</td></tr>
  <tr><td>q Yallop / q Odeh</td><td class="val">${res.rangeQOdehStr} / ${res.rangeQOdehStr}</td></tr>
</table>

<div class="concl">
  <h3>&#9670; KESIMPULAN AKHIR</h3>
  <pre style="font-family:inherit;white-space:pre-wrap;margin:0;font-size:11px;">${res.summary}</pre>
</div>
</body></html>"""

        val jobName = "Hisab_Awal_Bulan_VSOP_${System.currentTimeMillis()}"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, html, "text/HTML", "UTF-8", null)
    }

    fun cetakAddurul(
        context: Context, 
        res: IjtimaResult,
        markazName: String, 
        koordinat: String, 
        elevasi: String, 
        bulanTarget: String
    ) {
        val webView = WebView(context)
        val sb = StringBuilder()
        val logoBase64 = getLogoBase64(context)

        fun formatHMS(hours: Double): String {
            val isNeg = hours < 0
            var h = Math.abs(hours)
            val d = Math.floor(h).toInt()
            h = (h - d) * 60
            val m = Math.floor(h).toInt()
            val s = Math.round((h - m) * 60).toInt()
            return String.format(Locale.US, "%s%02d:%02d:%02d", if (isNeg) "-" else "", d, m, s)
        }
        fun Double.toDMS(): String {
            val isNeg = this < 0
            var v = Math.abs(this)
            val d = Math.floor(v).toInt()
            v = (v - d) * 60
            val m = Math.floor(v).toInt()
            val s = Math.round((v - m) * 60).toInt()
            return String.format(Locale.US, "%s%d° %d' %d\"", if (isNeg) "-" else "", d, m, s)
        }

        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Laporan Hisab Awal Bulan (Addurul Aniq)</title>
                <style>
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 11px; margin: 15px; color: #222; }
                    .header { text-align: center; border-bottom: 2px solid #2e7d32; padding-bottom: 5px; margin-bottom: 10px; }
                    .logo { max-width: 50px; margin-bottom: 5px; }
                    .title { font-size: 16px; font-weight: bold; color: #2e7d32; margin: 0; }
                    .subtitle { font-size: 11px; color: #555; margin-top: 3px; }
                    .section-title { font-size: 13px; font-weight: bold; margin-top: 15px; margin-bottom: 5px; color: #1565c0; border-bottom: 1px solid #1565c0; padding-bottom: 2px; text-transform: uppercase; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 15px; font-size: 10px; }
                    th, td { border: 1px solid #aaa; padding: 4px; text-align: center; }
                    th { font-weight: bold; }
                    td.text-left { text-align: left; }
                    td.text-right { text-align: right; font-weight: bold; }
                    .val { font-family: 'Courier New', Courier, monospace; }
                    .bg-blue { background-color: #f5f5f5; }
                    .bg-blue-light { background-color: #fafafa; }
                    .bg-purple { background-color: #f5f5f5; }
                    .bg-purple-light { background-color: #fafafa; }
                    .bg-yellow { background-color: #f5f5f5; }
                    .bg-yellow-light { background-color: #fafafa; }
                    .bg-green { background-color: #f5f5f5; }
                    .bg-green-light { background-color: #fafafa; }
                </style>
            </head>
            <body>
                <div class="header">
                    <img class="logo" src="$logoBase64" alt="Logo">
                    <h1 class="title">LEMBAGA FALAKIYAH PWNU JAWA BARAT</h1>
                    <div class="title" style="margin-top:4px; font-size:14px;">LAPORAN HISAB AWAL BULAN ${bulanTarget.uppercase()}</div>
                    <div class="subtitle"><b>Markaz Perhitungan:</b> $markazName</div>
                    <div class="subtitle"><b>Koordinat:</b> $koordinat &nbsp;|&nbsp; <b>Ketinggian:</b> $elevasi</div>
                    <div class="subtitle" style="margin-top:4px; font-style:italic;">Metode: Addurul Aniq</div>
                </div>

                <div class="section-title">1. WAKTU IJTIMA' GEOSENTRIS</div>
                <table>
                    <tr class="bg-blue">
                        <th class="text-left">TAHUN HIJRIAH</th><th>AL-ALAMAH (A)</th><th>HISSOTUL 'ARDI (F)</th><th>AL-KHOSSOH (M')</th><th>AL-MARKAS (M)</th>
                    </tr>
        """.trimIndent())

        res.ijtimaRows.forEach { row ->
            sb.append("<tr><td class='text-left'>${row.label}</td><td class='val'>${String.format(Locale.US, "%.4f", row.A)}</td><td class='val'>${String.format(Locale.US, "%.4f", row.F)}</td><td class='val'>${String.format(Locale.US, "%.4f", row.M_prime)}</td><td class='val'>${String.format(Locale.US, "%.4f", row.M)}</td></tr>")
        }

        sb.append("""
                </table>
                <table>
                    <tr class="bg-blue">
                        <th class="text-left">DALIL</th><th>HAROKAT</th><th>SATAR AWAL</th><th>SATAR TSANI</th><th>AL-KASRU</th><th>TA'DIL</th>
                    </tr>
        """.trimIndent())

        res.ijtimaTadil.forEach { td ->
            sb.append("<tr><td class='text-left'>${td.dalilName} (${td.rumus})</td><td class='val'>${String.format(Locale.US, "%.4f", td.harokat)}</td><td class='val'>${String.format(Locale.US, "%.4f", td.satar1)}</td><td class='val'>${String.format(Locale.US, "%.4f", td.satar2)}</td><td class='val'>${String.format(Locale.US, "%.4f", td.kasru)}</td><td class='val'>${String.format(Locale.US, "%.4f", td.tadil)}</td></tr>")
        }

        val totalTadilIjtima = res.ijtimaTadil.sumOf { it.tadil }
        sb.append("""
                    <tr class="bg-blue-light"><td colspan="5" class="text-right">JUMLAH TA'DIL</td><td class="val">${String.format(Locale.US, "%.4f", totalTadilIjtima)}</td></tr>
                    <tr class="bg-blue-light"><td colspan="5" class="text-right">WAKTU IJTIMA' (WD)</td><td class="val" style="font-weight:bold; font-size:12px;">${formatHMS(res.waktuIjtimaLT)}</td></tr>
                    <tr class="bg-blue-light"><td colspan="5" class="text-right">TANGGAL IJTIMA'</td><td class="val" style="font-weight:bold;">${res.hariPasaran}, ${res.tgl} ${res.namaBulan} ${res.thn}</td></tr>
                </table>

                <div class="section-title">2. HAROKAT POSISI</div>
                <table>
                    <tr class="bg-purple">
                        <th></th><th>h</th><th>p</th><th>S</th><th>m</th><th>M</th><th>A</th><th>N</th><th>D</th><th>O</th><th>ST</th>
                    </tr>
        """.trimIndent())

        val allItems = res.rows.toMutableList()
        allItems.add(7, res.jumlah1)
        allItems.add(res.jumlah2)

        allItems.forEach { row ->
            val isJumlah = row.label.startsWith("JUMLAH")
            val trClass = if (isJumlah) "class='bg-purple-light'" else ""
            val hstr = if(row.hari == 0) "" else row.hari.toString()
            val pstr = if(row.pasaran == 0) "" else row.pasaran.toString()
            
            sb.append("<tr $trClass><td class='text-left' style='font-weight:bold;'>${row.label}</td>")
            sb.append("<td>$hstr</td><td>$pstr</td>")
            sb.append("<td class='val'>${String.format(Locale.US, "%.4f", row.S)}</td>")
            sb.append("<td class='val'>${String.format(Locale.US, "%.4f", row.m_khosso)}</td>")
            sb.append("<td class='val'>${String.format(Locale.US, "%.4f", row.M_wasath)}</td>")
            sb.append("<td class='val'>${String.format(Locale.US, "%.4f", row.A_khosso)}</td>")
            sb.append("<td class='val'>${String.format(Locale.US, "%.4f", row.N_hissah)}</td>")
            sb.append("<td class='val'>${String.format(Locale.US, "%.4f", row.D_budu)}</td>")
            sb.append("<td class='val'>${String.format(Locale.US, "%.4f", row.O_mail)}</td>")
            sb.append("<td class='val'>${String.format(Locale.US, "%.4f", row.ST)}</td>")
            sb.append("</tr>")
        }

        sb.append("""
                </table>

                <div class="section-title">3. DATA MATAHARI</div>
                <table>
                    <tr class="bg-yellow">
                        <th class="text-left">PROPERTI</th><th>DALIL</th><th>HAROKAT</th><th>SATAR 1</th><th>SATAR 2</th><th>KASRU</th><th>TA'DIL</th>
                    </tr>
        """.trimIndent())

        // Data Matahari
        val sDetails = res.matahari.tadilDetails
        if (sDetails.size >= 2) {
            sb.append("<tr><td rowspan='3' class='text-left' style='vertical-align:middle; font-weight:bold;'>BUJUR MATAHARI (S')</td><td class='text-left'>${sDetails[0].dalilName} (${sDetails[0].rumus})</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[0].harokat)}</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[0].satar1)}</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[0].satar2)}</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[0].kasru)}</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[0].tadil)}</td></tr>")
            sb.append("<tr><td class='text-left'>${sDetails[1].dalilName} (${sDetails[1].rumus})</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[1].harokat)}</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[1].satar1)}</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[1].satar2)}</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[1].kasru)}</td><td class='val'>${String.format(Locale.US, "%.4f", sDetails[1].tadil)}</td></tr>")
            sb.append("<tr class='bg-yellow-light'><td colspan='5' class='text-right'>THUL SYAMS (S')</td><td class='val'>${res.matahari.sPrimeHaqiqi.toDMS()}</td></tr>")
        }
        
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>DEKLINASI (dm)</td><td class='val'>${res.matahari.dmHaqiqi.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>ASCENSIOREKTA (am)</td><td class='val'>${res.matahari.amHaqiqi.toDMS()}</td></tr>")

        val rDetails = res.matahari.jarakDetails
        if (rDetails.size >= 2) {
            sb.append("<tr><td rowspan='3' class='text-left' style='vertical-align:middle; font-weight:bold;'>JARAK BUMI-MATAHARI (R)</td><td class='text-left'>${rDetails[0].dalilName} (${rDetails[0].rumus})</td><td class='val'>${String.format(Locale.US, "%.4f", rDetails[0].harokat)}</td><td class='val'>${String.format(Locale.US, "%.4f", rDetails[0].satar1)}</td><td class='val'>${String.format(Locale.US, "%.4f", rDetails[0].satar2)}</td><td class='val'>${String.format(Locale.US, "%.4f", rDetails[0].kasru)}</td><td class='val'>${String.format(Locale.US, "%.6f", rDetails[0].tadil)}</td></tr>")
            sb.append("<tr><td class='text-left'>${rDetails[1].dalilName} (${rDetails[1].rumus})</td><td class='val'>${String.format(Locale.US, "%.4f", rDetails[1].harokat)}</td><td class='val'>${String.format(Locale.US, "%.4f", rDetails[1].satar1)}</td><td class='val'>${String.format(Locale.US, "%.4f", rDetails[1].satar2)}</td><td class='val'>${String.format(Locale.US, "%.4f", rDetails[1].kasru)}</td><td class='val'>${String.format(Locale.US, "%.6f", rDetails[1].tadil)}</td></tr>")
            sb.append("<tr class='bg-yellow-light'><td colspan='5' class='text-right'>JARAK MATAHARI (R)</td><td class='val'>${String.format(Locale.US, "%.6f AU", res.matahari.RHaqiqi)}</td></tr>")
        }

        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>SEMIDIAMETER (sd)</td><td class='val'>${res.matahari.sdHaqiqi.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>EQUATION OF TIME (e)</td><td class='val'>${formatHMS(res.matahari.eotHaqiqi)}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>DIP (Kerendahan Ufuk)</td><td class='val'>${res.matahari.dip.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>ALTITUDE (hm)</td><td class='val'>${res.matahari.hm.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>SUDUT WAKTU (GM)</td><td class='val'>${res.matahari.GM.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>SUNSET (Ghurub WD/WIB)</td><td class='val'>${formatHMS(res.ghrbWdHaqiqi)}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>AZIMUT MATAHARI (azm)</td><td class='val'>${res.matahari.azm.toDMS()}</td></tr>")

        sb.append("""
                </table>

                <div class="section-title">4. DATA BULAN</div>
                <table>
                    <tr class="bg-green">
                        <th class="text-left">PROPERTI</th><th>DALIL</th><th>HAROKAT</th><th>SATAR 1</th><th>SATAR 2</th><th>KASRU</th><th>TA'DIL</th>
                    </tr>
        """.trimIndent())

        // Thulul Qomar
        val moDetails = res.thulQomarTadil
        if (moDetails.isNotEmpty()) {
            sb.append("<tr><td rowspan='${moDetails.size + 1}' class='text-left' style='vertical-align:middle; font-weight:bold;'>BUJUR BULAN (Mo)</td>")
            moDetails.forEachIndexed { idx, d ->
                if (idx > 0) sb.append("<tr>")
                sb.append("<td class='text-left'>${d.dalilName} (${d.rumus})</td><td class='val'>${String.format(Locale.US, "%.4f", d.harokat)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.satar1)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.satar2)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.kasru)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.tadil)}</td></tr>")
            }
            sb.append("<tr class='bg-green-light'><td colspan='5' class='text-right'>THULUL QOMAR (Mo)</td><td class='val'>${res.moResult.toDMS()}</td></tr>")
        }

        // Ardhul Qomar
        val bDetails = res.ardhQomarTadil
        if (bDetails.isNotEmpty()) {
            sb.append("<tr><td rowspan='${bDetails.size + 1}' class='text-left' style='vertical-align:middle; font-weight:bold;'>LATTITUDE BULAN (B)</td>")
            bDetails.forEachIndexed { idx, d ->
                if (idx > 0) sb.append("<tr>")
                sb.append("<td class='text-left'>${d.dalilName} (${d.rumus})</td><td class='val'>${String.format(Locale.US, "%.4f", d.harokat)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.satar1)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.satar2)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.kasru)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.tadil)}</td></tr>")
            }
            sb.append("<tr class='bg-green-light'><td colspan='5' class='text-right'>ARDHUL QOMAR (B)</td><td class='val'>${res.ardhQamar.toDMS()}</td></tr>")
        }

        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>DEKLINASI BULAN (dc)</td><td class='val'>${res.dcResult.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>ASCENSIOREKTA BULAN (ac)</td><td class='val'>${res.acResult.toDMS()}</td></tr>")

        // Jarak Bumi Bulan
        val rMoonDetails = res.jarakBumiBulanTadil
        if (rMoonDetails.isNotEmpty()) {
            sb.append("<tr><td rowspan='${rMoonDetails.size + 1}' class='text-left' style='vertical-align:middle; font-weight:bold;'>JARAK BUMI-BULAN (r)</td>")
            rMoonDetails.forEachIndexed { idx, d ->
                if (idx > 0) sb.append("<tr>")
                sb.append("<td class='text-left'>${d.dalilName} (${d.rumus})</td><td class='val'>${String.format(Locale.US, "%.4f", d.harokat)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.satar1)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.satar2)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.kasru)}</td><td class='val'>${String.format(Locale.US, "%.4f", d.tadil)}</td></tr>")
            }
            sb.append("<tr class='bg-green-light'><td colspan='5' class='text-right'>JARAK BUMI-BULAN (r)</td><td class='val'>${String.format(Locale.US, "%.3f Km", res.distMoon)}</td></tr>")
        }

        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>HORIZONTAL PARALLAX (Hp)</td><td class='val'>${res.hpMoon.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>SEMIDIAMETER BULAN (sdc)</td><td class='val'>${res.sdcMoon.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>SUDUT WAKTU BULAN (GC)</td><td class='val'>${res.gcMoon.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>ALTITUDE BULAN (hc Markazi)</td><td class='val'>${res.hcMarkazi.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>AZIMUT BULAN (azc)</td><td class='val'>${res.azcMoon.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>BEDA AZIMUT (z)</td><td class='val'>${res.bedaAzm.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>REFRAKSI (Ref)</td><td class='val'>${res.refraksi.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>PARALLAX (P)</td><td class='val'>${res.parallax.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>ALTITUDE (hc Sathi / Toposentrik)</td><td class='val'>${res.hcSathi.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>ELO MARKAZI</td><td class='val'>${res.eloMarkazi.toDMS()}</td></tr>")
        sb.append("<tr><td colspan='6' class='text-left' style='font-weight:bold;'>ELO SATHI</td><td class='val'>${res.eloSathi.toDMS()}</td></tr>")

        sb.append("""
                </table>

                <div class="section-title">5. KESIMPULAN VISIBILITAS</div>
                <table>
                    <tr class="bg-blue">
                        <th class="text-left" colspan="2">NILAI KESIMPULAN</th>
                    </tr>
                    <tr><td class='text-left'>Nurul Hilal (nh)</td><td class="val">${String.format(Locale.US, "%.4f %%", res.nurulHilal)}</td></tr>
                    <tr><td class='text-left'>Muktsul Hilal (mh)</td><td class="val">${String.format(Locale.US, "%.2f menit", res.muktsulHilal)}</td></tr>
                    <tr><td class='text-left'>Beda Tinggi (Y)</td><td class="val">${res.bedaTinggi.toDMS()}</td></tr>
                    <tr><td class='text-left'>Bu'duz Zawiyah (C)</td><td class="val">${res.buduZawiyah.toDMS()}</td></tr>
                    <tr><td class='text-left'>Ghurubul Hilal (GH)</td><td class="val">${formatHMS(res.ghurubHilal)}</td></tr>
                    <tr><td class='text-left'>Umur Hilal</td><td class="val">${formatHMS(res.umurHilal)}</td></tr>
                </table>
            </body>
            </html>
        """.trimIndent())

        val jobName = "Hisab_Awal_Bulan_Addurul_${System.currentTimeMillis()}"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                val printAdapter = webView.createPrintDocumentAdapter(jobName)
                printManager.print(jobName, printAdapter, PrintAttributes.Builder().build())
            }
        }
        webView.loadDataWithBaseURL(null, sb.toString(), "text/HTML", "UTF-8", null)
    }
}

