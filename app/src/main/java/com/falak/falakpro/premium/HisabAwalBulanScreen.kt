package com.falak.falakpro.premium

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.falak.falakpro.AddurulAniq.AddurulAniqEngine
import com.falak.falakpro.AddurulAniq.IjtimaResult
import com.falak.falakpro.KriteriaHilal
import com.falak.falakpro.LocationMode
import com.falak.falakpro.dmsToDecimal
import com.falak.falakpro.evaluateKriteria
import com.falak.falakpro.fetchLocation
import com.falak.falakpro.hijriMonths
import com.falak.falakpro.masehiMonths
import com.falak.falakpro.setDmsFromDecimal
import com.falak.falakpro.ui.ChoiceChip
import com.falak.falakpro.ui.components.NasaTable
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.round

// Helper functions for Addurul Aniq rendering in this screen
private fun formatHMS(hours: Double): String {
    val neg = hours < 0
    val totalSeconds = round(abs(hours) * 3600).toInt()
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val sign = if (neg) "- " else ""
    return String.format(Locale.US, "%s%02d:%02d:%02d", sign, h, m, s)
}

private fun formatTimeOfDay(hours: Double, is24Hour: Boolean): String {
    var h = floor(hours).toInt()
    val m = floor((hours - h) * 60).toInt()
    val s = round((hours - h - m / 60.0) * 3600).toInt()
    if (is24Hour) {
        return String.format(Locale.US, "%02d:%02d:%02d", h % 24, m, s)
    } else {
        val amPm = if ((h % 24) >= 12) "PM" else "AM"
        val h12 = if (h % 12 == 0) 12 else h % 12
        return String.format(Locale.US, "%02d:%02d:%02d %s", h12, m, s, amPm)
    }
}

private fun formatHMSIndo(hours: Double): String {
    val neg = hours < 0
    val totalSeconds = round(abs(hours) * 3600).toInt()
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val sign = if (neg) "- " else ""
    return "$sign$h Jam $m menit $s detik"
}


private data class KesimpulanAwalBulanUi(
    val terpenuhi: Boolean,
    val tanggalAwalBulan: LocalDate,
    val umurBulanSebelumnya: Int,
    val penjelasan: String,
    val kghtDetailText: String? = null,
    val judulTanggal: String,
    val teksTanggal: String
)

private fun namaHariIndonesia(date: LocalDate): String = when (date.dayOfWeek) {
    java.time.DayOfWeek.SUNDAY -> "Minggu"
    java.time.DayOfWeek.MONDAY -> "Senin"
    java.time.DayOfWeek.TUESDAY -> "Selasa"
    java.time.DayOfWeek.WEDNESDAY -> "Rabu"
    java.time.DayOfWeek.THURSDAY -> "Kamis"
    java.time.DayOfWeek.FRIDAY -> "Jum'at"
    java.time.DayOfWeek.SATURDAY -> "Sabtu"
}

private fun namaBulanMasehiIndonesia(month: Int): String = when (month) {
    1 -> "Januari"; 2 -> "Februari"; 3 -> "Maret"; 4 -> "April"
    5 -> "Mei"; 6 -> "Juni"; 7 -> "Juli"; 8 -> "Agustus"
    9 -> "September"; 10 -> "Oktober"; 11 -> "November"; 12 -> "Desember"
    else -> ""
}

private fun namaPasaranIndonesia(date: LocalDate): String {
    val jd = CalendarFunctions.gregorianToJde(date.year, date.monthValue, date.dayOfMonth.toDouble())
    val idx = ((floor(jd + 0.5).toLong() + 1) % 5).toInt()
    return listOf("Kliwon", "Legi", "Pahing", "Pon", "Wage")[idx]
}


private fun kriteriaKeyUntukAwalBulan(kriteria: KriteriaHilal): String = when (kriteria) {
    KriteriaHilal.YALLOP -> "Yallop"
    KriteriaHilal.MABIMS_BARU -> "Mabims Baru"
    KriteriaHilal.MABIMS_LAMA -> "Mabims Lama"
    KriteriaHilal.WUJUDUL_HILAL -> "Wujudul Hilal"
    KriteriaHilal.KGHT_TURKI -> "KGHT Turki"
    KriteriaHilal.KGHT_MUHAMMADIYAH -> "KGHT Muhammadiyah"
    KriteriaHilal.LAPAN -> "LAPAN"
    KriteriaHilal.ODEH -> "ODEH"
    KriteriaHilal.DANJON -> "Danjon Limit"
}

private fun modePetaUntukKriteria(kriteria: KriteriaHilal): HilalVisibilityMapMode = when (kriteria) {
    KriteriaHilal.YALLOP -> HilalVisibilityMapMode.YALLOP
    KriteriaHilal.MABIMS_BARU -> HilalVisibilityMapMode.MABIMS_BARU
    KriteriaHilal.MABIMS_LAMA -> HilalVisibilityMapMode.MABIMS_LAMA
    KriteriaHilal.WUJUDUL_HILAL -> HilalVisibilityMapMode.WUJUDUL_HILAL
    KriteriaHilal.KGHT_TURKI -> HilalVisibilityMapMode.KGHT_TURKI
    KriteriaHilal.KGHT_MUHAMMADIYAH -> HilalVisibilityMapMode.KGHT_MUHAMMADIYAH
    KriteriaHilal.LAPAN -> HilalVisibilityMapMode.LAPAN
    KriteriaHilal.ODEH -> HilalVisibilityMapMode.ODEH
    KriteriaHilal.DANJON -> HilalVisibilityMapMode.DANJON
}

private fun ijtimaLocalJdFromAddurul(result: IjtimaResult, gregorianMonth: Int): Double {
    val localMidnight = CalendarFunctions.gregorianToJde(result.thn, gregorianMonth, result.tgl.toDouble())
    return localMidnight + result.waktuIjtimaLT / 24.0
}

private fun ijtimaLocalJdFromVsop(result: HilalResult, timezone: Double): Double {
    val ijtimaUt = result.julianDay - result.deltaT / 86400.0
    return ijtimaUt + timezone / 24.0
}

private fun zonaWaktuLabel(timezone: Double): String = when (timezone) {
    7.0 -> "WIB"
    8.0 -> "WITA"
    9.0 -> "WIT"
    else -> "LT"
}

