package com.falak.falakpro

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.falak.falakpro.location.LocationHelper
import com.falak.falakpro.premium.AstroAssetPreloader
import com.falak.falakpro.premium.HisabAwalBulanScreen
import com.falak.falakpro.premium.PreferencesHelper
import com.falak.falakpro.ui.AstronomicalCalendarScreen
import com.falak.falakpro.ui.DataFalakScreen
import com.falak.falakpro.ui.FalakProSplashScreen
import com.falak.falakpro.ui.GerhanaDetailScreen
import com.falak.falakpro.ui.GerhanaDetailViewModel
import com.falak.falakpro.ui.GerhanaScreen
import com.falak.falakpro.ui.HomeScreen
import com.falak.falakpro.ui.JadwalShalatScreen
import com.falak.falakpro.ui.KiblatScreen
import com.falak.falakpro.ui.KiblatSettingsScreen
import com.falak.falakpro.ui.QiblaKameraScreen
import com.falak.falakpro.ui.ScientificCalculatorScreen
import com.falak.falakpro.ui.SettingsScreen
import com.falak.falakpro.ui.VisibilityMapScreen

@Composable
fun MainApp(
    prefs: PreferencesHelper,
    onThemeChanged: () -> Unit
) {
    val context = LocalContext.current
    val locationHelper = remember { LocationHelper(context) }
    val locationData by locationHelper.locationState.collectAsState()

    var currentScreen by rememberSaveable(stateSaver = screenStateSaver) {
        mutableStateOf<Screen>(Screen.Splash)
    }
    var showKiblatCalibrationOnOpen by rememberSaveable { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<AppUpdateInfo?>(null) }

    LaunchedEffect(Unit) {
        AstroAssetPreloader.ensureSolar(context)
        updateInfo = runCatching {
            AppUpdateChecker.check(context)
        }.getOrNull()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            requestGpsLocation(context, locationHelper)
        } else {
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(currentScreen) {
        if (currentScreen is Screen.Home && prefs.lokasiOtomatis) {
            if (context.hasLocationPermission()) {
                locationHelper.startLocationUpdates()
            } else {
                permissionLauncher.launch(locationPermissions)
            }
        }
    }

    val gerhanaDetailViewModel: GerhanaDetailViewModel = viewModel()

    Scaffold(
        bottomBar = {
            MainBottomBar(
                currentScreen = currentScreen,
                onNavigate = { currentScreen = it }
            )
        }
    ) { paddingValues ->
        val contentModifier = if (currentScreen is Screen.Splash) {
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
                        onSplashFinished = { currentScreen = Screen.Home }
                    )
                }

                is Screen.Home -> {
                    HomeScreen(
                        locationData = locationData,
                        onRefreshLocation = {
                            if (context.hasLocationPermission()) {
                                requestGpsLocation(context, locationHelper)
                            } else {
                                permissionLauncher.launch(locationPermissions)
                            }
                        },
                        onNavigateToAwalBulan = { currentScreen = Screen.AwalBulan },
                        onNavigateToGerhana = { currentScreen = Screen.Gerhana },
                        onNavigateToJadwalShalat = { currentScreen = Screen.JadwalShalat },
                        onNavigateToKalender = { currentScreen = Screen.KalenderAstronomis },
                        onNavigateToKiblat = { currentScreen = Screen.Kiblat },
                        onNavigateToDataFalak = { currentScreen = Screen.DataFalak },
                        onNavigateToScientificCalculator = { currentScreen = Screen.ScientificCalculator },
                        onNavigateToSettings = { currentScreen = Screen.Settings }
                    )
                }

                is Screen.AwalBulan -> {
                    HisabAwalBulanScreen(
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToVisibilityMap = { request ->
                            currentScreen = Screen.VisibilityMap(request)
                        }
                    )
                }

                is Screen.VisibilityMap -> {
                    VisibilityMapScreen(
                        request = screen.request,
                        onNavigateBack = { currentScreen = Screen.AwalBulan }
                    )
                }

                is Screen.KalenderAstronomis -> {
                    AstronomicalCalendarScreen(
                        onNavigateBack = { currentScreen = Screen.Home },
                        locationData = locationData
                    )
                }

                is Screen.Gerhana -> {
                    GerhanaScreen(
                        onNavigateBack = { currentScreen = Screen.Home },
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
                        onBack = { currentScreen = Screen.Gerhana }
                    )
                }

                is Screen.JadwalShalat -> {
                    JadwalShalatScreen(
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToKiblat = {
                            showKiblatCalibrationOnOpen = true
                            currentScreen = Screen.Kiblat
                        }
                    )
                }

                is Screen.Kiblat -> {
                    KiblatScreen(
                        onNavigateBack = { currentScreen = Screen.Home },
                        onNavigateToKamera = { currentScreen = Screen.KiblatKamera },
                        onNavigateToSettings = { currentScreen = Screen.KiblatSettings },
                        showCalibrationOnOpen = showKiblatCalibrationOnOpen,
                        onCalibrationPromptConsumed = {
                            showKiblatCalibrationOnOpen = false
                        }
                    )
                }

                is Screen.KiblatKamera -> {
                    QiblaKameraScreen(
                        onNavigateBack = { currentScreen = Screen.Kiblat },
                        onNavigateToSettings = { currentScreen = Screen.KiblatSettings }
                    )
                }

                is Screen.KiblatSettings -> {
                    KiblatSettingsScreen(
                        onNavigateBack = { currentScreen = Screen.Kiblat }
                    )
                }

                is Screen.DataFalak -> {
                    DataFalakScreen(
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }

                is Screen.ScientificCalculator -> {
                    ScientificCalculatorScreen(
                        onNavigateBack = { currentScreen = Screen.Home }
                    )
                }

                is Screen.Settings -> {
                    SettingsScreen(
                        prefs = prefs,
                        onThemeChanged = onThemeChanged,
                        onNavigateBack = { currentScreen = Screen.Home },
                        onCheckUpdate = {
                            val availableUpdate = runCatching {
                                AppUpdateChecker.check(context)
                            }.getOrNull()
                            updateInfo = availableUpdate
                            availableUpdate != null
                        }
                    )
                }
            }
        }
    }

    updateInfo?.let { info ->
        UpdateAvailableDialog(
            info = info,
            onDismiss = {
                if (!info.forceUpdate) updateInfo = null
            },
            onDownload = {
                context.openUpdatePage(info.downloadUrl)
                if (!info.forceUpdate) updateInfo = null
            }
        )
    }
}

private val locationPermissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

private fun Context.hasLocationPermission(): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

private fun requestGpsLocation(
    context: Context,
    locationHelper: LocationHelper
) {
    PreferencesHelper(context).locationInputMode = "GPS"
    Toast.makeText(context, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
    locationHelper.refreshLocation { success ->
        Toast.makeText(
            context,
            if (success) {
                "Lokasi berhasil diperbarui!"
            } else {
                "Gagal memperbarui lokasi. Nyalakan GPS Anda."
            },
            Toast.LENGTH_SHORT
        ).show()
    }
}

@Composable
private fun UpdateAvailableDialog(
    info: AppUpdateInfo,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Update FalakPro Tersedia")
        },
        text = {
            val changelog = info.changelog.ifBlank { "Versi baru FalakPro sudah tersedia di Google Play Store." }
            Text(
                text = "Versi ${info.versionName} sudah dirilis.\n\n$changelog"
            )
        },
        confirmButton = {
            TextButton(onClick = onDownload) {
                Text("Update di Play Store")
            }
        },
        dismissButton = {
            if (!info.forceUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("Nanti")
                }
            }
        }
    )
}

private fun Context.openUpdatePage(url: String) {
    AppUpdateChecker.openPlayStore(this)
}
