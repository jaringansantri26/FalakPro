package com.falak.falakpro

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.falak.falakpro.location.LocationHelper
import com.falak.falakpro.premium.HisabAwalBulanScreen
import com.falak.falakpro.premium.HilalVisibilityMapMode
import com.falak.falakpro.premium.PreferencesHelper
import com.falak.falakpro.premium.VisibilityMapRequest
import com.falak.falakpro.ui.*
import com.falak.falakpro.ui.theme.FalakProTheme
import com.falak.falakpro.ui.theme.GreenLightBg
import com.falak.falakpro.ui.theme.GreenPrimary
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferencesHelper(newBase)
        val lang = prefs.appLanguage
        if (lang != "system") {
            val locale = Locale(lang)
            Locale.setDefault(locale)
            val config = Configuration(newBase.resources.configuration)
            config.setLocale(locale)
            val context = newBase.createConfigurationContext(config)
            super.attachBaseContext(context)
        } else {
            super.attachBaseContext(newBase)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val darkColor = Color(0xFF12161A).toArgb()
        window.setBackgroundDrawable(ColorDrawable(darkColor))

        setContent {
            val prefs = remember { PreferencesHelper(this) }
            var appTheme by remember { mutableIntStateOf(prefs.appTheme) }

            val isSystemDark = androidx.compose.foundation.isSystemInDarkTheme()
            val useDarkTheme = when(appTheme) {
                1 -> false
                2 -> true
                else -> isSystemDark
            }

            FalakProTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(prefs = prefs, onThemeChanged = { appTheme = prefs.appTheme })
                }
            }
        }
    }
}

sealed class Screen {
    object Splash : Screen()
    object Home : Screen()
    object AwalBulan : Screen()
    data class VisibilityMap(
        val request: VisibilityMapRequest? = null
    ) : Screen()

    // Kalender baru
    object KalenderAstronomis : Screen()

    // Kalender lama, kalau masih mau dipakai nanti
    object KalenderLama : Screen()

    object Gerhana : Screen()

    data class GerhanaDetail(
        val jde: Double,
        val isSolar: Boolean,
        val typology: String,
        val lat: Double,
        val lon: Double,
        val elev: Double,
        val timezone: Double,
        val locName: String = ""
    ) : Screen()

    object JadwalShalat : Screen()
    object Kiblat : Screen()
    object KiblatKamera : Screen()
    object KiblatSettings : Screen()
    object DataFalak : Screen()
    object ScientificCalculator : Screen()
    object Settings : Screen()
}

private val screenStateSaver = Saver<Screen, String>(
    save = { screen ->
        when (screen) {
            is Screen.Splash -> "splash"
            is Screen.Home -> "home"
            is Screen.AwalBulan -> "awal_bulan"
            is Screen.VisibilityMap -> saveVisibilityMapScreen(screen.request)
            is Screen.KalenderAstronomis -> "kalender_astronomis"
            is Screen.KalenderLama -> "kalender_lama"
            is Screen.Gerhana -> "gerhana"
            is Screen.GerhanaDetail -> "gerhana_detail:${screen.jde}:${screen.isSolar}:${screen.typology}:${screen.lat}:${screen.lon}:${screen.elev}:${screen.timezone}:${screen.locName}"
            is Screen.JadwalShalat -> "jadwal_shalat"
            is Screen.Kiblat -> "kiblat"
            is Screen.KiblatKamera -> "kiblat_kamera"
            is Screen.KiblatSettings -> "kiblat_settings"
            is Screen.DataFalak -> "data_falak"
            is Screen.ScientificCalculator -> "scientific_calculator"
            is Screen.Settings -> "settings"
        }
    },
    restore = { raw ->
        val pipeParts = raw.split('|')
        val parts = raw.split(":")
        when {
            pipeParts.firstOrNull() == "visibility_map" -> Screen.VisibilityMap(restoreVisibilityMapRequest(pipeParts))
            parts.firstOrNull() == "splash" -> Screen.Splash
            parts.firstOrNull() == "home" -> Screen.Home
            parts.firstOrNull() == "awal_bulan" -> Screen.AwalBulan
            parts.firstOrNull() == "kalender_astronomis" -> Screen.KalenderAstronomis
            parts.firstOrNull() == "kalender_lama" -> Screen.KalenderLama
            parts.firstOrNull() == "gerhana" -> Screen.Gerhana
            parts.firstOrNull() == "jadwal_shalat" -> Screen.JadwalShalat
            parts.firstOrNull() == "kiblat" -> Screen.Kiblat
            parts.firstOrNull() == "kiblat_kamera" -> Screen.KiblatKamera
            parts.firstOrNull() == "kiblat_settings" -> Screen.KiblatSettings
            parts.firstOrNull() == "data_falak" -> Screen.DataFalak
            parts.firstOrNull() == "scientific_calculator" -> Screen.ScientificCalculator
            parts.firstOrNull() == "settings" -> Screen.Settings
            else -> Screen.Home
        }
    }
)