private fun hitungHariIjtimaDalamBulanSebelumnya(
    targetYearH: Int,
    targetMonthH: Int,
    ijtimaDate: LocalDate,
    latitude: Double,
    longitude: Double,
    elevation: Double,
    timezone: Double,
    criteria: KriteriaHilal
): Int {
    val prevMonthH = if (targetMonthH == 1) 12 else targetMonthH - 1
    val prevYearH = if (targetMonthH == 1) targetYearH - 1 else targetYearH
    val ijtimaJdLocal = CalendarFunctions.gregorianToJde(ijtimaDate.year, ijtimaDate.monthValue, ijtimaDate.dayOfMonth.toDouble())
    val startPrevMonthJd = CalendarFunctions.getStartJdeOfHijriMonth(
        prevYearH,
        prevMonthH,
        latitude,
        longitude,
        elevation,
        timezone,
        kriteriaKeyUntukAwalBulan(criteria)
    )
    return (floor(ijtimaJdLocal) - floor(startPrevMonthJd) + 1).toInt().coerceAtLeast(1)
}

private fun buatKesimpulanAwalBulan(
    kriteria: KriteriaHilal,
    terpenuhi: Boolean,
    ijtimaDate: LocalDate,
    hariIjtimaDalamBulanSebelumnya: Int,
    namaBulanSebelumnya: String,
    namaBulanTarget: String,
    tahunHijriTarget: Int,
    pakaiPasaran: Boolean = true,
    kghtDetailText: String? = null
): KesimpulanAwalBulanUi {
    // Keputusan tanggal awal bulan mengikuti kaidah:
    // - kriteria terpenuhi     => hari ijtimak + 1
    // - kriteria tidak terpenuhi => hari ijtimak + 2
    val tanggalAwal = ijtimaDate.plusDays(if (terpenuhi) 1 else 2)

    // Umur bulan sebelumnya BUKAN tanggal awal bulan.
    // Umur bulan = hari ijtimak dalam bulan sebelumnya + 0/1, lalu dikunci 29..30,
    // sebab bulan Hijriyah tidak boleh 28 atau 31 hari.
    val umurBulanSebelumnya = if (terpenuhi) {
        hariIjtimaDalamBulanSebelumnya.coerceIn(29, 30)
    } else {
        30
    }

    val status = if (terpenuhi) "telah memenuhi" else "belum memenuhi"
    val penjelasan = if (isKghtGlobalCriteria(kriteria)) {
        "Berdasarkan kriteria ${kriteria.label}, hasil hisab global $status syarat minimum. " +
                "Maka bulan $namaBulanSebelumnya berumur $umurBulanSebelumnya hari."
    } else {
        "Berdasarkan kriteria ${kriteria.label}, hilal $status syarat minimum saat matahari terbenam. " +
                "Maka bulan $namaBulanSebelumnya berumur $umurBulanSebelumnya hari."
    }
    val hari = namaHariIndonesia(tanggalAwal)
    val pasaran = if (pakaiPasaran) " ${namaPasaranIndonesia(tanggalAwal)}" else ""
    val bulanMasehi = namaBulanMasehiIndonesia(tanggalAwal.monthValue)
    return KesimpulanAwalBulanUi(
        terpenuhi = terpenuhi,
        tanggalAwalBulan = tanggalAwal,
        umurBulanSebelumnya = umurBulanSebelumnya,
        penjelasan = penjelasan,
        kghtDetailText = kghtDetailText,
        judulTanggal = "Maka Tanggal 1 $namaBulanTarget $tahunHijriTarget H. Jatuh Pada :",
        teksTanggal = "$hari$pasaran, ${tanggalAwal.dayOfMonth} ${bulanMasehi.uppercase(Locale.getDefault())} ${tanggalAwal.year} M."
    )
}

private fun Double.toDMS(): String {
    val neg = this < 0
    val totalSeconds = round(abs(this) * 3600).toInt()
    val d = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    val sign = if (neg) "- " else ""
    return String.format(Locale.US, "%s%02d° %02d' %02d\"", sign, d, m, s)
}

private fun isKghtGlobalCriteria(kriteria: KriteriaHilal): Boolean =
    kriteria == KriteriaHilal.KGHT_TURKI || kriteria == KriteriaHilal.KGHT_MUHAMMADIYAH

private fun timezoneFromLongitude(lon: Double): Double =
    (round(lon / 15.0)).coerceIn(-12.0, 14.0)

private fun isTurkeyExcludedAlaskaRegion(lat: Double, lon: Double): Boolean {
    var lon180 = lon % 360.0
    if (lon180 > 180.0) lon180 -= 360.0
    if (lon180 < -180.0) lon180 += 360.0
    val aleutianFox = lat in 50.0..56.5 && lon180 in -170.0..-155.0
    val alaskaPeninsula = lat in 54.0..59.5 && lon180 in -165.0..-155.0
    return aleutianFox || alaskaPeninsula
}

/**
 * Evaluator Addurul Aniq dengan kriteria yang sama seperti VSOP.
 * Kriteria lokal tetap memakai data markaz Addurul.
 * Kriteria KHGT/KGHT tidak boleh lokal markaz: ia discan global ringan.
 */
private fun evaluateKriteriaAddurulTerpadu(
    engine: AddurulAniqEngine,
    targetYearH: Int,
    targetMonthH: Int,
    markazResult: IjtimaResult,
    kriteria: KriteriaHilal
): Boolean {
    return when (kriteria) {
        KriteriaHilal.KGHT_TURKI -> scanKghtGlobalAddurul(
            engine = engine,
            targetYearH = targetYearH,
            targetMonthH = targetMonthH,
            useTopocentricAltitude = true
        ).terpenuhi
        KriteriaHilal.KGHT_MUHAMMADIYAH -> scanKghtGlobalAddurul(
            engine = engine,
            targetYearH = targetYearH,
            targetMonthH = targetMonthH,
            useTopocentricAltitude = false
        ).terpenuhi
        else -> evaluateKriteria(markazResult, kriteria)
    }
}

private data class AddurulKghtGlobalResult(
    val terpenuhi: Boolean,
    val coordinateText: String = "",
    val altitudeText: String = "",
    val elongationText: String = ""
)

private val addurulKghtGlobalCache = mutableMapOf<String, AddurulKghtGlobalResult>()

/**
 * Scan global ringan untuk Addurul Aniq.
 * Ini sengaja hanya dipakai untuk KGHT/KHGT, supaya Addurul tidak lagi memutuskan KHGT
 * berdasarkan markaz lokal. Grid dibuat moderat: lintang 15°, bujur 30° + early stop.
 */
