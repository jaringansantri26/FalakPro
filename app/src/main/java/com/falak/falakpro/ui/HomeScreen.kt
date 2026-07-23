package com.falak.falakpro.ui


import com.falak.falakpro.R
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falak.falakpro.location.LocationData
import com.falak.falakpro.ui.theme.*
import com.falak.falakpro.premium.PreferencesHelper
import com.falak.falakpro.premium.MesinWaktuShalat
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.WaktuShalatSettingsResolver
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    locationData: LocationData?,
    onRefreshLocation: () -> Unit,
    onNavigateToAwalBulan: () -> Unit,
    onNavigateToGerhana: () -> Unit,
    onNavigateToJadwalShalat: () -> Unit,
    onNavigateToKalender: () -> Unit,
    onNavigateToKiblat: () -> Unit,
    onNavigateToDataFalak: () -> Unit,
    onNavigateToScientificCalculator: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { PreferencesHelper(context) }
    var currentTimeMillis by remember { mutableStateOf(System.currentTimeMillis()) }
    
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(1000)
        }
    }

    var locationInputMode by remember(locationData) { mutableStateOf(prefs.locationInputMode) }
    var locationRevision by remember { mutableIntStateOf(0) }
    var showLocationChoiceSheet by remember { mutableStateOf(false) }
    var showCityPickerDialog by remember { mutableStateOf(false) }
    val lokasiOtomatisState = locationInputMode == "GPS"
    val savedManualLat = remember(locationRevision) { prefs.manualLat }
    val savedManualLon = remember(locationRevision) { prefs.manualLon }
    val savedManualElev = remember(locationRevision) { prefs.manualElev }
    val savedManualTimezone = remember(locationRevision) { prefs.manualTimezone }
    val savedManualLocationName = remember(locationRevision) { prefs.manualLokasiNama }
    val lat = if (lokasiOtomatisState) (if (locationData != null && locationData.latitude != 0.0) locationData.latitude else -6.3133) else savedManualLat
    val lon = if (lokasiOtomatisState) (if (locationData != null && locationData.longitude != 0.0) locationData.longitude else 107.3191) else savedManualLon
    val elev = if (lokasiOtomatisState) (if (locationData != null && locationData.altitude != 0.0) locationData.altitude else prefs.ketinggianDataranTinggi) else savedManualElev
    val tz = if (lokasiOtomatisState) timezoneFromLongitude(lon) else savedManualTimezone
    val tzLabel = prayerTimezoneLabel(tz, lon)

    val currentDate = Calendar.getInstance()
    val resolvedSettings = WaktuShalatSettingsResolver.resolve(prefs)
    val kriteria = resolvedSettings.kriteria

    val jadwalToday by produceState<List<MesinWaktuShalat.HasilWaktuShalat>>(
        initialValue = emptyList(),
        currentDate.get(Calendar.DAY_OF_MONTH), lat, lon, elev, tz, prefs.kriteriaIndex, prefs.pengaturanOtomatis, prefs.sudutManualSubuh, prefs.sudutManualIsya, prefs.ikhDzuhur, prefs.ikhAshar, prefs.ikhMaghrib, prefs.ikhIsya, prefs.ikhSubuh, prefs.ikhTerbit, prefs.ikhDhuha, prefs.metodeAsharSyafii, prefs.pembulatanIndex, prefs.is24HourFormat
    ) {
        value = withContext(Dispatchers.Default) {
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

    var nextPrayerName = "-"
    var nextPrayerTime = "--:--"
    var nextPrayerMillis = 0L

    findNextPrayer(
        schedule = jadwalToday,
        prayerDate = currentDate,
        currentTimeMillis = currentTimeMillis,
        uppercaseName = true
    )?.let { next ->
        nextPrayerName = next.name
        nextPrayerTime = next.timeText
        nextPrayerMillis = next.timeMillis
    }

    var jadwalTomorrow by remember { mutableStateOf<List<MesinWaktuShalat.HasilWaktuShalat>?>(null) }

    if (nextPrayerName == "-") {
        val tomorrow = (currentDate.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
        LaunchedEffect(tomorrow.get(Calendar.DAY_OF_MONTH), lat, lon, elev, tz, prefs.kriteriaIndex, prefs.pengaturanOtomatis, prefs.sudutManualSubuh, prefs.sudutManualIsya, prefs.ikhDzuhur, prefs.ikhAshar, prefs.ikhMaghrib, prefs.ikhIsya, prefs.ikhSubuh, prefs.ikhTerbit, prefs.ikhDhuha, prefs.metodeAsharSyafii, prefs.pembulatanIndex, prefs.is24HourFormat) {
            jadwalTomorrow = withContext(Dispatchers.Default) {
                MesinWaktuShalat.hitung(
                    konteks = context,
                    tahun = tomorrow.get(Calendar.YEAR),
                    bulan = tomorrow.get(Calendar.MONTH) + 1,
                    hari = tomorrow.get(Calendar.DAY_OF_MONTH),
                    lintang = lat, bujur = lon, elevasi = elev, zonaWaktu = tz, kriteria = kriteria,
                    ikhSubuh = prefs.ikhSubuh, ikhTerbit = prefs.ikhTerbit,
                    ikhDhuha = prefs.ikhDhuha, ikhDzuhur = prefs.ikhDzuhur, ikhAshar = prefs.ikhAshar,
                    ikhMaghrib = prefs.ikhMaghrib, ikhIsya = prefs.ikhIsya,
                    pembulatan = resolvedSettings.pembulatan, gunakanElevasi = true, faktorAshar = resolvedSettings.faktorAshar,
                    is24HourFormat = prefs.is24HourFormat
                )
            }
        }
        
        jadwalTomorrow?.let { jTomorrow ->
            findNextPrayer(
                schedule = jTomorrow,
                prayerDate = tomorrow,
                currentTimeMillis = currentTimeMillis,
                uppercaseName = true
            )?.let { next ->
                nextPrayerName = next.name
                nextPrayerTime = next.timeText
                nextPrayerMillis = next.timeMillis
            }
        }
    }

    var countdownStr = "00:00:00"
    if (nextPrayerMillis > currentTimeMillis) {
        val diff = nextPrayerMillis - currentTimeMillis
        val s = (diff / 1000) % 60
        val m = (diff / (1000 * 60)) % 60
        val h = (diff / (1000 * 60 * 60))
        countdownStr = String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }

    val dateFormat = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
    val gregorianDateStr = dateFormat.format(currentDate.time)
    
    val currentJd = remember(currentDate.get(Calendar.YEAR), currentDate.get(Calendar.MONTH), currentDate.get(Calendar.DAY_OF_MONTH)) {
        CalendarFunctions.gregorianToJde(
            currentDate.get(Calendar.YEAR),
            currentDate.get(Calendar.MONTH) + 1,
            currentDate.get(Calendar.DAY_OF_MONTH).toDouble()
        )
    }
    
    val hijriCriteria = prefs.kriteriaAwalBulan
    var hijri by remember(currentJd, hijriCriteria) { mutableStateOf<Triple<Int, Int, Int>?>(null) }
    LaunchedEffect(currentJd, hijriCriteria) {
        hijri = runCatching {
            calculateSyncedHijriDate(context, currentJd, hijriCriteria)
        }.getOrNull()
    }
    
    val hijriDateStr = hijri?.let {
        val mName = CalendarFunctions.HIJRI_MONTH_NAMES.getOrNull(it.second - 1) ?: ""
        "${it.third} $mName ${it.first} H"
    } ?: "Memuat Hijriyah"
    val fullDateStr = "$gregorianDateStr / $hijriDateStr"

    val locName = if (lokasiOtomatisState) (if (locationData != null && locationData.address != "Mencari Lokasi...") locationData.address else "Mencari Lokasi...") else savedManualLocationName
    val isLoadingLocation = lokasiOtomatisState && locationData == null

    if (showLocationChoiceSheet) {
        LocationChoiceSheet(
            onDismiss = { showLocationChoiceSheet = false },
            onSearchLocation = {
                showLocationChoiceSheet = false
                showCityPickerDialog = true
            },
            onUseCurrentLocation = {
                showLocationChoiceSheet = false
                locationInputMode = "GPS"
                prefs.locationInputMode = "GPS"
                onRefreshLocation()
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // --- Logo ---
            Box(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo_falakpro),
                    contentDescription = "FalakPro Logo",
                    modifier = Modifier.height(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Location bar ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isLoadingLocation) stringResource(R.string.searching_location) else locName,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1
                        )
                        if (!isLoadingLocation) {
                            Text(
                                text = "\u202A" + formatToDms(lat, true) + ", " + formatToDms(lon, false) + ", ${elev.toInt()} m\u202C",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 9.sp,
                                style = TextStyle(textDirection = TextDirection.Ltr)
                            )
                        }
                    }
                    TextButton(
                        onClick = { showLocationChoiceSheet = true },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.update_location),
                            color = GreenPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // --- Prayer time hero card --- Green NU gradient
            val greenGradient = Brush.linearGradient(
                listOf(Color(0xFF0B6B35), GreenPrimary, Color(0xFF34A85A))
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(brush = greenGradient)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SHALAT BERIKUTNYA",
                            color = Color.White.copy(alpha = 0.75f),
                            fontSize = 8.5.sp,
                            letterSpacing = 1.8.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = nextPrayerName,
                            color = Color(0xFFFFEE88),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Row(
                            modifier = Modifier.clickable { onNavigateToJadwalShalat() },
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = nextPrayerTime,
                                color = Color.White,
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = tzLabel,
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = Color.Black.copy(alpha = 0.20f)
                        ) {
                            Text(
                                text = "\u202A\u23F1  $countdownStr\u202C",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                style = TextStyle(textDirection = TextDirection.Ltr)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    AnalogClock(modifier = Modifier.size(100.dp))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // --- Date bar ---
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = GreenPrimary.copy(alpha = 0.08f),
                border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.25f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.DateRange,
                        contentDescription = null,
                        tint = GreenPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(7.dp))
                    Text(
                        text = fullDateStr,
                        color = GreenPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // --- Menu grid ---
            val menuItems = listOf(
                MenuData(stringResource(R.string.menu_awal_bulan), FalakIcons.HisabColor, onNavigateToAwalBulan),
                MenuData(stringResource(R.string.menu_gerhana), FalakIcons.EclipseColor, onNavigateToGerhana),
                MenuData(stringResource(R.string.menu_jadwal_shalat), FalakIcons.PrayerColor, onNavigateToJadwalShalat),
                MenuData(stringResource(R.string.menu_kalender), FalakIcons.CalendarColor, onNavigateToKalender),
                MenuData(stringResource(R.string.menu_kiblat), FalakIcons.KiblatColor, onNavigateToKiblat),
                MenuData(stringResource(R.string.menu_data_falak), FalakIcons.DataColor, onNavigateToDataFalak),
                MenuData(stringResource(R.string.menu_kalkulator), Icons.Filled.Calculate, onNavigateToScientificCalculator),
                MenuData(stringResource(R.string.menu_pengaturan), FalakIcons.SettingsColor, onNavigateToSettings)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                userScrollEnabled = true
            ) {
                items(menuItems) { menu ->
                    ModernMenuItem(menu)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

fun formatToDms(decimal: Double, isLatitude: Boolean): String {
    val absValue = Math.abs(decimal)
    val degrees = absValue.toInt()
    val minutes = ((absValue - degrees) * 60).toInt()
    val seconds = (((absValue - degrees) * 60 - minutes) * 60).toInt()
    
    val direction = if (isLatitude) {
        if (decimal >= 0) "LU" else "LS"
    } else {
        if (decimal >= 0) "BT" else "BB"
    }
    return String.format(Locale.US, "%02d°%02d'%02d\" %s", degrees, minutes, seconds, direction)
}

@Composable
fun AnalogClock(modifier: Modifier = Modifier) {
    var time by remember { mutableStateOf(Calendar.getInstance()) }
    LaunchedEffect(Unit) {
        while (true) {
            time = Calendar.getInstance()
            delay(1000)
        }
    }

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2

        // Outer ring — white
        drawCircle(color = Color.White, radius = radius, style = Stroke(width = 2.dp.toPx()))

        // Tick marks
        for (i in 0 until 60) {
            val angleDeg = i * 6f - 90f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val isHour = i % 5 == 0
            val lineLength = if (isHour) 10.dp.toPx() else 5.dp.toPx()
            val strokeWidth = if (isHour) 2.dp.toPx() else 1.dp.toPx()
            val color = if (isHour) Color.White else Color.White.copy(alpha = 0.35f)
            val outerPoint = Offset(center.x + radius * cos(angleRad).toFloat(), center.y + radius * sin(angleRad).toFloat())
            val innerPoint = Offset(center.x + (radius - lineLength) * cos(angleRad).toFloat(), center.y + (radius - lineLength) * sin(angleRad).toFloat())
            drawLine(color = color, start = innerPoint, end = outerPoint, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }

        // Center dot — white
        drawCircle(color = Color.White, radius = 3.5.dp.toPx())

        val hours = time.get(Calendar.HOUR)
        val minutes = time.get(Calendar.MINUTE)
        val seconds = time.get(Calendar.SECOND)
        val hourAngle = (hours + minutes / 60f) * 30f - 90f
        val minAngle = minutes * 6f - 90f
        val secAngle = seconds * 6f - 90f

        // Hour hand — white, thick
        drawLine(color = Color.White, start = center, end = Offset(center.x + radius * 0.45f * cos(Math.toRadians(hourAngle.toDouble())).toFloat(), center.y + radius * 0.45f * sin(Math.toRadians(hourAngle.toDouble())).toFloat()), strokeWidth = 4.dp.toPx(), cap = StrokeCap.Round)
        // Minute hand — white, medium
        drawLine(color = Color.White, start = center, end = Offset(center.x + radius * 0.65f * cos(Math.toRadians(minAngle.toDouble())).toFloat(), center.y + radius * 0.65f * sin(Math.toRadians(minAngle.toDouble())).toFloat()), strokeWidth = 2.5.dp.toPx(), cap = StrokeCap.Round)
        // Second hand — gold/yellow
        drawLine(color = Color(0xFFFFEE88), start = center, end = Offset(center.x + radius * 0.8f * cos(Math.toRadians(secAngle.toDouble())).toFloat(), center.y + radius * 0.8f * sin(Math.toRadians(secAngle.toDouble())).toFloat()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun ModernMenuItem(menu: MenuData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { menu.onClick() }
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, GreenPrimary.copy(alpha = 0.25f))
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = menu.icon,
                    contentDescription = menu.title,
                    tint = Color.Unspecified,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(5.dp))
        Text(
            text = menu.title,
            fontSize = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}

data class MenuData(val title: String, val icon: ImageVector, val onClick: () -> Unit)
