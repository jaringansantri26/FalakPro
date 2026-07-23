package com.falak.falakpro.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.falak.falakpro.location.LocationHelper
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.MesinWaktuShalat
import com.falak.falakpro.premium.PreferencesHelper
import com.falak.falakpro.premium.WaktuShalatCache
import com.falak.falakpro.premium.WaktuShalatSettingsResolver
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import android.content.pm.PackageManager

// --- Colors ---
private val JadwalGradientTop = Color(0xFF0B6B35)
private val JadwalGradientBottom = Color(0xFF00897B)
private val JadwalHighlightBackground = Color(0xFFE0F2F1)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JadwalShalatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToKiblat: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesHelper(context) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsVersion by remember { mutableStateOf(0) }

    if (showSettings) {
        PengaturanWaktuShalatScreen(
            prefs = prefs,
            onBack = { 
                settingsVersion++
                showSettings = false 
            }
        )
    } else {
        JadwalShalatMainContent(
            context = context,
            prefs = prefs,
            settingsVersion = settingsVersion,
            onNavigateBack = onNavigateBack,
            onNavigateToKiblat = onNavigateToKiblat,
            onOpenSettings = { showSettings = true }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JadwalShalatMainContent(
    context: Context,
    prefs: PreferencesHelper,
    settingsVersion: Int,
    onNavigateBack: () -> Unit,
    onNavigateToKiblat: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val locationHelper = remember { LocationHelper(context) }
    val locationState by locationHelper.locationState.collectAsState()
    
    var locationInputMode by remember(settingsVersion) { mutableStateOf(prefs.locationInputMode) }
    var locationRevision by remember { mutableIntStateOf(0) }
    var showLocationChoiceSheet by remember { mutableStateOf(false) }
    var showCityPickerDialog by remember { mutableStateOf(false) }
    val lokasiOtomatisState = locationInputMode == "GPS"
    var showCetakDialog by remember { mutableStateOf(false) }
    
    var currentDate by remember { mutableStateOf(Calendar.getInstance()) }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(lokasiOtomatisState) {
        if (lokasiOtomatisState) {
            locationHelper.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            locationInputMode = "GPS"
            prefs.locationInputMode = "GPS"
            Toast.makeText(context, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
            locationHelper.refreshLocation { success ->
                Toast.makeText(
                    context,
                    if (success) "Lokasi berhasil diperbarui!"
                    else "Gagal memperbarui lokasi. Nyalakan GPS Anda.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    fun useCurrentLocation() {
        val hasPermission =
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            locationInputMode = "GPS"
            prefs.locationInputMode = "GPS"
            Toast.makeText(context, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
            locationHelper.refreshLocation { success ->
                Toast.makeText(
                    context,
                    if (success) "Lokasi berhasil diperbarui!"
                    else "Gagal memperbarui lokasi. Nyalakan GPS Anda.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // Sync GPS altitude to DataranTinggi altitude if automatic
    LaunchedEffect(locationState.altitude, lokasiOtomatisState) {
        if (lokasiOtomatisState && locationState.altitude != 0.0) {
            prefs.ketinggianDataranTinggi = locationState.altitude
        }
    }

    val savedManualLat = remember(locationRevision, settingsVersion) { prefs.manualLat }
    val savedManualLon = remember(locationRevision, settingsVersion) { prefs.manualLon }
    val savedManualElev = remember(locationRevision, settingsVersion) { prefs.ketinggianDataranTinggi }
    val savedManualTimezone = remember(locationRevision, settingsVersion) { prefs.manualTimezone }
    val savedManualLocationName = remember(locationRevision, settingsVersion) { prefs.manualLokasiNama }
    val lat = if (lokasiOtomatisState) (if (locationState.latitude != 0.0) locationState.latitude else -6.3133) else savedManualLat
    val lon = if (lokasiOtomatisState) (if (locationState.longitude != 0.0) locationState.longitude else 107.3191) else savedManualLon
    val elev = if (lokasiOtomatisState && locationState.altitude != 0.0) locationState.altitude else savedManualElev
    val tz = if (lokasiOtomatisState) timezoneFromLongitude(lon) else savedManualTimezone
    val tzLabel = prayerTimezoneLabel(tz, lon)
    val locName = if (lokasiOtomatisState) (if (locationState.address != "Mencari Lokasi...") locationState.address else "Lokasi Tidak Diketahui") else savedManualLocationName
    val selectedGregorianJd = remember(
        currentDate.get(Calendar.YEAR),
        currentDate.get(Calendar.MONTH),
        currentDate.get(Calendar.DAY_OF_MONTH)
    ) {
        CalendarFunctions.gregorianToJde(
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH) + 1,
            currentDate.get(Calendar.DAY_OF_MONTH).toDouble()
        )
    }
    val hijriCriteria = prefs.kriteriaAwalBulan
    val selectedHijri by produceState<Triple<Int, Int, Int>?>(
        initialValue = null,
        selectedGregorianJd,
        hijriCriteria
    ) {
        value = runCatching {
            calculateSyncedHijriDate(context, selectedGregorianJd, hijriCriteria)
        }.getOrNull()
    }

    val resolvedSettings = WaktuShalatSettingsResolver.resolve(prefs)
    val kriteria = resolvedSettings.kriteria

    val tomorrowDateForNext = remember(currentDate) {
        (currentDate.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
    }
    val todayKey = remember(currentDate, lat, lon, elev, tz, kriteria, resolvedSettings, prefs.ikhSubuh, prefs.ikhTerbit, prefs.ikhDhuha, prefs.ikhDzuhur, prefs.ikhAshar, prefs.ikhMaghrib, prefs.ikhIsya, prefs.is24HourFormat) {
        WaktuShalatCache.key(
            year = currentDate.get(Calendar.YEAR),
            month = currentDate.get(Calendar.MONTH) + 1,
            day = currentDate.get(Calendar.DAY_OF_MONTH),
            lat = lat,
            lon = lon,
            elev = elev,
            timezone = tz,
            kriteria = kriteria,
            subuh = prefs.ikhSubuh,
            terbit = prefs.ikhTerbit,
            dhuha = prefs.ikhDhuha,
            dzuhur = prefs.ikhDzuhur,
            ashar = prefs.ikhAshar,
            maghrib = prefs.ikhMaghrib,
            isya = prefs.ikhIsya,
            pembulatan = resolvedSettings.pembulatan,
            faktorAshar = resolvedSettings.faktorAshar,
            is24HourFormat = prefs.is24HourFormat
        )
    }
    val tomorrowKey = remember(tomorrowDateForNext, lat, lon, elev, tz, kriteria, resolvedSettings, prefs.ikhSubuh, prefs.ikhTerbit, prefs.ikhDhuha, prefs.ikhDzuhur, prefs.ikhAshar, prefs.ikhMaghrib, prefs.ikhIsya, prefs.is24HourFormat) {
        WaktuShalatCache.key(
            year = tomorrowDateForNext.get(Calendar.YEAR),
            month = tomorrowDateForNext.get(Calendar.MONTH) + 1,
            day = tomorrowDateForNext.get(Calendar.DAY_OF_MONTH),
            lat = lat,
            lon = lon,
            elev = elev,
            timezone = tz,
            kriteria = kriteria,
            subuh = prefs.ikhSubuh,
            terbit = prefs.ikhTerbit,
            dhuha = prefs.ikhDhuha,
            dzuhur = prefs.ikhDzuhur,
            ashar = prefs.ikhAshar,
            maghrib = prefs.ikhMaghrib,
            isya = prefs.ikhIsya,
            pembulatan = resolvedSettings.pembulatan,
            faktorAshar = resolvedSettings.faktorAshar,
            is24HourFormat = prefs.is24HourFormat
        )
    }
    val emptySchedule = remember {
        listOf("Imsak", "Subuh", "Terbit", "Dhuha", "Dzuhur", "Ashar", "Maghrib", "Isya")
            .map { MesinWaktuShalat.HasilWaktuShalat(it, "--:--", "--:--:--", 0.0) }
    }
    val jadwal by produceState(
        initialValue = WaktuShalatCache.peek(todayKey) ?: emptySchedule,
        todayKey
    ) {
        value = WaktuShalatCache.getOrCompute(context, todayKey) {
            MesinWaktuShalat.hitung(
                konteks = context,
                tahun = currentDate.get(Calendar.YEAR),
                bulan = currentDate.get(Calendar.MONTH) + 1,
                hari = currentDate.get(Calendar.DAY_OF_MONTH),
                lintang = lat,
                bujur = lon,
                elevasi = elev,
                zonaWaktu = tz,
                kriteria = kriteria,
                ikhSubuh = prefs.ikhSubuh,
                ikhTerbit = prefs.ikhTerbit,
                ikhDhuha = prefs.ikhDhuha,
                ikhDzuhur = prefs.ikhDzuhur,
                ikhAshar = prefs.ikhAshar,
                ikhMaghrib = prefs.ikhMaghrib,
                ikhIsya = prefs.ikhIsya,
                pembulatan = resolvedSettings.pembulatan,
                gunakanElevasi = true,
                faktorAshar = resolvedSettings.faktorAshar,
                is24HourFormat = prefs.is24HourFormat
            )
        }
    }
    val jadwalTomorrowForNext by produceState(
        initialValue = WaktuShalatCache.peek(tomorrowKey) ?: emptySchedule,
        tomorrowKey
    ) {
        value = WaktuShalatCache.getOrCompute(context, tomorrowKey) {
            MesinWaktuShalat.hitung(
                konteks = context,
                tahun = tomorrowDateForNext.get(Calendar.YEAR),
                bulan = tomorrowDateForNext.get(Calendar.MONTH) + 1,
                hari = tomorrowDateForNext.get(Calendar.DAY_OF_MONTH),
                lintang = lat,
                bujur = lon,
                elevasi = elev,
                zonaWaktu = tz,
                kriteria = kriteria,
                ikhSubuh = prefs.ikhSubuh,
                ikhTerbit = prefs.ikhTerbit,
                ikhDhuha = prefs.ikhDhuha,
                ikhDzuhur = prefs.ikhDzuhur,
                ikhAshar = prefs.ikhAshar,
                ikhMaghrib = prefs.ikhMaghrib,
                ikhIsya = prefs.ikhIsya,
                pembulatan = resolvedSettings.pembulatan,
                gunakanElevasi = true,
                faktorAshar = resolvedSettings.faktorAshar,
                is24HourFormat = prefs.is24HourFormat
            )
        }
    }

    // Find next prayer
    var nextPrayerName = "-"
    var nextPrayerTime = "--:--"
    var nextPrayerMillis = 0L

    findNextPrayer(
        schedule = jadwal,
        prayerDate = currentDate,
        currentTimeMillis = currentTimeMillis
    )?.let { next ->
        nextPrayerName = next.name
        nextPrayerTime = next.timeText
        nextPrayerMillis = next.timeMillis
    }
    if (nextPrayerName == "-") {
        findNextPrayer(
            schedule = jadwalTomorrowForNext,
            prayerDate = tomorrowDateForNext,
            currentTimeMillis = currentTimeMillis
        )?.let { next ->
            nextPrayerName = next.name
            nextPrayerTime = next.timeText
            nextPrayerMillis = next.timeMillis
        }
    }

    var countdownStr = "00 : 00 : 00"
    if (nextPrayerMillis > currentTimeMillis) {
        val diff = nextPrayerMillis - currentTimeMillis
        val s = (diff / 1000) % 60
        val m = (diff / (1000 * 60)) % 60
        val h = (diff / (1000 * 60 * 60)) % 24
        countdownStr = String.format(Locale.US, "%02d : %02d : %02d", h, m, s)
    }

    if (showLocationChoiceSheet) {
        LocationChoiceSheet(
            onDismiss = { showLocationChoiceSheet = false },
            onSearchLocation = {
                showLocationChoiceSheet = false
                showCityPickerDialog = true
            },
            onUseCurrentLocation = {
                showLocationChoiceSheet = false
                useCurrentLocation()
            }
        )
    }
    if (showCityPickerDialog) {
        CityLocationPickerDialog(
            onDismiss = { showCityPickerDialog = false },
            onSelect = { city ->
                applyCityLocationToPrefs(prefs, city)
                locationInputMode = "DAFTAR_KOTA"
                locationRevision++
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Gradient Background Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.45f)
                .background(Brush.verticalGradient(listOf(JadwalGradientTop, JadwalGradientBottom)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Top App Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 40.dp, start = 16.dp, end = 16.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                    Text(
                        text = "Jadwal Shalat",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                Row {
                    IconButton(onClick = { showCetakDialog = true }) {
                        Icon(Icons.Outlined.Print, contentDescription = "Cetak Jadwal", tint = Color.White)
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings", tint = Color.White)
                    }
                }
            }

            // Location & Next Prayer Info
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = locName, color = Color.White.copy(alpha = 0.9f), fontSize = 14.sp)
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "$nextPrayerName $nextPrayerTime $tzLabel",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "- $countdownStr",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.GpsFixed, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Update",
                            color = Color.White,
                            fontSize = 14.sp,
                            modifier = Modifier.clickable { showLocationChoiceSheet = true }
                        )
                    }
                    Row(modifier = Modifier.clickable { onNavigateToKiblat() }, verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Explore, contentDescription = "Buka Kompas Kiblat", tint = Color.White, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Arah Kiblat", color = Color.White, fontSize = 14.sp)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // White Card with schedule
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = Color.White
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Date Selector
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 16.dp, bottom = 8.dp),
                        shape = RoundedCornerShape(12.dp),
                        shadowElevation = 4.dp,
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(onClick = { 
                                val c = currentDate.clone() as Calendar
                                c.add(Calendar.DAY_OF_MONTH, -1)
                                currentDate = c 
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Prev Day", tint = JadwalGradientTop)
                            }
                            
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val sdf = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
                                Text(text = sdf.format(currentDate.time), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                                
                                val hijriText = selectedHijri?.let { hijri ->
                                    val mName = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(hijri.second - 1) { "" }
                                    "${hijri.third} $mName ${hijri.first}"
                                } ?: "Memuat Hijriyah"
                                Text(text = hijriText, fontSize = 13.sp, color = Color.Gray)
                            }
                            
                            IconButton(onClick = { 
                                val c = currentDate.clone() as Calendar
                                c.add(Calendar.DAY_OF_MONTH, 1)
                                currentDate = c 
                            }) {
                                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next Day", tint = JadwalGradientTop)
                            }
                        }
                    }
                    
                    // List of Prayers
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                    ) {
                        val icons = mapOf(
                            "Imsak" to Icons.Outlined.NightsStay,
                            "Subuh" to Icons.Outlined.CloudQueue,
                            "Terbit" to Icons.Outlined.WbTwilight,
                            "Dhuha" to Icons.Outlined.WbSunny,
                            "Dzuhur" to Icons.Outlined.LightMode,
                            "Ashar" to Icons.Outlined.Cloud,
                            "Maghrib" to Icons.Outlined.WbTwilight,
                            "Isya" to Icons.Outlined.Bedtime
                        )
                        
                        jadwal.forEach { w ->
                            val isActive = w.nama == nextPrayerName
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isActive) JadwalHighlightBackground else Color.Transparent)
                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = icons[w.nama] ?: Icons.Outlined.Schedule,
                                        contentDescription = null,
                                        tint = Color.DarkGray,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text(text = w.nama, fontSize = 16.sp, color = Color.DarkGray)
                                }
                                
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = w.teksWaktu, fontSize = 16.sp, color = Color.DarkGray, fontWeight = FontWeight.SemiBold)
                                    Text(text = w.teksWaktuMurni, fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    if (showCetakDialog) {
        DialogCetakBulanan(
            onDismiss = { showCetakDialog = false },
            onCetakMasehi = { tahun, bulan ->
                showCetakDialog = false
                PencetakJadwalShalat.cetakJadwalBulanan(
                    context = context,
                    tahun = tahun,
                    bulan = bulan,
                    lintang = lat,
                    bujur = lon,
                    elevasi = elev,
                    zonaWaktu = tz,
                    namaLokasi = locName,
                    prefs = prefs
                )
            },
            onCetakHijriyah = { tahun, bulan ->
                showCetakDialog = false
                PencetakJadwalShalat.cetakImsakiyahBulanan(
                    context = context,
                    tahunHijriah = tahun,
                    bulanHijriah = bulan,
                    lintang = lat,
                    bujur = lon,
                    elevasi = elev,
                    zonaWaktu = tz,
                    namaLokasi = locName,
                    prefs = prefs
                )
            }
        )
    }
}

@Composable
fun DialogCetakBulanan(
    onDismiss: () -> Unit,
    onCetakMasehi: (tahun: Int, bulan: Int) -> Unit,
    onCetakHijriyah: (tahun: Int, bulan: Int) -> Unit
) {
    var jenisJadwal by remember { mutableStateOf(0) } // 0: Masehi, 1: Hijriyah
    
    // Masehi selections
    val currentCal = remember { Calendar.getInstance() }
    var masehiBulan by remember { mutableStateOf(currentCal.get(Calendar.MONTH) + 1) }
    var masehiTahunText by remember { mutableStateOf(currentCal.get(Calendar.YEAR).toString()) }
    
    // Hijriyah selections
    val defaultHijri = remember {
        val jd = CalendarFunctions.gregorianToJde(
            currentCal.get(Calendar.YEAR),
            currentCal.get(Calendar.MONTH) + 1,
            currentCal.get(Calendar.DAY_OF_MONTH).toDouble()
        )
        CalendarFunctions.jdeToHijri(jd)
    }
    var hijriBulan by remember { mutableStateOf(defaultHijri.second) }
    var hijriTahunText by remember { mutableStateOf(defaultHijri.first.toString()) }

    var masehiMonthExpanded by remember { mutableStateOf(false) }
    var hijriMonthExpanded by remember { mutableStateOf(false) }

    val bulanMasehiNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cetak Jadwal Bulanan", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Radio buttons for Jenis Jadwal
                Text("Pilih Jenis Jadwal:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { jenisJadwal = 0 }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (jenisJadwal == 0), onClick = { jenisJadwal = 0 })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jadwal Waktu Shalat (Gregorian/Masehi)")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { jenisJadwal = 1 }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = (jenisJadwal == 1), onClick = { jenisJadwal = 1 })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Jadwal Imsakiyah (Hijriyah)")
                    }
                }

                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))

                if (jenisJadwal == 0) {
                    // Masehi Form
                    Text("Pilihan Bulan & Tahun Masehi:", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                    
                    // Month dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { masehiMonthExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(bulanMasehiNames[masehiBulan - 1])
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = masehiMonthExpanded,
                            onDismissRequest = { masehiMonthExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            bulanMasehiNames.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        masehiBulan = idx + 1
                                        masehiMonthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Year Text Field
                    OutlinedTextField(
                        value = masehiTahunText,
                        onValueChange = { masehiTahunText = it.filter { char -> char.isDigit() } },
                        label = { Text("Tahun Masehi") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    // Hijriyah Form
                    Text("Pilihan Bulan & Tahun Hijriyah:", fontWeight = FontWeight.Medium, fontSize = 14.sp)

                    // Month dropdown
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = { hijriMonthExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(CalendarFunctions.HIJRI_MONTH_NAMES[hijriBulan - 1])
                                Icon(Icons.Filled.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(
                            expanded = hijriMonthExpanded,
                            onDismissRequest = { hijriMonthExpanded = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            CalendarFunctions.HIJRI_MONTH_NAMES.forEachIndexed { idx, name ->
                                DropdownMenuItem(
                                    text = { Text(name) },
                                    onClick = {
                                        hijriBulan = idx + 1
                                        hijriMonthExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Year Text Field
                    OutlinedTextField(
                        value = hijriTahunText,
                        onValueChange = { hijriTahunText = it.filter { char -> char.isDigit() } },
                        label = { Text("Tahun Hijriyah") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (jenisJadwal == 0) {
                        val y = masehiTahunText.toIntOrNull() ?: currentCal.get(Calendar.YEAR)
                        onCetakMasehi(y, masehiBulan)
                    } else {
                        val y = hijriTahunText.toIntOrNull() ?: defaultHijri.first
                        onCetakHijriyah(y, hijriBulan)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F7C6A))
            ) {
                Text("Cetak")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        }
    )
}