private fun scanKghtGlobalAddurul(
    engine: AddurulAniqEngine,
    targetYearH: Int,
    targetMonthH: Int,
    useTopocentricAltitude: Boolean
): AddurulKghtGlobalResult {
    val key = "${targetYearH}_${targetMonthH}_${if (useTopocentricAltitude) "TURKI" else "MUHAMMADIYAH"}"
    addurulKghtGlobalCache[key]?.let { return it }

    fun evaluatePriorityPoint(lat: Double, lon: Double): AddurulKghtGlobalResult? {
        if (useTopocentricAltitude && isTurkeyExcludedAlaskaRegion(lat, lon)) return null
        val tz = timezoneFromLongitude(lon)
        return try {
            val r = engine.hitungIjtima(targetYearH, targetMonthH, lat, lon, 0.0, tz)
            val altitude = if (useTopocentricAltitude) r.hcSathi else r.hcMarkazi
            val elongation = r.eloMarkazi
            if (altitude >= 5.0 && elongation >= 8.0) {
                AddurulKghtGlobalResult(
                    terpenuhi = true,
                    coordinateText = String.format(Locale.US, "%.2f°, %.2f°", lat, lon),
                    altitudeText = altitude.toDMS(),
                    elongationText = elongation.toDMS()
                )
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    val priorityPoints = if (useTopocentricAltitude) {
        listOf(0.0 to -60.0, 15.0 to -75.0, -15.0 to -75.0, 30.0 to -90.0)
    } else {
        listOf(50.0 to -170.0, 55.0 to -165.0, 60.0 to -160.0, 50.0 to -150.0)
    }
    for ((lat, lon) in priorityPoints) {
        val result = evaluatePriorityPoint(lat, lon)
        if (result != null) {
            addurulKghtGlobalCache[key] = result
            return result
        }
    }

    var lat = -60.0
    while (lat <= 60.0001) {
        var lon = -180.0
        while (lon <= 180.0001) {
            val tz = timezoneFromLongitude(lon)
            try {
                if (useTopocentricAltitude && isTurkeyExcludedAlaskaRegion(lat, lon)) {
                    lon += 30.0
                    continue
                }
                val r = engine.hitungIjtima(targetYearH, targetMonthH, lat, lon, 0.0, tz)
                val altitude = if (useTopocentricAltitude) r.hcSathi else r.hcMarkazi
                val elongation = r.eloMarkazi
                if (altitude >= 5.0 && elongation >= 8.0) {
                    val result = AddurulKghtGlobalResult(
                        terpenuhi = true,
                        coordinateText = String.format(Locale.US, "%.2f°, %.2f°", lat, lon),
                        altitudeText = altitude.toDMS(),
                        elongationText = elongation.toDMS()
                    )
                    addurulKghtGlobalCache[key] = result
                    return result
                }
            } catch (_: Exception) {
                // Titik ekstrem tertentu bisa gagal karena kondisi horizon/waktu; lanjut titik lain.
            }
            lon += 30.0
        }
        lat += 15.0
    }

    val result = AddurulKghtGlobalResult(terpenuhi = false)
    addurulKghtGlobalCache[key] = result
    return result
}

private fun formatKghtGlobalDetail(
    kriteria: KriteriaHilal,
    coordinateText: String,
    altitudeText: String,
    elongationText: String,
    terpenuhi: Boolean
): String {
    val coordinateInvalid = coordinateText.isBlank() || coordinateText == "tidak ditemukan"
    val altitudeInvalid = altitudeText.isBlank() || altitudeText == "-" || altitudeText.contains("-999")
    val elongationInvalid = elongationText.isBlank() || elongationText == "-" || elongationText.contains("-999")

    if (!terpenuhi && (coordinateInvalid || altitudeInvalid || elongationInvalid)) {
        return "Belum ditemukan titik global yang memenuhi kriteria pada jendela waktu KHGT."
    }

    val statusText = if (terpenuhi) "Kriteria terpenuhi pada koordinat" else "Titik terbaik hasil scan berada pada koordinat"
    val altLabel = if (kriteria == KriteriaHilal.KGHT_TURKI) "Ketinggian Hilal Toposentrik" else "Ketinggian Hilal Geosentrik"
    val elongLabel = "Elongasi Geosentrik"
    return """
$statusText $coordinateText, dengan rincian:
$altLabel : $altitudeText
$elongLabel : $elongationText
""".trimIndent()
}


@Composable
private fun KesimpulanAwalBulanCard(
    kesimpulanUi: KesimpulanAwalBulanUi,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Kesimpulan Awal Bulan",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 4.dp),
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.5f)
            )

            Text(
                kesimpulanUi.penjelasan,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            kesimpulanUi.kghtDetailText?.let { detail ->
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    detail,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = kesimpulanUi.judulTanggal,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = kesimpulanUi.teksTanggal,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HisabAwalBulanScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToVisibilityMap: (VisibilityMapRequest) -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val addurulEngine = remember { AddurulAniqEngine(context) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // State untuk input Hijriah
    var hijriYear by remember { mutableStateOf("1447") }
    var hijriMonth by remember { mutableStateOf("8") }
    var bulanDropdownExpanded by remember { mutableStateOf(false) }

    // State lokasi
    var locationMode by remember { mutableStateOf(LocationMode.OTOMATIS) }
    var locDropdownExpanded by remember { mutableStateOf(false) }

    // State metode hisab
    var metodeHisab by remember { mutableStateOf("VSOP87 / ELP-MPP02") }
    var metodeDropdownExpanded by remember { mutableStateOf(false) }

    val prefs = remember { PreferencesHelper(context) }
    val awalBulanKriteriaOptions = remember {
        KriteriaHilal.values().filterNot { kriteria ->
            kriteria == KriteriaHilal.KGHT_TURKI ||
                    kriteria == KriteriaHilal.KGHT_MUHAMMADIYAH
        }
    }

    // State kriteria
    var selectedKriteria by remember {
        val savedLabel = prefs.kriteriaAwalBulan
        val kriteria = awalBulanKriteriaOptions.find { it.label == savedLabel } ?: KriteriaHilal.MABIMS_BARU
        mutableStateOf(kriteria)
    }
    LaunchedEffect(Unit) {
        if (prefs.kriteriaAwalBulan != selectedKriteria.label) {
            prefs.kriteriaAwalBulan = selectedKriteria.label
        }
    }
    var kriteriaExpanded by remember { mutableStateOf(false) }

    // State manual coordinates
    var locName by remember { mutableStateOf("") }
    var latDeg by remember { mutableStateOf("7") }
    var latMin by remember { mutableStateOf("31") }
    var latSec by remember { mutableStateOf("24") }
    var latDir by remember { mutableStateOf("LS") }
    var lonDeg by remember { mutableStateOf("108") }
    var lonMin by remember { mutableStateOf("16") }
    var lonSec by remember { mutableStateOf("7") }
    var lonDir by remember { mutableStateOf("BT") }
    var altitude by remember { mutableStateOf("10") }
    var timezone by remember { mutableStateOf("7") }

    // GPS state
    var gpsLat by remember { mutableStateOf(0.0) }
    var gpsLon by remember { mutableStateOf(0.0) }
    var gpsElev by remember { mutableStateOf(0.0) }
    var gpsName by remember { mutableStateOf("") }
    var gpsStatus by remember { mutableStateOf("Siap") }

    @SuppressLint("MissingPermission")
    fun fetchGpsLocation() {
        gpsStatus = "Mencari lokasi..."
        fetchLocation(context, fusedLocationClient) { lat, lon, alt, name ->
            gpsLat = lat
            gpsLon = lon
            gpsElev = alt
            gpsName = name
            gpsStatus = "Lokasi: $name"
            setDmsFromDecimal(lat, isLat = true) { d, m, s, dir ->
                latDeg = d; latMin = m; latSec = s; latDir = dir
            }
            setDmsFromDecimal(lon, isLat = false) { d, m, s, dir ->
                lonDeg = d; lonMin = m; lonSec = s; lonDir = dir
            }
            altitude = String.format(Locale.US, "%.0f", alt)
            locName = name
            android.widget.Toast.makeText(
                context,
                "GPS: $name (${String.format(Locale.US, "%.4f", lat)}, ${String.format(Locale.US, "%.4f", lon)})",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.all { it }
        if (granted) {
            fetchGpsLocation()
        } else {
            android.widget.Toast.makeText(context, "Izin lokasi tidak diberikan, gunakan Manual", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // Results State
    var addurulResult by remember { mutableStateOf<IjtimaResult?>(null) }
    var premiumResult by remember { mutableStateOf<HilalResult?>(null) }
    var addurulKghtScanResult by remember { mutableStateOf<AddurulKghtGlobalResult?>(null) }
    var calculatedMetode by remember { mutableStateOf("") }
    var isCalculating by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf("") }
    var showInput by remember { mutableStateOf(true) }
    var showDetail by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Hisab Awal Bulan Terpadu", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Tombol Ubah Parameter jika form disembunyikan
            AnimatedVisibility(visible = !showInput) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    TextButton(onClick = { showInput = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Ubah Parameter", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("UBAH PARAMETER")
                    }
                }
            }

            // FORM INPUT
            AnimatedVisibility(visible = showInput) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Parameter Hisab Terpadu", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                        // Input Bulan & Tahun Hijriyah
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedMonthName = remember(hijriMonth) {
                                val monthInt = hijriMonth.toIntOrNull() ?: 1
                                val name = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(monthInt - 1) { "Muharram" }
                                "$monthInt. $name"
                            }

                            ExposedDropdownMenuBox(
                                expanded = bulanDropdownExpanded,
                                onExpandedChange = { bulanDropdownExpanded = it },
                                modifier = Modifier.weight(1.4f)
                            ) {
                                OutlinedTextField(
                                    value = selectedMonthName,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Bulan Hijriyah") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bulanDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = bulanDropdownExpanded,
                                    onDismissRequest = { bulanDropdownExpanded = false }
                                ) {
                                    CalendarFunctions.HIJRI_MONTH_NAMES.forEachIndexed { index, name ->
                                        val monthNum = index + 1
                                        DropdownMenuItem(
                                            text = { Text("$monthNum. $name") },
                                            onClick = {
                                                hijriMonth = monthNum.toString()
                                                bulanDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = hijriYear,
                                onValueChange = { hijriYear = it },
                                label = { Text("Tahun Hijriyah") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(0.9f)
                            )
                        }

                        // Pilihan Metode Hisab
                        ExposedDropdownMenuBox(
                            expanded = metodeDropdownExpanded,
                            onExpandedChange = { metodeDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = metodeHisab,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Metode Hisab") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = metodeDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = metodeDropdownExpanded,
                                onDismissRequest = { metodeDropdownExpanded = false }
                            ) {
                                listOf("Addurul Aniq", "VSOP87 / ELP-MPP02").forEach { m ->
                                    DropdownMenuItem(
                                        text = { Text(m) },
                                        onClick = { metodeHisab = m; metodeDropdownExpanded = false }
                                    )
                                }
                            }
                        }

                        // Pilihan Kriteria
                        ExposedDropdownMenuBox(
                            expanded = kriteriaExpanded,
                            onExpandedChange = { kriteriaExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedKriteria.label,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kriteria Awal Bulan") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = kriteriaExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = kriteriaExpanded,
                                onDismissRequest = { kriteriaExpanded = false }
                            ) {
                                awalBulanKriteriaOptions.forEach { k ->
                                    DropdownMenuItem(
                                        text = { Text(k.label) },
                                        onClick = {
                                            selectedKriteria = k
                                            prefs.kriteriaAwalBulan = k.label
                                            kriteriaExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Pilihan Lokasi Mode
                        ExposedDropdownMenuBox(
                            expanded = locDropdownExpanded,
                            onExpandedChange = { locDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = if (locationMode == LocationMode.OTOMATIS) "Otomatis (GPS)" else "Manual",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Mode Lokasi") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locDropdownExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = locDropdownExpanded,
                                onDismissRequest = { locDropdownExpanded = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Otomatis (GPS)") },
                                    onClick = { locationMode = LocationMode.OTOMATIS; locDropdownExpanded = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Manual") },
                                    onClick = { locationMode = LocationMode.MANUAL; locDropdownExpanded = false }
                                )
                            }
                        }

                        if (locationMode == LocationMode.OTOMATIS) {
                            Button(
                                onClick = {
                                    val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                                    if (perms.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                                        fetchGpsLocation()
                                    } else {
                                        permissionLauncher.launch(perms)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.MyLocation, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Dapatkan Lokasi GPS Saat Ini")
                            }
                            Text(gpsStatus, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.secondary)
                            if (gpsLat != 0.0) {
                                Column(modifier = Modifier.padding(top = 4.dp)) {
                                    Text("Koordinat GPS:", color = Color.Gray, fontSize = 12.sp)
                                    Text("Lintang: ${String.format(Locale.US, "%.4f", gpsLat)}, Bujur: ${String.format(Locale.US, "%.4f", gpsLon)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    Text("Elevasi: ${String.format(Locale.US, "%.1f", gpsElev)} m", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = timezone,
                                        onValueChange = { timezone = it },
                                        label = { Text("Timezone (GMT+)") },
                                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }
                        } else {
                            OutlinedTextField(value = locName, onValueChange = { locName = it }, label = { Text("Nama Lokasi") }, modifier = Modifier.fillMaxWidth())
                            // Lintang DMS
                            Text("Lintang (DMS)", fontSize = 12.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(value = latDeg, onValueChange = { latDeg = it }, label = { Text("Deg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = latMin, onValueChange = { latMin = it }, label = { Text("Min") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = latSec, onValueChange = { latSec = it }, label = { Text("Sec") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                Button(onClick = { latDir = if (latDir == "LU") "LS" else "LU" }, modifier = Modifier.width(60.dp).padding(top = 8.dp), contentPadding = PaddingValues(0.dp)) { Text(latDir, fontSize = 12.sp) }
                            }
                            // Bujur DMS
                            Text("Bujur (DMS)", fontSize = 12.sp, color = Color.Gray)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                OutlinedTextField(value = lonDeg, onValueChange = { lonDeg = it }, label = { Text("Deg") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = lonMin, onValueChange = { lonMin = it }, label = { Text("Min") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = lonSec, onValueChange = { lonSec = it }, label = { Text("Sec") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                Button(onClick = { lonDir = if (lonDir == "BT") "BB" else "BT" }, modifier = Modifier.width(60.dp).padding(top = 8.dp), contentPadding = PaddingValues(0.dp)) { Text(lonDir, fontSize = 12.sp) }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = altitude, onValueChange = { altitude = it }, label = { Text("Tinggi (m)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                                OutlinedTextField(value = timezone, onValueChange = { timezone = it }, label = { Text("Timezone (GMT+)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                            }
                        }

                        // Tombol HISAB
                        Button(
                            onClick = {
                                val hYear = hijriYear.toIntOrNull()
                                val hMonth = hijriMonth.toIntOrNull()
                                if (hYear == null || hMonth == null) {
                                    errorMsg = "Tahun atau bulan hijriyah tidak valid"
                                    return@Button
                                }
                                val finalLat = if (locationMode == LocationMode.OTOMATIS && gpsLat != 0.0) gpsLat else dmsToDecimal(latDeg, latMin, latSec, latDir)
                                val finalLon = if (locationMode == LocationMode.OTOMATIS && gpsLon != 0.0) gpsLon else dmsToDecimal(lonDeg, lonMin, lonSec, lonDir)
                                val finalElev = if (locationMode == LocationMode.OTOMATIS && gpsElev != 0.0) gpsElev else altitude.toDoubleOrNull() ?: 0.0
                                val finalTz = timezone.toDoubleOrNull() ?: 7.0

                                errorMsg = ""
                                isCalculating = true
                                coroutineScope.launch {
                                    try {
                                        if (metodeHisab == "Addurul Aniq") {
                                            val resultPair = withContext(Dispatchers.Default) {
                                                val localResult = addurulEngine.hitungIjtima(hYear, hMonth, finalLat, finalLon, finalElev, finalTz)
                                                val globalScan = if (isKghtGlobalCriteria(selectedKriteria)) {
                                                    scanKghtGlobalAddurul(
                                                        engine = addurulEngine,
                                                        targetYearH = hYear,
                                                        targetMonthH = hMonth,
                                                        useTopocentricAltitude = selectedKriteria == KriteriaHilal.KGHT_TURKI
                                                    )
                                                } else null
                                                localResult to globalScan
                                            }
                                            addurulResult = resultPair.first
                                            addurulKghtScanResult = resultPair.second
                                            premiumResult = null
                                            calculatedMetode = "Addurul Aniq"
                                        } else {
                                            val result = withContext(Dispatchers.Default) {
                                                context.assets.open("mpp02_core.bin").use { ElpDataProvider.initialize(it) }
                                                context.assets.open("earth_vsop87d.bin").use { Vsop87SolarEngine.initialize(it) }
                                                HilalEngine.calculateHilalStart(
                                                    hijriYear = hYear,
                                                    hijriMonth = hMonth,
                                                    latitude = finalLat,
                                                    longitude = finalLon,
                                                    elevation = finalElev,
                                                    timezone = finalTz,
                                                    selectedKriteria = selectedKriteria
                                                )
                                            }
                                            premiumResult = result
                                            addurulResult = null
                                            addurulKghtScanResult = null
                                            calculatedMetode = "VSOP87 / ELP-MPP02"
                                        }
                                        showInput = false
                                    } catch (e: Exception) {
                                        errorMsg = "Gagal menghitung: ${e.message}"
                                    } finally {
                                        isCalculating = false
                                    }
                                }
                            },
                            enabled = !isCalculating,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(if (isCalculating) "MENGHITUNG..." else "HISAB", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp), fontSize = 16.sp)
                        }
                        if (errorMsg.isNotEmpty()) {
                            Text(errorMsg, color = Color.Red, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // TAMPILAN HASIL ADDURUL ANIQ
            if (calculatedMetode == "Addurul Aniq" && addurulResult != null) {
                val res = addurulResult!!
                val mIdx = masehiMonths.indexOf(res.namaBulan) + 1

                val akhirBulanIdx = if (res.targetMonth == 1) 11 else res.targetMonth - 2
                val akhirTahunH = if (res.targetMonth == 1) res.targetYear - 1 else res.targetYear
                val targetMonthName = hijriMonths[akhirBulanIdx]

                val nextMonthName = hijriMonths[res.targetMonth - 1]
                val nextYearH = res.targetYear

                val finalLocName = if (locationMode == LocationMode.OTOMATIS && gpsName.isNotEmpty()) gpsName else locName
                val dispLat = if (locationMode == LocationMode.OTOMATIS && gpsLat != 0.0) String.format(Locale.US, "%.4f°", gpsLat) else "$latDeg° $latMin' $latSec\" $latDir"
                val dispLon = if (locationMode == LocationMode.OTOMATIS && gpsLon != 0.0) String.format(Locale.US, "%.4f°", gpsLon) else "$lonDeg° $lonMin' $lonSec\" $lonDir"
                val dispElev = if (locationMode == LocationMode.OTOMATIS && gpsElev != 0.0) String.format(Locale.US, "%.1f", gpsElev) else altitude
                val finalLatNum = if (locationMode == LocationMode.OTOMATIS && gpsLat != 0.0) gpsLat else dmsToDecimal(latDeg, latMin, latSec, latDir)
                val finalLonNum = if (locationMode == LocationMode.OTOMATIS && gpsLon != 0.0) gpsLon else dmsToDecimal(lonDeg, lonMin, lonSec, lonDir)
                val finalElevNum = if (locationMode == LocationMode.OTOMATIS && gpsElev != 0.0) gpsElev else altitude.toDoubleOrNull() ?: 0.0
                val finalTzNum = timezone.toDoubleOrNull() ?: 7.0
                val isAwalBulan = if (isKghtGlobalCriteria(selectedKriteria)) {
                    addurulKghtScanResult?.terpenuhi == true
                } else {
                    evaluateKriteria(res, selectedKriteria)
                }

                if (!isKghtGlobalCriteria(selectedKriteria)) {
                    // 1. Markaz Hisab Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("1. Markaz Hisab (Addurul Aniq)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                            SummaryRow("Lokasi", finalLocName.ifEmpty { "Lokasi Manual" })
                            SummaryRow("Koordinat", "$dispLat, $dispLon")
                            SummaryRow("Tinggi Tempat", "$dispElev mdpl")
                            SummaryRow("Zona Waktu", "GMT+${timezone}")
                        }
                    }

                    // 2. Data Kesimpulan Hisab Card
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("2. Data Kesimpulan Hisab", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            SummaryRow("Ijtima' akhir bulan", "$targetMonthName $akhirTahunH H.")
                            SummaryRow("Hari / Tanggal", "${res.hariPasaran.uppercase(Locale.getDefault())} ${res.tgl} ${res.namaBulan.uppercase(Locale.getDefault())} ${res.thn} M.")
                            SummaryRow("Jam Ijtima'", "${formatTimeOfDay(res.waktuIjtimaLT, prefs.is24HourFormat)} WIB")
                            SummaryRow("Matahari terbenam", "${formatTimeOfDay(res.ghrbWdHaqiqi, prefs.is24HourFormat)} WIB")
                            SummaryRow("Hilal terbenam", "${formatTimeOfDay(res.ghurubHilal, prefs.is24HourFormat)} WIB")

                            SummaryRow("Ketinggian Hilal Hakiki", res.hcMarkazi.toDMS(), isNegativeRed = res.hcMarkazi < 0)
                            SummaryRow("Ketinggian Hilal Mar'i", res.hcSathi.toDMS(), isNegativeRed = res.hcSathi < 0)
                            SummaryRow("Elongasi Hilal", res.eloMarkazi.toDMS())

                            SummaryRow("Umur Hilal", if (res.hcSathi < 0) "" else formatHMSIndo(res.umurHilal))

                            val lamaHilalText = if (res.ghurubHilal <= res.ghrbWdHaqiqi || res.muktsulHilal < 0) "HILAL SUDAH DI BAWAH UFUK" else "${String.format(Locale.US, "%.0f", floor(res.muktsulHilal))} menit ${String.format(Locale.US, "%.0f", (res.muktsulHilal - floor(res.muktsulHilal))*60)} detik"
                            SummaryRow("Lamanya Hilal di atas ufuq", lamaHilalText)

                            SummaryRow("Azimut Matahari", res.matahari.azm.toDMS())
                            SummaryRow("Azimut Hilal", res.azcMoon.toDMS())
                            SummaryRow("Lebar cahaya Hilal", String.format(Locale.US, "%.2f %%", res.nurulHilal))

                            val dirMatahari = if (res.bedaAzm < 0) "di selatan matahari" else "di utara matahari"
                            SummaryRow("Jarak & Letak Hilal dari Matahari", "${res.bedaAzm.toDMS()} ( $dirMatahari )")

                            val keadaanText = if (res.bedaAzm < 0) "miring ke selatan" else "miring ke utara"
                            SummaryRow("Keadaan Hilal", keadaanText)

                            val posisiText = if (res.dcResult < 0) "di selatan katulistiwa" else "di utara katulistiwa"
                            SummaryRow("Posisi Hilal", posisiText)

                            val letakSunText = if (res.matahari.azm < 270) "di selatan titik barat" else "di utara titik barat"
                            SummaryRow("Letak Matahari terbenam", letakSunText)
                        }
                    }

                }

                // Kalkulasi kesimpulan final memakai satu engine redaksi
                val ijtimaDateAddurul = LocalDate.of(res.thn, mIdx, res.tgl)
                val hariIjtimaDalamBulanSebelumnyaAddurul = if (isKghtGlobalCriteria(selectedKriteria)) {
                    29
                } else {
                    hitungHariIjtimaDalamBulanSebelumnya(
                        targetYearH = res.targetYear,
                        targetMonthH = res.targetMonth,
                        ijtimaDate = ijtimaDateAddurul,
                        latitude = finalLatNum,
                        longitude = finalLonNum,
                        elevation = finalElevNum,
                        timezone = finalTzNum,
                        criteria = selectedKriteria
                    )
                }
                val addurulKghtDetail = if (isKghtGlobalCriteria(selectedKriteria)) {
                    val scan = addurulKghtScanResult
                    formatKghtGlobalDetail(
                        kriteria = selectedKriteria,
                        coordinateText = scan?.coordinateText.orEmpty(),
                        altitudeText = scan?.altitudeText.orEmpty(),
                        elongationText = scan?.elongationText.orEmpty(),
                        terpenuhi = scan?.terpenuhi == true
                    )
                } else null

                val kesimpulanUi = buatKesimpulanAwalBulan(
                    kriteria = selectedKriteria,
                    terpenuhi = isAwalBulan,
                    ijtimaDate = ijtimaDateAddurul,
                    hariIjtimaDalamBulanSebelumnya = hariIjtimaDalamBulanSebelumnyaAddurul,
                    namaBulanSebelumnya = targetMonthName,
                    namaBulanTarget = nextMonthName,
                    tahunHijriTarget = nextYearH,
                    pakaiPasaran = true,
                    kghtDetailText = addurulKghtDetail
                )

                // Kesimpulan final memakai satu komponen redaksi bersama
                KesimpulanAwalBulanCard(kesimpulanUi = kesimpulanUi)

                OutlinedButton(
                    onClick = {
                        val monthForMap = mIdx.coerceIn(1, 12)
                        onNavigateToVisibilityMap(
                            VisibilityMapRequest(
                                hijriYear = res.targetYear,
                                hijriMonth = res.targetMonth,
                                ijtimaGeoJde = HilalEngine.calculateMeeusIjtima(res.targetYear, res.targetMonth),
                                ijtimaLocalJd = ijtimaLocalJdFromAddurul(res, monthForMap),
                                timezone = finalTzNum,
                                mode = modePetaUntukKriteria(selectedKriteria),
                                magribLocalTimeText = "${formatTimeOfDay(res.ghrbWdHaqiqi, prefs.is24HourFormat)} ${zonaWaktuLabel(finalTzNum)}",
                                locationName = finalLocName.ifEmpty { "Lokasi Manual" },
                                latitude = finalLatNum,
                                longitude = finalLonNum,
                                elevation = finalElevNum
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("BUKA PETA VISIBILITAS")
                }
                val ctx = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        com.falak.falakpro.ui.PencetakHisabAwalBulan.cetakAddurul(
                            context = ctx,
                            res = res,
                            markazName = finalLocName.ifEmpty { "Lokasi Manual" },
                            koordinat = "$dispLat, $dispLon",
                            elevasi = "$dispElev mdpl",
                            bulanTarget = "$nextMonthName ${nextYearH} H"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CETAK DETAIL KE PDF")
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // TAMPILAN HASIL PREMIUM VSOP/ELP
            if (calculatedMetode == "VSOP87 / ELP-MPP02" && premiumResult != null) {
                val res = premiumResult!!
                val finalLocName = if (locationMode == LocationMode.OTOMATIS && gpsName.isNotEmpty()) gpsName else locName
                val dispLat = if (locationMode == LocationMode.OTOMATIS && gpsLat != 0.0) String.format(Locale.US, "%.4f°", gpsLat) else "$latDeg° $latMin' $latSec\" $latDir"
                val dispLon = if (locationMode == LocationMode.OTOMATIS && gpsLon != 0.0) String.format(Locale.US, "%.4f°", gpsLon) else "$lonDeg° $lonMin' $lonSec\" $lonDir"
                val dispElev = if (locationMode == LocationMode.OTOMATIS && gpsElev != 0.0) String.format(Locale.US, "%.1f", gpsElev) else altitude
                val finalLatNumVsop = if (locationMode == LocationMode.OTOMATIS && gpsLat != 0.0) gpsLat else dmsToDecimal(latDeg, latMin, latSec, latDir)
                val finalLonNumVsop = if (locationMode == LocationMode.OTOMATIS && gpsLon != 0.0) gpsLon else dmsToDecimal(lonDeg, lonMin, lonSec, lonDir)
                val finalElevNumVsop = if (locationMode == LocationMode.OTOMATIS && gpsElev != 0.0) gpsElev else altitude.toDoubleOrNull() ?: 0.0
                val finalTzNumVsop = timezone.toDoubleOrNull() ?: 7.0

                // 1. Markaz Hisab Card
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("1. Markaz Hisab (VSOP87/ELP-MPP02)", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        SummaryRow("Lokasi", finalLocName.ifEmpty { "Lokasi Manual" })
                        SummaryRow("Koordinat", "$dispLat, $dispLon")
                        SummaryRow("Tinggi Tempat", "$dispElev mdpl")
                        SummaryRow("Zona Waktu", "GMT+${timezone}")
                        SummaryRow("Metode", res.algorithm)
                    }
                }

                // 2. Data Hisab Lengkap - Multisection
                if (!isKghtGlobalCriteria(selectedKriteria)) {
                    HisabDetailSections(res = res)
                }

                // Kesimpulan final
                val isImkan = when (selectedKriteria) {
                    KriteriaHilal.YALLOP -> res.isVisibleYallop
                    KriteriaHilal.MABIMS_BARU -> res.isVisibleMabimsBaru
                    KriteriaHilal.MABIMS_LAMA -> res.isVisibleMabimsLama
                    KriteriaHilal.WUJUDUL_HILAL -> res.isVisibleWujudulHilal
                    KriteriaHilal.KGHT_TURKI -> res.isVisibleKghtTurki
                    KriteriaHilal.KGHT_MUHAMMADIYAH -> res.isVisibleKghtMuhammadiyah
                    KriteriaHilal.LAPAN -> res.isVisibleLapan
                    KriteriaHilal.ODEH -> res.isVisibleOdeh
                    KriteriaHilal.DANJON -> res.isVisibleDanjon
                }

                val prevMonthIdx = ((res.hijriMonth - 2) + 12) % 12
                val prevMonthName = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(prevMonthIdx) { "" }
                val currentMonthName = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse((res.hijriMonth - 1).coerceIn(0, 11)) { "" }

                val ijtimaDateVsop = res.gregorianDate.toLocalDate()
                val hariIjtimaDalamBulanSebelumnyaVsop = if (isKghtGlobalCriteria(selectedKriteria)) {
                    29
                } else {
                    hitungHariIjtimaDalamBulanSebelumnya(
                        targetYearH = res.hijriYear,
                        targetMonthH = res.hijriMonth,
                        ijtimaDate = ijtimaDateVsop,
                        latitude = finalLatNumVsop,
                        longitude = finalLonNumVsop,
                        elevation = finalElevNumVsop,
                        timezone = finalTzNumVsop,
                        criteria = selectedKriteria
                    )
                }

                val vsopKghtDetail = if (isKghtGlobalCriteria(selectedKriteria)) {
                    if (selectedKriteria == KriteriaHilal.KGHT_TURKI) {
                        formatKghtGlobalDetail(
                            kriteria = selectedKriteria,
                            coordinateText = res.kghtTurkiLocation.ifEmpty { "tidak ditemukan" },
                            altitudeText = res.kghtTurkiAltitudeTopoStr.ifEmpty { "-" },
                            elongationText = res.kghtTurkiElongationGeoStr.ifEmpty { "-" },
                            terpenuhi = isImkan
                        )
                    } else {
                        formatKghtGlobalDetail(
                            kriteria = selectedKriteria,
                            coordinateText = res.kghtMuhammadiyahLocation.ifEmpty { "tidak ditemukan" },
                            altitudeText = res.kghtMuhammadiyahAltitudeGeoStr.ifEmpty { "-" },
                            elongationText = res.kghtMuhammadiyahElongationGeoStr.ifEmpty { "-" },
                            terpenuhi = isImkan
                        )
                    }
                } else null

                val kesimpulanUi = buatKesimpulanAwalBulan(
                    kriteria = selectedKriteria,
                    terpenuhi = isImkan,
                    ijtimaDate = ijtimaDateVsop,
                    hariIjtimaDalamBulanSebelumnya = hariIjtimaDalamBulanSebelumnyaVsop,
                    namaBulanSebelumnya = prevMonthName,
                    namaBulanTarget = currentMonthName,
                    tahunHijriTarget = res.hijriYear,
                    pakaiPasaran = true,
                    kghtDetailText = vsopKghtDetail
                )

                KesimpulanAwalBulanCard(kesimpulanUi = kesimpulanUi)

                val ctx = androidx.compose.ui.platform.LocalContext.current
                OutlinedButton(
                    onClick = {
                        com.falak.falakpro.ui.PencetakHisabAwalBulan.cetakVsop(
                            context = ctx,
                            res = res,
                            markazName = finalLocName.ifEmpty { "Lokasi Manual" },
                            koordinat = "$dispLat, $dispLon",
                            elevasi = "$dispElev mdpl",
                            bulanTarget = "$currentMonthName ${res.hijriYear} H"
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("CETAK DETAIL KE PDF")
                }

                OutlinedButton(
                    onClick = {
                        onNavigateToVisibilityMap(
                            VisibilityMapRequest(
                                hijriYear = res.hijriYear,
                                hijriMonth = res.hijriMonth,
                                ijtimaGeoJde = res.julianDay,
                                ijtimaLocalJd = ijtimaLocalJdFromVsop(res, finalTzNumVsop),
                                timezone = finalTzNumVsop,
                                mode = modePetaUntukKriteria(selectedKriteria),
                                magribLocalTimeText = res.ghurubSun,
                                locationName = res.locationName.ifEmpty { "Lokasi Manual" },
                                latitude = finalLatNumVsop,
                                longitude = finalLonNumVsop,
                                elevation = finalElevNumVsop
                            )
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("BUKA PETA VISIBILITAS")
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String, isNegativeRed: Boolean = false) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (isNegativeRed) Color.Red else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
@Composable
fun HisabDetailSections(res: HilalResult) {
    // 2. Ijtima' (Konjungsi)
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(icon = Icons.Default.Star, title = "2. Ijtima' (Konjungsi)")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow("Waktu Ijtima' Geosentris", res.ijtimaGeoStr)
            SummaryRow("Waktu Ijtima' Toposentris", res.ijtimaTopoStr)
            SummaryRow("Julian Day Ephemeris (JDE)", String.format(Locale.US, "%.5f", res.julianDay))
            SummaryRow("Selisih Waktu (?T)", String.format(Locale.US, "%.2f detik", res.deltaT))
            SummaryRow("Umur Hilal Saat Maghrib", " hari")
        }
    }

    // 3. Waktu Maghrib & Hilal
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(icon = Icons.Default.Info, title = "3. Waktu Maghrib & Terbenam Bulan")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow("Ghurub / Matahari Terbenam", res.ghurubSun)
            SummaryRow("Terbenamnya Bulan (Moonset)", res.ghurubMoon)
            SummaryRow("Selisih Waktu (Lama Hilal)", res.bestTimeStr)
            SummaryRow("JDE Saat Matahari Terbenam", res.saatPerhitunganStr)
        }
    }

    // 4. Ketinggian Hilal
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(icon = Icons.Default.ExpandLess, title = "4. Ketinggian Hilal")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Tinggi Geosentris (Pusat Bumi)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            SummaryRow("Ketinggian Hakiki (Pusat piringan)", res.altGeoBulanStr)
            
            Text("Tinggi Toposentris (Hakiki Permukaan Bumi)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            SummaryRow("Piringan Atas (Upper Limb)", res.altTopoBulanAtasStr, isNegativeRed = res.altTopoBulanAtasStr.contains("-"))
            SummaryRow("Pusat (Center)", res.altTopoBulanTengahStr, isNegativeRed = res.altTopoBulanTengahStr.contains("-"))
            SummaryRow("Piringan Bawah (Lower Limb)", res.altTopoBulanBawahStr, isNegativeRed = res.altTopoBulanBawahStr.contains("-"))
            
            Text("Tinggi Mar'i (Apparent / Terlihat)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            SummaryRow("Piringan Atas (Upper Limb)", res.altMariBulanAtasStr, isNegativeRed = res.altMariBulanAtasStr.contains("-"))
            SummaryRow("Pusat (Center)", res.altMariBulanTengahStr, isNegativeRed = res.altMariBulanTengahStr.contains("-"))
            SummaryRow("Piringan Bawah (Lower Limb)", res.altMariBulanBawahStr, isNegativeRed = res.altMariBulanBawahStr.contains("-"))
        }
    }

    // 5. Elongasi & Iluminasi
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(icon = Icons.Default.ExpandMore, title = "5. Sudut Elongasi & Fraksi Cahaya")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow("Elongasi Geosentris", res.elongasiGeoStr)
            SummaryRow("Elongasi Toposentris (Hakiki)", res.elongasiTopoStr)
            SummaryRow("Fraksi Iluminasi (Cahaya)", res.illumination)
            SummaryRow("Lebar Sabit (Crescent Width)", res.lebarSabitStr)
            SummaryRow("Nilai q Yallop / q Odeh", res.rangeQOdehStr + " / " + res.rangeQOdehStr)
        }
    }

    // 6. Posisi Benda Langit
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(icon = Icons.Default.MyLocation, title = "6. Posisi Ekliptika & Ekuatorial")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text("Bulan (Moon)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp))
            SummaryRow("Azimut Bulan", res.azBulanStr)
            SummaryRow("Bujur Ekliptika", res.bujurBulanStr)
            SummaryRow("Lintang Ekliptika", res.lintangBulanStr)
            SummaryRow("Asensio Rekta (RA)", res.raBulanStr)
            SummaryRow("Deklinasi", res.decBulanStr)
            
            Text("Matahari (Sun)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
            SummaryRow("Azimut Matahari", res.azMatahariStr)
            SummaryRow("Bujur Ekliptika", res.bujurMatahariStr)
            SummaryRow("Lintang Ekliptika", res.lintangMatahariStr)
            SummaryRow("Asensio Rekta (RA)", res.raMatahariStr)
            SummaryRow("Deklinasi", res.decMatahariStr)
        }
    }

    // 7. Data Teknis Lainnya
    Card(
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionHeader(icon = Icons.Default.Info, title = "7. Parameter Fisik")
            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
            SummaryRow("Jarak Bumi - Bulan", res.jarakBumiBulanStr)
            SummaryRow("Semi-Diameter (SD)", res.semidiameter)
            SummaryRow("Horizontal Parallax (HP)", res.hpBulanStr)
            SummaryRow("Azimut Bulan Terbenam", res.arahTerbenamBulanStr)
        }
    }
}

@Composable
fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}

