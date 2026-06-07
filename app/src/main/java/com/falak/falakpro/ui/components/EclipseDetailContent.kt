package com.falak.falakpro.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Warning
import com.falak.falakpro.premium.*
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

private val NAMA_BULAN = listOf("Jan", "Feb", "Mar", "Apr", "Mei", "Jun", "Jul", "Ags", "Sep", "Okt", "Nov", "Des")
private fun formatDateString(jde: Double): String {
    val greg = CalendarFunctions.jdeToGregorian(jde)
    return "${greg.third.toInt()} ${NAMA_BULAN[greg.second - 1]} ${greg.first}"
}

@Composable
fun EclipseDetailContent(detail: EclipseDetail) {
    val scrollState = rememberScrollState()
    val tealPrimary = Color(0xFF00897B)
    val bgWhite = MaterialTheme.colorScheme.background
    val textDark = MaterialTheme.colorScheme.onSurface

    // Helper: ambil contact dari list berdasarkan nama
    fun cp(name: String) = detail.contacts.find { it.name == name }

    // Helper: format satu baris kontak untuk NasaTable (mengembalikan null jika contact null)
    fun contactRow(label: String, code: String, c: ContactPoint?): List<String>? {
        return if (c != null) {
            val ut = c.jdeTD - detail.deltaT / 86400.0
            listOf(label, code,
                formatJdeToTimeFull(c.jdeTD) + "\n" + formatDateString(c.jdeTD),
                formatJdeToTimeFull(ut) + "\n" + formatDateString(ut),
                formatDms(c.latitude,  isLat = true),
                formatDms(c.longitude, isLon = true),
                c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
            )
        } else null
    }

    val typeStr = when (detail.type) {
        EclipseParityEngine.EclipseType.TOTAL   -> "Total"
        EclipseParityEngine.EclipseType.ANNULAR -> "Cincin"
        EclipseParityEngine.EclipseType.HYBRID  -> "Hibrida (Cincin/Total)"
        EclipseParityEngine.EclipseType.PARTIAL -> "Sebagian"
        else -> "Gerhana Matahari"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgWhite)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            val dateStr = detail.contacts.firstOrNull()?.jdeTD?.let { formatDateString(it - detail.deltaT / 86400.0) } ?: ""
            Text("HISAB GERHANA MATAHARI GLOBAL" + if(dateStr.isNotEmpty()) " - $dateStr" else "", color = tealPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("T0 = ${detail.t0}", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.SemiBold)
            Text("Delta T = ${detail.deltaT} dtk", color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(horizontal = 12.dp), fontWeight = FontWeight.SemiBold)
        }

        Spacer(Modifier.height(16.dp))
        NasaSectionHeader("Karakteristik Gerhana")
        NasaTable(
            headers = listOf("Parameter", "Nilai"),
            rows = listOf(
                listOf("Jenis Gerhana",   typeStr),
                listOf("Magnitudo Gerhana", "%.5f".format(Locale.US, detail.magnitude)),
                listOf("Gamma",            "%.5f".format(Locale.US, detail.gamma))
            ),
            weights = listOf(1.6f, 1f)
        )

        Spacer(Modifier.height(16.dp))
        NasaSectionHeader("Kontak Bayangan dengan Bumi")
        val allContactRows = mutableListOf<List<String>>()
        contactRow("P1  Mulai Penumbra",        "P1", cp("P1"))?.let  { allContactRows.add(it) }
        contactRow("U1  Kontak Eksternal Umbra","U1", cp("U1"))?.let  { allContactRows.add(it) }
        contactRow("U2  Kontak Internal Umbra", "U2", cp("U2"))?.let  { allContactRows.add(it) }
        contactRow("Mx  Puncak Gerhana", "Mx", cp("Mx"))?.let { allContactRows.add(it) }
        contactRow("U3  Kontak Internal Umbra", "U3", cp("U3"))?.let  { allContactRows.add(it) }
        contactRow("U4  Kontak Eksternal Umbra","U4", cp("U4"))?.let  { allContactRows.add(it) }
        contactRow("P4  Akhir Penumbra",        "P4", cp("P4"))?.let  { allContactRows.add(it) }
        NasaScrollableTable(
            headers = listOf("Peristiwa Kontak", "Kode", "Waktu TD", "Waktu UT", "Lintang", "Bujur", "Sudut P", "Jarak Sumbu"),
            rows = allContactRows,
            columnWidths = listOf(175.dp, 45.dp, 90.dp, 90.dp, 110.dp, 110.dp, 75.dp, 85.dp)
        )

        Spacer(Modifier.height(16.dp))
        NasaSectionHeader("Koordinat Geosentris Matahari & Bulan saat Puncak")
        NasaTable(
            headers = listOf("Koordinat", "Matahari", "Bulan"),
            rows = listOf(
                listOf("Asensio Rekta (RA)",    formatHms(detail.sunRA),        formatHms(detail.moonRA)),
                listOf("Deklinasi (Dec)",       formatDmsSigned(detail.sunDec), formatDmsSigned(detail.moonDec)),
                listOf("Semi-Diameter (SD)",    formatDmsSigned(detail.sunSD),  formatDmsSigned(detail.moonSD)),
                listOf("Paralaks Hor. (HP)",    formatDmsSigned(detail.sunHP),  formatDmsSigned(detail.moonHP))
            ),
            weights = listOf(1.4f, 1f, 1f)
        )

        Spacer(Modifier.height(16.dp))
        NasaSectionHeader("Parameter Prediksi")
        NasaTable(
            headers = listOf("Parameter", "Nilai"),
            rows = listOf(
                listOf("Delta T (TD - UT)", "%.1f detik".format(detail.deltaT)),
                listOf("T0 (Jam UT)",  "%.1f".format(detail.t0)),
                listOf("Jenis Gerhana", typeStr)
            ),
            weights = listOf(1f, 1f)
        )

        Spacer(Modifier.height(16.dp))
        NasaSectionHeader("Elemen Besselian Polinomial")
        BesselianTable(detail.besselianTable)

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun LocalEclipseDetailContent(detail: LocalEclipseDetail) {
    val scrollState = rememberScrollState()
    val tealPrimary = Color(0xFF00897B)
    val bgWhite = MaterialTheme.colorScheme.background
    val textDark = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgWhite)
            .padding(12.dp)
            .verticalScroll(scrollState)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            Text("Eclipse Data: Local Circumstances", color = tealPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Time Zone: UT + ${detail.timezone}", color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))

        val u1Alt = detail.u1?.latitude ?: -999.0
        val u2Alt = detail.u2?.latitude ?: -999.0
        val u3Alt = detail.u3?.latitude ?: -999.0
        val u4Alt = detail.u4?.latitude ?: -999.0
        val mxAlt = detail.mx.latitude
        val maxAlt = maxOf(u1Alt, u2Alt, u3Alt, u4Alt, mxAlt)
        
        val visibilityWarning = when {
            detail.type == "TIDAK TERJADI GERHANA" -> "Tidak Terjadi Gerhana di Lokasi Ini."
            maxAlt <= 0.0 -> "Tidak Teramati: Seluruh proses gerhana terjadi saat Matahari berada di bawah ufuk (malam hari/tidak terlihat)."
            detail.u1 != null && u1Alt < 0.0 -> "Teramati Sebagian: Kontak awal gerhana terjadi di bawah ufuk. Matahari terbit saat gerhana sudah berlangsung."
            detail.u4 != null && u4Alt < 0.0 -> "Teramati Sebagian: Kontak akhir gerhana terjadi di bawah ufuk. Matahari terbenam saat gerhana sedang berlangsung."
            else -> null
        }
        
        if (visibilityWarning != null) {
            Surface(
                color = if (maxAlt <= 0.0 || detail.type == "TIDAK TERJADI GERHANA") Color(0xFFFFEBEE) else Color(0xFFFFF9C4),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (maxAlt <= 0.0 || detail.type == "TIDAK TERJADI GERHANA") Color(0xFFEF5350) else Color(0xFFFBC02D)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Warning",
                        tint = if (maxAlt <= 0.0 || detail.type == "TIDAK TERJADI GERHANA") Color(0xFFC62828) else Color(0xFFF57F17),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = visibilityWarning,
                        color = if (maxAlt <= 0.0 || detail.type == "TIDAK TERJADI GERHANA") Color(0xFFC62828) else Color(0xFF5D4037),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 16.sp
                    )
                }
            }
        }
        
        // 0. Observer Location - format sederhana seperti Home Screen
        NasaSectionHeader("Observer Location")
        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            if (detail.obsName.isNotBlank()) {
                Text(
                    text = detail.obsName,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = buildObsLine(detail.obsLat, detail.obsLon, detail.obsElev),
                color = Color.DarkGray,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace
            )
        }
        
        Spacer(Modifier.height(16.dp))
        
        // 1. Eclipse Characteristics
        NasaSectionHeader("Eclipse Characteristics")
        NasaTable(
            headers = listOf("Parameter", "Value"),
            rows = listOf(
                listOf("Eclipse Type", detail.type.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }),
                listOf("Eclipse Magnitude", "%.5f".format(detail.magnitude).replace(".", ",")),
                listOf("Eclipse Obscuration", "%.5f".format(detail.obscuration).replace(".", ",") + " %"),
                listOf("Duration of Eclipse", formatDuration(detail.u1?.jdeTD, detail.u4?.jdeTD)),
                listOf("Duration of Total/Annular", formatDuration(detail.u2?.jdeTD, detail.u3?.jdeTD))
            ),
            weights = listOf(1.6f, 1f)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 2. Contacts - 6 kolom (Alt, Az, P. Angle, Axis Dist)
        NasaSectionHeader("Local Contacts of Eclipse")
        val contactRows = mutableListOf<List<String>>()
        
        val ptStringLocal = { label: String, c: ContactPoint? ->
            if (c != null) {
                val locJde = c.jdeTD - detail.deltaT / 86400.0 + detail.timezone / 24.0
                listOf(
                    label,
                    formatJdeToTimeFull(locJde) + "\n" + formatDateString(locJde),
                    formatDm(c.latitude),
                    formatDm(c.longitude),
                    c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                    c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                )
            } else null
        }

        ptStringLocal("C1  Kontak Pertama", detail.u1)?.let { contactRows.add(it) }
        ptStringLocal("C2  Kontak Internal", detail.u2)?.let { contactRows.add(it) }
        ptStringLocal("Mx  Puncak Gerhana",  detail.mx)?.let { contactRows.add(it) }
        ptStringLocal("C3  Kontak Internal", detail.u3)?.let { contactRows.add(it) }
        ptStringLocal("C4  Kontak Akhir",    detail.u4)?.let { contactRows.add(it) }

        NasaScrollableTable(
            headers = listOf("Kontak", "Waktu (LT)", "Alt", "Az", "P. Angle", "Axis Dist"),
            rows = contactRows,
            columnWidths = listOf(140.dp, 90.dp, 75.dp, 75.dp, 75.dp, 75.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 3. Sunrise, Transit, Sunset
        NasaSectionHeader("Sunrise, Transit, and Sunset (Local Time)")
        NasaTable(
            headers = listOf("Event", "Time"),
            rows = listOf(
                listOf("Sunrise", detail.sunrise),
                listOf("Solar Transit", detail.transit),
                listOf("Sunset", detail.sunset)
            ),
            weights = listOf(1f, 1f)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 4. Coordinates
        NasaSectionHeader("Geocentric Coordinates of Sun and Moon at Maximum")
        NasaTable(
            headers = listOf("Coordinate", "Sun", "Moon"),
            rows = listOf(
                listOf("Right Ascension", formatHms(detail.sunRA), formatHms(detail.moonRA)),
                listOf("Declination", formatDmsSigned(detail.sunDec), formatDmsSigned(detail.moonDec)),
                listOf("Semi-Diameter", formatDmsSigned(detail.sunSD), formatDmsSigned(detail.moonSD)),
                listOf("Eq. Hor. Parallax", formatDmsSigned(detail.sunHP), formatDmsSigned(detail.moonHP))
            ),
            weights = listOf(1f, 1f, 1f)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 5. Prediction Parameters
        NasaSectionHeader("Prediction Parameters")
        NasaTable(
            headers = listOf("Parameter", "Value"),
            rows = listOf(
                listOf("Delta T", "%.2f s".format(detail.deltaT).replace(".", ",")),
                listOf("T0", "%.1f".format(detail.t0).replace(".", ",")),
                listOf("Tan f1 (Penumbra)", "%.7f".format(detail.tanf1).replace(".", ",")),
                listOf("Tan f2 (Umbra)", "%.7f".format(detail.tanf2).replace(".", ","))
            ),
            weights = listOf(1f, 1f)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 6. Besselian Elements
        NasaSectionHeader("Polynomial Besselian Elements")
        BesselianTable(detail.besselianTable)
        
        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun NasaSectionHeader(title: String) {
    val tealPrimary = Color(0xFF00897B)
    Text(
        text = title,
        color = tealPrimary,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp, top = 8.dp)
    )
    HorizontalDivider(color = Color(0xFFB2DFDB), thickness = 1.dp, modifier = Modifier.padding(bottom = 8.dp))
}

@Composable
fun NasaScrollableTable(
    headers: List<String>,
    rows: List<List<String>>,
    columnWidths: List<Dp>
) {
    val scrollState = rememberScrollState()
    val tealPrimary = Color(0xFF00897B)
    val textDark = MaterialTheme.colorScheme.onSurface
    val dividerColor = Color(0xFFE0E0E0)
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB2DFDB)), shape = RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surface)
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .background(Color(0xFFE0F2F1))
                    .padding(vertical = 8.dp, horizontal = 4.dp)
            ) {
                headers.forEachIndexed { index, header ->
                    Text(
                        text = header,
                        modifier = Modifier.width(columnWidths[index]),
                        color = tealPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                    )
                }
            }
            HorizontalDivider(color = Color(0xFFB2DFDB), thickness = 1.dp)
            
            // Data rows
            rows.forEachIndexed { rowIndex, row ->
                Row(
                    modifier = Modifier
                        .background(if (rowIndex % 2 == 1) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface)
                        .padding(vertical = 8.dp, horizontal = 4.dp)
                ) {
                    row.forEachIndexed { index, value ->
                        Text(
                            text = value,
                            modifier = Modifier.width(columnWidths[index]),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                        )
                    }
                }
                if (rowIndex < rows.size - 1) {
                    HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
                }
            }
        }
    }
}

