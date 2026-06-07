package com.falak.falakpro.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import android.app.Activity
import com.falak.falakpro.R
import com.falak.falakpro.premium.PreferencesHelper

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    prefs: PreferencesHelper,
    onThemeChanged: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    
    // Read initial states
    var appTheme by remember { mutableIntStateOf(prefs.appTheme) } // 0=System, 1=Light, 2=Dark
    var use24HourFormat by remember { mutableStateOf(prefs.is24HourFormat) }
    var showNotifications by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf(prefs.appLanguage) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title), fontWeight = FontWeight.Bold) },
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
            // Appearance Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_appearance),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(stringResource(R.string.settings_theme), fontWeight = FontWeight.Bold)
                    Text(stringResource(R.string.settings_theme_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    val themeOptions = listOf(
                        0 to stringResource(R.string.theme_system),
                        1 to stringResource(R.string.theme_light),
                        2 to stringResource(R.string.theme_dark)
                    )
                    themeOptions.forEach { (value, label) ->
                        RadioButtonSetting(
                            title = label,
                            selected = appTheme == value,
                            onClick = {
                                appTheme = value
                                prefs.appTheme = value
                                onThemeChanged()
                            }
                        )
                    }
                    
                    HorizontalDivider()
                    
                    SwitchSetting(
                        title = stringResource(R.string.settings_time_format),
                        description = stringResource(R.string.settings_time_format_desc),
                        checked = use24HourFormat,
                        onCheckedChange = { 
                            use24HourFormat = it
                            prefs.is24HourFormat = it
                        }
                    )
                }
            }
            
            // Notification Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "Notifikasi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    SwitchSetting(
                        title = "Notifikasi Shalat",
                        description = "Dapatkan pengingat waktu shalat",
                        checked = showNotifications,
                        onCheckedChange = { showNotifications = it }
                    )
                }
            }
            
            // Language Settings
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(stringResource(R.string.settings_language_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    val languages = listOf(
                        "system" to stringResource(R.string.lang_system),
                        "id" to stringResource(R.string.lang_id),
                        "en" to stringResource(R.string.lang_en),
                        "ar" to stringResource(R.string.lang_ar)
                    )
                    languages.forEach { (code, label) ->
                        RadioButtonSetting(
                            title = label,
                            selected = selectedLanguage == code,
                            onClick = {
                                if (selectedLanguage != code) {
                                    selectedLanguage = code
                                    prefs.appLanguage = code
                                    (context as? Activity)?.recreate()
                                }
                            }
                        )
                    }
                }
            }
            
            // About Section
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val titleText = when (selectedLanguage) {
                        "en" -> "About Application"
                        "ar" -> "عن التطبيق"
                        else -> "Tentang Aplikasi"
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            text = titleText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Text(
                        text = getAboutText(selectedLanguage),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }
    }
}