private fun saveVisibilityMapScreen(request: VisibilityMapRequest?): String {
    request ?: return "visibility_map"
    return listOf(
        "visibility_map",
        request.hijriYear.toString(),
        request.hijriMonth.toString(),
        request.ijtimaGeoJde.toString(),
        request.ijtimaLocalJd.toString(),
        request.timezone.toString(),
        request.mode.name,
        encodeScreenText(request.magribLocalTimeText),
        encodeScreenText(request.locationName),
        request.latitude.toString(),
        request.longitude.toString(),
        request.elevation.toString()
    ).joinToString("|")
}

private fun restoreVisibilityMapRequest(parts: List<String>): VisibilityMapRequest? {
    if (parts.size < 12) return null
    return VisibilityMapRequest(
        hijriYear = parts[1].toIntOrNull() ?: return null,
        hijriMonth = parts[2].toIntOrNull() ?: return null,
        ijtimaGeoJde = parts[3].toDoubleOrNull() ?: return null,
        ijtimaLocalJd = parts[4].toDoubleOrNull() ?: return null,
        timezone = parts[5].toDoubleOrNull() ?: 7.0,
        mode = runCatching { HilalVisibilityMapMode.valueOf(parts[6]) }.getOrDefault(HilalVisibilityMapMode.MABIMS_BARU),
        magribLocalTimeText = decodeScreenText(parts[7]),
        locationName = decodeScreenText(parts[8]),
        latitude = parts[9].toDoubleOrNull() ?: 0.0,
        longitude = parts[10].toDoubleOrNull() ?: 0.0,
        elevation = parts[11].toDoubleOrNull() ?: 0.0
    )
}

private fun encodeScreenText(value: String): String =
    URLEncoder.encode(value, StandardCharsets.UTF_8.name())

private fun decodeScreenText(value: String): String =
    URLDecoder.decode(value, StandardCharsets.UTF_8.name())