@Composable
fun NasaTable(headers: List<String>, rows: List<List<String>>, weights: List<Float>) {
    val tealPrimary = Color(0xFF00897B)
    val textDark = MaterialTheme.colorScheme.onSurface
    val dividerColor = Color(0xFFE0E0E0)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Header Row
        Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
            headers.forEachIndexed { index, header ->
                Text(
                    text = header,
                    modifier = Modifier.weight(weights[index]),
                    color = tealPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                )
            }
        }
        HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
        
        // Data Rows
        rows.forEach { row ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                row.forEachIndexed { index, value ->
                    Text(
                        text = value,
                        modifier = Modifier.weight(weights[index]),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = if (index == 0) TextAlign.Start else TextAlign.Center
                    )
                }
            }
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
        }
    }
}

@Composable
fun ContactItem(c: ContactPoint, deltaT: Double, timezone: Double = 0.0, tzLabel: String = "UT") {
    val jdeUT  = c.jdeTD - deltaT / 86400.0
    val jdeLoc = jdeUT + timezone / 24.0
    val tealPrimary = Color(0xFF00897B)
    val textDark = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (c.name == "Mx") tealPrimary else Color(0xFFB2DFDB)),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.name, color = tealPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Column(horizontalAlignment = androidx.compose.ui.Alignment.End) {
                    Text("TD: ${formatJdeToTimeFull(c.jdeTD)}", color = Color.Gray, fontSize = 11.sp)
                    Text("$tzLabel: ${formatJdeToTimeFull(jdeLoc)}", color = MaterialTheme.colorScheme.onSurface, fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (!c.latitude.isNaN()) {
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                    Column {
                        Text("Lintang: ${formatDms(c.latitude, isLat = true)}", color = Color.DarkGray, fontSize = 11.sp)
                        Text("Bujur  : ${formatDms(c.longitude, isLon = true)}", color = Color.DarkGray, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    val textDark = MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = Color.DarkGray, fontSize = 12.sp)
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun SunMoonDataTable(
    sunRA: Double, sunDec: Double, sunSD: Double, sunHP: Double,
    moonRA: Double, moonDec: Double, moonSD: Double, moonHP: Double
) {
    val tealPrimary = Color(0xFF00897B)
    Column(modifier = Modifier.fillMaxWidth().padding(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Text("MATAHARI", color = tealPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text("BULAN", color = tealPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        DataRow("RA", formatHms(sunRA), formatHms(moonRA))
        DataRow("Dc", formatDmsSigned(sunDec), formatDmsSigned(moonDec))
        DataRow("SD", formatDmsSigned(sunSD), formatDmsSigned(moonSD))
        DataRow("HP", formatDmsSigned(sunHP), formatDmsSigned(moonHP))
    }
}

@Composable
fun DataRow(label: String, val1: String, val2: String) {
    val textDark = MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, modifier = Modifier.weight(0.5f), color = Color.DarkGray, fontSize = 11.sp)
        Text(val1, modifier = Modifier.weight(2f), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
        Text(val2, modifier = Modifier.weight(2f), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, textAlign = TextAlign.Center, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun BesselianTable(rows: List<BesselianRow>, isLunar: Boolean = false) {
    val scrollState = rememberScrollState()
    val headers = if (isLunar) listOf("Orde", "x", "y", "d", "f1", "f2", "f3") else listOf("Orde", "x", "y", "d", "L1", "L2", "mu")
    val tealPrimary = Color(0xFF00897B)
    val textDark = MaterialTheme.colorScheme.onSurface
    val dividerColor = Color(0xFFE0E0E0)
    
    Column(modifier = Modifier.horizontalScroll(scrollState).background(MaterialTheme.colorScheme.surface).border(androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFB2DFDB)))) {
        Row(Modifier.background(tealPrimary).padding(vertical = 8.dp, horizontal = 4.dp)) {
            headers.forEach {
                Text(text = it, modifier = Modifier.width(if (it == "Orde") 40.dp else 75.dp), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
        rows.forEach { row ->
            Row(Modifier.padding(vertical = 6.dp, horizontal = 4.dp)) {
                Text(text = row.orde.toString(), modifier = Modifier.width(40.dp), color = tealPrimary, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(text = "%.5f".format(Locale.US, row.x), modifier = Modifier.width(75.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                Text(text = "%.5f".format(Locale.US, row.y), modifier = Modifier.width(75.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                Text(text = "%.5f".format(Locale.US, row.d), modifier = Modifier.width(75.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                Text(text = "%.5f".format(Locale.US, row.L1), modifier = Modifier.width(75.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                Text(text = "%.5f".format(Locale.US, row.L2), modifier = Modifier.width(75.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
                Text(text = "%.5f".format(Locale.US, row.mu), modifier = Modifier.width(75.dp), color = MaterialTheme.colorScheme.onSurface, fontSize = 11.sp, fontFamily = FontFamily.Monospace, textAlign = TextAlign.Center)
            }
            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)
        }
    }
}

fun formatHms(degrees: Double): String {
    val h = floor(degrees / 15.0).toInt()
    val m = floor((degrees / 15.0 - h) * 60.0).toInt()
    val s = ((degrees / 15.0 - h) * 60.0 - m) * 60.0
    return "+%02dh %02dm %04.1fs".format(h, m, s).replace(".", ",")
}

fun formatDmsSigned(degrees: Double): String {
    val absDeg = abs(degrees)
    val d = floor(absDeg).toInt()
    val m = floor((absDeg - d) * 60.0).toInt()
    val s = ((absDeg - d) * 60.0 - m) * 60.0
    val sign = if (degrees >= 0) "+" else "-"
    return "%s%02d° %02d' %04.1f''".format(sign, d, m, s).replace(".", ",")
}

/** Format sudut hanya derajat + menit (tanpa detik): +05° 31' */
fun formatDm(degrees: Double): String {
    val sign = if (degrees >= 0) "+" else "-"
    val abs = kotlin.math.abs(degrees)
    val d = abs.toInt()
    val m = ((abs - d) * 60).toInt()
    return "%s%02d° %02d'".format(sign, d, m)
}

/** Format koordinat observer dalam satu baris: 06°18'48" LS, 107°19'15" BT, 37 m */
fun buildObsLine(lat: Double, lon: Double, elev: Double): String {
    fun dms(deg: Double, isLat: Boolean): String {
        val a = kotlin.math.abs(deg)
        val d = a.toInt()
        val m = ((a - d) * 60).toInt()
        val s = ((a - d) * 60 - m) * 60
        val dir = if (isLat) (if (deg >= 0) "LU" else "LS") else (if (deg >= 0) "BT" else "BB")
        return "%02d\u00b0%02d'%02.0f\" %s".format(d, m, s, dir)
    }
    val elevStr = "%.0f m".format(elev)
    return "${dms(lat, true)}, ${dms(lon, false)}, $elevStr"
}

fun formatDuration(jdeStart: Double?, jdeEnd: Double?): String {
    if (jdeStart == null || jdeEnd == null) return "00:00:00"
    val diff = (jdeEnd - jdeStart) * 24.0
    val h = floor(diff).toInt()
    val m = floor((diff - h) * 60.0).toInt()
    val s = round(((diff - h) * 60.0 - m) * 60.0).toInt()
    return "%02d:%02d:%02d".format(h, m, s)
}

fun formatJdeToTimeFull(jde: Double): String {
    val h = (jde + 0.5) % 1.0 * 24.0
    val hh = floor(h).toInt()
    val mm = floor((h - hh) * 60.0).toInt()
    val ss = ((h - hh) * 60.0 - mm) * 60.0
    return "%02d:%02d:%04.1f".format(hh, mm, ss).replace(".", ",")
}

fun formatDms(deg: Double, isLat: Boolean = false, isLon: Boolean = false): String {
    val absDeg = abs(deg)
    val d = absDeg.toInt()
    val m = ((absDeg - d) * 60.0).toInt()
    val s = ((absDeg - d) * 60.0 - m) * 60.0
    val symbol = when {
        isLat -> if (deg >= 0) " N" else " S"
        isLon -> if (deg >= 0) " E" else " W"
        else -> if (deg >= 0) "+" else "-"
    }
    return if (isLat || isLon) {
        "%02d° %02d' %04.1f\"%s".format(d, m, s, symbol).replace(".", ",")
    } else {
        "%s%02d° %02d' %04.1f\"".format(symbol, d, m, s).replace(".", ",")
    }
}

@Composable
fun LunarDetailContent(detail: LunarEclipseDetail) {
    val scrollState = rememberScrollState()
    val tealPrimary = Color(0xFF00897B)
    val bgWhite = MaterialTheme.colorScheme.background
    val textDark = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgWhite)
            .padding(12.dp)
            .verticalScroll(scrollState)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
            val dateStr = detail.mx.jdeTD.let { formatDateString(it - detail.deltaT / 86400.0) }
            Text("HISAB GERHANA BULAN" + if(dateStr.isNotEmpty()) " - $dateStr" else "", color = tealPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text("Prediction Ephemerides: VSOP87 / ELP-MPP02", color = Color.Gray, fontSize = 12.sp)
            if (detail.tzLabel.isNotBlank() && detail.tzLabel != "UT")
                Text("Zona Waktu: ${detail.tzLabel} (UT+${detail.timezone.toInt()})", color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))
        
        // 1. Karakteristik Gerhana
        NasaSectionHeader("Karakteristik Gerhana")
        NasaTable(
            headers = listOf("Parameter", "Nilai"),
            rows = listOf(
                listOf("Jenis Gerhana", when (detail.type) {
                    "TOTAL" -> "Total"
                    "SEBAGIAN" -> "Sebagian"
                    "PENUMBRAL" -> "Penumbral"
                    else -> "Tidak Terjadi"
                }),
                listOf("Magnitudo Penumbral", "%.4f".format(detail.magPenumbra).replace(".", ",")),
                listOf("Magnitudo Umbral",    "%.4f".format(detail.magUmbra).replace(".", ",")),
                listOf("Gamma",               "%.4f".format(detail.gamma).replace(".", ",")),
                listOf("Epsilon",             "%.4f°".format(detail.epsilon).replace(".", ","))
            ),
            weights = listOf(1.5f, 1f)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 2. Contacts - tampilkan Waktu Lokal
        NasaSectionHeader("Kontak Bayangan dengan Bumi")
        val contactRows = mutableListOf<List<String>>()
        val localTzLabel = if (detail.tzLabel.isNotBlank()) detail.tzLabel else "UT+${detail.timezone.toInt()}"
        
        // Helper: konversi TD → Waktu Lokal (UT + timezone)
        val ptString = { event: String, code: String, c: ContactPoint? ->
            if (c != null) {
                val utJde  = c.jdeTD - detail.deltaT / 86400.0
                val locJde = utJde + detail.timezone / 24.0
                val tzSuffix = if (detail.tzLabel == "UT") "UT" else "LOKAL"
                listOf(
                    event,
                    code,
                    formatJdeToTimeFull(c.jdeTD) + "\n" + formatDateString(c.jdeTD),             // TD (referensi)
                    formatJdeToTimeFull(locJde) + "\n" + formatDateString(locJde),             // Waktu Lokal
                    c.zenithLat?.let { formatDms(it, isLat = true) } ?: "-",
                    c.zenithLon?.let { formatDms(it, isLon = true) } ?: "-",
                    c.positionAngle?.let { String.format(java.util.Locale.US, "%.1f°", it) } ?: "-",
                    c.axisDistance?.let { String.format(java.util.Locale.US, "%.4f°", it) } ?: "-"
                )
            } else null
        }

        ptString("P1  Mulai Penumbra",         "P1", detail.p1)?.let { contactRows.add(it) }
        ptString("U1  Mulai Gerhana Sebagian", "U1", detail.u1)?.let { contactRows.add(it) }
        ptString("U2  Mulai Gerhana Total",    "U2", detail.u2)?.let { contactRows.add(it) }
        ptString("Mx  Puncak Gerhana",         "Mx", detail.mx)?.let { contactRows.add(it) }
        ptString("U3  Akhir Gerhana Total",    "U3", detail.u3)?.let { contactRows.add(it) }
        ptString("U4  Akhir Gerhana Sebagian", "U4", detail.u4)?.let { contactRows.add(it) }
        ptString("P4  Akhir Penumbra",         "P4", detail.p4)?.let { contactRows.add(it) }

        NasaScrollableTable(
            headers = listOf("Peristiwa Kontak", "Kode", "Waktu TD", "Waktu $localTzLabel", "Lintang Zenith", "Bujur Zenith", "Sudut P", "Jarak Sumbu"),
            rows = contactRows,
            columnWidths = listOf(160.dp, 50.dp, 90.dp, 90.dp, 130.dp, 130.dp, 75.dp, 75.dp)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 3. Durasi Gerhana
        NasaSectionHeader("Durasi Gerhana")
        val durRows = mutableListOf<List<String>>()
        val formatDur = { c1: ContactPoint?, c2: ContactPoint? -> formatDuration(c1?.jdeTD, c2?.jdeTD) }
        
        durRows.add(listOf("Penumbral (P4 - P1)", formatDur(detail.p1, detail.p4)))
        if (detail.u1 != null && detail.u4 != null) {
            durRows.add(listOf("Sebagian (U4 - U1)", formatDur(detail.u1, detail.u4)))
        }
        if (detail.u2 != null && detail.u3 != null) {
            durRows.add(listOf("Total (U3 - U2)", formatDur(detail.u2, detail.u3)))
        }
        NasaTable(
            headers = listOf("Fase Gerhana", "Durasi"),
            rows = durRows,
            weights = listOf(1.6f, 1f)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 4. Koordinat Geosentris
        NasaSectionHeader("Koordinat Geosentris Matahari & Bulan saat Puncak")
        NasaTable(
            headers = listOf("Koordinat", "Matahari", "Bulan"),
            rows = listOf(
                listOf("Asensio Rekta (RA)",  formatHms(detail.sunRA),        formatHms(detail.moonRA)),
                listOf("Deklinasi (Dec)",      formatDmsSigned(detail.sunDec), formatDmsSigned(detail.moonDec)),
                listOf("Semi-Diameter (SD)",   formatDmsSigned(detail.sunSD),  formatDmsSigned(detail.moonSD)),
                listOf("Paralaks Hor. (HP)",   formatDmsSigned(detail.sunHP),  formatDmsSigned(detail.moonHP))
            ),
            weights = listOf(1.2f, 1f, 1f)
        )
        
        Spacer(Modifier.height(16.dp))
        
        // 5. Bayangan Bumi & Parameter Prediksi
        NasaSectionHeader("Bayangan Bumi & Parameter Prediksi")
        NasaTable(
            headers = listOf("Parameter", "Nilai"),
            rows = listOf(
                listOf("Jari-jari Penumbra",  "%.4f°".format(detail.rPenumbra).replace(".", ",")),
                listOf("Jari-jari Umbra",     "%.4f°".format(detail.rUmbra).replace(".", ",")),
                listOf("Delta T (TD - UT)",        "%.1f detik".format(detail.deltaT).replace(".", ",")),
                listOf("Aturan Bayangan",     detail.shadowRule),
                listOf("Pembesaran Bayangan", detail.shadowEnlargement),
                listOf("Seri Saros",          detail.sarosSeries)
            ),
            weights = listOf(1.5f, 1f)
        )
        
        Spacer(Modifier.height(16.dp))

        // 6. Elemen Besselian Polinomial
        NasaSectionHeader("Elemen Besselian Polinomial")
        BesselianTable(detail.besselianTable, isLunar = true)

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
fun CombinedSolarEclipseDetailContent(local: LocalEclipseDetail, global: EclipseDetail) {
    val scrollState = rememberScrollState()
    val tealPrimary = Color(0xFF00897B)
    val bgWhite = MaterialTheme.colorScheme.background
    val textDark = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgWhite)
            .padding(16.dp)
            .verticalScroll(scrollState)
    ) {
        // --- TITLE ---
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            val dateStr = local.mx.jdeTD.let { formatDateString(it - local.deltaT / 86400.0) }
            Text("DATA GERHANA MATAHARI LOKAL" + if(dateStr.isNotEmpty()) " - $dateStr" else "", color = tealPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Text("Integrasi Parameter Lokal & Global", color = Color.Gray, fontSize = 12.sp)
        }
        Spacer(Modifier.height(16.dp))

        // --- SECTION 1: KEADAAN LOKAL ---
        NasaSectionHeader("Keadaan Lokal (Local Circumstances)")
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Location Details
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, tint = tealPrimary, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (local.obsName.isNotBlank()) local.obsName else "Lokasi Pengamat",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = buildObsLine(local.obsLat, local.obsLon, local.obsElev) + " | TZ: UT+${local.timezone}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.DarkGray
                )
                
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))

                if (local.type == "TIDAK TERJADI GERHANA") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFEBEE), shape = RoundedCornerShape(8.dp))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "GERHANA TIDAK TERLIHAT DARI LOKASI INI",
                            color = Color(0xFFC62828),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    // Local Eclipse Info
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("Tipe Lokal", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = local.type.lowercase().split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } },
                                fontWeight = FontWeight.Bold,
                                color = tealPrimary,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Magnitudo", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "%.5f".format(Locale.US, local.magnitude),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text("Obscuration", fontSize = 11.sp, color = Color.Gray)
                            Text(
                                text = "%.5f".format(Locale.US, local.obscuration) + " %",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                        }
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    Text("Durasi Kontak Lokal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Durasi Gerhana: ${formatDuration(local.u1?.jdeTD, local.u4?.jdeTD)}", fontSize = 12.sp, color = Color.DarkGray)
                        Text("Durasi Total/Cincin: ${formatDuration(local.u2?.jdeTD, local.u3?.jdeTD)}", fontSize = 12.sp, color = Color.DarkGray)
                    }

                    Spacer(Modifier.height(16.dp))
                    
                    // Local Contacts Table
                    val contactRows = mutableListOf<List<String>>()
                    val ptStringLocal = { label: String, c: ContactPoint? ->
                        if (c != null) {
                            listOf(
                                label,
                                formatJdeToTimeFull(c.jdeTD + local.timezone / 24.0),
                                formatDm(c.latitude),
                                formatDm(c.longitude),
                                c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                                c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                            )
                        } else null
                    }
                    ptStringLocal("C1  Kontak Pertama", local.u1)?.let { contactRows.add(it) }
                    ptStringLocal("C2  Kontak Internal", local.u2)?.let { contactRows.add(it) }
                    ptStringLocal("Mx  Puncak Gerhana",  local.mx)?.let { contactRows.add(it) }
                    ptStringLocal("C3  Kontak Internal", local.u3)?.let { contactRows.add(it) }
                    ptStringLocal("C4  Kontak Akhir",    local.u4)?.let { contactRows.add(it) }

                    Text("Kontak Lokal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    NasaScrollableTable(
                        headers = listOf("Kontak", "Waktu (LT)", "Alt", "Az", "P. Angle", "Axis Dist"),
                        rows = contactRows,
                        columnWidths = listOf(140.dp, 90.dp, 75.dp, 75.dp, 75.dp, 75.dp)
                    )

                    Spacer(Modifier.height(16.dp))
                    // Sunrise, Transit, Sunset
                    Text("Keadaan Matahari Lokal:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(6.dp))
                    NasaTable(
                        headers = listOf("Peristiwa", "Waktu Lokal"),
                        rows = listOf(
                            listOf("Terbit Matahari", local.sunrise),
                            listOf("Transit (Tengah Hari)", local.transit),
                            listOf("Terbenam Matahari", local.sunset)
                        ),
                        weights = listOf(1f, 1f)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- SECTION 2: KEADAAN GLOBAL ---
        NasaSectionHeader("Keadaan Global (Global Circumstances)")
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                val typeStr = when (global.type) {
                    EclipseParityEngine.EclipseType.TOTAL   -> "Total"
                    EclipseParityEngine.EclipseType.ANNULAR -> "Cincin"
                    EclipseParityEngine.EclipseType.HYBRID  -> "Hibrida (Cincin/Total)"
                    EclipseParityEngine.EclipseType.PARTIAL -> "Sebagian"
                    else -> "Gerhana Matahari"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Tipe Global", fontSize = 11.sp, color = Color.Gray)
                        Text(typeStr, fontWeight = FontWeight.Bold, color = tealPrimary, fontSize = 14.sp)
                    }
                    Column {
                        Text("Magnitudo Maks", fontSize = 11.sp, color = Color.Gray)
                        Text("%.5f".format(Locale.US, global.magnitude), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                    Column {
                        Text("Gamma", fontSize = 11.sp, color = Color.Gray)
                        Text("%.5f".format(Locale.US, global.gamma), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                
                // Helper to search contact
                val cp = { name: String -> global.contacts.find { it.name == name } }
                val contactRow = { label: String, code: String, c: ContactPoint? ->
                    if (c != null) {
                        val ut = c.jdeTD - global.deltaT / 86400.0
                        listOf(label, code,
                            formatJdeToTimeFull(c.jdeTD) + "\n" + formatDateString(c.jdeTD),
                            formatJdeToTimeFull(ut) + "\n" + formatDateString(ut),
                            formatDms(c.latitude,  isLat = true),
                            formatDms(c.longitude, isLon = true),
                            c.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                            c.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                        )
                    } else {
                        listOf(label, code, "-", "-", "-", "-", "-", "-")
                    }
                }

                Text("Kontak Penumbra dengan Bumi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                NasaScrollableTable(
                    headers = listOf("Kontak", "Kode", "Waktu TD", "Waktu UT", "Latitude", "Longitude", "P. Angle", "Axis Dist"),
                    rows = listOf(
                        contactRow("Mulai Penumbra (P1)", "P1", cp("P1")),
                        contactRow("Akhir Penumbra (P4)", "P4", cp("P4"))
                    ),
                    columnWidths = listOf(160.dp, 50.dp, 90.dp, 90.dp, 130.dp, 130.dp, 75.dp, 75.dp)
                )

                Spacer(Modifier.height(16.dp))
                Text("Kontak Umbra dengan Bumi:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                NasaScrollableTable(
                    headers = listOf("Kontak", "Kode", "Waktu TD", "Waktu UT", "Latitude", "Longitude", "P. Angle", "Axis Dist"),
                    rows = listOf(
                        contactRow("Eksternal Umbra (U1)", "U1", cp("U1")),
                        contactRow("Internal Umbra (U2)", "U2", cp("U2")),
                        contactRow("Internal Umbra (U3)", "U3", cp("U3")),
                        contactRow("Eksternal Umbra (U4)", "U4", cp("U4"))
                    ),
                    columnWidths = listOf(160.dp, 50.dp, 90.dp, 90.dp, 130.dp, 130.dp, 75.dp, 75.dp)
                )

                Spacer(Modifier.height(16.dp))
                Text("Puncak Gerhana Global (Greatest Eclipse):", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(6.dp))
                val mx = cp("Mx")
                if (mx != null) {
                    val mxUT = mx.jdeTD - global.deltaT / 86400.0
                    NasaScrollableTable(
                        headers = listOf("Kontak", "Waktu TD", "Waktu UT", "Latitude", "Longitude", "P. Angle", "Axis Dist"),
                        rows = listOf(
                            listOf("Puncak (Greatest)",
                                formatJdeToTimeFull(mx.jdeTD) + "\n" + formatDateString(mx.jdeTD),
                                formatJdeToTimeFull(mxUT) + "\n" + formatDateString(mxUT),
                                formatDms(mx.latitude,  isLat = true),
                                formatDms(mx.longitude, isLon = true),
                                mx.positionAngle?.let { String.format(Locale.US, "%.1f°", it) } ?: "-",
                                mx.axisDistance?.let { String.format(Locale.US, "%.4f°", it) } ?: "-"
                            )
                        ),
                        columnWidths = listOf(130.dp, 90.dp, 90.dp, 130.dp, 130.dp, 75.dp, 75.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- SECTION 3: KOORDINAT GEOSENTRIS ---
        NasaSectionHeader("Koordinat Geosentris saat Puncak Gerhana")
        NasaTable(
            headers = listOf("Parameter", "Matahari", "Bulan"),
            rows = listOf(
                listOf("Right Ascension (RA)", formatHms(global.sunRA), formatHms(global.moonRA)),
                listOf("Declination (Dec)", formatDmsSigned(global.sunDec), formatDmsSigned(global.moonDec)),
                listOf("Semi-Diameter (SD)", formatDmsSigned(global.sunSD), formatDmsSigned(global.moonSD)),
                listOf("Hor. Parallax (HP)", formatDmsSigned(global.sunHP), formatDmsSigned(global.moonHP))
            ),
            weights = listOf(1.2f, 1f, 1f)
        )

        Spacer(Modifier.height(16.dp))

        // --- SECTION 4: BESSELIAN ELEMENTS & PREDICTION PARAMETERS ---
        NasaSectionHeader("Prediction & Besselian Elements")
        NasaTable(
            headers = listOf("Parameter", "Nilai"),
            rows = listOf(
                listOf("Delta T (TD - UT)", "%.2f detik".format(Locale.US, global.deltaT)),
                listOf("T0 (Jam UTC)", "%.1f".format(Locale.US, global.t0)),
                listOf("Tan f1 (Penumbra)", "%.7f".format(Locale.US, local.tanf1)),
                listOf("Tan f2 (Umbra)", "%.7f".format(Locale.US, local.tanf2))
            ),
            weights = listOf(1.2f, 1f)
        )
        
        Spacer(Modifier.height(12.dp))
        Text("Elemen Besselian Polynomial:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(6.dp))
        BesselianTable(global.besselianTable)

        Spacer(Modifier.height(40.dp))
    }
}