private fun getAboutText(language: String): String {
    return when (language) {
        "en" -> """
            FalakPro
            Islamic Astronomy & Calendar Application
            Developed by Lembaga Falakiyah PWNU Jawa Barat

            ABOUT FALAKPRO
            FalakPro is a professional Islamic Astronomy application developed by the Falakiyah Institute of PWNU West Java.
            It integrates modern astronomy, Islamic calendrical calculations, Qibla determination, eclipse prediction, ephemeris generation, and traditional Islamic astronomical methods into a single platform.

            KEY FEATURES
            • Integrated Calendar (Gregorian, Hijri, Javanese, Sundanese)
            • Prayer Times & Ramadan Timetable
            • Qibla Compass & AR Qibla Camera
            • Solar & Lunar Eclipse Prediction
            • Ephemeris Tables & Nautical Almanac
            • Daily & Global Qibla Rashdul
            • Scientific Calculator

            HIJRI CALENDAR CALCULATION METHODS
            Method 1: Modern Astronomy
            Using VSOP87D Solar Theory & ELPMPP02 Lunar Theory for highly precise calculations of Conjunction, Crescent Altitude, Elongation, Moon Age, Illumination Fraction, etc.

            Method 2: Ad-Durr al-Aniq Method
            Based on the classical Islamic astronomy book "Ad-Durr al-Aniq fi Ilm al-Falak" by KH. Ahmad Ghozali Muhammad Fathullah. Widely used in Indonesian Islamic boarding schools and Nahdlatul Ulama institutions.

            DEVELOPER
            Lembaga Falakiyah PWNU Jawa Barat
            Programmer: Asep Jalaludin Bakrie (Lead Developer & System Architect)
            📞 0817-2238-56

            © 2026 FalakPro
            Developed by Lembaga Falakiyah PWNU Jawa Barat
            Lead Developer: Asep Jalaludin Bakrie
            All Rights Reserved 🌙🕌☀️📅🧭🔭⚓
        """.trimIndent()
        
        "ar" -> """
            فلك برو
            تطبيق علم الفلك الإسلامي والتقويم
            تم التطوير بواسطة مؤسسة الفلكية التابعة لـ PWNU جاوة الغربية

            عن فلك برو
            فلك برو هو تطبيق متخصص في علم الفلك الإسلامي. يجمع التطبيق بين الحسابات الفلكية الحديثة والتراث الفلكي الإسلامي في منصة واحدة.

            المميزات
            • التقويم الميلادي، الهجري، الجاوي، والسوندي
            • مواقيت الصلاة وإمساكية رمضان
            • بوصلة القبلة وكاميرا القبلة
            • حساب الأهلة
            • الكسوف الشمسي والخسوف القمري
            • الجداول الفلكية والألمنك البحري
            • حساب سمت القبلة والآلة الحاسبة العلمية

            طرق حساب بداية الشهر الهجري
            الطريقة الأولى: باستخدام نظريات الفلك الحديثة VSOP87D و ELPMPP02 لحساب دقيق للاقتران، ارتفاع الهلال، والاستطالة.
            الطريقة الثانية: اعتماداً على كتاب "الدر الأنيق في علم الفلك" للعالم الشيخ أحمد غزالي محمد فتح الله. وهو من أشهر المراجع الفلكية المعاصرة المستخدمة في المعاهد الإسلامية بإندونيسيا.

            المطور
            مؤسسة الفلكية PWNU جاوة الغربية
            المبرمج: Asep Jalaludin Bakrie
            📞 0817-2238-56

            © 2026 فلك برو
            تطوير مؤسسة الفلكية PWNU جاوة الغربية
            المطور الرئيسي: أسيب جلال الدين بكري
            جميع الحقوق محفوظة 🌙🕌☀️📅🧭🔭⚓
        """.trimIndent()
        
        else -> """
            FalakPro
            Islamic Astronomy & Calendar Application
            Developed by Lembaga Falakiyah PWNU Jawa Barat

            TENTANG FALAKPRO
            FalakPro adalah aplikasi astronomi Islam dan ilmu falak yang dikembangkan oleh Lembaga Falakiyah PWNU Jawa Barat sebagai platform terpadu untuk kebutuhan hisab, rukyat, kalender, arah kiblat, jadwal salat, gerhana, dan data astronomi profesional.
            
            Aplikasi ini dirancang untuk kalangan pesantren, akademisi, lembaga keagamaan, masjid, guru, mahasiswa, peneliti, dan masyarakat umum yang membutuhkan data astronomi dan falak dengan tingkat akurasi tinggi.
            
            FalakPro menggabungkan metode astronomi modern dan metode hisab turats (klasik) sehingga pengguna dapat membandingkan hasil perhitungan dari berbagai pendekatan keilmuan dalam satu aplikasi.

            FITUR UTAMA
            🗓️ Kalender Terpadu
            • Kalender Masehi, Hijriyah, Saka Jawa, Caka Sunda
            • Hari Pasaran Jawa, Pancawara Caka Sunda, Wuku Sunda
            
            🕌 Jadwal Salat & 🌙 Imsakiyah Bulanan
            • Subuh, Syuruq, Dhuha, Zuhur, Asar, Magrib, Isya
            • Koreksi elevasi, zona waktu, GPS otomatis
            • Cetak PDF format A4 siap cetak

            🧭 Kompas Kiblat & 📷 Kamera Kiblat (AR)
            • Azimut kiblat, kompas digital akurasi tinggi
            • Penentuan arah dengan Augmented Reality (AR)

            🌙 Hisab Awal Bulan Hijriyah
            • Ijtimak, Tinggi Hilal, Elongasi, Umur Hilal, Fraksi iluminasi, Lag Time, dll.

            ☀️🌕 Gerhana Matahari & Bulan
            • Prediksi gerhana sebagian, cincin, total, hibrida.

            📖 Tabel Ephemeris & ⚓ Almanak Nautika
            • Data harian dan bulanan Matahari & Bulan (RA, Deklinasi, Jarak, GHA, SHA, dll).

            ☀️🕋 Rasydul Qiblat Harian & Global
            • Waktu bayangan Matahari menunjukkan arah kiblat (termasuk 27-28 Mei & 15-16 Juli).

            METODE PERHITUNGAN AWAL BULAN HIJRIYAH
            Metode 1: Astronomi Modern (VSOP87D + ELPMPP02)
            Menggunakan algoritma modern presisi tinggi untuk menghitung parameter hilal. Referensi: Bretagnon & Francou (VSOP87), Chapront-Touzé & Chapront (ELPMPP02), Jean Meeus.

            Metode 2: Hisab Kitab Ad-Durr al-Aniq
            Berdasarkan kitab karya KH. Ahmad Ghozali Muhammad Fathullah. Metode hisab tahqiq yang digunakan luas di lingkungan pesantren dan Nahdlatul Ulama dengan ketelitian tinggi.

            PENGEMBANG
            Lembaga Falakiyah PWNU Jawa Barat
            Lembaga resmi Nahdlatul Ulama bidang Hisab Rukyat, Kalender Hijriyah, Arah Kiblat, dan Edukasi Astronomi Islam.

            Programmer:
            Asep Jalaludin Bakrie
            Lead Developer & System Architect
            📞 0817-2238-56

            © 2026 FalakPro
            Developed by Lembaga Falakiyah PWNU Jawa Barat
            Lead Developer: Asep Jalaludin Bakrie
            All Rights Reserved 🌙🕌☀️📅🧭🔭⚓
        """.trimIndent()
    }
}

@Composable
fun SwitchSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun RadioButtonSetting(
    title: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        RadioButton(
            selected = selected,
            onClick = onClick
        )
    }
}

