package com.falak.falakpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import com.falak.falakpro.location.LocationHelper
import com.falak.falakpro.premium.MesinWaktuShalat
import com.falak.falakpro.premium.PreferencesHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PengaturanWaktuShalatScreen(
    prefs: PreferencesHelper,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val locationState by locationHelper.locationState.collectAsState()
    
    LaunchedEffect(Unit) {
        locationHelper.startLocationUpdates()
    }

    var lokasiOtomatis by remember { mutableStateOf(prefs.lokasiOtomatis) }
    var manualLat by remember { mutableStateOf(prefs.manualLat) }
    var manualLon by remember { mutableStateOf(prefs.manualLon) }
    var manualElev by remember { mutableStateOf(prefs.manualElev) }
    var manualTimezone by remember { mutableStateOf(prefs.manualTimezone) }
    var manualLokasiNama by remember { mutableStateOf(prefs.manualLokasiNama) }
    var ketinggianDataranTinggi by remember { mutableStateOf(prefs.ketinggianDataranTinggi) }
    var pembulatanIndex by remember { mutableStateOf(prefs.pembulatanIndex) }

    val lat = if (lokasiOtomatis) (if (locationState.latitude != 0.0) locationState.latitude else -6.3133) else manualLat
    val lon = if (lokasiOtomatis) (if (locationState.longitude != 0.0) locationState.longitude else 107.3191) else manualLon
    val elev = if (lokasiOtomatis) locationState.altitude else manualElev
    val tz = if (lokasiOtomatis) timezoneFromLongitude(lon) else manualTimezone
    val locName = if (lokasiOtomatis) (if (locationState.address != "Mencari Lokasi...") locationState.address else "Lokasi Tidak Diketahui") else manualLokasiNama

    // Helper formatting DMS
    fun deg(d: Double): String {
        val a = Math.abs(d)
        val dd = Math.floor(a).toInt()
        val mm = Math.floor((a - dd) * 60).toInt()
        val ss = Math.round((a - dd - mm / 60.0) * 3600).toInt()
        return "${dd}°${mm}'${ss}\""
    }
    val latStr = "${deg(lat)} ${if(lat < 0) "LS" else "LU"}"
    val lonStr = "${deg(lon)} ${if(lon > 0) "BT" else "BB"}"

    var showMetodeDialog by remember { mutableStateOf(false) }
    
    val kriteriaList = MesinWaktuShalat.DAFTAR_KRITERIA
    var selectedKriteriaIndex by remember { mutableStateOf(prefs.kriteriaIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan Waktu Shalat", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F7C6A))
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Lokasi Waktu Shalat
            SettingSectionTitle("Lokasi Waktu Shalat")
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        lokasiOtomatis = !lokasiOtomatis
                        prefs.lokasiOtomatis = lokasiOtomatis
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Lokasi Otomatis", fontSize = 16.sp, color = Color.Black)
                Switch(
                    checked = lokasiOtomatis, 
                    onCheckedChange = { 
                        lokasiOtomatis = it
                        prefs.lokasiOtomatis = it
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Color(0xFF0F7C6A))
                )
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            
            var showManualLokasiDialog by remember { mutableStateOf(false) }
            SettingItemValue(
                title = "Pilih Lokasi", 
                value = locName,
                onClick = { 
                    if (!lokasiOtomatis) {
                        showManualLokasiDialog = true
                    }
                }
            )
            
            if (showManualLokasiDialog) {
                PilihLokasiManualDialog(
                    prefs = prefs,
                    onSave = { name, latVal, lonVal, elevVal, timezoneVal ->
                        manualLokasiNama = name
                        manualLat = latVal
                        manualLon = lonVal
                        manualElev = elevVal
                        manualTimezone = timezoneVal
                        ketinggianDataranTinggi = elevVal
                    },
                    onDismiss = { showManualLokasiDialog = false }
                )
            }
            
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                Text("Koordinat: $latStr  $lonStr", fontSize = 14.sp, color = Color.DarkGray)
                Text("Zona waktu: UTC${if (tz >= 0) "+" else ""}${formatTimezoneValue(tz)} ${prayerTimezoneLabel(tz, lon)}", fontSize = 14.sp, color = Color.DarkGray)
                Text("Ketinggian: ${elev.toInt()} meter", fontSize = 14.sp, color = Color.DarkGray)
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Data ketinggian diperoleh otomatis dari koordinat lokasi Anda, sehingga waktu maghrib dan terbit ditampilkan lebih akurat.",
                    fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp
                )
            }
            DividerSection()

            // Perhitungan Waktu Shalat
            SettingSectionTitle("Perhitungan Waktu Shalat")
            
            var otomatis by remember { mutableStateOf(prefs.pengaturanOtomatis) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { 
                        otomatis = !otomatis
                        prefs.pengaturanOtomatis = otomatis
                    }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Pengaturan Otomatis", fontSize = 16.sp, color = Color.Black)
                Switch(
                    checked = otomatis, 
                    onCheckedChange = { 
                        otomatis = it
                        prefs.pengaturanOtomatis = it
                    }
                )
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            
            val currentKriteria = if (otomatis) kriteriaList[1] else kriteriaList.getOrElse(selectedKriteriaIndex) { kriteriaList[1] }
            
            SettingItemValue(
                title = "Metode",
                value = currentKriteria.nama,
                onClick = { if (!otomatis) showMetodeDialog = true }
            )
            
            var sudutManualSubuh by remember { mutableStateOf(prefs.sudutManualSubuh) }
            var sudutManualIsya by remember { mutableStateOf(prefs.sudutManualIsya) }
            
            if (!otomatis && selectedKriteriaIndex == 0) {
                var sudutManualSubuhStr by remember { mutableStateOf(prefs.sudutManualSubuh.toString()) }
                var sudutManualIsyaStr by remember { mutableStateOf(prefs.sudutManualIsya.toString()) }

                // Kolom untuk memilih ketinggian Subuh dan Isya
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    OutlinedTextField(
                        value = sudutManualSubuhStr,
                        onValueChange = { 
                            sudutManualSubuhStr = it
                            it.toFloatOrNull()?.let { v -> 
                                sudutManualSubuh = v
                                prefs.sudutManualSubuh = v 
                            }
                        },
                        label = { Text("Ketinggian Sudut Subuh") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = sudutManualIsyaStr,
                        onValueChange = { 
                            sudutManualIsyaStr = it
                            it.toFloatOrNull()?.let { v -> 
                                sudutManualIsya = v
                                prefs.sudutManualIsya = v 
                            }
                        },
                        label = { Text("Ketinggian Sudut Isya") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            
            val desc = if (otomatis || currentKriteria.nama.contains("Lembaga Falakiyah NU")) {
                "Dalam perhitungan Waktu Shalat, Lembaga Falakiyah NU, Indonesia menggunakan tinggi sudut posisi matahari untuk Subuh -20.0° dan Isya -18.0°"
            } else if (selectedKriteriaIndex == 0) {
                "Menggunakan penyesuaian sudut manual dengan Subuh ${String.format(java.util.Locale.US, "%.1f", sudutManualSubuh)}° dan Isya ${String.format(java.util.Locale.US, "%.1f", sudutManualIsya)}°"
            } else {
                "Dalam perhitungan Waktu Shalat menggunakan kriteria ini, tinggi sudut Subuh adalah ${currentKriteria.sudutSubuh}° dan Isya ${if(currentKriteria.sudutIsya == 0.0) "berdasarkan offset" else currentKriteria.sudutIsya.toString()+"°"}"
            }
            
            Text(
                text = desc,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp
            )
            DividerSection()

            // Ihtiyath
            SettingSectionTitle("Ihtiyath")
            var dzuhur by remember { mutableStateOf(prefs.ikhDzuhur) }
            var ashar by remember { mutableStateOf(prefs.ikhAshar) }
            var maghrib by remember { mutableStateOf(prefs.ikhMaghrib) }
            var isya by remember { mutableStateOf(prefs.ikhIsya) }
            var subuh by remember { mutableStateOf(prefs.ikhSubuh) }

            SettingItemValue("Zuhur", "$dzuhur Menit", { dzuhur = (dzuhur + 1) % 6; prefs.ikhDzuhur = dzuhur })
            SettingItemValue("Ashar", "$ashar Menit", { ashar = (ashar + 1) % 6; prefs.ikhAshar = ashar })
            SettingItemValue("Maghrib", "$maghrib Menit", { maghrib = (maghrib + 1) % 6; prefs.ikhMaghrib = maghrib })
            SettingItemValue("Isya'", "$isya Menit", { isya = (isya + 1) % 6; prefs.ikhIsya = isya })
            SettingItemValue("Subuh", "$subuh Menit", { subuh = (subuh + 1) % 6; prefs.ikhSubuh = subuh })
            
            Text(
                text = "Ihtiyath adalah waktu tambahan pada hasil perhitungan waktu shalat sebenarnya untuk mengantisipasi jam yang kurang akurat, serta menjangkau wilayah yang lebih luas. Namun, penambahan ini juga bisa berarti memperpanjang batas akhir waktu shalat sebelumnya.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp
            )
            DividerSection()

            // Lokasi Dataran Tinggi
            SettingSectionTitle("Lokasi Dataran Tinggi")
            
            var showDataranTinggiDialog by remember { mutableStateOf(false) }
            SettingItemValue(
                title = "Ketinggian Lokasi", 
                value = "${ketinggianDataranTinggi.toInt()} meter", 
                onClick = { showDataranTinggiDialog = true }
            )
            
            if (showDataranTinggiDialog) {
                KetinggianDataranTinggiDialog(
                    prefs = prefs,
                    onSave = {
                        ketinggianDataranTinggi = it
                    },
                    onDismiss = { showDataranTinggiDialog = false }
                )
            }
            Text(
                text = "Jadwal shalat disesuaikan otomatis dengan ketinggian lokasi Anda, untuk akurasi waktu maghrib dan terbit.",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                fontSize = 12.sp, color = Color.Gray, lineHeight = 18.sp
            )
            DividerSection()

            // Metode Perhitungan Tambahan
            SettingSectionTitle("Metode Perhitungan Tambahan")
            var isSyafii by remember { mutableStateOf(prefs.metodeAsharSyafii) }
            SettingItemValue(
                title = "Metode Ashar",
                value = if (isSyafii) "Standar (Syafi'i, Maliki, Hanbali)" else "Hanafi",
                onClick = { 
                    isSyafii = !isSyafii
                    prefs.metodeAsharSyafii = isSyafii
                }
            )
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
            
            var showPembulatanDialog by remember { mutableStateOf(false) }
            val pembulatanText = when (pembulatanIndex) {
                1 -> "Ke Atas (Ceil)"
                2 -> "Ke Bawah (Floor)"
                else -> "Normal (Terdekat)"
            }
            SettingItemValue(
                title = "Metode Pembulatan",
                value = pembulatanText,
                onClick = { showPembulatanDialog = true }
            )
            
            if (showPembulatanDialog) {
                PembulatanSelectionDialog(
                    selectedIndex = pembulatanIndex,
                    onSelect = { index ->
                        pembulatanIndex = index
                        prefs.pembulatanIndex = index
                        showPembulatanDialog = false
                    },
                    onDismiss = { showPembulatanDialog = false }
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showMetodeDialog) {
        MetodeSelectionDialog(
            kriteriaList = MesinWaktuShalat.DAFTAR_KRITERIA,
            selectedIndex = prefs.kriteriaIndex,
            onSelect = { 
                prefs.kriteriaIndex = it
                selectedKriteriaIndex = it
                showMetodeDialog = false 
            },
            onDismiss = { showMetodeDialog = false }
        )
    }
}

@Composable
fun SettingSectionTitle(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Color.Black,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun SettingItemValue(title: String, value: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = title, fontSize = 16.sp, color = Color.Black)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value,
                fontSize = 14.sp,
                color = Color.Gray,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = 200.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
        }
    }
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
fun DividerSection() {
    Spacer(modifier = Modifier.height(16.dp))
    Box(modifier = Modifier.fillMaxWidth().height(8.dp).background(Color(0xFFF3F4F6)))
    Spacer(modifier = Modifier.height(8.dp))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetodeSelectionDialog(
    kriteriaList: List<MesinWaktuShalat.KriteriaWaktuShalat>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Perhitungan Waktu Shalat", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F7C6A))
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            kriteriaList.forEachIndexed { index, k ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(index) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = k.nama, fontSize = 16.sp, color = Color.Black)
                        val subText = if (k.nama.contains("Manual")) "" else {
                            "Subuh ${k.sudutSubuh}° / Isya ${if(k.sudutIsya == 0.0) "90 atau 120 menit Setelah Maghrib" else "${k.sudutIsya}°"}"
                        }
                        if (subText.isNotEmpty()) {
                            Text(text = subText, fontSize = 13.sp, color = Color.Gray)
                        }
                    }
                    RadioButton(
                        selected = (index == selectedIndex),
                        onClick = { onSelect(index) },
                        colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0F7C6A))
                    )
                }
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
fun PilihLokasiManualDialog(
    prefs: PreferencesHelper,
    onSave: (String, Double, Double, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var namaLokasi by remember { mutableStateOf(prefs.manualLokasiNama) }
    
    // Convert current double to DMS
    fun getDms(d: Double): Triple<Int, Int, Int> {
        val a = Math.abs(d)
        val dd = Math.floor(a).toInt()
        val mm = Math.floor((a - dd) * 60).toInt()
        val ss = Math.round((a - dd - mm / 60.0) * 3600).toInt()
        return Triple(dd, mm, ss)
    }
    
    val latDms = getDms(prefs.manualLat)
    val lonDms = getDms(prefs.manualLon)
    
    var latDeg by remember { mutableStateOf(latDms.first.toString()) }
    var latMin by remember { mutableStateOf(latDms.second.toString()) }
    var latSec by remember { mutableStateOf(latDms.third.toString()) }
    var latDir by remember { mutableStateOf(if (prefs.manualLat < 0) "LS" else "LU") }
    
    var lonDeg by remember { mutableStateOf(lonDms.first.toString()) }
    var lonMin by remember { mutableStateOf(lonDms.second.toString()) }
    var lonSec by remember { mutableStateOf(lonDms.third.toString()) }
    var lonDir by remember { mutableStateOf(if (prefs.manualLon > 0) "BT" else "BB") }
    
    var elev by remember { mutableStateOf(prefs.manualElev.toInt().toString()) }
    var timezone by remember { mutableStateOf(formatTimezoneValue(prefs.manualTimezone)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Input Lokasi Manual", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                OutlinedTextField(
                    value = namaLokasi,
                    onValueChange = { namaLokasi = it },
                    label = { Text("Nama Lokasi") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Text("Garis Lintang (Latitude)", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(value = latDeg, onValueChange = { latDeg = it }, label = { Text("Derajat") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = latMin, onValueChange = { latMin = it }, label = { Text("Menit") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = latSec, onValueChange = { latSec = it }, label = { Text("Detik") }, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = latDir == "LU", onClick = { latDir = "LU" })
                    Text("LU")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = latDir == "LS", onClick = { latDir = "LS" })
                    Text("LS")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Garis Bujur (Longitude)", fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedTextField(value = lonDeg, onValueChange = { lonDeg = it }, label = { Text("Derajat") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lonMin, onValueChange = { lonMin = it }, label = { Text("Menit") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = lonSec, onValueChange = { lonSec = it }, label = { Text("Detik") }, modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = lonDir == "BT", onClick = { lonDir = "BT" })
                    Text("BT")
                    Spacer(modifier = Modifier.width(16.dp))
                    RadioButton(selected = lonDir == "BB", onClick = { lonDir = "BB" })
                    Text("BB")
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = elev,
                    onValueChange = { elev = it },
                    label = { Text("Ketinggian (meter)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = timezone,
                    onValueChange = { timezone = it },
                    label = { Text("Zona waktu (UTC+)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val latD = latDeg.toDoubleOrNull() ?: 0.0
                    val latM = latMin.toDoubleOrNull() ?: 0.0
                    val latS = latSec.toDoubleOrNull() ?: 0.0
                    var lat = latD + (latM / 60.0) + (latS / 3600.0)
                    if (latDir == "LS") lat = -lat
                    
                    val lonD = lonDeg.toDoubleOrNull() ?: 0.0
                    val lonM = lonMin.toDoubleOrNull() ?: 0.0
                    val lonS = lonSec.toDoubleOrNull() ?: 0.0
                    var lon = lonD + (lonM / 60.0) + (lonS / 3600.0)
                    if (lonDir == "BB") lon = -lon
                    
                    prefs.manualLokasiNama = namaLokasi
                    prefs.manualLat = lat
                    prefs.manualLon = lon
                    
                    val parsedElev = elev.toDoubleOrNull() ?: 0.0
                    val parsedTimezone = timezone.toDoubleOrNull()
                        ?: timezoneFromLongitude(lon)
                    prefs.manualElev = parsedElev
                    prefs.manualTimezone = parsedTimezone
                    // Automatically sync to dataran tinggi altitude when saving manual location
                    prefs.ketinggianDataranTinggi = parsedElev
                    
                    onSave(namaLokasi, lat, lon, parsedElev, parsedTimezone)
                    onDismiss()
                }
            ) {
                Text("Simpan", color = Color(0xFF0F7C6A))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = Color.Gray) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun KetinggianDataranTinggiDialog(
    prefs: PreferencesHelper,
    onSave: (Double) -> Unit,
    onDismiss: () -> Unit
) {
    var elev by remember { mutableStateOf(prefs.ketinggianDataranTinggi.toInt().toString()) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ketinggian Lokasi", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = elev,
                onValueChange = { elev = it },
                label = { Text("Ketinggian (meter)") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = elev.toDoubleOrNull() ?: 0.0
                    prefs.ketinggianDataranTinggi = value
                    onSave(value)
                    onDismiss()
                }
            ) {
                Text("Simpan", color = Color(0xFF0F7C6A))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal", color = Color.Gray) }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun PembulatanSelectionDialog(
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(
        "Normal (Terdekat)",
        "Ke Atas (Ceil)",
        "Ke Bawah (Floor)"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Metode Pembulatan Waktu", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                options.forEachIndexed { index, option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(index) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = (index == selectedIndex),
                            onClick = { onSelect(index) },
                            colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0F7C6A))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = option, fontSize = 16.sp, color = Color.Black)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = Color.Gray)
            }
        },
        containerColor = MaterialTheme.colorScheme.surface
    )
}

