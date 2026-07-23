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
                ?: return ""
            val stream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            val bytes = stream.toByteArray()
            bitmap.recycle()
            "data:image/png;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            android.util.Log.e("PencetakHisab", "Gagal encode logo: ${e.message}")
            ""
        }
    }

    private fun dmsTextToDouble(text: String): Double {
        val sign = if (text.trim().startsWith("-")) -1.0 else 1.0
        val nums = Regex("""\d+(?:[.,]\d+)?""").findAll(text)
            .map { it.value.replace(',', '.').toDoubleOrNull() ?: 0.0 }
            .toList()
        if (nums.isEmpty()) return 0.0
        return sign * (nums.getOrElse(0) { 0.0 } + nums.getOrElse(1) { 0.0 } / 60.0 + nums.getOrElse(2) { 0.0 } / 3600.0)
    }

    private fun horizonPositionSvg(
        sunAzimuth: Double,
        sunAltitude: Double,
        moonAzimuth: Double,
        moonAltitude: Double,
        moonsetAzimuth: Double? = null,
        elongationDeg: Double? = null,
        illuminationText: String = "",
        crescentWidthText: String = ""
    ): String {
        fun deltaAz(az: Double): Double {
            var value = (az - sunAzimuth) % 360.0
            if (value > 180.0) value -= 360.0
            if (value < -180.0) value += 360.0
            return value
        }

        fun xFromDelta(delta: Double): Double =
            (90.0 + (delta.coerceIn(-10.0, 10.0) + 10.0) / 20.0 * 620.0)

        fun yFromAlt(alt: Double): Double =
            (420.0 - alt.coerceIn(0.0, 16.0) / 16.0 * 320.0)

        val sunX = xFromDelta(0.0)
        val sunY = yFromAlt(0.0)
        val moonDelta = deltaAz(moonAzimuth)
        val moonX = xFromDelta(moonDelta)
        val moonY = yFromAlt(moonAltitude)
        val moonsetDelta = moonsetAzimuth?.let { deltaAz(it) }
        val moonsetX = moonsetDelta?.let { xFromDelta(it) }
        val moonsetY = yFromAlt(0.0)
        val elongation = elongationDeg ?: kotlin.math.hypot(moonDelta, moonAltitude - sunAltitude)

        val verticalGrid = (-10..10).joinToString("\n") { deg ->
            val x = xFromDelta(deg.toDouble())
            val major = deg % 5 == 0
            val color = if (major) "#9fb8ca" else "#d6e2ea"
            val width = if (major) "1.1" else "0.7"
            """<line x1="$x" y1="100" x2="$x" y2="420" stroke="$color" stroke-width="$width"/>"""
        }
        val horizontalGrid = (0..16).joinToString("\n") { deg ->
            val y = yFromAlt(deg.toDouble())
            val major = deg % 2 == 0
            val color = if (major) "#9fb8ca" else "#d6e2ea"
            val width = if (major) "1.1" else "0.7"
            """<line x1="90" y1="$y" x2="710" y2="$y" stroke="$color" stroke-width="$width"/>"""
        }
        val bottomLabels = (-10..10 step 2).joinToString("\n") { deg ->
            val x = xFromDelta(deg.toDouble())
            """<text x="$x" y="450" text-anchor="middle" font-size="13" fill="#0f766e">${kotlin.math.abs(deg)}°</text>"""
        }
        val sideLabels = (0..16 step 2).joinToString("\n") { deg ->
            val y = yFromAlt(deg.toDouble()) + 4.0
            """<text x="70" y="$y" text-anchor="end" font-size="13" font-weight="bold" fill="#334155">${deg}°</text><text x="730" y="$y" font-size="13" font-weight="bold" fill="#334155">${deg}°</text>"""
        }
        val moonsetPoint = moonsetDelta?.let { delta ->
            val x = xFromDelta(delta)
            """
              <circle cx="$x" cy="$moonsetY" r="8" fill="#dc2626" stroke="#7f1d1d" stroke-width="2"/>
              <text x="$x" y="466" text-anchor="middle" font-size="10" fill="#7f1d1d">Moonset ${String.format(Locale.US, "%.1f°", delta)}</text>
            """.trimIndent()
        }.orEmpty()

        return """
            <svg viewBox="0 0 800 520" width="100%" height="350" xmlns="http://www.w3.org/2000/svg">
              <defs>
                <linearGradient id="skyPrint" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#e8f4ff"/>
                  <stop offset="60%" stop-color="#f7fbff"/>
                  <stop offset="100%" stop-color="#fff7df"/>
                </linearGradient>
              </defs>
              <rect x="0" y="0" width="800" height="520" rx="8" fill="#ffffff" stroke="#1f2937" stroke-width="1.4"/>
              <text x="400" y="28" text-anchor="middle" font-size="18" font-weight="bold" fill="#0f172a">PETA POSISI HILAL LOKAL SAAT GHURUB</text>
              <text x="400" y="48" text-anchor="middle" font-size="11" fill="#475569">Azimut relatif terhadap titik Matahari terbenam. Kanan = hilal di utara/kanan Matahari, kiri = selatan/kiri Matahari.</text>

              <rect x="90" y="100" width="620" height="320" fill="url(#skyPrint)" stroke="#334155" stroke-width="1.2"/>
              $verticalGrid
              $horizontalGrid
              <path d="M90 420 C245 255 555 255 710 420" fill="none" stroke="#22c55e" stroke-width="1.2"/>
              <path d="M120 350 C280 260 520 260 680 350" fill="none" stroke="#38bdf8" stroke-width="1" opacity="0.8"/>
              <path d="M160 300 C310 230 490 230 640 300" fill="none" stroke="#a78bfa" stroke-width="0.9" opacity="0.7"/>
              <line x1="90" y1="420" x2="710" y2="420" stroke="#b45309" stroke-width="2"/>
              <text x="700" y="414" text-anchor="end" font-size="10" fill="#92400e">Ufuk Hakiki</text>
              $sideLabels

              <line x1="$moonX" y1="$moonY" x2="$moonX" y2="420" stroke="#2563eb" stroke-width="1.6" stroke-dasharray="5 4"/>
              <line x1="$sunX" y1="$sunY" x2="$moonX" y2="$moonY" stroke="#eab308" stroke-width="2"/>
              <text x="${(moonX + 12.0).coerceAtMost(650.0)}" y="${(moonY - 12.0).coerceAtLeast(86.0)}" font-size="11" fill="#1d4ed8">Tinggi hilal ${String.format(Locale.US, "%.2f°", moonAltitude)}</text>
              <text x="${((sunX + moonX) / 2.0 + 8.0).coerceAtMost(650.0)}" y="${((sunY + moonY) / 2.0 - 8.0).coerceIn(95.0, 410.0)}" font-size="11" fill="#854d0e">Elongasi ${String.format(Locale.US, "%.2f°", elongation)}</text>

              <circle cx="$sunX" cy="$sunY" r="10" fill="#facc15" stroke="#ca8a04" stroke-width="2"/>
              <text x="$sunX" y="444" text-anchor="middle" font-size="11" font-weight="bold" fill="#854d0e">Sunset 0°</text>
              <circle cx="$moonX" cy="$moonY" r="9" fill="#111827" stroke="#ffffff" stroke-width="2"/>
              <path d="M${moonX - 3.0} ${moonY - 8.0} C${moonX + 8.0} ${moonY - 4.0} ${moonX + 8.0} ${moonY + 5.0} ${moonX - 3.0} ${moonY + 8.0}" fill="#ffffff" opacity="0.85"/>
              <text x="${(moonX + 14.0).coerceAtMost(650.0)}" y="${(moonY + 5.0).coerceIn(105.0, 412.0)}" font-size="12" font-weight="bold" fill="#111827">Hilal (${String.format(Locale.US, "%+.2f°", moonDelta)})</text>
              $moonsetPoint

              <line x1="90" y1="434" x2="710" y2="434" stroke="#334155" stroke-width="1"/>
              $bottomLabels
              <text x="400" y="486" text-anchor="middle" font-size="14" font-weight="bold" fill="#0f766e">SUN-MOON AZIMUTH</text>
              <text x="34" y="265" transform="rotate(-90 34 265)" text-anchor="middle" font-size="14" font-weight="bold" fill="#334155">MOON ALTITUDE</text>
              <text x="766" y="265" transform="rotate(90 766 265)" text-anchor="middle" font-size="14" font-weight="bold" fill="#334155">MOON ALTITUDE</text>

              <rect x="112" y="62" width="160" height="26" rx="4" fill="#eef6ff" stroke="#cbd5e1"/>
              <circle cx="126" cy="75" r="5" fill="#facc15"/><text x="140" y="79" font-size="11" fill="#334155">Matahari terbenam</text>
              <rect x="320" y="62" width="125" height="26" rx="4" fill="#eef6ff" stroke="#cbd5e1"/>
              <circle cx="334" cy="75" r="5" fill="#111827"/><text x="348" y="79" font-size="11" fill="#334155">Hilal</text>
              <rect x="490" y="62" width="185" height="26" rx="4" fill="#eef6ff" stroke="#cbd5e1"/>
              <line x1="504" y1="75" x2="526" y2="75" stroke="#eab308" stroke-width="2"/><text x="534" y="79" font-size="11" fill="#334155">Jarak sudut/elongasi</text>

              <text x="92" y="505" font-size="10" fill="#475569">Iluminasi: ${illuminationText.ifBlank { "-" }} | Lebar sabit: ${crescentWidthText.ifBlank { "-" }} | Azimut Matahari: ${String.format(Locale.US, "%.2f°", sunAzimuth)} | Azimut Hilal: ${String.format(Locale.US, "%.2f°", moonAzimuth)}</text>
            </svg>
        """.trimIndent()
    }

    private fun printConclusionHtml(title: String, body: String): String = """
        <div class="concl">
          <h3>&#9670; $title</h3>
          <pre style="font-family:inherit;white-space:pre-wrap;margin:0;font-size:11px;">$body</pre>
        </div>
    """.trimIndent()

    private fun screenConclusionHtml(
        penjelasan: String,
        detail: String?,
        judulTanggal: String,
        teksTanggal: String
    ): String = """
        <div class="concl keep">
          <h3>&#9670; KESIMPULAN AWAL BULAN</h3>
          <p>$penjelasan</p>
          ${detail?.let { "<p><b>${it.replace("\n", "<br>")}</b></p>" } ?: ""}
          <div class="date-title">$judulTanggal</div>
          <div class="date-final">$teksTanggal</div>
        </div>
    """.trimIndent()

    private fun printAttributesA4(): PrintAttributes =
        PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

    private fun hijriMonthName(month: Int): String = listOf(
        "Muharram", "Shafar", "Rabi'ul Awwal", "Rabi'ul Akhir",
        "Jumadal Ula", "Jumadal Akhirah", "Rajab", "Sya'ban",
        "Ramadhan", "Syawwal", "Dzul Qa'dah", "Dzul Hijjah"
    ).getOrElse((month - 1).coerceIn(0, 11)) { "Hijriah" }

    fun cetakVsop(
        context: Context, 
        res: HilalResult, 
        markazName: String, 
        koordinat: String, 
        elevasi: String, 
        bulanTarget: String,
        kesimpulanPenjelasan: String = res.summary,
        kesimpulanDetail: String? = null,
        kesimpulanJudulTanggal: String = "Kesimpulan:",
        kesimpulanTanggal: String = res.conclusion
    ) {
        val webView = WebView(context)
        val logoBase64 = getLogoBase64(context)

        val moonAz = dmsTextToDouble(res.azBulanStr)
        val moonAlt = dmsTextToDouble(res.altMariStr)
        val sunAz = dmsTextToDouble(res.azMatahariStr)
        val sunAlt = dmsTextToDouble(res.altTopoMatahariStr)
        val moonsetAz = dmsTextToDouble(res.arahTerbenamBulanStr)
        val hilalMapSvg = horizonPositionSvg(
            sunAzimuth = sunAz,
            sunAltitude = sunAlt,
            moonAzimuth = moonAz,
            moonAltitude = moonAlt,
            moonsetAzimuth = moonsetAz,
            elongationDeg = dmsTextToDouble(res.elongasiTopoStr),
            illuminationText = res.illumination,
            crescentWidthText = res.lebarSabitStr
        )
        val html = """
<!DOCTYPE html>
<html>
<head>
<meta charset="utf-8">
<style>
  @page { size: A4; margin: 8mm; }
  body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 10.5px; margin: 0; color: #333; }
  .header { text-align: center; border-bottom: 2px solid #2e7d32; padding-bottom: 5px; margin-bottom: 10px; }
  .logo { max-width: 50px; margin-bottom: 5px; }
  .title { font-size: 16px; font-weight: bold; color: #2e7d32; margin: 0; }
  .subtitle { font-size: 11px; color: #555; margin-top: 3px; }
  h3 { font-size: 12px; color: #1565c0; border-bottom: 1px solid #1565c0; padding-bottom: 2px; margin-top: 9px; margin-bottom: 4px; }
  table { width: 100%; border-collapse: collapse; margin-bottom: 7px; page-break-inside: avoid; }
  tr, td, th, .keep { page-break-inside: avoid; break-inside: avoid; }
  th, td { border: 1px solid #bbb; padding: 3px; text-align: left; }
  th { background-color: #f0f0f0; font-weight: bold; }
  .val { font-family: 'Courier New', Courier, monospace; font-size:11px; }
  .note { font-size:10px; color:#666; font-style:italic; }
  .concl { margin-top:9px; padding:8px; border:1px solid #1565c0; background:#f5f9ff; border-radius:5px;}
  .concl p { margin: 0 0 5px 0; }
  .date-title { color:#1565c0; font-size:12px; font-weight:bold; margin-top:6px; }
  .date-final { font-size:15px; font-weight:bold; margin-top:3px; }
  .small { font-size:10px; }
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
  <tr><td>Ijtima' Toposentris</td><td class="val">${res.ijtimaTopoStr}</td></tr>
</table>

<h3>&#9670; 2. GHURUB MATAHARI &amp; BULAN</h3>
<table>
  <tr><td>Julian Day (JDE) saat Ghurub</td><td class="val">${res.saatPerhitunganStr}</td></tr>
  <tr><td>Ghurub Matahari</td><td class="val">${res.ghurubSun}</td></tr>
  <tr><td>Ghurub Bulan</td><td class="val">${res.ghurubMoon}</td></tr>
  <tr><td>Waktu Matahari Terbenam s.d Bulan Terbenam</td><td class="val" style="font-weight:bold;">${res.bestTimeStr}</td></tr>
  <tr><td>Umur Hilal</td><td class="val">${res.dayOfIjtimaInPrevMonth} hari</td></tr>
  <tr><td>Jarak Bumi-Bulan</td><td class="val">${res.jarakBumiBulanStr}</td></tr>
  <tr><td>Horizontal Parallax Bulan</td><td class="val">${res.hpBulanStr}</td></tr>
</table>

<h3>&#9670; 3. POSISI MATAHARI SAAT GHURUB</h3>
<table>
  <tr><th>Properti</th><th>Geosentris (Apparent)</th><th>Toposentrik</th></tr>
  <tr><td>Bujur Ekliptika</td><td class="val">${res.bujurMatahariStr}</td><td>-</td></tr>
  <tr><td>Lintang Ekliptika</td><td class="val">${res.lintangMatahariStr}</td><td>-</td></tr>
  <tr><td>Asensio Rekta (RA)</td><td class="val">${res.raMatahariStr}</td><td class="val">${res.raMatahariStr}</td></tr>
  <tr><td>Deklinasi</td><td class="val">${res.decMatahariStr}</td><td class="val">${res.decMatahariStr}</td></tr>
  <tr><td>Altitude Matahari</td><td>-</td><td class="val">${res.altTopoMatahariStr}</td></tr>
  <tr><td>Azimut Matahari</td><td>-</td><td class="val">${res.azMatahariStr}</td></tr>
</table>

<h3>&#9670; 4. POSISI BULAN SAAT GHURUB</h3>
<table>
  <tr><th>Properti</th><th>Geosentris</th><th>Toposentrik</th></tr>
  <tr><td>Bujur Ekliptika</td><td class="val">${res.bujurBulanStr}</td><td class="val">${res.bujurBulanStr}</td></tr>
  <tr><td>Lintang Ekliptika</td><td class="val">${res.lintangBulanStr}</td><td class="val">${res.lintangBulanStr}</td></tr>
  <tr><td>Asensio Rekta (RA)</td><td class="val">${res.raBulanStr}</td><td class="val">${res.raBulanStr}</td></tr>
  <tr><td>Deklinasi</td><td class="val">${res.decBulanStr}</td><td class="val">${res.decBulanStr}</td></tr>
  <tr><td>Azimut Bulan</td><td>-</td><td class="val">${res.azBulanStr}</td></tr>
</table>
<table>
  <tr><th>Tinggi Piringan</th><th>Tinggi Toposentrik (Hk)</th><th>Tinggi Mar'i (App)</th></tr>
  <tr><td>Atas (Upper Limb)</td><td class="val">${res.altTopoBulanAtasStr}</td><td class="val" style="font-weight:bold;">${res.altMariBulanAtasStr}</td></tr>
  <tr><td>Tengah (Center)</td><td class="val">${res.altTopoBulanTengahStr}</td><td class="val">${res.altMariBulanTengahStr}</td></tr>
  <tr><td>Bawah (Lower Limb)</td><td class="val">${res.altTopoBulanBawahStr}</td><td class="val">${res.altMariBulanBawahStr}</td></tr>
</table>
<div class="note">* Tinggi Mar'i Atas mempertimbangkan refraksi, semi-diameter, dan paralaks.</div>

<h3>&#9670; 5. ELONGASI &amp; PARAMETER FISIK BULAN</h3>
<table>
  <tr><td>Elongasi (Geosentrik / True)</td><td class="val">${res.elongasiGeoStr}</td></tr>
  <tr><td>Elongasi Toposentrik (Hakiki)</td><td class="val" style="font-weight:bold;">${res.elongasiTopoStr}</td></tr>
  <tr><td>Fraksi Iluminasi (Cahaya)</td><td class="val">${res.illumination}</td></tr>
  <tr><td>Lebar Sabit Hilal (W)</td><td class="val">${res.lebarSabitStr}</td></tr>
  <tr><td>q Yallop / q Odeh</td><td class="val">${res.rangeQOdehStr} / ${res.rangeQOdehStr}</td></tr>
</table>

<h3>&#9670; 6. PETA POSISI HILAL</h3>
$hilalMapSvg

${screenConclusionHtml(kesimpulanPenjelasan, kesimpulanDetail, kesimpulanJudulTanggal, kesimpulanTanggal)}
</body></html>"""

        val jobName = "Hisab_Awal_Bulan_VSOP_${System.currentTimeMillis()}"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                try {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        printManager.print(jobName, printAdapter, printAttributesA4())
                    } else {
                        android.widget.Toast.makeText(context, "Layanan cetak tidak tersedia", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PencetakHisab", "Gagal cetak VSOP: ${e.message}", e)
                    android.widget.Toast.makeText(context, "Gagal mencetak: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
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
        bulanTarget: String,
        kesimpulanPenjelasan: String = "",
        kesimpulanDetail: String? = null,
        kesimpulanJudulTanggal: String = "Kesimpulan:",
        kesimpulanTanggal: String = ""
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
        fun fmt(value: Double, digits: Int = 4): String =
            String.format(Locale.US, "%.${digits}f", value).replace('.', ',')
        fun fmtBlank(value: Double, digits: Int = 4): String =
            if (value == 0.0) "" else fmt(value, digits)
        fun keyForRow(row: com.falak.falakpro.AddurulAniq.PosisiRow): String {
            val keys = res.processKeys ?: return ""
            return when (row.label.uppercase(Locale.US)) {
                "MAJMU'AH", "MAJMUAH" -> keys.majmuah.toString()
                "MABSUTHOH", "MABSUTOH" -> keys.mabsuthoh.toString()
                "BULAN" -> keys.bulan.toString()
                "HARI" -> keys.hari.toString()
                "JAM GHRB" -> keys.jamGhurub.toString()
                "MENIT GHRB" -> keys.menitGhurub.toString()
                "DETIK GHRB" -> keys.detikGhurub.toString()
                "SFT JAM" -> keys.sftJam.toString()
                "SFT MENIT" -> keys.sftMenit.toString()
                "SFT DETIK" -> keys.sftDetik.toString()
                else -> ""
            }
        }
        val processKeys = res.processKeys
        val prevMonth = if (res.targetMonth == 1) 12 else res.targetMonth - 1
        val prevYear = if (res.targetMonth == 1) res.targetYear - 1 else res.targetYear
        val processTitle = "AKHIR BULAN ${hijriMonthName(prevMonth).uppercase(Locale.US)} $prevYear H."
        val ijtimaSumA = res.ijtimaRows.sumOf { it.A }
        val ijtimaSumF = res.ijtimaRows.sumOf { it.F }
        val ijtimaSumMp = res.ijtimaRows.sumOf { it.M_prime }
        val ijtimaSumM = res.ijtimaRows.sumOf { it.M }
        val processIjtimaRows = res.ijtimaRows.joinToString("") { row ->
            val key = when (row.label.uppercase(Locale.US)) {
                "MAJMU'AH", "MAJMUAH" -> processKeys?.majmuah?.toString().orEmpty()
                "MABSUTHOH", "MABSUTOH" -> processKeys?.mabsuthoh?.toString().orEmpty()
                "BULAN" -> processKeys?.bulan?.toString().orEmpty()
                else -> ""
            }
            "<tr><td class='label'>${row.label}</td><td class='key'>$key</td><td>${fmt(row.A, 3)}</td><td>${fmt(row.F)}</td><td>${fmt(row.M_prime)}</td><td>${fmt(row.M)}</td></tr>"
        }
        val processIjtimaTable = """
            <table class="process orange keep">
              <tr><th colspan="6" class="big-title">$processTitle</th></tr>
              <tr><th colspan="2">TAHUN HIJRIAH</th><th>AL - ALAMAH<br>(A)</th><th>HISSOTUL 'ARDI<br>(F)</th><th>AL - KHOSSOH<br>(M')</th><th>AL - MARKAS<br>(M)</th></tr>
              $processIjtimaRows
              <tr class="total"><td colspan="2">JUMLAH</td><td>${fmt(ijtimaSumA, 3)}</td><td>${fmt(ijtimaSumF)}</td><td>${fmt(ijtimaSumMp)}</td><td>${fmt(ijtimaSumM)}</td></tr>
            </table>
        """.trimIndent()

        val processRows = res.rows.toMutableList().apply {
            add(7, com.falak.falakpro.AddurulAniq.PosisiRow("Tambahan Hari"))
            add(8, res.jumlah1)
            add(res.jumlah2)
        }
        val processHarokatRows = processRows.joinToString("") { row ->
            val isTotal = row.label.startsWith("JUMLAH")
            val isZeroLine = row.label == "Tambahan Hari"
            if (isTotal) {
                """
                    <tr class="total">
                      <td class="label" colspan="2">${row.label}</td><td>${row.alamat}</td><td>${row.hari}</td><td>${row.pasaran}</td>
                      <td>${fmt(row.S)}</td><td>${fmt(row.m_khosso)}</td><td>${fmt(row.M_wasath)}</td><td>${fmt(row.A_khosso)}</td><td>${fmt(row.N_hissah)}</td><td>${fmt(row.D_budu)}</td><td>${fmt(row.O_mail, 5)}</td><td>${fmt(row.ST)}</td>
                    </tr>
                """.trimIndent()
            } else if (isZeroLine) {
                """
                    <tr>
                      <td class="label">Tambahan Hari</td><td class="key">0</td><td>0</td><td>0</td><td>0</td>
                      <td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td><td>0</td>
                    </tr>
                """.trimIndent()
            } else {
                val key = keyForRow(row)
                val h = if (row.hari == 0) "" else row.hari.toString()
                val p = if (row.pasaran == 0) "" else row.pasaran.toString()
                """
                    <tr>
                      <td class="label">${row.label}</td><td class="key">$key</td><td>${if (row.alamat == 0L) "" else row.alamat}</td><td>$h</td><td>$p</td>
                      <td>${fmtBlank(row.S)}</td><td>${fmtBlank(row.m_khosso)}</td><td>${fmtBlank(row.M_wasath)}</td><td>${fmtBlank(row.A_khosso)}</td><td>${fmtBlank(row.N_hissah)}</td><td>${fmtBlank(row.D_budu)}</td><td>${fmtBlank(row.O_mail, 5)}</td><td>${fmtBlank(row.ST)}</td>
                    </tr>
                """.trimIndent()
            }
        }
        val processHarokatTable = """
            <table class="process purple keep">
              <tr><th colspan="2">السنة الهجرية</th><th>العلامة</th><th colspan="2">الأيام<br><span>h&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;p</span></th><th>وسط الشمس<br>S</th><th>خاصتها<br>m</th><th>وسط القمر<br>M</th><th>خاصته<br>A</th><th>حصة العرض<br>N</th><th>البعد<br>D</th><th>الميل الكلي<br>O</th><th>الوقت النجمي<br>ST</th></tr>
              $processHarokatRows
            </table>
        """.trimIndent()

        val addurulMapSvg = horizonPositionSvg(
            sunAzimuth = res.matahari.azm,
            sunAltitude = res.matahari.hm,
            moonAzimuth = res.azcMoon,
            moonAltitude = res.hcSathi,
            elongationDeg = res.eloSathi,
            illuminationText = String.format(Locale.US, "%.4f %%", res.nurulHilal),
            crescentWidthText = String.format(Locale.US, "%.2f°", res.buduZawiyah)
        )

        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="utf-8">
                <title>Laporan Hisab Awal Bulan (Addurul Aniq)</title>
                <style>
                    @page { size: A4; margin: 8mm; }
                    body { font-family: 'Helvetica Neue', Helvetica, Arial, sans-serif; font-size: 9px; margin: 0; color: #222; }
                    .header { text-align: center; border-bottom: 2px solid #2e7d32; padding-bottom: 5px; margin-bottom: 10px; }
                    .logo { max-width: 50px; margin-bottom: 5px; }
                    .title { font-size: 16px; font-weight: bold; color: #2e7d32; margin: 0; }
                    .subtitle { font-size: 11px; color: #555; margin-top: 3px; }
                    .section-title { font-size: 12px; font-weight: bold; margin-top: 8px; margin-bottom: 4px; color: #1565c0; border-bottom: 1px solid #1565c0; padding-bottom: 2px; text-transform: uppercase; }
                    table { width: 100%; border-collapse: collapse; margin-bottom: 7px; font-size: 8.5px; page-break-inside: avoid; }
                    tr, td, th, .keep { page-break-inside: avoid; break-inside: avoid; }
                    th, td { border: 1px solid #333; padding: 2px; text-align: center; background: #fff; }
                    th { font-weight: bold; }
                    td.text-left { text-align: left; }
                    td.text-right { text-align: right; font-weight: bold; }
                    .val { font-family: 'Courier New', Courier, monospace; }
                    .process .big-title { font-size: 15px; letter-spacing: 1px; }
                    .process .label { text-align: left; font-weight: bold; }
                    .process .key { font-weight: bold; }
                    .process .total td, .process .total { font-weight: bold; font-size: 11px; }
                    .bg-blue, .bg-blue-light, .bg-purple, .bg-purple-light, .bg-yellow, .bg-yellow-light, .bg-green, .bg-green-light { background-color: #fff; }
                    .concl { margin-top:8px; padding:7px; border:1px solid #1565c0; background:#fff; border-radius:4px;}
                    .concl p { margin: 0 0 5px 0; }
                    .date-title { color:#1565c0; font-size:11px; font-weight:bold; margin-top:5px; }
                    .date-final { font-size:13px; font-weight:bold; margin-top:3px; }
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

                $processIjtimaTable
                $processHarokatTable

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
                <div class="section-title">6. PETA POSISI HILAL</div>
                $addurulMapSvg
                <div class="section-title">7. KESIMPULAN AKHIR</div>
                <table>
                    <tr><td class='text-left'>Ijtima' akhir bulan</td><td class="val">${res.hariPasaran}, ${res.tgl} ${res.namaBulan} ${res.thn} M</td></tr>
                    <tr><td class='text-left'>Jam Ijtima'</td><td class="val">${formatHMS(res.waktuIjtimaLT)}</td></tr>
                    <tr><td class='text-left'>Matahari Terbenam</td><td class="val">${formatHMS(res.ghrbWdHaqiqi)}</td></tr>
                    <tr><td class='text-left'>Hilal Terbenam</td><td class="val">${formatHMS(res.ghurubHilal)}</td></tr>
                    <tr><td class='text-left'>Tinggi Hilal Mar'i</td><td class="val">${res.hcSathi.toDMS()}</td></tr>
                    <tr><td class='text-left'>Elongasi Hilal</td><td class="val">${res.eloMarkazi.toDMS()}</td></tr>
                    <tr><td class='text-left'>Nurul Hilal</td><td class="val">${String.format(Locale.US, "%.4f %%", res.nurulHilal)}</td></tr>
                </table>
                ${screenConclusionHtml(kesimpulanPenjelasan, kesimpulanDetail, kesimpulanJudulTanggal, kesimpulanTanggal)}
            </body>
            </html>
        """.trimIndent())

        val jobName = "Hisab_Awal_Bulan_Addurul_${System.currentTimeMillis()}"
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                try {
                    val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
                    if (printManager != null) {
                        val printAdapter = webView.createPrintDocumentAdapter(jobName)
                        printManager.print(jobName, printAdapter, printAttributesA4())
                    } else {
                        android.widget.Toast.makeText(context, "Layanan cetak tidak tersedia", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    android.util.Log.e("PencetakHisab", "Gagal cetak Addurul: ${e.message}", e)
                    android.widget.Toast.makeText(context, "Gagal mencetak: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        webView.loadDataWithBaseURL(null, sb.toString(), "text/HTML", "UTF-8", null)
    }
}
