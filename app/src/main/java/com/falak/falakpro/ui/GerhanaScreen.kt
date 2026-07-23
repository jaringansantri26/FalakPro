package com.falak.falakpro.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.falak.falakpro.premium.EclipseResultItem
import com.falak.falakpro.ui.theme.GreenPrimary
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

// Warna aksen emas (sama dengan DataFalakScreen)
private val GoldAccent = Color(0xFFD4AF37)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerhanaScreen(
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (Double, Boolean, String, Double, Double, Double, Double, String) -> Unit,
    viewModel: GerhanaViewModel = viewModel()
) {
    val context = LocalContext.current
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // --- UI State ---
    var showSettings by remember { mutableStateOf(false) }
    var eclipseTab by remember { mutableIntStateOf(0) } // 0: Matahari, 1: Bulan
    val eclipseTabs = listOf("Gerhana Matahari", "Gerhana Bulan")

    var yearStr by remember { mutableStateOf("2027") }
    var typology by remember { mutableStateOf("Global") }
    var locationMode by remember { mutableStateOf("Otomatis") }
    var showCityPickerDialog by remember { mutableStateOf(false) }

    // --- Location State ---
    var locName by remember { mutableStateOf("") }
    var latStr by remember { mutableStateOf("") }
    var lonStr by remember { mutableStateOf("") }
    var altitude by remember { mutableStateOf("10") }
    var timezone by remember { mutableStateOf("7") }

    // --- GPS State ---
    var gpsLat  by remember { mutableStateOf(-6.1754) }
    var gpsLon  by remember { mutableStateOf(106.8272) }
    var gpsElev by remember { mutableStateOf(10.0) }
    var gpsName by remember { mutableStateOf("Jakarta (Default)") }

    @SuppressLint("MissingPermission")
    fun fetchGpsLocation() {
        val fusedClient = LocationServices.getFusedLocationProviderClient(context)
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    gpsLat  = location.latitude
                    gpsLon  = location.longitude
                    gpsElev = location.altitude
                    gpsName = try {
                        val geocoder = Geocoder(context, Locale("id", "ID"))
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val subLocality = addr.subLocality ?: ""
                            val rawLocality = addr.locality ?: addr.subAdminArea ?: "Lokasi Terdeteksi"
                            val cleanLocality = rawLocality
                                .replace("Kecamatan ", "", ignoreCase = true)
                                .replace("Kabupaten ", "", ignoreCase = true)
                                .replace("Kota ", "", ignoreCase = true)
                                .trim()
                            if (subLocality.isNotEmpty()) "$subLocality, $cleanLocality" else cleanLocality
                        } else "Lokasi GPS Aktif"
                    } catch (e: Exception) { "Lokasi GPS Aktif" }
                    
                    // Prefill manual inputs!
                    latStr = String.format(Locale.US, "%.6f", location.latitude)
                    lonStr = String.format(Locale.US, "%.6f", location.longitude)
                    altitude = String.format(Locale.US, "%.0f", location.altitude)
                    locName = gpsName
                    
                    android.widget.Toast.makeText(context,
                        "GPS Berhasil: $gpsName (${String.format(Locale.US, "%.4f", gpsLat)}, ${String.format(Locale.US, "%.4f", gpsLon)})",
                        android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "GPS tidak tersedia, menggunakan Manual", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            fetchGpsLocation()
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fetchGpsLocation()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Tentukan lokasi aktif
    val activeLat = if (locationMode != "Otomatis") {
        latStr.toDoubleOrNull() ?: gpsLat
    } else gpsLat

    val activeLon = if (locationMode != "Otomatis") {
        lonStr.toDoubleOrNull() ?: gpsLon
    } else gpsLon

    val activeElev = if (locationMode != "Otomatis") {
        altitude.toDoubleOrNull() ?: gpsElev
    } else gpsElev

    val activeLocName = if (locationMode != "Otomatis") {
        locName.ifEmpty { "Manual" }
    } else {
        gpsName.ifEmpty { "Koordinat" }
    }

    if (showCityPickerDialog) {
        CityLocationPickerDialog(
            onDismiss = { showCityPickerDialog = false },
            onSelect = { city ->
                locationMode = "Daftar Kota"
                locName = city.displayName
                latStr = String.format(Locale.US, "%.6f", city.latitude)
                lonStr = String.format(Locale.US, "%.6f", city.longitude)
                altitude = String.format(Locale.US, "%.0f", city.elevation)
                timezone = String.format(Locale.US, "%.1f", city.timezone)
            }
        )
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 80.dp)
        ) {
            // =============================================
            // GRADIENT HEADER
            // =============================================
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(GreenPrimary, GreenPrimary.copy(alpha = 0.8f))
                        )
                    )
                    .padding(top = 16.dp, bottom = 24.dp, start = 20.dp, end = 20.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Tombol Back
                        IconButton(
                            onClick = onNavigateBack,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Kembali",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Kalkulator Gerhana",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, null, tint = GoldAccent.copy(alpha = 0.85f), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = activeLocName,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            val latDir2 = if (activeLat >= 0) "LU" else "LS"
                            val lonDir2 = if (activeLon >= 0) "BT" else "BB"
                            Text(
                                text = "${String.format("%.4f", kotlin.math.abs(activeLat))}° $latDir2, " +
                                       "${String.format("%.4f", kotlin.math.abs(activeLon))}° $lonDir2 " +
                                       "(Elev: ${activeElev.toInt()}m)",
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 11.sp
                            )
                        }

                        // Tombol Opsi / Tutup
                        Surface(
                            color = if (showSettings) Color.White else Color.White.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.clickable { showSettings = !showSettings }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Icon(
                                    if (showSettings) Icons.Default.Close else Icons.Default.Tune,
                                    null,
                                    tint = if (showSettings) GreenPrimary else Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    if (showSettings) "Tutup" else "Opsi",
                                    color = if (showSettings) GreenPrimary else Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }            // =============================================
            // PANEL PENGATURAN (collapsible)
            // =============================================
            AnimatedVisibility(
                visible = showSettings,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Pengaturan Lokasi", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 16.sp)

                        Text("Mode Lokasi", fontSize = 12.sp, color = Color.Gray)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                label = "GPS Otomatis",
                                isSelected = locationMode == "Otomatis",
                                onClick = {
                                    locationMode = "Otomatis"
                                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                        fetchGpsLocation()
                                    } else {
                                        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ChoiceChip(
                                label = "Daftar Kota",
                                isSelected = locationMode == "Daftar Kota",
                                onClick = {
                                    locationMode = "Daftar Kota"
                                    showCityPickerDialog = true
                                },
                                modifier = Modifier.weight(1f)
                            )
                            ChoiceChip(
                                label = "Manual",
                                isSelected = locationMode == "Manual",
                                onClick = { locationMode = "Manual" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        AnimatedVisibility(visible = locationMode != "Otomatis") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (locationMode == "Daftar Kota") {
                                    Button(
                                        onClick = { showCityPickerDialog = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text("Pilih Kota dari Database Offline")
                                    }
                                }
                                OutlinedTextField(
                                    value = locName,
                                    onValueChange = { locName = it },
                                    label = { Text("Nama Lokasi") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = latStr,
                                        onValueChange = { latStr = it },
                                        label = { Text("Lintang (Desimal)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = lonStr,
                                        onValueChange = { lonStr = it },
                                        label = { Text("Bujur (Desimal)") },
                                        modifier = Modifier.weight(1f),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = altitude,
                                        onValueChange = { altitude = it },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Ketinggian (m)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    OutlinedTextField(
                                        value = timezone,
                                        onValueChange = { timezone = it },
                                        modifier = Modifier.weight(1f),
                                        label = { Text("Timezone (GMT)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                }
                            }
                        }

                        AnimatedVisibility(visible = locationMode == "Otomatis") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFE8F5E9), shape = RoundedCornerShape(10.dp))
                                        .padding(12.dp)
                                ) {
                                    Column {
                                        Text("Lokasi Terdeteksi:", fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 12.sp)
                                        Text("Nama: $gpsName", fontSize = 13.sp, color = Color.Black)
                                        Text("Koordinat: ${String.format(Locale.US, "%.5f", gpsLat)}°, ${String.format(Locale.US, "%.5f", gpsLon)}°", fontSize = 12.sp, color = Color.DarkGray)
                                        Text("Ketinggian: ${gpsElev.toInt()} m", fontSize = 12.sp, color = Color.DarkGray)
                                    }
                                }
                                OutlinedTextField(
                                    value = timezone,
                                    onValueChange = { timezone = it },
                                    modifier = Modifier.fillMaxWidth(),
                                    label = { Text("Timezone (GMT)") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    shape = RoundedCornerShape(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // =============================================
            // TAB: MATAHARI / BULAN
            // =============================================
            TabRow(
                selectedTabIndex = eclipseTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = GreenPrimary,
                indicator = { tabPositions ->
                    TabRowDefaults.Indicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[eclipseTab]),
                        color = GreenPrimary
                    )
                }
            ) {
                eclipseTabs.forEachIndexed { index, title ->
                    Tab(
                        selected = eclipseTab == index,
                        onClick = {
                            eclipseTab = index
                            viewModel.searchResults.value = emptyList()
                        },
                        text = { Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // =============================================
            // MAIN CONTENT
            // =============================================
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Parameter Pencarian",
                            fontWeight = FontWeight.Bold,
                            color = GreenPrimary,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = yearStr,
                            onValueChange = { yearStr = it },
                            label = { Text("Tahun Masehi") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("Jangkauan Pencarian", fontSize = 12.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ChoiceChip(
                                label = "Lokal",
                                isSelected = typology == "Lokal",
                                onClick = { typology = "Lokal" },
                                modifier = Modifier.weight(1f)
                            )
                            ChoiceChip(
                                label = "Global",
                                isSelected = typology == "Global",
                                onClick = { typology = "Global" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = {
                                val yearInt = yearStr.toIntOrNull() ?: 2027
                                val isSolar = eclipseTab == 0
                                viewModel.search(
                                    year = yearInt,
                                    context = context,
                                    isSolar = isSolar,
                                    typology = typology,
                                    lat = activeLat,
                                    lon = activeLon,
                                    elev = activeElev,
                                    timezone = timezone.toDoubleOrNull() ?: 7.0,
                                    locName = activeLocName
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Search, null)
                                Spacer(modifier = Modifier.width(8.dp))
                                val btnText = if (eclipseTab == 0) "CARI GERHANA MATAHARI" else "CARI GERHANA BULAN"
                                Text(btnText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Hasil pencarian
                if (searchResults.isNotEmpty()) {
                    Text(
                        "Hasil Pencarian Tahun $yearStr",
                        fontWeight = FontWeight.Bold,
                        color = GreenPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    searchResults.forEach { result ->
                        EclipseItemCard(result) { jde, isSolar ->
                            onNavigateToDetail(
                                jde, isSolar, typology,
                                activeLat, activeLon, activeElev,
                                timezone.toDoubleOrNull() ?: 7.0,
                                activeLocName
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                } else if (!isLoading && yearStr.isNotEmpty() && searchResults.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        val msg = if (typology == "Lokal") {
                            "Tidak Ada Gerhana yang Terlihat di Lokasi Anda Pada Tahun Ini"
                        } else {
                            "Tidak Ada Gerhana Secara Global Pada Tahun Ini"
                        }
                        Text(msg, color = Color.Gray, fontSize = 14.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

// =============================================
// CARD HASIL GERHANA - desain baru
// =============================================
@Composable
fun EclipseItemCard(result: EclipseResultItem, onNavigateToDetail: (Double, Boolean) -> Unit) {
    val isSolar = result.isSolar
    val accentColor = if (isSolar) Color(0xFFFF9800) else Color(0xFF2196F3)
    val accentBg = accentColor.copy(alpha = 0.08f)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetail(result.jdeGreatest, result.isSolar) },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Ikon besar
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accentBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSolar) Icons.Default.WbSunny else Icons.Default.Brightness2,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(result.title, fontWeight = FontWeight.Bold, color = GreenPrimary, fontSize = 15.sp)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarToday, null, tint = Color.Gray, modifier = Modifier.size(12.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(result.dateString, fontSize = 13.sp, color = Color.DarkGray)
                        result.localTime?.let {
                            if (it != "Tidak Terlihat") {
                                Spacer(Modifier.width(8.dp))
                                Text("| $it", fontSize = 12.sp, color = GreenPrimary, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    // Badge tipe/visibilitas
                    val visibilityText = result.typeString.substringBefore("|").trim()
                    val badgeColor = if (visibilityText.contains("Terlihat Lokal", ignoreCase = true)) GreenPrimary else Color.Gray
                    val badgeBg = badgeColor.copy(alpha = 0.08f)
                    Surface(
                        color = badgeBg,
                        shape = RoundedCornerShape(6.dp),
                        border = BorderStroke(0.5.dp, badgeColor.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = visibilityText,
                            color = badgeColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Magnitude info
                val magText = result.typeString
                    .substringAfter("|", "")
                    .trim()
                    .ifEmpty { "Mag: ${String.format("%.3f", result.magnitude)}" }

                Text(
                    text = magText,
                    fontSize = 12.sp,
                    color = Color.DarkGray,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )

                // Tombol Detail
                Surface(
                    color = GreenPrimary,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.clickable { onNavigateToDetail(result.jdeGreatest, result.isSolar) }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Detail", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.Default.ChevronRight, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun ChoiceChip(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) GreenPrimary else Color(0xFFF5F5F5),
        border = BorderStroke(1.dp, if (isSelected) GreenPrimary else Color.LightGray)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                label,
                color = if (isSelected) Color.White else Color.Black,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

