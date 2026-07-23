package com.falak.falakpro.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.falak.falakpro.location.LocationHelper
import com.falak.falakpro.premium.PreferencesHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.*

private fun formatDms(deg: Double?): String {
    if (deg == null || deg.isNaN()) return "-"
    val absDeg = abs(deg % 360.0)
    val d = floor(absDeg).toInt()
    val mFull = (absDeg - d) * 60.0
    val m = floor(mFull).toInt()
    val s = (mFull - m) * 60.0
    val sign = if (deg < 0) "-" else ""
    return String.format(Locale.US, "%s%d° %02d' %02d\"", sign, d, m, s.toInt())
}

private fun formatDecimalHourHms(hour: Double): String {
    val normalized = ((hour % 24.0) + 24.0) % 24.0
    val h = floor(normalized).toInt()
    val minuteFloat = (normalized - h) * 60.0
    val m = floor(minuteFloat).toInt()
    val s = round((minuteFloat - m) * 60.0).toInt()
    return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
}

private data class QiblaDateKey(val year: Int, val month: Int, val day: Int)

private fun currentQiblaDateKey(): QiblaDateKey {
    val calendar = Calendar.getInstance()
    return QiblaDateKey(
        year = calendar.get(Calendar.YEAR),
        month = calendar.get(Calendar.MONTH) + 1,
        day = calendar.get(Calendar.DAY_OF_MONTH)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KiblatScreen(
    onNavigateBack: () -> Unit,
    onNavigateToKamera: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    showCalibrationOnOpen: Boolean = false,
    onCalibrationPromptConsumed: () -> Unit = {}
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesHelper(context) }
    val showSunMoon by rememberKiblatShowSunMoonState(prefs)
    val locationHelper = remember { LocationHelper(context) }
    val locationState by locationHelper.locationState.collectAsState()
    
    var lokasiOtomatisState by remember { mutableStateOf(prefs.lokasiOtomatis) }
    var locationInputMode by remember { mutableStateOf(prefs.locationInputMode) }
    var locationRevision by remember { mutableIntStateOf(0) }
    var showLocationChoiceSheet by remember { mutableStateOf(false) }
    var showCityPickerDialog by remember { mutableStateOf(false) }
    
    // States for Manual Location Input
    var locName by remember { mutableStateOf(prefs.manualLokasiNama) }
    
    fun getDms(d: Double): Triple<Int, Int, Int> {
        val a = abs(d)
        val dd = floor(a).toInt()
        val mm = floor((a - dd) * 60).toInt()
        val ss = Math.round((a - dd - mm / 60.0) * 3600).toInt()
        return Triple(dd, mm, ss)
    }
    
    val initialLatDms = getDms(prefs.manualLat)
    val initialLonDms = getDms(prefs.manualLon)
    
    var latDeg by remember { mutableStateOf(initialLatDms.first.toString()) }
    var latMin by remember { mutableStateOf(initialLatDms.second.toString()) }
    var latSec by remember { mutableStateOf(initialLatDms.third.toString()) }
    var latSouth by remember { mutableStateOf(prefs.manualLat < 0) }
    
    var lonDeg by remember { mutableStateOf(initialLonDms.first.toString()) }
    var lonMin by remember { mutableStateOf(initialLonDms.second.toString()) }
    var lonSec by remember { mutableStateOf(initialLonDms.third.toString()) }
    var lonEast by remember { mutableStateOf(prefs.manualLon >= 0) }
    
    var elevation by remember { mutableStateOf(prefs.manualElev) }
    
    fun updateManualLat() {
        val deg = latDeg.toDoubleOrNull() ?: 0.0
        val min = latMin.toDoubleOrNull() ?: 0.0
        val sec = latSec.toDoubleOrNull() ?: 0.0
        var decimal = deg + min / 60.0 + sec / 3600.0
        if (latSouth) {
            decimal = -decimal
        }
        prefs.manualLat = decimal
    }
    
    fun updateManualLon() {
        val deg = lonDeg.toDoubleOrNull() ?: 0.0
        val min = lonMin.toDoubleOrNull() ?: 0.0
        val sec = lonSec.toDoubleOrNull() ?: 0.0
        var decimal = deg + min / 60.0 + sec / 3600.0
        if (!lonEast) {
            decimal = -decimal
        }
        prefs.manualLon = decimal
    }

    val useGpsLocation = locationInputMode == "GPS"
    val savedManualLat = remember(locationRevision) { prefs.manualLat }
    val savedManualLon = remember(locationRevision) { prefs.manualLon }
    val savedManualTimezone = remember(locationRevision) { prefs.manualTimezone }
    val savedManualLocationName = remember(locationRevision) { prefs.manualLokasiNama }
    val lat = if (useGpsLocation) {
        if (locationState.latitude != 0.0) locationState.latitude else -6.3133
    } else {
        savedManualLat
    }
    
    val lon = if (useGpsLocation) {
        if (locationState.longitude != 0.0) locationState.longitude else 107.3191
    } else {
        savedManualLon
    }
    
    val tzOffset = if (useGpsLocation) timezoneFromLongitude(lon) else savedManualTimezone
    
    var showSettings by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    LaunchedEffect(showCalibrationOnOpen) {
        if (showCalibrationOnOpen) {
            showCalibrationDialog = true
            onCalibrationPromptConsumed()
        }
    }
    
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val textMeasurer = rememberTextMeasurer()
    
    var phoneAzimuth by remember { mutableStateOf(0f) }
    var pitch by remember { mutableStateOf(0f) }
    var roll by remember { mutableStateOf(0f) }

    // Azimuth correction from settings prefs (persisted from calibration)
    var azimuthCorrection by remember { mutableStateOf(if (prefs.kiblatKoreksiAzimut) prefs.kiblatKoreksiNilai else 0f) }

    // Magnetic declination for this location (positive = East declination)
    var magneticDeclination by remember { mutableStateOf(0f) }
    LaunchedEffect(lat, lon) {
        val geoField = GeomagneticField(
            lat.toFloat(), lon.toFloat(), prefs.manualElev.toFloat(),
            System.currentTimeMillis()
        )
        magneticDeclination = geoField.declination
    }

    // Update settings and correction whenever screen is composed / re-entered
    LaunchedEffect(Unit) {
        azimuthCorrection = if (prefs.kiblatKoreksiAzimut) prefs.kiblatKoreksiNilai else 0f
    }

    // Effective phone azimuth:
    // - Tanpa kalibrasi (kiblatKoreksiAzimut = false): azimuthCorrection = 0 → Utara Magnetik ditambah deklinasi → Utara Sejati
    // - Dengan kalibrasi (kiblatKoreksiAzimut = true) : azimuthCorrection = offset kalibrasi → Utara Sejati
    val effectiveAzimuth = if (prefs.kiblatKoreksiAzimut) {
        (phoneAzimuth + azimuthCorrection + 360f) % 360f
    } else {
        (phoneAzimuth + magneticDeclination + 360f) % 360f
    }

    val northReferenceLabel = if (prefs.kiblatKoreksiAzimut) "Utara Sejati" else "Utara Magnetik"
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            locationInputMode = "GPS"
            lokasiOtomatisState = true
            prefs.locationInputMode = "GPS"
            Toast.makeText(context, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
            locationHelper.refreshLocation { success ->
                if (success) {
                    Toast.makeText(context, "Lokasi GPS berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal GPS. Menggunakan estimasi jaringan.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    fun useCurrentGpsLocation() {
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
            lokasiOtomatisState = true
            prefs.locationInputMode = "GPS"
            Toast.makeText(context, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
            locationHelper.refreshLocation { success ->
                if (success) {
                    Toast.makeText(context, "Lokasi GPS berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Gagal GPS. Menggunakan estimasi jaringan.", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }
    
    LaunchedEffect(lokasiOtomatisState) {
        if (!lokasiOtomatisState) return@LaunchedEffect

        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasPermission) {
            locationHelper.startLocationUpdates()
        } else {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }
    
    DisposableEffect(sensorManager) {
        val listener = object : SensorEventListener {
            val rotationMatrix = FloatArray(9)
            val orientationAngles = FloatArray(3)
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.getOrientation(rotationMatrix, orientationAngles)
                    
                    // Azimuth
                    var az = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    if (az < 0) az += 360f
                    phoneAzimuth = az
                    
                    // Pitch and Roll for Leveling
                    pitch = Math.toDegrees(orientationAngles[1].toDouble()).toFloat()
                    roll = Math.toDegrees(orientationAngles[2].toDouble()).toFloat()
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (sensor != null) {
            sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }
    
    val (qiblaVinAz, qiblaVinDist) = remember(lat, lon, prefs.kiblatMethod) { 
        when (prefs.kiblatMethod) {
            0 -> Pair(QiblaEngine.calculateSpherical(lat, lon), 0.0)
            1 -> Pair(QiblaEngine.calculateEllipsoid(lat, lon), 0.0)
            else -> QiblaEngine.calculateVincenty(lat, lon)
        }
    }
    var sunAzimuth by remember { mutableStateOf<Double?>(null) }
    var sunAltitude by remember { mutableStateOf<Double?>(null) }
    var moonAzimuth by remember { mutableStateOf<Double?>(null) }
    var moonAltitude by remember { mutableStateOf<Double?>(null) }
    var rashdulQiblah by remember { mutableStateOf<QiblaEngine.RashdulQiblaResult?>(null) }
    var rashdulDate by remember { mutableStateOf(currentQiblaDateKey()) }

    LaunchedEffect(rashdulDate, lat, lon, tzOffset, prefs.kiblatMethod) {
        rashdulQiblah = withContext(Dispatchers.Default) {
            runCatching {
                QiblaEngine.calculateRashdulQibla(
                    lat = lat,
                    lon = lon,
                    timezone = tzOffset,
                    year = rashdulDate.year,
                    month = rashdulDate.month,
                    day = rashdulDate.day,
                    method = prefs.kiblatMethod,
                    context = context
                )
            }.getOrNull()
        }
    }
    
    LaunchedEffect(lat, lon, tzOffset) {
        while(true) {
            val now = Calendar.getInstance()
            val today = QiblaDateKey(
                year = now.get(Calendar.YEAR),
                month = now.get(Calendar.MONTH) + 1,
                day = now.get(Calendar.DAY_OF_MONTH)
            )
            if (today != rashdulDate) {
                rashdulDate = today
            }
            val localTimeFraction = now.get(Calendar.HOUR_OF_DAY) / 24.0 + 
                                   now.get(Calendar.MINUTE) / 1440.0 + 
                                   now.get(Calendar.SECOND) / 86400.0
            
            val jdNow = Julian.fromCalendar(
                now.get(Calendar.YEAR), 
                now.get(Calendar.MONTH) + 1, 
                now.get(Calendar.DAY_OF_MONTH).toDouble()
            ) + localTimeFraction - (tzOffset / 24.0)
            
            val sunPos = QiblaEngine.calculateSunPosition(jdNow, lat, lon, context)
            sunAzimuth = sunPos.first
            sunAltitude = sunPos.second
            
            val moonPos = QiblaEngine.calculateMoonPosition(jdNow, lat, lon, context)
            moonAzimuth = moonPos.first
            moonAltitude = moonPos.second

            delay(1000)
        }
    }

    // Alignment logic (within 1.5 degrees tolerance)
    val isAligned = remember(effectiveAzimuth, qiblaVinAz) {
        val diff = abs(effectiveAzimuth - qiblaVinAz.toFloat()) % 360f
        diff < 1.5f || diff > 358.5f
    }
    
    // Play beep sound and vibrate ONCE when alignment transitions to aligned (only if enabled in settings)
    LaunchedEffect(isAligned) {
        if (isAligned && prefs.kiblatBeepSound) {
            try {
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                if (vibrator != null && vibrator.hasVibrator()) {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        vibrator.vibrate(
                            android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(200)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            
            try {
                val toneGen = android.media.ToneGenerator(android.media.AudioManager.STREAM_NOTIFICATION, 95)
                toneGen.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 180)
                delay(300)
                toneGen.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Infinite transition for blinking effect when aligned
    val infiniteTransition = rememberInfiniteTransition(label = "Blink")
    val blinkAlpha by if (isAligned) {
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(350, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "BlinkAlpha"
        )
    } else {
        remember { mutableStateOf(1.0f) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF009688), // Beautiful Teal
                        Color(0xFF00796B)
                    )
                )
            )
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Arah kiblat",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                        }
                    },
                    actions = {
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Default.Info, contentDescription = "Info", tint = Color.White)
                        }
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Ubah Lokasi Manual") },
                                onClick = {
                                    showMenu = false
                                    showSettings = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Deteksi GPS Otomatis") },
                                onClick = {
                                    showMenu = false
                                    useCurrentGpsLocation()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Pengaturan Kompas") },
                                onClick = {
                                    showMenu = false
                                    onNavigateToSettings()
                                }
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Info Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = "Lokasi",
                            tint = Color(0xFFE57373),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (lokasiOtomatisState) locationState.address else savedManualLocationName,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Update",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showLocationChoiceSheet = true }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    Text(
                        text = "Azimut HP ($northReferenceLabel)",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Dynamic phone heading in large format, blinks when aligned
                    Text(
                        text = formatDms(effectiveAzimuth.toDouble()),
                        color = if (isAligned) Color(0xFFFFF176) else Color.White,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.graphicsLayer {
                            if (isAligned) {
                                alpha = blinkAlpha
                            }
                        }
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Fixed Qibla Azimuth in DMS format
                    Text(
                        text = "Sudut Kiblat: ${formatDms(qiblaVinAz)}",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )

                    // Declination info when not calibrated
                    if (!prefs.kiblatKoreksiAzimut) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Deklinasi magnetik: ${String.format(java.util.Locale.US, "%+.2f°", magneticDeclination)}",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 11.sp
                        )
                    }
                }

                // Compass Visual Section
                Box(
                    modifier = Modifier
                        .size(290.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Pulsing green/gold circle behind compass when aligned
                    if (isAligned) {
                        Box(
                            modifier = Modifier
                                .size(286.dp)
                                .graphicsLayer { alpha = blinkAlpha * 0.25f }
                                .background(Color(0xFFFFF176), CircleShape)
                        )
                    }
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.width / 2 - 16.dp.toPx()
                        
                        // Dial White Circle Background with shadow
                        drawCircle(
                            color = Color.White,
                            radius = radius
                        )
                        drawCircle(
                            color = Color(0xFFE0E0E0),
                            radius = radius,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Rotates the entire dial containing ticks, letters, sun lines, and Qibla arrow
                        rotate(-effectiveAzimuth, center) {
                            
                            // Draw Central Mandala Ornament
                            for (i in 0 until 12) {
                                val angleRad = Math.toRadians(i * 30.0)
                                val cOffset = Offset(
                                    center.x + 8.dp.toPx() * cos(angleRad).toFloat(),
                                    center.y + 8.dp.toPx() * sin(angleRad).toFloat()
                                )
                                drawCircle(
                                    color = Color(0xFFECEFF1).copy(alpha = 0.9f),
                                    radius = 22.dp.toPx(),
                                    center = cOffset,
                                    style = Stroke(width = 1.2.dp.toPx())
                                )
                            }
                            
                            // Draw Dial Ticks
                            for (i in 0 until 120) {
                                val angleDeg = i * 3.0
                                val angleRad = Math.toRadians(angleDeg - 90.0)
                                val isMajor = i % 10 == 0
                                val tickLen = if (isMajor) 12.dp.toPx() else 6.dp.toPx()
                                val start = Offset(
                                    center.x + (radius - tickLen) * cos(angleRad).toFloat(),
                                    center.y + (radius - tickLen) * sin(angleRad).toFloat()
                                )
                                val end = Offset(
                                    center.x + radius * cos(angleRad).toFloat(),
                                    center.y + radius * sin(angleRad).toFloat()
                                )
                                drawLine(
                                    color = if (isMajor) Color(0xFF90A4AE) else Color(0xFFCFD8DC),
                                    start = start,
                                    end = end,
                                    strokeWidth = if (isMajor) 1.5.dp.toPx() else 1.dp.toPx()
                                )
                            }

                            // T, S, B, U Labels
                            val labels = listOf("U" to 0, "T" to 90, "S" to 180, "B" to 270)
                            labels.forEach { (label, angle) ->
                                val angleRad = Math.toRadians(angle.toDouble() - 90.0)
                                val textPos = Offset(
                                    center.x + (radius - 24.dp.toPx()) * cos(angleRad).toFloat(),
                                    center.y + (radius - 24.dp.toPx()) * sin(angleRad).toFloat()
                                )
                                val textLayout = textMeasurer.measure(
                                    text = AnnotatedString(label),
                                    style = TextStyle(
                                        color = Color(0xFF00796B),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                )
                                drawText(
                                    textLayout,
                                    topLeft = textPos - Offset(textLayout.size.width / 2f, textLayout.size.height / 2f)
                                )
                            }

                            if (showSunMoon) {
                                sunAzimuth?.let { sa ->
                                    val sunAngleRad = Math.toRadians(sa - 90.0)

                                    drawLine(
                                        color = Color.LightGray.copy(alpha = 0.5f),
                                        start = center,
                                        end = center - Offset(
                                            (radius - 30.dp.toPx()) * cos(sunAngleRad).toFloat(),
                                            (radius - 30.dp.toPx()) * sin(sunAngleRad).toFloat()
                                        ),
                                        strokeWidth = 1.5.dp.toPx()
                                    )

                                    drawLine(
                                        color = Color(0xFFFFD54F),
                                        start = center,
                                        end = center + Offset(
                                            (radius - 30.dp.toPx()) * cos(sunAngleRad).toFloat(),
                                            (radius - 30.dp.toPx()) * sin(sunAngleRad).toFloat()
                                        ),
                                        strokeWidth = 2.dp.toPx()
                                    )

                                    drawCircle(
                                        color = Color(0xFFFFB300),
                                        radius = 8.dp.toPx(),
                                        center = center + Offset(
                                            (radius - 30.dp.toPx()) * cos(sunAngleRad).toFloat(),
                                            (radius - 30.dp.toPx()) * sin(sunAngleRad).toFloat()
                                        )
                                    )
                                }
                            }

                            // Kaaba Icon on outer rim at Qibla angle
                            val kAngleRad = Math.toRadians(qiblaVinAz - 90.0)
                            val kCenter = Offset(
                                center.x + (radius - 12.dp.toPx()) * cos(kAngleRad).toFloat(),
                                center.y + (radius - 12.dp.toPx()) * sin(kAngleRad).toFloat()
                            )
                            
                            // Kaaba Outer Ring
                            drawCircle(
                                color = Color.White,
                                radius = 15.dp.toPx(),
                                center = kCenter
                            )
                            drawCircle(
                                color = Color(0xFFD4AF37), // Gold outline
                                radius = 15.dp.toPx(),
                                center = kCenter,
                                style = Stroke(width = 1.5.dp.toPx())
                            )
                            
                            // Draw Kaaba Body
                            val kSize = 13.dp.toPx()
                            drawRect(
                                color = Color(0xFF1E1E1E), // Black Cube
                                topLeft = kCenter - Offset(kSize / 2, kSize / 2),
                                size = Size(kSize, kSize)
                            )
                            
                            // Draw Kaaba Gold Kiswah Belt
                            drawRect(
                                color = Color(0xFFD4AF37),
                                topLeft = kCenter - Offset(kSize / 2, kSize / 2 - 2.5.dp.toPx()),
                                size = Size(kSize, 2.dp.toPx())
                            )

                            // 1. Compass Needle: ALWAYS POINTS NORTH-SOUTH (0 degrees on dial)
                            val needleWidth = 9.dp.toPx()
                            val needleLength = radius - 45.dp.toPx()
                            
                            // North Pointer (pointing to 0 degrees / U)
                            val pathLeftRed = Path().apply {
                                moveTo(center.x, center.y)
                                lineTo(center.x - needleWidth, center.y)
                                lineTo(center.x, center.y - needleLength)
                                close()
                            }
                            drawPath(pathLeftRed, Color(0xFFD32F2F))
                            
                            val pathRightRed = Path().apply {
                                moveTo(center.x, center.y)
                                lineTo(center.x + needleWidth, center.y)
                                lineTo(center.x, center.y - needleLength)
                                close()
                            }
                            drawPath(pathRightRed, Color(0xFFE57373))
                            
                            // South Pointer (pointing to 180 degrees / S)
                            val pathLeftGrey = Path().apply {
                                moveTo(center.x, center.y)
                                lineTo(center.x - needleWidth, center.y)
                                lineTo(center.x, center.y + needleLength)
                                close()
                            }
                            drawPath(pathLeftGrey, Color(0xFFECEFF1))
                            
                            val pathRightGrey = Path().apply {
                                moveTo(center.x, center.y)
                                lineTo(center.x + needleWidth, center.y)
                                lineTo(center.x, center.y + needleLength)
                                close()
                            }
                            drawPath(pathRightGrey, Color(0xFFB0BEC5))

                            // 2. Qibla Arrow Pointer: Points towards Kaaba (qiblaVinAz on dial)
                            rotate(qiblaVinAz.toFloat(), center) {
                                // Draw a solid line from the center to the edge (radius - 24.dp)
                                drawLine(
                                    color = Color(0xFFD4AF37), // Gold
                                    start = center,
                                    end = center - Offset(0f, radius - 24.dp.toPx()),
                                    strokeWidth = 3.5.dp.toPx(),
                                    cap = StrokeCap.Round
                                )
                                
                                // Draw arrowhead pointing outwards
                                val arrowTip = center - Offset(0f, radius - 15.dp.toPx())
                                val arrowSide = 8.dp.toPx()
                                val arrowPath = Path().apply {
                                    moveTo(arrowTip.x, arrowTip.y)
                                    lineTo(arrowTip.x - arrowSide, arrowTip.y + arrowSide * 1.5f)
                                    lineTo(arrowTip.x + arrowSide, arrowTip.y + arrowSide * 1.5f)
                                    close()
                                }
                                drawPath(arrowPath, Color(0xFFD4AF37))
                            }
                        }

                        // 3. SPIRIT LEVEL / BUBBLE LEVEL (Waterpass) - Drawn in the center, fixed relative to screen (not rotated)
                        // This uses a transparent liquid design directly on the dial (no solid green container background)
                        val levelRadius = 38.dp.toPx()
                        val bubbleRadius = 12.dp.toPx()
                        val isLevel = abs(pitch) < 3.0f && abs(roll) < 3.0f

                        // Outer thin boundaries of the waterpass glass vial
                        drawCircle(
                            color = Color(0xFF90A4AE).copy(alpha = 0.4f),
                            radius = levelRadius,
                            center = center,
                            style = Stroke(width = 1.dp.toPx())
                        )

                        // Center black/dark target ring printed on glass (where the bubble should align)
                        drawCircle(
                            color = if (isLevel) Color(0xFF4CAF50).copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.4f),
                            radius = bubbleRadius + 2.5.dp.toPx(),
                            center = center,
                            style = Stroke(width = 1.2.dp.toPx())
                        )

                        // Calculate bubble offset based on pitch (y-axis tilt) and roll (x-axis tilt)
                        val maxOffset = levelRadius - bubbleRadius - 1.dp.toPx()
                        val bubbleOffset = Offset(
                            x = (roll / 15f).coerceIn(-1f, 1f) * maxOffset,
                            y = (pitch / 15f).coerceIn(-1f, 1f) * maxOffset
                        )

                        // Draw transparent 3D water bubble
                        // A: Bubble translucent interior highlight
                        drawCircle(
                            color = if (isLevel) Color(0xFF4CAF50).copy(alpha = 0.25f) else Color(0xFFB0BEC5).copy(alpha = 0.25f),
                            radius = bubbleRadius,
                            center = center + bubbleOffset
                        )
                        
                        // B: Bubble refractive boundary outline
                        drawCircle(
                            color = if (isLevel) Color(0xFF4CAF50).copy(alpha = 0.8f) else Color(0xFF78909C).copy(alpha = 0.7f),
                            radius = bubbleRadius,
                            center = center + bubbleOffset,
                            style = Stroke(width = 1.5.dp.toPx())
                        )
                        
                        // C: 3D specular light glare highlight (simulating reflections on a liquid droplet/bubble)
                        drawCircle(
                            color = Color.White.copy(alpha = 0.9f),
                            radius = 3.dp.toPx(),
                            center = center + bubbleOffset - Offset(3.5.dp.toPx(), 3.5.dp.toPx())
                        )
                    }
                }

                if (showSunMoon) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Matahari", color = Color(0xFFFFD54F), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Az: ${formatDms(sunAzimuth)}", color = Color.White, fontSize = 11.sp)
                                    Text("Alt: ${formatDms(sunAltitude)}", color = Color.White, fontSize = 11.sp)
                                    Text("Byg: ${formatDms(sunAzimuth?.let { (it + 180.0) % 360.0 })}", color = Color.White, fontSize = 11.sp)
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Bulan", color = Color(0xFFE0E0E0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Az: ${formatDms(moonAzimuth)}", color = Color.White, fontSize = 11.sp)
                                    Text("Alt: ${formatDms(moonAltitude)}", color = Color.White, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Rasydu Qiblat Harian", color = Color(0xFF81C784), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = rashdulQiblah?.let { formatDecimalHourHms(it.localHour) } ?: "-",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    rashdulQiblah?.let {
                                        val targetText = if (it.target == QiblaEngine.RashdulQiblaResult.Target.SHADOW) {
                                            "Bayangan = Kiblat"
                                        } else {
                                            "Matahari = Kiblat"
                                        }
                                        Text(targetText, color = Color.White.copy(alpha = 0.8f), fontSize = 10.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Warnings & Camera Button Section
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                ) {
                    val isLevel = abs(pitch) < 3.0f && abs(roll) < 3.0f
                    if (!isLevel) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.ScreenRotation, null, tint = Color.Yellow, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Pegang HP secara datar agar kompas akurat",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // Level success indicator
                        Spacer(modifier = Modifier.height(38.dp))
                    }

                    Button(
                        onClick = onNavigateToKamera,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(27.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = Color(0xFF009688)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Arahkan Kiblat dengan Kamera",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }

    // Info Dialog
    if (showCalibrationDialog) {
        QiblaCalibrationDialog(
            onDismiss = { showCalibrationDialog = false }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("Tentang Kompas Kiblat", fontWeight = FontWeight.Bold) },
            text = {
                val northInfo = if (prefs.kiblatKoreksiAzimut)
                    "Kompas saat ini menggunakan acuan Utara Sejati (True North) berdasarkan kalibrasi astronomis."
                else
                    "Kompas saat ini menggunakan acuan Utara Magnetik (default). Deklinasi magnetik lokal: ${String.format(java.util.Locale.US, "%+.2f°", magneticDeclination)}.\nUntuk akurasi lebih tinggi, lakukan kalibrasi matahari via menu Kamera AR."
                Text(
                    "Jarum merah-putih menunjukan arah Utara-Selatan (Utara di tanda U).\n\n" +
                    "Garis panah kuning emas menunjukan arah Kiblat Ka'bah.\n\n" +
                    "Garis kuning tipis dengan lingkaran kuning menunjukan arah real-time matahari saat ini.\n" +
                    "Jika kompas magnetik sensor terganggu logam/besi, arahkan kompas agar garis kuning matahari sejajar dengan bayangan benda asli di sekitar Anda.\n\n" +
                    northInfo
                )
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("OK", color = Color(0xFF00796B), fontWeight = FontWeight.Bold)
                }
            }
        )
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
                useCurrentGpsLocation()
            }
        )
    }

    // Manual Location Settings Dialog
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text("Ubah Lokasi Manual", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = locationInputMode == "MANUAL",
                            onClick = {
                                locationInputMode = "MANUAL"
                                lokasiOtomatisState = false
                                prefs.locationInputMode = "MANUAL"
                            },
                            label = { Text("Manual") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = locationInputMode == "DAFTAR_KOTA",
                            onClick = {
                                locationInputMode = "DAFTAR_KOTA"
                                lokasiOtomatisState = false
                                prefs.locationInputMode = "DAFTAR_KOTA"
                                showCityPickerDialog = true
                            },
                            label = { Text("Kota") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = locationInputMode == "GPS",
                            onClick = {
                                locationInputMode = "GPS"
                                lokasiOtomatisState = true
                                prefs.locationInputMode = "GPS"
                            },
                            label = { Text("GPS") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (locationInputMode == "DAFTAR_KOTA") {
                        Button(
                            onClick = { showCityPickerDialog = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Pilih Kota dari Database Offline")
                        }
                    }
                    LocationInputBlock(
                        locName = locName,
                        onLocNameChange = {
                            locName = it
                            prefs.manualLokasiNama = it
                        },
                        latDeg = latDeg,
                        onLatDegChange = {
                            latDeg = it
                            updateManualLat()
                        },
                        latMin = latMin,
                        onLatMinChange = {
                            latMin = it
                            updateManualLat()
                        },
                        latSec = latSec,
                        onLatSecChange = {
                            latSec = it
                            updateManualLat()
                        },
                        latSouth = latSouth,
                        onLatSouthChange = {
                            latSouth = it
                            updateManualLat()
                        },
                        lonDeg = lonDeg,
                        onLonDegChange = {
                            lonDeg = it
                            updateManualLon()
                        },
                        lonMin = lonMin,
                        onLonMinChange = {
                            lonMin = it
                            updateManualLon()
                        },
                        lonSec = lonSec,
                        onLonSecChange = {
                            lonSec = it
                            updateManualLon()
                        },
                        lonEast = lonEast,
                        onLonEastChange = {
                            lonEast = it
                            updateManualLon()
                        },
                        elevation = elevation,
                        onElevationChange = {
                            elevation = it
                            prefs.manualElev = it
                        },
                        lokasiOtomatis = lokasiOtomatisState,
                        onLokasiOtomatisChange = { auto ->
                            lokasiOtomatisState = auto
                            locationInputMode = if (auto) "GPS" else "MANUAL"
                            prefs.locationInputMode = locationInputMode
                        },
                        onUseGps = {
                            lokasiOtomatisState = true
                            locationInputMode = "GPS"
                            prefs.locationInputMode = "GPS"
                            Toast.makeText(context, "Mencari lokasi...", Toast.LENGTH_SHORT).show()
                            locationHelper.refreshLocation { success ->
                                if (success) {
                                    Toast.makeText(context, "Lokasi GPS berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Gagal GPS. Menggunakan estimasi jaringan.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showSettings = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00796B))
                ) {
                    Text("Simpan", color = Color.White)
                }
            }
        )
    }
    if (showCityPickerDialog) {
        CityLocationPickerDialog(
            onDismiss = { showCityPickerDialog = false },
            onSelect = { city ->
                locationInputMode = "DAFTAR_KOTA"
                lokasiOtomatisState = false
                locName = city.displayName
                applyCityLocationToPrefs(prefs, city)
                locationRevision++
                val latParts = getDms(city.latitude)
                latDeg = latParts.first.toString()
                latMin = latParts.second.toString()
                latSec = latParts.third.toString()
                latSouth = city.latitude < 0
                val lonParts = getDms(city.longitude)
                lonDeg = lonParts.first.toString()
                lonMin = lonParts.second.toString()
                lonSec = lonParts.third.toString()
                lonEast = city.longitude >= 0
                elevation = city.elevation
            }
        )
    }
}

@Composable
private fun QiblaCalibrationDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Kalibrasi Kompas Anda",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = Color.Black
            )
        },
        text = {
            Column {
                Text(
                    "Lakukan gerakan 3 kali, seperti gambar berikut:",
                    fontSize = 18.sp,
                    lineHeight = 28.sp,
                    color = Color(0xFF6F6F76)
                )
                Spacer(Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .background(Color.White, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    FigureEightCalibrationAnimation(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF009D8B))
            ) {
                Text("Oke", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
private fun FigureEightCalibrationAnimation(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "calibration_figure_eight")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "figure_eight_progress"
    )

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val scale = min(size.width, size.height) * 0.34f
        val path = Path()
        for (i in 0..240) {
            val t = (i / 240f) * (2f * PI.toFloat())
            val x = center.x + scale * sin(t)
            val y = center.y + scale * sin(t) * cos(t)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = Color(0xFF40C4FF),
            style = Stroke(
                width = 4.dp.toPx(),
                cap = StrokeCap.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(9.dp.toPx(), 8.dp.toPx()), 0f)
            )
        )

        val t = progress * (2f * PI.toFloat())
        val phoneCenter = Offset(
            x = center.x + scale * sin(t),
            y = center.y + scale * sin(t) * cos(t)
        )
        val dx = scale * cos(t)
        val dy = scale * (cos(t) * cos(t) - sin(t) * sin(t))
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 18f

        drawCircle(Color(0xFF2BBEF2), radius = 7.dp.toPx(), center = phoneCenter)
        rotate(angle, phoneCenter) {
            drawRoundRect(
                color = Color(0xFFE5A48E),
                topLeft = Offset(phoneCenter.x - 22.dp.toPx(), phoneCenter.y + 14.dp.toPx()),
                size = Size(30.dp.toPx(), 64.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )
            drawRoundRect(
                color = Color.Black,
                topLeft = Offset(phoneCenter.x - 16.dp.toPx(), phoneCenter.y - 38.dp.toPx()),
                size = Size(34.dp.toPx(), 72.dp.toPx()),
                cornerRadius = CornerRadius(5.dp.toPx(), 5.dp.toPx())
            )
            drawRoundRect(
                color = Color(0xFF20242A),
                topLeft = Offset(phoneCenter.x - 12.dp.toPx(), phoneCenter.y - 32.dp.toPx()),
                size = Size(26.dp.toPx(), 58.dp.toPx()),
                cornerRadius = CornerRadius(3.dp.toPx(), 3.dp.toPx())
            )
        }
    }
}

@Composable
fun ResultRow(label: String, value: String, color: Color = Color.Black) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontSize = 13.sp, color = Color.Gray)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationInputBlock(
    locName: String,
    onLocNameChange: (String) -> Unit,
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
    elevation: Double,
    onElevationChange: (Double) -> Unit,
    lokasiOtomatis: Boolean,
    onLokasiOtomatisChange: (Boolean) -> Unit,
    onUseGps: () -> Unit
) {
    val inputTeal = Color(0xFF00796B)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Lokasi Otomatis (GPS)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Switch(
                checked = lokasiOtomatis,
                onCheckedChange = onLokasiOtomatisChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = inputTeal,
                    checkedTrackColor = inputTeal.copy(alpha = 0.5f)
                )
            )
        }
        
        if (lokasiOtomatis) {
            Button(
                onClick = onUseGps,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = inputTeal),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Perbarui Lokasi GPS Sekarang", fontSize = 13.sp)
            }
        } else {
            // Nama Lokasi Manual
            OutlinedTextField(
                value = locName,
                onValueChange = onLocNameChange,
                label = { Text("Nama Lokasi") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = inputTeal,
                    focusedLabelColor = inputTeal
                )
            )
            
            // Latitude DMS
            Text("Latitude (Lintang)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = inputTeal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = latDeg,
                    onValueChange = { onLatDegChange(it) },
                    label = { Text("Deg") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inputTeal,
                        focusedLabelColor = inputTeal
                    )
                )
                OutlinedTextField(
                    value = latMin,
                    onValueChange = { onLatMinChange(it) },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inputTeal,
                        focusedLabelColor = inputTeal
                    )
                )
                OutlinedTextField(
                    value = latSec,
                    onValueChange = { onLatSecChange(it) },
                    label = { Text("Sec") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inputTeal,
                        focusedLabelColor = inputTeal
                    )
                )
                
                Button(
                    onClick = { onLatSouthChange(!latSouth) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (latSouth) Color.Red else inputTeal),
                    modifier = Modifier.width(54.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (latSouth) "LS" else "LU", fontSize = 11.sp, color = Color.White)
                }
            }
            
            // Longitude DMS
            Text("Longitude (Bujur)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = inputTeal)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = lonDeg,
                    onValueChange = { onLonDegChange(it) },
                    label = { Text("Deg") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inputTeal,
                        focusedLabelColor = inputTeal
                    )
                )
                OutlinedTextField(
                    value = lonMin,
                    onValueChange = { onLonMinChange(it) },
                    label = { Text("Min") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inputTeal,
                        focusedLabelColor = inputTeal
                    )
                )
                OutlinedTextField(
                    value = lonSec,
                    onValueChange = { onLonSecChange(it) },
                    label = { Text("Sec") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = inputTeal,
                        focusedLabelColor = inputTeal
                    )
                )
                
                Button(
                    onClick = { onLonEastChange(!lonEast) },
                    colors = ButtonDefaults.buttonColors(containerColor = if (lonEast) inputTeal else Color.Red),
                    modifier = Modifier.width(54.dp),
                    contentPadding = PaddingValues(0.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (lonEast) "BT" else "BB", fontSize = 11.sp, color = Color.White)
                }
            }
            
            // Ketinggian
            OutlinedTextField(
                value = if (elevation == 0.0) "" else elevation.toString(),
                onValueChange = { onElevationChange(it.toDoubleOrNull() ?: 0.0) },
                label = { Text("Ketinggian (meter)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = inputTeal,
                    focusedLabelColor = inputTeal
                )
            )
        }
    }
}