@Composable
fun MainApp(prefs: PreferencesHelper, onThemeChanged: () -> Unit) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }

    var currentScreen by rememberSaveable(stateSaver = screenStateSaver) { mutableStateOf<Screen>(Screen.Splash) }
    val locationData by locationHelper.locationState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            val prefs = PreferencesHelper(context)
            prefs.lokasiOtomatis = true

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

    LaunchedEffect(currentScreen) {
        if (currentScreen is Screen.Home) {
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
                locationHelper.startLocationUpdates()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    val gerhanaDetailViewModel: GerhanaDetailViewModel = viewModel()

    Scaffold(
        bottomBar = {
            if (
                currentScreen is Screen.Home ||
                currentScreen is Screen.KalenderAstronomis ||
                currentScreen is Screen.KalenderLama ||
                currentScreen is Screen.Kiblat ||
                currentScreen is Screen.Settings
            ) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = currentScreen is Screen.Home,
                        onClick = { currentScreen = Screen.Home },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.Home)
                                    Icons.Filled.Home
                                else
                                    Icons.Outlined.Home,
                                contentDescription = stringResource(R.string.menu_beranda),
                                tint = if (currentScreen is Screen.Home) GreenPrimary else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.menu_beranda),
                                color = if (currentScreen is Screen.Home) GreenPrimary else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = GreenLightBg
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.KalenderAstronomis,
                        onClick = { currentScreen = Screen.KalenderAstronomis },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.KalenderAstronomis)
                                    Icons.Filled.DateRange
                                else
                                    Icons.Outlined.DateRange,
                                contentDescription = stringResource(R.string.menu_kalender),
                                tint = if (currentScreen is Screen.KalenderAstronomis) GreenPrimary else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.menu_kalender),
                                color = if (currentScreen is Screen.KalenderAstronomis) GreenPrimary else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = GreenLightBg
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Kiblat,
                        onClick = { currentScreen = Screen.Kiblat },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.Kiblat)
                                    Icons.Filled.Explore
                                else
                                    Icons.Outlined.Explore,
                                contentDescription = stringResource(R.string.menu_kiblat),
                                tint = if (currentScreen is Screen.Kiblat) GreenPrimary else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.menu_kiblat),
                                color = if (currentScreen is Screen.Kiblat) GreenPrimary else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = GreenLightBg
                        )
                    )

                    NavigationBarItem(
                        selected = currentScreen is Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        icon = {
                            Icon(
                                imageVector = if (currentScreen is Screen.Settings)
                                    Icons.Filled.Settings
                                else
                                    Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.menu_pengaturan),
                                tint = if (currentScreen is Screen.Settings) GreenPrimary else Color.Gray
                            )
                        },
                        label = {
                            Text(
                                stringResource(R.string.menu_pengaturan),
                                color = if (currentScreen is Screen.Settings) GreenPrimary else Color.Gray
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = GreenLightBg
                        )
                    )
                }
            }
        }
    ) { paddingValues ->

        val contentModifier =
            if (currentScreen is Screen.Splash) {
                Modifier.fillMaxSize()
            } else {
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            }

        Surface(
            modifier = contentModifier,
            color = MaterialTheme.colorScheme.background
        ) {
            when (val screen = currentScreen) {
                is Screen.Splash -> {
                    FalakProSplashScreen(
                        onSplashFinished = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.Home -> {
                    HomeScreen(
                        locationData = locationData,
                        onRefreshLocation = {
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
                                val prefs = PreferencesHelper(context)
                                prefs.lokasiOtomatis = true

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
                        },
                        onNavigateToAwalBulan = {
                            currentScreen = Screen.AwalBulan
                        },
                        onNavigateToGerhana = {
                            currentScreen = Screen.Gerhana
                        },
                        onNavigateToJadwalShalat = {
                            currentScreen = Screen.JadwalShalat
                        },
                        onNavigateToKalender = {
                            currentScreen = Screen.KalenderAstronomis
                        },
                        onNavigateToKiblat = {
                            currentScreen = Screen.Kiblat
                        },
                        onNavigateToDataFalak = {
                            currentScreen = Screen.DataFalak
                        },
                        onNavigateToScientificCalculator = {
                            currentScreen = Screen.ScientificCalculator
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.Settings
                        }
                    )
                }

                is Screen.AwalBulan -> {
                    HisabAwalBulanScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        },
                        onNavigateToVisibilityMap = { request ->
                            currentScreen = Screen.VisibilityMap(request)
                        }
                    )
                }

                is Screen.VisibilityMap -> {
                    VisibilityMapScreen(
                        request = screen.request,
                        onNavigateBack = {
                            currentScreen = Screen.AwalBulan
                        }
                    )
                }

                is Screen.KalenderAstronomis -> {
                    AstronomicalCalendarScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        },
                        locationData = locationData
                    )
                }

                is Screen.KalenderLama -> {
                    CalendarScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        },
                        locationData = locationData
                    )
                }

                is Screen.Gerhana -> {
                    GerhanaScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        },
                        onNavigateToDetail = { jde, isSolar, typology, lat, lon, elev, tz, name ->
                            currentScreen = Screen.GerhanaDetail(
                                jde = jde,
                                isSolar = isSolar,
                                typology = typology,
                                lat = lat,
                                lon = lon,
                                elev = elev,
                                timezone = tz,
                                locName = name
                            )
                        }
                    )
                }

                is Screen.GerhanaDetail -> {
                    GerhanaDetailScreen(
                        jdeApprox = screen.jde,
                        isSolar = screen.isSolar,
                        typology = screen.typology,
                        lat = screen.lat,
                        lon = screen.lon,
                        elev = screen.elev,
                        timezone = screen.timezone,
                        locName = screen.locName,
                        viewModel = gerhanaDetailViewModel,
                        onBack = {
                            currentScreen = Screen.Gerhana
                        }
                    )
                }

                is Screen.JadwalShalat -> {
                    JadwalShalatScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.Kiblat -> {
                    KiblatScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        },
                        onNavigateToKamera = {
                            currentScreen = Screen.KiblatKamera
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.KiblatSettings
                        }
                    )
                }

                is Screen.KiblatKamera -> {
                    QiblaKameraScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Kiblat
                        },
                        onNavigateToSettings = {
                            currentScreen = Screen.KiblatSettings
                        }
                    )
                }

                is Screen.KiblatSettings -> {
                    KiblatSettingsScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Kiblat
                        }
                    )
                }

                is Screen.DataFalak -> {
                    DataFalakScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.ScientificCalculator -> {
                    ScientificCalculatorScreen(
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        }
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        prefs = prefs,
                        onThemeChanged = onThemeChanged,
                        onNavigateBack = {
                            currentScreen = Screen.Home
                        }
                    )
                }
            }
        }
    }
}
