package com.falak.falakpro.ui

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.falak.falakpro.location.LocationHelper
import com.falak.falakpro.premium.AstroAssetPreloader
import com.falak.falakpro.premium.AstroDataUtils
import com.falak.falakpro.premium.AstroMath
import com.falak.falakpro.premium.AstroTransform
import com.falak.falakpro.premium.DynamicalTimeEngine
import com.falak.falakpro.premium.ElpMpp02LunarEngine
import com.falak.falakpro.premium.Iau2006Nutation
import com.falak.falakpro.premium.LunarFunctions
import com.falak.falakpro.premium.PreferencesHelper
import com.falak.falakpro.premium.SolarFunctions
import com.falak.falakpro.premium.Vsop87SolarEngine
import com.falak.falakpro.ui.theme.GreenPrimary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

@Composable
fun SunMoonPositionPanel(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val prefs = remember { PreferencesHelper(context) }
    val locationHelper = remember { LocationHelper(context) }
    val locationState by locationHelper.locationState.collectAsState()
    val now = remember { Calendar.getInstance() }

    var tick by remember { mutableIntStateOf(0) }
    var sunMoonTab by remember { mutableIntStateOf(0) }
    var manualCalculationInput by remember { mutableStateOf<SunMoonInput?>(null) }
    var showManualInput by remember { mutableStateOf(true) }
    var locationInputMode by remember { mutableStateOf(prefs.locationInputMode) }
    var showCityPicker by remember { mutableStateOf(false) }
    var locationName by remember { mutableStateOf(prefs.manualLokasiNama) }
    val initialLat = remember { decimalToDmsParts(prefs.manualLat) }
    val initialLon = remember { decimalToDmsParts(prefs.manualLon) }
    var latDeg by remember { mutableStateOf(initialLat.deg.toString()) }
    var latMin by remember { mutableStateOf(initialLat.min.toString()) }
    var latSec by remember { mutableStateOf(initialLat.sec.toString()) }
    var latSouth by remember { mutableStateOf(prefs.manualLat < 0.0) }
    var lonDeg by remember { mutableStateOf(initialLon.deg.toString()) }
    var lonMin by remember { mutableStateOf(initialLon.min.toString()) }
    var lonSec by remember { mutableStateOf(initialLon.sec.toString()) }
    var lonEast by remember { mutableStateOf(prefs.manualLon >= 0.0) }
    var elevText by remember { mutableStateOf(String.format(Locale.US, "%.1f", prefs.manualElev)) }
    var timezoneText by remember { mutableStateOf(String.format(Locale.US, "%.1f", prefs.manualTimezone)) }
    var yearText by remember { mutableStateOf(now.get(Calendar.YEAR).toString()) }
    var monthText by remember { mutableStateOf((now.get(Calendar.MONTH) + 1).toString()) }
    var dayText by remember { mutableStateOf(now.get(Calendar.DAY_OF_MONTH).toString()) }
    var hourText by remember { mutableStateOf(now.get(Calendar.HOUR_OF_DAY).toString()) }
    var minuteText by remember { mutableStateOf(now.get(Calendar.MINUTE).toString()) }
    var secondText by remember { mutableStateOf(now.get(Calendar.SECOND).toString()) }

    fun setNow() {
        val c = Calendar.getInstance()
        yearText = c.get(Calendar.YEAR).toString()
        monthText = (c.get(Calendar.MONTH) + 1).toString()
        dayText = c.get(Calendar.DAY_OF_MONTH).toString()
        hourText = c.get(Calendar.HOUR_OF_DAY).toString()
        minuteText = c.get(Calendar.MINUTE).toString()
        secondText = c.get(Calendar.SECOND).toString()
    }

    fun setDmsFromDecimal(lat: Double, lon: Double) {
        val latParts = decimalToDmsParts(lat)
        latDeg = latParts.deg.toString()
        latMin = latParts.min.toString()
        latSec = latParts.sec.toString()
        latSouth = lat < 0.0
        val lonParts = decimalToDmsParts(lon)
        lonDeg = lonParts.deg.toString()
        lonMin = lonParts.min.toString()
        lonSec = lonParts.sec.toString()
        lonEast = lon >= 0.0
    }

    fun useGpsLocation() {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            locationInputMode = "GPS"
            prefs.locationInputMode = "GPS"
            Toast.makeText(context, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
            locationHelper.refreshLocation { success ->
                Toast.makeText(
                    context,
                    if (success) "Lokasi GPS berhasil diperbarui!" else "Gagal memperbarui lokasi. Nyalakan GPS Anda.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            useGpsLocation()
        } else {
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            locationHelper.startLocationUpdates()
        }
    }

    fun requestGpsLocation() {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            useGpsLocation()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    LaunchedEffect(locationInputMode) {
        if (locationInputMode == "GPS") {
            locationHelper.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            tick++
            delay(1000)
        }
    }

    val input = remember(
        locationInputMode, locationName, latDeg, latMin, latSec, latSouth, lonDeg, lonMin, lonSec, lonEast,
        elevText, timezoneText, locationState, yearText, monthText, dayText, hourText, minuteText, secondText, tick
    ) {
        val manualLat = dmsToDecimal(latDeg, latMin, latSec, positive = !latSouth)
        val manualLon = dmsToDecimal(lonDeg, lonMin, lonSec, positive = lonEast)
        val useGps = locationInputMode == "GPS"
        val activeLat = if (useGps && locationState.latitude != 0.0) locationState.latitude else manualLat
        val activeLon = if (useGps && locationState.longitude != 0.0) locationState.longitude else manualLon
        val activeElevation = if (useGps && locationState.altitude != 0.0) locationState.altitude else (elevText.toDoubleOrNull() ?: 0.0)
        val activeTimezone = if (useGps) timezoneFromLongitude(activeLon) else (timezoneText.toDoubleOrNull() ?: 7.0)
        val activeName = if (useGps && locationState.address != "Mencari Lokasi...") locationState.address else locationName.ifBlank { "Lokasi" }
        SunMoonInput(
            locationName = activeName,
            latitude = activeLat,
            longitude = activeLon,
            elevation = activeElevation,
            timezone = activeTimezone,
            year = yearText.toIntOrNull() ?: now.get(Calendar.YEAR),
            month = monthText.toIntOrNull()?.coerceIn(1, 12) ?: (now.get(Calendar.MONTH) + 1),
            day = dayText.toIntOrNull()?.coerceIn(1, 31) ?: now.get(Calendar.DAY_OF_MONTH),
            hour = hourText.toIntOrNull()?.coerceIn(0, 23) ?: now.get(Calendar.HOUR_OF_DAY),
            minute = minuteText.toIntOrNull()?.coerceIn(0, 59) ?: now.get(Calendar.MINUTE),
            second = secondText.toIntOrNull()?.coerceIn(0, 59) ?: now.get(Calendar.SECOND)
        )
    }

    if (showCityPicker) {
        CityLocationPickerDialog(
            onDismiss = { showCityPicker = false },
            onSelect = { city ->
                applyCityLocationToPrefs(prefs, city)
                locationInputMode = "DAFTAR_KOTA"
                locationName = city.displayName
                setDmsFromDecimal(city.latitude, city.longitude)
                elevText = String.format(Locale.US, "%.1f", city.elevation)
                timezoneText = String.format(Locale.US, "%.1f", city.timezone)
            }
        )
    }

    val realtimeInput = remember(locationState, tick) {
        val c = Calendar.getInstance()
        if (locationState.latitude == 0.0 && locationState.longitude == 0.0) {
            null
        } else {
            SunMoonInput(
                locationName = if (locationState.address != "Mencari Lokasi...") locationState.address else "Lokasi perangkat",
                latitude = locationState.latitude,
                longitude = locationState.longitude,
                elevation = locationState.altitude,
                timezone = timezoneFromLongitude(locationState.longitude),
                year = c.get(Calendar.YEAR),
                month = c.get(Calendar.MONTH) + 1,
                day = c.get(Calendar.DAY_OF_MONTH),
                hour = c.get(Calendar.HOUR_OF_DAY),
                minute = c.get(Calendar.MINUTE),
                second = c.get(Calendar.SECOND)
            )
        }
    }

    val inputResult by produceState<SunMoonResult?>(initialValue = null, manualCalculationInput) {
        value = manualCalculationInput?.let { requestedInput ->
            withContext(Dispatchers.Default) {
                AstroAssetPreloader.ensureCore(context)
                computeSunMoonResult(requestedInput)
            }
        }
    }
    val realtimeResult by produceState<SunMoonResult?>(initialValue = null, realtimeInput) {
        value = realtimeInput?.let { currentInput ->
            withContext(Dispatchers.Default) {
                AstroAssetPreloader.ensureCore(context)
                computeSunMoonResult(currentInput)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        TabRow(selectedTabIndex = sunMoonTab, containerColor = Color.Transparent, contentColor = GreenPrimary) {
            Tab(
                selected = sunMoonTab == 0,
                onClick = { sunMoonTab = 0 },
                text = { Text("Realtime", fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = sunMoonTab == 1,
                onClick = { sunMoonTab = 1 },
                text = { Text("Hitung Manual", fontWeight = FontWeight.Bold) }
            )
        }

        if (sunMoonTab == 0) {
            SectionTitle("Data matahari bulan realtime")
            realtimeResult?.let { data ->
                SummaryCard(data)
                BodyPositionCard("Matahari Realtime", data.sun)
                BodyPositionCard("Bulan Realtime", data.moon)
            } ?: Text("Menunggu lokasi perangkat...", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            if (showManualInput) {
                InputCard(
                    locationInputMode = locationInputMode,
                    onLocationModeChange = { mode ->
                        when (mode) {
                            "GPS" -> requestGpsLocation()
                            "DAFTAR_KOTA" -> {
                                locationInputMode = "DAFTAR_KOTA"
                                prefs.locationInputMode = "DAFTAR_KOTA"
                                showCityPicker = true
                            }
                            else -> {
                                locationInputMode = "MANUAL"
                                prefs.locationInputMode = "MANUAL"
                            }
                        }
                    },
                    onPickCity = { showCityPicker = true },
                    locationName = locationName,
                    onLocationNameChange = { locationName = it },
                    latDeg = latDeg,
                    onLatDegChange = { latDeg = it },
                    latMin = latMin,
                    onLatMinChange = { latMin = it },
                    latSec = latSec,
                    onLatSecChange = { latSec = it },
                    latSouth = latSouth,
                    onLatSouthChange = { latSouth = it },
                    lonDeg = lonDeg,
                    onLonDegChange = { lonDeg = it },
                    lonMin = lonMin,
                    onLonMinChange = { lonMin = it },
                    lonSec = lonSec,
                    onLonSecChange = { lonSec = it },
                    lonEast = lonEast,
                    onLonEastChange = { lonEast = it },
                    elevText = elevText,
                    onElevChange = { elevText = it },
                    timezoneText = timezoneText,
                    onTimezoneChange = { timezoneText = it },
                    yearText = yearText,
                    onYearChange = { yearText = it },
                    monthText = monthText,
                    onMonthChange = { monthText = it },
                    dayText = dayText,
                    onDayChange = { dayText = it },
                    hourText = hourText,
                    onHourChange = { hourText = it },
                    minuteText = minuteText,
                    onMinuteChange = { minuteText = it },
                    secondText = secondText,
                    onSecondChange = { secondText = it },
                    onSetNow = { setNow(); tick++ },
                    onCalculate = {
                        manualCalculationInput = input
                        showManualInput = false
                    }
                )
            } else {
                Button(onClick = { showManualInput = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Ubah Input")
                }
            }

            SectionTitle("Hasil perhitungan manual")
            inputResult?.let { data ->
                SummaryCard(data)
                BodyPositionCard("Matahari", data.sun)
                BodyPositionCard("Bulan", data.moon)
            } ?: Text("Isi input lalu tekan HITUNG.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InputCard(
    locationInputMode: String,
    onLocationModeChange: (String) -> Unit,
    onPickCity: () -> Unit,
    locationName: String,
    onLocationNameChange: (String) -> Unit,
    latDeg: String,
    onLatDegChange: (String) -> Unit,
    latMin: String,
    onLatMinChange: (String) -> Unit,
    latSec: String,
    onLatSecChange: (String) -> Unit,
    latSouth: Boolean,
    onLatSouthChange: (Boolean) -> Unit,
    lonDeg: String,
    onLonDegChange: (String) -> Unit,
    lonMin: String,
    onLonMinChange: (String) -> Unit,
    lonSec: String,
    onLonSecChange: (String) -> Unit,
    lonEast: Boolean,
    onLonEastChange: (Boolean) -> Unit,
    elevText: String,
    onElevChange: (String) -> Unit,
    timezoneText: String,
    onTimezoneChange: (String) -> Unit,
    yearText: String,
    onYearChange: (String) -> Unit,
    monthText: String,
    onMonthChange: (String) -> Unit,
    dayText: String,
    onDayChange: (String) -> Unit,
    hourText: String,
    onHourChange: (String) -> Unit,
    minuteText: String,
    onMinuteChange: (String) -> Unit,
    secondText: String,
    onSecondChange: (String) -> Unit,
    onSetNow: () -> Unit,
    onCalculate: () -> Unit
) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val showLocationFields = locationInputMode != "GPS"
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text("Input lokasi & waktu", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = locationInputMode == "MANUAL",
                    onClick = { onLocationModeChange("MANUAL") },
                    label = { Text("Manual") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = locationInputMode == "DAFTAR_KOTA",
                    onClick = { onLocationModeChange("DAFTAR_KOTA") },
                    label = { Text("Kota") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = locationInputMode == "GPS",
                    onClick = { onLocationModeChange("GPS") },
                    label = { Text("GPS") },
                    modifier = Modifier.weight(1f)
                )
            }
            if (showLocationFields) {
                if (locationInputMode == "DAFTAR_KOTA") {
                    Button(onClick = onPickCity, modifier = Modifier.fillMaxWidth()) {
                        Text("Pilih Kota dari Database Offline")
                    }
                }
                OutlinedTextField(value = locationName, onValueChange = onLocationNameChange, label = { Text("Nama lokasi") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                DmsInputRow(
                    title = "Lintang",
                    deg = latDeg,
                    onDegChange = onLatDegChange,
                    min = latMin,
                    onMinChange = onLatMinChange,
                    sec = latSec,
                    onSecChange = onLatSecChange,
                    negative = latSouth,
                    onNegativeChange = onLatSouthChange,
                    positiveLabel = "LU",
                    negativeLabel = "LS",
                    enabled = true
                )
                DmsInputRow(
                    title = "Bujur",
                    deg = lonDeg,
                    onDegChange = onLonDegChange,
                    min = lonMin,
                    onMinChange = onLonMinChange,
                    sec = lonSec,
                    onSecChange = onLonSecChange,
                    negative = !lonEast,
                    onNegativeChange = { onLonEastChange(!it) },
                    positiveLabel = "BT",
                    negativeLabel = "BB",
                    enabled = true
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberField("Tinggi m", elevText, onElevChange, Modifier.weight(1f))
                    NumberField("Time zone", timezoneText, onTimezoneChange, Modifier.weight(1f))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Tanggal", dayText, onDayChange, Modifier.weight(1f))
                NumberField("Bulan", monthText, onMonthChange, Modifier.weight(1f))
                NumberField("Tahun", yearText, onYearChange, Modifier.weight(1.2f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NumberField("Jam", hourText, onHourChange, Modifier.weight(1f))
                NumberField("Menit", minuteText, onMinuteChange, Modifier.weight(1f))
                NumberField("Detik", secondText, onSecondChange, Modifier.weight(1f))
            }
            Button(onClick = onSetNow, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Schedule, contentDescription = null)
                Spacer(Modifier.padding(horizontal = 4.dp))
                Text("Pakai Waktu Sekarang")
            }
            Button(onClick = onCalculate, modifier = Modifier.fillMaxWidth(), colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("HITUNG POSISI", fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Black,
        fontSize = 18.sp,
        color = GreenPrimary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = modifier
    )
}

@Composable
private fun DmsInputRow(
    title: String,
    deg: String,
    onDegChange: (String) -> Unit,
    min: String,
    onMinChange: (String) -> Unit,
    sec: String,
    onSecChange: (String) -> Unit,
    negative: Boolean,
    onNegativeChange: (Boolean) -> Unit,
    positiveLabel: String,
    negativeLabel: String,
    enabled: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            NumberField("°", deg, onDegChange, Modifier.weight(1f), enabled)
            NumberField("M", min, onMinChange, Modifier.width(86.dp), enabled)
            NumberField("S", sec, onSecChange, Modifier.width(86.dp), enabled)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !negative,
                onClick = { if (enabled) onNegativeChange(false) },
                label = { Text(positiveLabel) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
            FilterChip(
                selected = negative,
                onClick = { if (enabled) onNegativeChange(true) },
                label = { Text(negativeLabel) },
                enabled = enabled,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SummaryCard(result: SunMoonResult) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(result.input.locationName, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = GreenPrimary)
            DataRow("JD UT", fmt(result.jdUt, 6))
            DataRow("JDE TD", fmt(result.jdeTd, 6))
            DataRow("Delta T", "${fmt(result.deltaT, 2)} detik")
            DataRow("GAST", fmtDeg(result.gast))
            DataRow("EoT", "${fmt(result.eotMinutes, 3)} menit")
        }
    }
}

@Composable
private fun BodyPositionCard(title: String, body: BodyPositionResult) {
    Card(shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontWeight = FontWeight.Black, fontSize = 20.sp)
            Text("Geosentris true", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PositionRows(body.geoTrue)
            Text("Geosentris apparent", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PositionRows(body.geoApparent)
            Text("Toposentris true", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PositionRows(body.topoTrue)
            Text("Toposentris apparent", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            PositionRows(body.topoApparent)
            DataRow("Jarak", body.distanceText)
            DataRow("Horizontal parallax", fmtDeg(body.horizontalParallaxDeg))
            DataRow("SD", fmtDeg(body.semidiameterDeg))
            body.illuminationPercent?.let { DataRow("Iluminasi", "${fmt(it, 3)} %") }
        }
    }
}

@Composable
private fun PositionRows(position: PositionValues) {
    DataRow("Bujur", fmtDeg(position.longitude))
    DataRow("Lintang", fmtDeg(position.latitude))
    DataRow("RA", fmtHms(position.ra))
    DataRow("Deklinasi", fmtDeg(position.dec))
    DataRow("Azimuth", fmtDeg(position.azimuth))
    DataRow("Alt", fmtDeg(position.altitude))
}

@Composable
private fun DataRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

private data class SunMoonInput(
    val locationName: String,
    val latitude: Double,
    val longitude: Double,
    val elevation: Double,
    val timezone: Double,
    val year: Int,
    val month: Int,
    val day: Int,
    val hour: Int,
    val minute: Int,
    val second: Int
)

private data class SunMoonResult(
    val input: SunMoonInput,
    val jdUt: Double,
    val jdeTd: Double,
    val deltaT: Double,
    val gast: Double,
    val eotMinutes: Double,
    val sun: BodyPositionResult,
    val moon: BodyPositionResult
)

private data class BodyPositionResult(
    val geoTrue: PositionValues,
    val geoApparent: PositionValues,
    val topoTrue: PositionValues,
    val topoApparent: PositionValues,
    val distanceText: String,
    val horizontalParallaxDeg: Double,
    val semidiameterDeg: Double,
    val illuminationPercent: Double?
)

private data class PositionValues(
    val longitude: Double,
    val latitude: Double,
    val ra: Double,
    val dec: Double,
    val azimuth: Double,
    val altitude: Double
)

private fun computeSunMoonResult(input: SunMoonInput): SunMoonResult {
    val localHour = input.hour + input.minute / 60.0 + input.second / 3600.0
    val jdUt = AstroTimeKmjd(input.day, input.month, input.year, localHour, input.timezone)
    val deltaT = DynamicalTimeEngine.deltaT(jdUt)
    val jdeTd = jdUt + deltaT / 86400.0
    val gast = AstroDataUtils.calculateGAST(jdUt)
    val eot = SolarFunctions.equationOfTime(jdeTd)

    val sunTrue = Vsop87SolarEngine.computeGeometric(jdeTd)
    val sunApp = Vsop87SolarEngine.compute(jdeTd)
    val moonTrue = ElpMpp02LunarEngine.computeTrue(jdeTd)
    val moonApp = ElpMpp02LunarEngine.computeGeometric(jdeTd)

    val sunHp = AstroMath.SOLAR_PARALLAX_ARCSEC / 3600.0 / sunApp.distanceAU
    val sunSd = Math.toDegrees(asin((695700.0 / (sunApp.distanceAU * 149597870.7)).coerceIn(-1.0, 1.0)))
    val illum = LunarFunctions.moonIllumination(
        sunApp.ra, sunApp.dec, sunApp.distanceAU,
        moonApp.ra, moonApp.dec, moonApp.distanceAU * 149597870.7
    ).illuminatedFraction * 100.0

    return SunMoonResult(
        input = input,
        jdUt = jdUt,
        jdeTd = jdeTd,
        deltaT = deltaT,
        gast = gast,
        eotMinutes = eot,
        sun = bodyResult(
            trueLon = sunTrue.longitudeEcliptic,
            trueLat = sunTrue.latitudeEcliptic,
            trueRa = sunTrue.ra,
            trueDec = sunTrue.dec,
            appLon = sunApp.longitudeEcliptic,
            appLat = sunApp.latitudeEcliptic,
            appRa = sunApp.ra,
            appDec = sunApp.dec,
            hpDeg = sunHp,
            distanceText = "${fmt(sunApp.distanceAU, 8)} AU",
            sdDeg = sunSd,
            illumination = null,
            input = input,
            jdUt = jdUt,
            jdeTd = jdeTd
        ),
        moon = bodyResult(
            trueLon = moonTrue.longitudeEcliptic,
            trueLat = moonTrue.latitudeEcliptic,
            trueRa = moonTrue.ra,
            trueDec = moonTrue.dec,
            appLon = moonApp.longitudeEcliptic,
            appLat = moonApp.latitudeEcliptic,
            appRa = moonApp.ra,
            appDec = moonApp.dec,
            hpDeg = moonApp.horizontalParallax,
            distanceText = "${fmt(moonApp.distanceAU * 149597870.7, 1)} km",
            sdDeg = moonApp.semidiameter,
            illumination = illum,
            input = input,
            jdUt = jdUt,
            jdeTd = jdeTd
        )
    )
}

private fun bodyResult(
    trueLon: Double,
    trueLat: Double,
    trueRa: Double,
    trueDec: Double,
    appLon: Double,
    appLat: Double,
    appRa: Double,
    appDec: Double,
    hpDeg: Double,
    distanceText: String,
    sdDeg: Double,
    illumination: Double?,
    input: SunMoonInput,
    jdUt: Double,
    jdeTd: Double
): BodyPositionResult {
    val topoTrue = topocentricEquatorial(trueRa, trueDec, hpDeg, jdUt, input.latitude, input.longitude, input.elevation)
    val topoApp = topocentricEquatorial(appRa, appDec, hpDeg, jdUt, input.latitude, input.longitude, input.elevation)
    val topoTrueEcl = equatorialToEcliptic(topoTrue.first, topoTrue.second, Iau2006Nutation.meanObliquityDeg(jdeTd))
    val topoAppEcl = equatorialToEcliptic(topoApp.first, topoApp.second, AstroDataUtils.calculateTrueObliquity(jdeTd))
    val geoTrueHz = horizontal(trueRa, trueDec, jdUt, input.latitude, input.longitude)
    val geoAppHz = horizontal(appRa, appDec, jdUt, input.latitude, input.longitude)
    val topoTrueHz = horizontal(topoTrue.first, topoTrue.second, jdUt, input.latitude, input.longitude)
    val topoAppHz = horizontal(topoApp.first, topoApp.second, jdUt, input.latitude, input.longitude)

    return BodyPositionResult(
        geoTrue = PositionValues(trueLon, trueLat, trueRa, trueDec, geoTrueHz.second, geoTrueHz.first),
        geoApparent = PositionValues(appLon, appLat, appRa, appDec, geoAppHz.second, geoAppHz.first),
        topoTrue = PositionValues(topoTrueEcl.first, topoTrueEcl.second, topoTrue.first, topoTrue.second, topoTrueHz.second, topoTrueHz.first),
        topoApparent = PositionValues(topoAppEcl.first, topoAppEcl.second, topoApp.first, topoApp.second, topoAppHz.second, topoAppHz.first),
        distanceText = distanceText,
        horizontalParallaxDeg = hpDeg,
        semidiameterDeg = sdDeg,
        illuminationPercent = illumination
    )
}

private fun horizontal(ra: Double, dec: Double, jdUt: Double, lat: Double, lon: Double): Pair<Double, Double> {
    val gast = AstroDataUtils.calculateGAST(jdUt)
    val ha = AstroMath.mod(gast + lon - ra, 360.0)
    return AstroTransform.equatorialToHorizontal(ha, dec, lat)
}

private fun topocentricEquatorial(
    ra: Double,
    dec: Double,
    hpDeg: Double,
    jdUt: Double,
    lat: Double,
    lon: Double,
    elev: Double
): Pair<Double, Double> {
    val gast = AstroDataUtils.calculateGAST(jdUt)
    val ha = AstroMath.mod(gast + lon - ra, 360.0)
    val u = atan(0.99664719 * tan(Math.toRadians(lat)))
    val x = cos(u) + (elev / AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_M) * cos(Math.toRadians(lat))
    val y = 0.99664719 * sin(u) + (elev / AstroTransform.AA_EARTH_EQUATORIAL_RADIUS_M) * sin(Math.toRadians(lat))
    val hp = Math.toRadians(hpDeg)
    val haRad = Math.toRadians(ha)
    val decRad = Math.toRadians(dec)
    val deltaRa = Math.toDegrees(atan2(-x * sin(hp) * sin(haRad), cos(decRad) - x * sin(hp) * cos(haRad)))
    val raTopo = AstroMath.mod(ra + deltaRa, 360.0)
    val decTopo = Math.toDegrees(
        atan2(
            (sin(decRad) - y * sin(hp)) * cos(Math.toRadians(deltaRa)),
            cos(decRad) - x * sin(hp) * cos(haRad)
        )
    )
    return Pair(raTopo, decTopo)
}

private fun equatorialToEcliptic(ra: Double, dec: Double, epsilon: Double): Pair<Double, Double> {
    val raRad = Math.toRadians(ra)
    val decRad = Math.toRadians(dec)
    val epsRad = Math.toRadians(epsilon)
    val beta = Math.toDegrees(
        asin(sin(decRad) * cos(epsRad) - cos(decRad) * sin(epsRad) * sin(raRad))
    )
    val y = sin(raRad) * cos(epsRad) + tan(decRad) * sin(epsRad)
    val x = cos(raRad)
    val lambda = AstroMath.mod(Math.toDegrees(atan2(y, x)), 360.0)
    return Pair(lambda, beta)
}

private fun AstroTimeKmjd(day: Int, month: Int, year: Int, hourDes: Double, timezone: Double): Double =
    com.falak.falakpro.premium.AstroTime.kmjd(day, month, year, hourDes, timezone)

private data class DmsParts(val deg: Int, val min: Int, val sec: Int)

private fun decimalToDmsParts(value: Double): DmsParts {
    val absValue = kotlin.math.abs(value)
    val deg = absValue.toInt()
    val minuteFull = (absValue - deg) * 60.0
    val min = minuteFull.toInt()
    val sec = kotlin.math.round((minuteFull - min) * 60.0).toInt().coerceIn(0, 59)
    return DmsParts(deg, min, sec)
}

private fun dmsToDecimal(deg: String, min: String, sec: String, positive: Boolean): Double {
    val value = (deg.toDoubleOrNull() ?: 0.0) +
        (min.toDoubleOrNull() ?: 0.0) / 60.0 +
        (sec.toDoubleOrNull() ?: 0.0) / 3600.0
    return if (positive) value else -value
}

private fun fmt(value: Double, digits: Int): String =
    "%.${digits}f".format(Locale.US, value)

private fun fmtDeg(value: Double): String =
    fmtDms(value)

private fun fmtDms(value: Double): String {
    val sign = if (value < 0.0) "-" else ""
    val absValue = kotlin.math.abs(value)
    var deg = absValue.toInt()
    val minuteFull = (absValue - deg) * 60.0
    var min = minuteFull.toInt()
    var sec = (minuteFull - min) * 60.0
    if (sec >= 59.9995) {
        sec = 0.0
        min += 1
    }
    if (min >= 60) {
        min = 0
        deg += 1
    }
    return "%s%d° %02d' %06.3f\"".format(Locale.US, sign, deg, min, sec)
}

private fun fmtHms(raDeg: Double): String {
    val totalSeconds = AstroMath.mod(raDeg, 360.0) / 15.0 * 3600.0
    val h = (totalSeconds / 3600.0).toInt()
    val m = ((totalSeconds - h * 3600.0) / 60.0).toInt()
    val s = totalSeconds - h * 3600.0 - m * 60.0
    return "%02d:%02d:%06.3f".format(Locale.US, h, m, s)
}
