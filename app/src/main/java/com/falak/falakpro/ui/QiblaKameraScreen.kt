package com.falak.falakpro.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.falak.falakpro.R
import com.falak.falakpro.location.LocationHelper
import com.falak.falakpro.premium.PreferencesHelper
import com.falak.falakpro.premium.AstroMoonEngine
import com.falak.falakpro.premium.AstroDataUtils
import com.falak.falakpro.premium.AstroMath
import com.falak.falakpro.premium.AstroAssetPreloader
import kotlinx.coroutines.delay
import java.util.*
import kotlin.math.*

@Composable
fun QiblaKameraScreen(
    onNavigateBack: () -> Unit,
    onNavigateToSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val prefs = remember { PreferencesHelper(context) }
    val showSunMoon by rememberKiblatShowSunMoonState(prefs)
    val locationHelper = remember { LocationHelper(context) }
    val locationState by locationHelper.locationState.collectAsState()

    val lat = if (prefs.lokasiOtomatis && locationState.latitude != 0.0) locationState.latitude else prefs.manualLat
    val lon = if (prefs.lokasiOtomatis && locationState.longitude != 0.0) locationState.longitude else prefs.manualLon

    var hasCameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var hasLocationPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasCameraPermission = permissions[Manifest.permission.CAMERA] ?: hasCameraPermission
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: hasLocationPermission
        
        if (prefs.lokasiOtomatis && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
            locationHelper.startLocationUpdates()
        }
    }

    LaunchedEffect(Unit) {
        val needsCamera = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
        val needsLocation = prefs.lokasiOtomatis &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
        
        if (needsCamera || needsLocation) {
            val permissions = buildList {
                add(Manifest.permission.CAMERA)
                if (prefs.lokasiOtomatis) {
                    add(Manifest.permission.ACCESS_FINE_LOCATION)
                    add(Manifest.permission.ACCESS_COARSE_LOCATION)
                }
            }.toTypedArray()
            permissionLauncher.launch(permissions)
        } else if (prefs.lokasiOtomatis) {
            locationHelper.startLocationUpdates()
        }
    }

    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    var rawAzimuth by remember { mutableStateOf(0f) }
    var pitch by remember { mutableStateOf(0f) }
    var sensorAccuracy by remember { mutableIntStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var showCalibrationHint by remember { mutableStateOf(false) }

    val ALPHA = 0.15f
    DisposableEffect(sensorManager) {
        val rotationMatrix = FloatArray(9)
        val remappedMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)
        var azLp = 0f
        var pitchLp = 0f

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                    SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_X, SensorManager.AXIS_Z, remappedMatrix)
                    SensorManager.getOrientation(remappedMatrix, orientationAngles)

                    var az = Math.toDegrees(orientationAngles[0].toDouble()).toFloat()
                    if (az < 0) az += 360f
                    // Camera elevation: back camera points in device -Z direction.
                    // rotationMatrix[8] is world-Z component of device-Z axis.
                    // So camera world-Z = -rotationMatrix[8], elevation = arcsin of that.
                    // Gives 0° when phone vertical, +° up, -° down.
                    val p = Math.toDegrees(asin(-rotationMatrix[8].toDouble().coerceIn(-1.0, 1.0))).toFloat()

                    val azDiff = ((az - azLp + 540f) % 360f) - 180f
                    azLp += ALPHA * azDiff
                    azLp = (azLp + 360f) % 360f
                    pitchLp += ALPHA * (p - pitchLp)

                    rawAzimuth = azLp
                    pitch = pitchLp
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                if (sensor?.type == Sensor.TYPE_ROTATION_VECTOR) {
                    sensorAccuracy = accuracy
                    showCalibrationHint = accuracy <= SensorManager.SENSOR_STATUS_ACCURACY_LOW
                }
            }
        }
        sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let { 
            sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) 
        }
        onDispose { sensorManager.unregisterListener(listener) }
    }

    val (qiblaAz, _) = remember(lat, lon, prefs.kiblatMethod) { 
        when (prefs.kiblatMethod) {
            0 -> Pair(QiblaEngine.calculateSpherical(lat, lon), 0.0)
            1 -> Pair(QiblaEngine.calculateEllipsoid(lat, lon), 0.0)
            else -> QiblaEngine.calculateVincenty(lat, lon)
        }
    }

    var magneticDeclination by remember { mutableStateOf(0f) }
    LaunchedEffect(lat, lon) {
        val geoField = GeomagneticField(lat.toFloat(), lon.toFloat(), prefs.manualElev.toFloat(), System.currentTimeMillis())
        magneticDeclination = geoField.declination
    }

    var azimuthCorrection by remember { mutableStateOf(if (prefs.kiblatKoreksiAzimut) prefs.kiblatKoreksiNilai else 0f) }
    LaunchedEffect(Unit) {
        azimuthCorrection = if (prefs.kiblatKoreksiAzimut) prefs.kiblatKoreksiNilai else 0f
    }

    // Effective azimuth: True North if calibrated or corrected by declination
    val effectiveAzimuth = if (prefs.kiblatKoreksiAzimut) {
        (rawAzimuth + azimuthCorrection + 360f) % 360f
    } else {
        (rawAzimuth + magneticDeclination + 360f) % 360f
    }

    fun angleDiff(a: Float, b: Float): Float {
        var d = (a - b + 360f) % 360f
        if (d > 180f) d -= 360f
        return d
    }
    val headingError = angleDiff(effectiveAzimuth, qiblaAz.toFloat())
    val isPointingQibla = abs(headingError) < 3f

    // ── Vibration / Beep feedback when aligned ──
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }
    var lastFeedbackTime by remember { mutableLongStateOf(0L) }

    LaunchedEffect(isPointingQibla) {
        if (isPointingQibla && prefs.kiblatBeepSound) {
            val now = System.currentTimeMillis()
            if (now - lastFeedbackTime > 2000L) {
                lastFeedbackTime = now
                // Vibrate
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(200)
                }
                // Beep
                try {
                    val toneGen = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80)
                    toneGen.startTone(ToneGenerator.TONE_PROP_BEEP, 150)
                    delay(200)
                    toneGen.release()
                } catch (_: Exception) {}
            }
        }
    }

    // ── Pulsing animation for aligned state ──
    val infiniteTransition = rememberInfiniteTransition(label = "qibla_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    // Astro Data
    var sunAzimuth by remember { mutableStateOf<Double?>(null) }
    var sunAltitude by remember { mutableStateOf<Double?>(null) }
    var moonAzimuth by remember { mutableStateOf<Double?>(null) }
    var moonAltitude by remember { mutableStateOf<Double?>(null) }
    var utcTimeString by remember { mutableStateOf("") }

    LaunchedEffect(lat, lon) {
        while (true) {
            try {
                val now = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                utcTimeString = String.format(Locale.US, "%04d.%d.%d %02d:%02d:%02d", 
                    now.get(Calendar.YEAR), now.get(Calendar.MONTH)+1, now.get(Calendar.DAY_OF_MONTH),
                    now.get(Calendar.HOUR_OF_DAY), now.get(Calendar.MINUTE), now.get(Calendar.SECOND))

                val jdNow = Julian.fromCalendar(now.get(Calendar.YEAR), now.get(Calendar.MONTH) + 1, now.get(Calendar.DAY_OF_MONTH).toDouble()) + 
                            (now.get(Calendar.HOUR_OF_DAY) / 24.0 + now.get(Calendar.MINUTE) / 1440.0 + now.get(Calendar.SECOND) / 86400.0)
                
                // KOREKSI WAKTU DINAMIK (Delta T):
                val deltaT = com.falak.falakpro.premium.DynamicalTimeEngine.deltaT(jdNow)
                val jdeNow = jdNow + deltaT / 86400.0

                // ── VSOP87 + IAU2000A Nutation ──
                val earth = com.falak.falakpro.ui.VsopFactory.createEarth(context)
                val sun = com.falak.falakpro.ui.SunEngine.compute(jdeNow, earth, context)
                val nut = com.falak.falakpro.ui.NutationIAU2000A(context).compute(jdeNow)
                val gast = AstroDataUtils.calculateGAST(jdNow) 

                // ── Sun: topocentric diurnal parallax (Meeus Ch.40) then Azimuth/Altitude ──
                val latR = Math.toRadians(lat)
                val lhaSun = AstroMath.mod(gast + lon - sun.rightAscension, 360.0)
                val hR_sun = Math.toRadians(lhaSun)
                val decR_sun = Math.toRadians(sun.declination)

                // Parallax
                val pi0_sun = Math.toRadians(AstroMath.SOLAR_PARALLAX_ARCSEC / 3600.0 / sun.distanceAU)
                val uSun = atan(0.99664719 * tan(latR))
                val rsPSun = 0.99664719 * sin(uSun)
                val rcPSun = cos(uSun)
                val dHSun = atan2(-rcPSun * sin(pi0_sun) * sin(hR_sun), cos(decR_sun) - rcPSun * sin(pi0_sun) * cos(hR_sun))
                val hTSun = hR_sun - dHSun
                val decTSun = atan2((sin(decR_sun) - rsPSun * sin(pi0_sun)) * cos(dHSun), cos(decR_sun) - rcPSun * sin(pi0_sun) * cos(hR_sun))

                // Geometric altitude (no refraction)
                val altGeom = Math.toDegrees(asin(sin(latR)*sin(decTSun) + cos(latR)*cos(decTSun)*cos(hTSun)))

                // Atmospheric refraction correction (Bennett formula, Meeus Ch.16)
                // R in arcmin: R = 1.02/tan(h+10.3/(h+5.11)) when h in degrees
                val altR_deg = altGeom + 0.0
                val refraction = if (altR_deg > -0.575) {
                    val hRad = Math.toRadians(altR_deg + 10.3 / (altR_deg + 5.11))
                    1.02 / tan(hRad) / 60.0  // degrees
                } else {
                    -20.774 / tan(Math.toRadians(altR_deg)) / 3600.0
                }
                sunAltitude = altGeom + refraction

                // Azimuth dari Utara searah jarum jam (N=0°, E=90°, S=180°, W=270°)
                // Sesuai standar NASA JPL Horizons: atan2(-sin(HA)·cos(δ), sin(δ)·cos(φ) - cos(δ)·sin(φ)·cos(HA))
                val ySun = -sin(hTSun) * cos(decTSun)
                val xSun = sin(decTSun) * cos(latR) - cos(decTSun) * sin(latR) * cos(hTSun)
                sunAzimuth = AstroMath.mod(Math.toDegrees(atan2(ySun, xSun)), 360.0)

                // Moon Alt/Az: aset sudah dipreload sekali agar loop realtime tidak membuka file biner berulang.
                AstroAssetPreloader.ensureCoreBlocking(context)
                // ── Moon topocentric Az/Alt + refraction ──
                val moonTopo = AstroMoonEngine.getTopocentricPosition(jdNow, lon, lat, prefs.manualElev)
                val hR_moon = Math.toRadians(AstroMath.mod(gast + lon - moonTopo.first, 360.0))
                val decR_moon = Math.toRadians(moonTopo.second)
                val moonAltGeom = Math.toDegrees(asin(sin(latR)*sin(decR_moon) + cos(latR)*cos(decR_moon)*cos(hR_moon)))
                val moonRefraction = if (moonAltGeom > -0.575) {
                    val hr2 = Math.toRadians(moonAltGeom + 10.3 / (moonAltGeom + 5.11))
                    1.02 / tan(hr2) / 60.0
                } else {
                    -20.774 / tan(Math.toRadians(moonAltGeom)) / 3600.0
                }
                moonAltitude = moonAltGeom + moonRefraction
                // Azimuth dari Utara searah jarum jam (N=0°, E=90°, S=180°, W=270°)
                val y_moon = -sin(hR_moon) * cos(decR_moon)
                val x_moon = sin(decR_moon) * cos(latR) - cos(decR_moon) * sin(latR) * cos(hR_moon)
                moonAzimuth = AstroMath.mod(Math.toDegrees(atan2(y_moon, x_moon)), 360.0)

            } catch (e: Exception) { e.printStackTrace() }
            delay(1000)
        }
    }

    val textMeasurer = rememberTextMeasurer()

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val resolutionSelector = ResolutionSelector.Builder().setResolutionStrategy(ResolutionStrategy.HIGHEST_AVAILABLE_STRATEGY).build()
                        val preview = androidx.camera.core.Preview.Builder().setResolutionSelector(resolutionSelector).build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview)
                        } catch (e: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                }
            )
        }

        // Overlay for Calibration Hint (Figure-8)
        if (showCalibrationHint) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0x99000000)) // Semi-transparent black
                    .clickable { showCalibrationHint = false }, // Allow user to dismiss manually
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Kalibrasi",
                        tint = Color.White,
                        modifier = Modifier.size(64.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Akurasi Kompas Rendah",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Putar perangkat Anda membentuk angka 8 untuk mengkalibrasi sensor kompas.",
                        color = Color.White,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showCalibrationHint = false },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray)
                    ) {
                        Text("Tutup", color = Color.White)
                    }
                }
            }
        }

        // Overlay Box
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val hFovDeg = 60f
            val pxPerDegH = maxWidth.value / hFovDeg
            // Fix Kaaba vertically, only move horizontally
            val kaabaOffsetX = (-headingError * pxPerDegH).dp

            // Center crosshair (Fixed to screen)
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cx = size.width / 2
                val cy = size.height / 2
                drawLine(Color.Red, Offset(cx - 80.dp.toPx(), cy), Offset(cx + 80.dp.toPx(), cy), 1.5.dp.toPx())
                drawLine(Color.Red, Offset(cx, cy - 80.dp.toPx()), Offset(cx, cy + 80.dp.toPx()), 1.5.dp.toPx())
            }

            // Kaaba Icon (Moves horizontally)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .offset(x = kaabaOffsetX)
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val cardinal = when {
                        qiblaAz < 22.5 -> "N"; qiblaAz < 67.5 -> "NE"; qiblaAz < 112.5 -> "E"
                        qiblaAz < 157.5 -> "SE"; qiblaAz < 202.5 -> "S"; qiblaAz < 247.5 -> "SW"
                        qiblaAz < 292.5 -> "W"; qiblaAz < 337.5 -> "NW"; else -> "N"
                    }
                    Text(cardinal, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                    Box(contentAlignment = Alignment.Center) {
                        // Pulsing circle when aligned
                        if (isPointingQibla) {
                            Canvas(
                                modifier = Modifier
                                    .size(90.dp)
                                    .scale(pulseScale)
                                    .alpha(pulseAlpha)
                            ) {
                                drawCircle(
                                    color = Color(0xFF4CAF50),
                                    radius = size.minDimension / 2,
                                    style = Stroke(width = 4.dp.toPx())
                                )
                            }
                        }
                        Image(
                            painter = painterResource(id = R.drawable.ic_kaaba),
                            contentDescription = "Ka'bah",
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }
        }

        // Top Left Info (with semi-transparent background for sunlight readability)
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 12.dp, top = 24.dp)
                .background(Color(0x77000000), shape = RoundedCornerShape(8.dp))
                .padding(8.dp)
        ) {
            // Format coordinate to DMS with direction
            fun fmtCoordLat(v: Double): String {
                val a = abs(v); val d = a.toInt()
                val mf = (a - d) * 60.0; val m = mf.toInt()
                val s = (mf - m) * 60.0
                val dir = if (v >= 0) "LU" else "LS"
                return String.format(Locale.US, "%d° %02d' %04.1f\" %s", d, m, s, dir)
            }
            fun fmtCoordLon(v: Double): String {
                val a = abs(v); val d = a.toInt()
                val mf = (a - d) * 60.0; val m = mf.toInt()
                val s = (mf - m) * 60.0
                val dir = if (v >= 0) "BT" else "BB"
                return String.format(Locale.US, "%d° %02d' %04.1f\" %s", d, m, s, dir)
            }
            // Format azimuth/altitude to DMS (Stellarium style)
            fun fmtDms(v: Double): String {
                val a = abs(v); val d = a.toInt()
                val mf = (a - d) * 60.0; val m = mf.toInt()
                val s = (mf - m) * 60.0
                val sign = if (v < 0) "-" else ""
                return String.format(Locale.US, "%s%d° %02d' %04.1f\"", sign, d, m, s)
            }
            fun fmtAzAlt(az: Double?, alt: Double?): String =
                if (az != null && alt != null) "${fmtDms(az)} | ${fmtDms(alt)}" else "-"

            Row { Text("Lintang", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.width(90.dp)); Text(": ${fmtCoordLat(lat)}", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp) }
            Row { Text("Bujur", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.width(90.dp)); Text(": ${fmtCoordLon(lon)}", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp) }
            Row { Text("Kiblat", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.width(90.dp)); Text(": ${fmtDms(qiblaAz)}", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp) }
            Row { Text("Az. Koreksi", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.width(90.dp)); Text(": ${fmtDms(azimuthCorrection.toDouble())}", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp) }
            Spacer(Modifier.height(12.dp))
            Row { Text("UTC Time", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.width(90.dp)); Text(": $utcTimeString", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp) }
            if (showSunMoon) {
                Row { Text("☀ Az|Alt", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.width(90.dp)); Text(": ${fmtAzAlt(sunAzimuth, sunAltitude)}", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp) }
                Row { Text("Bayangan ☀", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.width(90.dp)); Text(": ${sunAzimuth?.let { fmtDms((it + 180.0) % 360.0) } ?: "-"}", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp) }
                Row { Text("🌙 Az|Alt", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp, modifier=Modifier.width(90.dp)); Text(": ${fmtAzAlt(moonAzimuth, moonAltitude)}", color=Color.White, fontWeight=FontWeight.Bold, fontSize=11.sp) }
            }
        }

        // Top Right Back Button
        IconButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 16.dp, end = 8.dp)
                .background(Color(0x55000000), shape = RoundedCornerShape(50))
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali ke Kompas", tint = Color.White, modifier = Modifier.size(28.dp))
        }

        // Right Pitch Ruler — sliding scale, same style as horizontal compass bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(58.dp)
                .fillMaxHeight(0.55f)
                .background(Color(0x88000000))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val cy = size.height / 2
                val halfRange = 20f  // show ±20° from current pitch
                val pxPerDeg = size.height / (halfRange * 2f)

                val startDeg = (pitch - halfRange).toInt() - 1
                val endDeg   = (pitch + halfRange).toInt() + 1
                var deg = startDeg
                while (deg <= endDeg) {
                    val y = cy - (deg - pitch) * pxPerDeg
                    if (y in 0f..size.height) {
                        when {
                            deg % 10 == 0 -> {
                                // Major tick + label
                                drawLine(
                                    Color.White,
                                    Offset(size.width - 18.dp.toPx(), y),
                                    Offset(size.width, y),
                                    1.5.dp.toPx()
                                )
                                val lbl = "${deg}°"
                                val meas = textMeasurer.measure(
                                    AnnotatedString(lbl),
                                    TextStyle(color = Color.White, fontSize = 11.sp)
                                )
                                drawText(
                                    meas,
                                    topLeft = Offset(
                                        size.width - 18.dp.toPx() - meas.size.width - 3.dp.toPx(),
                                        y - meas.size.height / 2
                                    )
                                )
                            }
                            deg % 5 == 0 -> {
                                // Minor tick only
                                drawLine(
                                    Color.White.copy(alpha = 0.6f),
                                    Offset(size.width - 9.dp.toPx(), y),
                                    Offset(size.width, y),
                                    1.dp.toPx()
                                )
                            }
                        }
                    }
                    deg++
                }
                // Fixed center red marker line
                drawLine(Color.Red, Offset(0f, cy), Offset(size.width, cy), 2.dp.toPx())
            }
            // Current pitch value displayed at top of ruler
            Text(
                text = "${String.format(Locale.US, "%.1f", pitch).replace(".", ",")}°",
                color = Color.Red,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 3.dp)
            )
        }


        // Bottom Controls
        val calibLabel = when (prefs.kiblatKalibrasiObjek) {
            1 -> "Kalibrasi dengan Posisi Matahari"
            2 -> "Kalibrasi dengan Bayangan Matahari"
            3 -> "Kalibrasi dengan Posisi Bulan"
            else -> "Pilih objek kalibrasi di pengaturan"
        }
        val calibTarget = when (prefs.kiblatKalibrasiObjek) {
            1 -> sunAzimuth
            2 -> sunAzimuth?.let { (it + 180.0) % 360.0 }
            3 -> moonAzimuth
            else -> null
        }

        Column(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(bottom = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            if (prefs.kiblatKalibrasiObjek != 0) {
                Text(calibLabel, color = Color.White, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (calibTarget != null) {
                            val correction = ((calibTarget - rawAzimuth).toFloat() + 360f) % 360f
                            val corrFinal = if (correction > 180f) correction - 360f else correction
                            azimuthCorrection = corrFinal
                            prefs.kiblatKoreksiNilai = corrFinal
                            prefs.kiblatKoreksiAzimut = true
                            Toast.makeText(context, "Kalibrasi berhasil", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Data objek belum tersedia", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0)),
                    shape = RoundedCornerShape(4.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp)
                ) {
                    Text("KALIBRASI", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(56.dp)) // Maintain space if no button
            }

            Spacer(Modifier.height(16.dp))
            Text("Azimut", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            // Format azimuth as DMS
            val azD = effectiveAzimuth.toInt()
            val azMf = (effectiveAzimuth - azD) * 60.0
            val azM = azMf.toInt()
            val azS = (azMf - azM) * 60.0
            Text(
                String.format(Locale.US, "%d° %02d' %04.1f\"", azD, azM, azS),
                color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium
            )
            
            // Compass Bar
            Box(modifier = Modifier.fillMaxWidth().height(50.dp).background(Color(0x66000000))) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2
                    val pxPerDeg = size.width / 36f // 18 degrees half range
                    val start = effectiveAzimuth - 18f
                    val end = effectiveAzimuth + 18f
                    var deg = floor(start.toDouble()).toInt()
                    while (deg <= end.toInt() + 1) {
                        val x = cx + (deg - effectiveAzimuth) * pxPerDeg
                        val norm = ((deg % 360) + 360) % 360
                        if (deg % 5 == 0) {
                            drawLine(Color.White, Offset(x, size.height), Offset(x, size.height - 15.dp.toPx()), 1.dp.toPx())
                            val lbl = "${norm}°"
                            val meas = textMeasurer.measure(AnnotatedString(lbl), TextStyle(color = Color.White, fontSize = 12.sp))
                            drawText(meas, topLeft = Offset(x - meas.size.width/2, size.height - 15.dp.toPx() - meas.size.height - 2.dp.toPx()))
                        } else {
                            drawLine(Color.White.copy(alpha = 0.6f), Offset(x, size.height), Offset(x, size.height - 8.dp.toPx()), 1.dp.toPx())
                        }
                        deg++
                    }
                    drawLine(Color.Red, Offset(cx, 0f), Offset(cx, size.height), 2.dp.toPx())
                }
            }
        }

        // Left Icons
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(start = 16.dp, bottom = 24.dp)) {
            IconButton(onClick = {
                azimuthCorrection = 0f
                prefs.kiblatKoreksiNilai = 0f
                prefs.kiblatKoreksiAzimut = false
                Toast.makeText(context, "Reset berhasil", Toast.LENGTH_SHORT).show()
            }) {
                Icon(Icons.Default.Refresh, "Reset", tint = Color.White, modifier = Modifier.size(32.dp))
            }
            Spacer(Modifier.height(16.dp))
            IconButton(onClick = onNavigateToSettings) {
                Icon(Icons.Default.Settings, "Settings", tint = Color.White, modifier = Modifier.size(32.dp))
            }
        }
    }
}

