package com.falak.falakpro

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.Priority
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// --- ENUMS & DATA MODELS ---

enum class LocationMode {
    OTOMATIS,
    MANUAL
}

enum class KriteriaHilal(val label: String) {
    YALLOP("Yallop"),
    MABIMS_BARU("MABIMS Baru / Neo-MABIMS (3° / 6,4°)"),
    MABIMS_LAMA("MABIMS Lama (2° / 3° / umur 8 jam)"),
    WUJUDUL_HILAL("Wujudul Hilal"),
    KGHT_TURKI("KGHT Turki / Turki Global (tinggi toposentrik 5° / elongasi geosentrik 8°)"),
    KGHT_MUHAMMADIYAH("KGHT Muhammadiyah (5° / 8° geosentrik-global)"),
    LAPAN("LAPAN (2° / 3°)"),
    ODEH("Odeh / Q Odeh"),
    DANJON("Danjon Limit (elongasi ≥ 7°)");

    val shortLabel: String
        get() = when (this) {
            YALLOP -> "Yallop"
            MABIMS_BARU -> "MABIMS Baru"
            MABIMS_LAMA -> "MABIMS Lama"
            WUJUDUL_HILAL -> "Wujudul Hilal"
            KGHT_TURKI -> "KGHT Turki"
            KGHT_MUHAMMADIYAH -> "KGHT Muhammadiyah"
            LAPAN -> "LAPAN"
            ODEH -> "Odeh"
            DANJON -> "Danjon"
        }
}

// --- COMMON HELPER FUNCTIONS ---

fun dmsToDecimal(deg: String, min: String, sec: String, dir: String): Double {
    val d = deg.toDoubleOrNull() ?: 0.0
    val m = min.toDoubleOrNull() ?: 0.0
    val s = sec.toDoubleOrNull() ?: 0.0
    var res = d + m / 60.0 + s / 3600.0
    if (dir == "LS" || dir == "BB" || dir == "S" || dir == "W" || dir == "Selatan" || dir == "Barat") res = -res
    return res
}

fun setDmsFromDecimal(decimal: Double, isLat: Boolean, onResult: (String, String, String, String) -> Unit) {
    val absVal = abs(decimal)
    val d = floor(absVal).toInt()
    val mFull = (absVal - d) * 60.0
    val m = floor(mFull).toInt()
    val s = (mFull - m) * 60.0

    val dir = if (isLat) {
        if (decimal >= 0) "LU" else "LS"
    } else {
        if (decimal >= 0) "BT" else "BB"
    }

    onResult(d.toString(), m.toString(), String.format(Locale.US, "%.1f", s), dir)
}

@SuppressLint("MissingPermission")
fun fetchLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    onResult: (Double, Double, Double, String) -> Unit
) {
    fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
        .addOnSuccessListener { loc ->
            if (loc != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    var locName = "Lokasi GPS"
                    try {
                        val geocoder = android.location.Geocoder(context, Locale("id", "ID"))
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val subLocality = addr.subLocality ?: ""
                            val locality = addr.locality ?: ""
                            val subAdmin = addr.subAdminArea ?: ""
                            
                            val parts = mutableListOf<String>()
                            
                            val cleanSubLoc = subLocality.replace("Kecamatan ", "", ignoreCase = true)
                                                         .replace("Desa ", "", ignoreCase = true)
                                                         .replace("Kelurahan ", "", ignoreCase = true).trim()
                            val cleanLocality = locality.replace("Kecamatan ", "", ignoreCase = true)
                                                        .replace("Kota ", "", ignoreCase = true).trim()
                            val cleanSubAdmin = subAdmin.replace("Kabupaten ", "", ignoreCase = true)
                                                        .replace("Kota ", "", ignoreCase = true).trim()

                            if (cleanSubLoc.isNotEmpty()) parts.add(cleanSubLoc)
                            if (cleanLocality.isNotEmpty() && cleanLocality != cleanSubLoc) parts.add("Kec. $cleanLocality")
                            if (cleanSubAdmin.isNotEmpty() && cleanSubAdmin != cleanLocality && cleanSubAdmin != cleanSubLoc) {
                                if (subAdmin.contains("Kota", ignoreCase = true)) {
                                    parts.add("Kota $cleanSubAdmin")
                                } else {
                                    parts.add("Kab. $cleanSubAdmin")
                                }
                            }
                            
                            if (parts.isNotEmpty()) {
                                locName = parts.joinToString(", ")
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                    withContext(Dispatchers.Main) {
                        onResult(loc.latitude, loc.longitude, loc.altitude, locName)
                    }
                }
            }
        }
}

// --- COMMON UI COMPONENTS ---

@Composable
fun ResultSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            content()
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, color: Color = Color.Unspecified) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = if (color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else color)
    }
}

// Dummy lists to satisfy imports until actual ones are found
val hijriMonths = listOf("Muharram", "Safar", "Rabiul Awwal", "Rabiul Akhir", "Jumadil Ula", "Jumadil Akhira", "Rajab", "Sya'ban", "Ramadhan", "Syawwal", "Dzulqa'dah", "Dzulhijjah")
val masehiMonths = listOf("Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")

// Kriteria evaluation logic
fun evaluateKriteria(res: com.falak.falakpro.AddurulAniq.IjtimaResult, kriteria: KriteriaHilal): Boolean {
    val ijtimaBeforeGhurub = res.waktuIjtimaLT < res.ghrbWdHaqiqi
    return when (kriteria) {
        KriteriaHilal.YALLOP -> ijtimaBeforeGhurub && yallopQ(res.bedaTinggi, res.eloSathi, res.sdcMoon) > -0.293
        KriteriaHilal.MABIMS_BARU -> res.hcSathi >= 3.0 && res.eloSathi >= 6.4
        KriteriaHilal.MABIMS_LAMA -> res.hcSathi >= 2.0 && res.eloSathi >= 3.0 && res.umurHilal >= 8.0
        KriteriaHilal.WUJUDUL_HILAL -> ijtimaBeforeGhurub && res.hcMarkazi > 0.0
        KriteriaHilal.KGHT_TURKI -> res.hcSathi >= 5.0 && res.eloMarkazi >= 8.0
        KriteriaHilal.KGHT_MUHAMMADIYAH -> res.hcMarkazi >= 5.0 && res.eloMarkazi >= 8.0
        KriteriaHilal.LAPAN -> res.hcSathi >= 2.0 && res.eloSathi >= 3.0
        KriteriaHilal.ODEH -> res.hcSathi >= 2.5
        KriteriaHilal.DANJON -> res.eloSathi >= 7.0 || res.eloMarkazi >= 7.0
    }
}

private fun yallopQ(arcV: Double, arcL: Double, moonSemiDiameterDeg: Double): Double {
    val w = moonSemiDiameterDeg.coerceAtLeast(0.0) * 60.0 *
        (1.0 - cos(Math.toRadians(arcL.coerceAtLeast(0.0))))
    return (arcV - (11.8371 - 6.3226 * w + 0.7319 * w * w - 0.1018 * w * w * w)) / 10.0
}
