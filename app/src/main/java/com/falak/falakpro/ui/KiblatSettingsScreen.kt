package com.falak.falakpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falak.falakpro.premium.PreferencesHelper

private val TealPrimary = Color(0xFF009688)
private val TealDark = Color(0xFF00796B)
private val SectionLabel = Color(0xFF009688)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiblatSettingsScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesHelper(context) }

    var beepSound by remember { mutableStateOf(prefs.kiblatBeepSound) }
    var showSunMoon by remember { mutableStateOf(prefs.kiblatShowSunMoon) }
    var crossfinderTheme by remember { mutableStateOf(prefs.kiblatCrossfinderTheme) }
    var kalibrasiObjek by remember { mutableStateOf(prefs.kiblatKalibrasiObjek) }
    var koreksiAzimut by remember { mutableStateOf(prefs.kiblatKoreksiAzimut) }

    var showCrossfinderDialog by remember { mutableStateOf(false) }
    var showKalibrasiDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    
    var kiblatMethod by remember { mutableStateOf(prefs.kiblatMethod) }

    val crossfinderLabels = listOf("Default", "Minimal", "Neon")
    val kalibrasiLabels = listOf("None", "Posisi Matahari", "Bayangan Matahari", "Posisi Bulan")
    val methodLabels = listOf("Spherical", "Ellipsoid", "Vincenty")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Pengaturan Kompas Kiblat",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = Color.Black,
                    navigationIconContentColor = Color.Black
                )
            )
        },
        containerColor = Color(0xFFF5F5F5)
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            SectionTitle("Prasyarat")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Text(
                    "Aplikasi ini perlu mengakses lokasi perangkat, kamera, sensor magnetik, dan sensor accelerometer untuk bekerja dengan benar.",
                    modifier = Modifier.padding(16.dp),
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(24.dp))

            SectionTitle("Pengaturan Umum")
            SettingsCard {
                // Data Matahari dan Bulan
                SettingsToggleItem(
                    title = "Data Matahari dan Bulan",
                    subtitle = "Tampilkan data Matahari dan Bulan berdasarkan lokasi pengamat",
                    checked = showSunMoon,
                    onCheckedChange = {
                        showSunMoon = it
                        prefs.kiblatShowSunMoon = it
                    }
                )

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Beep Sound
                SettingsToggleItem(
                    title = "Beep sound untuk Kiblat",
                    subtitle = "Suara beep ketika smartphone mengarah ke Kiblat",
                    checked = beepSound,
                    onCheckedChange = {
                        beepSound = it
                        prefs.kiblatBeepSound = it
                    }
                )

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Tema Cross Finder
                SettingsClickItem(
                    title = "Tema cross finder",
                    subtitle = crossfinderLabels[crossfinderTheme],
                    onClick = { showCrossfinderDialog = true }
                )

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Metode Perhitungan
                SettingsClickItem(
                    title = "Metode Perhitungan",
                    subtitle = methodLabels[kiblatMethod],
                    onClick = { showMethodDialog = true }
                )
            }

            Spacer(Modifier.height(24.dp))

            SectionTitle("Kalibrasi Utara Benar")
            SettingsCard {
                // Cara Kalibrasi
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Bagaimana cara kalibrasi?",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Arahkan pratinjau kamera Anda ke objek kalibrasi yang Anda pilih di bawah. " +
                        "Arahkan obyek Anda ke tengah layar di cross finder dan tekan tombol kalibrasi. " +
                        "Perlu diketahui bahwa mengarahkan kamera langsung ke matahari tanpa filter sinar " +
                        "matahari dalam durasi lama dapat merusak sensor kamera.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 19.sp
                    )
                }

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Pilih Objek Kalibrasi
                SettingsClickItem(
                    title = "Pilih obyek kalibrasi",
                    subtitle = kalibrasiLabels[kalibrasiObjek],
                    onClick = { showKalibrasiDialog = true }
                )

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Terapkan koreksi azimut
                SettingsToggleItem(
                    title = "Terapkan koreksi azimut",
                    subtitle = "Aktif: acuan Utara Sejati (True North) berdasarkan kalibrasi astronomis.\nNonaktif: acuan Utara Magnetik (default sensor kompas).",
                    checked = koreksiAzimut,
                    onCheckedChange = {
                        koreksiAzimut = it
                        prefs.kiblatKoreksiAzimut = it
                    }
                )

                HorizontalDivider(color = Color(0xFFEEEEEE))

                // Reset Kalibrasi
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            prefs.kiblatKoreksiNilai = 0f
                            android.widget.Toast.makeText(context, "Kalibrasi direset.", android.widget.Toast.LENGTH_SHORT).show()
                        }
                        .padding(16.dp)
                ) {
                    Text(
                        "Reset kalibrasi",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFFD32F2F)
                    )
                    Text(
                        "Koreksi azimut saat ini: ${String.format("%.2f°", prefs.kiblatKoreksiNilai)}",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            SectionTitle("Tentang")
            SettingsCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Kompas Kiblat FalakPro",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color.Black
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Menggunakan algoritma Vincenty untuk perhitungan azimuth geodetik presisi tinggi. " +
                        "Mendukung kalibrasi matahari untuk koreksi utara sejati (true north) terhadap utara magnetis.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 19.sp
                    )
                }
            }

            Spacer(Modifier.height(32.dp))
        }
    }


    // Crossfinder Theme Dialog
    if (showCrossfinderDialog) {
        AlertDialog(
            onDismissRequest = { showCrossfinderDialog = false },
            title = { Text("Tema cross finder", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    crossfinderLabels.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    crossfinderTheme = index
                                    prefs.kiblatCrossfinderTheme = index
                                    showCrossfinderDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = crossfinderTheme == index,
                                onClick = {
                                    crossfinderTheme = index
                                    prefs.kiblatCrossfinderTheme = index
                                    showCrossfinderDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TealPrimary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCrossfinderDialog = false }) {
                    Text("BATAL", color = TealPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Kalibrasi Object Dialog
    if (showKalibrasiDialog) {
        AlertDialog(
            onDismissRequest = { showKalibrasiDialog = false },
            title = { Text("Pilih obyek kalibrasi", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    kalibrasiLabels.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    kalibrasiObjek = index
                                    prefs.kiblatKalibrasiObjek = index
                                    showKalibrasiDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = kalibrasiObjek == index,
                                onClick = {
                                    kalibrasiObjek = index
                                    prefs.kiblatKalibrasiObjek = index
                                    showKalibrasiDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TealPrimary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showKalibrasiDialog = false }) {
                    Text("BATAL", color = TealPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Method Selection Dialog
    if (showMethodDialog) {
        AlertDialog(
            onDismissRequest = { showMethodDialog = false },
            title = { Text("Pilih Metode Perhitungan", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    methodLabels.forEachIndexed { index, label ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    kiblatMethod = index
                                    prefs.kiblatMethod = index
                                    showMethodDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = kiblatMethod == index,
                                onClick = {
                                    kiblatMethod = index
                                    prefs.kiblatMethod = index
                                    showMethodDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = TealPrimary
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, fontSize = 15.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMethodDialog = false }) {
                    Text("BATAL", color = TealPrimary, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}


@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        color = SectionLabel,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
    )
}

@Composable
private fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        content = content
    )
}

@Composable
private fun SettingsToggleItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            Text(subtitle, fontSize = 13.sp, color = Color.Gray)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TealPrimary,
                checkedTrackColor = TealPrimary.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun SettingsClickItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        Text(subtitle, fontSize = 13.sp, color = Color.Gray)
    }
}

