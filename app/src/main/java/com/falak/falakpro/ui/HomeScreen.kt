package com.falak.falakpro.ui

import com.falak.falakpro.R
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

    var lokasiOtomatisState by remember(locationData) { mutableStateOf(prefs.lokasiOtomatis) }
    val lat = if (lokasiOtomatisState) (if (locationData != null && locationData.latitude != 0.0) locationData.latitude else -6.3133) else prefs.manualLat
    val lon = if (lokasiOtomatisState) (if (locationData != null && locationData.longitude != 0.0) locationData.longitude else 107.3191) else prefs.manualLon
    val elev = if (lokasiOtomatisState) (if (locationData != null && locationData.altitude != 0.0) locationData.altitude else prefs.ketinggianDataranTinggi) else prefs.manualElev
    val tz = if (lokasiOtomatisState) timezoneFromLongitude(lon) else prefs.manualTimezone
    val tzLabel = prayerTimezoneLabel(tz, lon)

    val currentDate = Calendar.getInstance()
    val kriteria = if (prefs.pengaturanOtomatis) {
        MesinWaktuShalat.DAFTAR_KRITERIA[1]
    } else if (prefs.kriteriaIndex == 0) {
        MesinWaktuShalat.KriteriaWaktuShalat(
            "Sesuaikan Sudut Manual",
            prefs.sudutManualSubuh.toDouble(),
            prefs.sudutManualIsya.toDouble(),
            1.0,
            4.5
        )
    } else {
        MesinWaktuShalat.DAFTAR_KRITERIA.getOrElse(prefs.kriteriaIndex) { MesinWaktuShalat.KRITERIA_LFNU }
    }
    
    val asharFactor = if (prefs.metodeAsharSyafii) 1.0 else 2.0

    val modePembulatan = when (prefs.pembulatanIndex) {
        1 -> MesinWaktuShalat.ModePembulatan.KE_ATAS
        2 -> MesinWaktuShalat.ModePembulatan.KE_BAWAH
        else -> MesinWaktuShalat.ModePembulatan.NORMAL
    }

    val jadwalToday by produceState<List<MesinWaktuShalat.HasilWaktuShalat>>(
        initialValue = emptyList(),
        currentDate.get(Calendar.DAY_OF_MONTH), lat, lon, elev, tz, prefs.kriteriaIndex, prefs.pengaturanOtomatis, prefs.sudutManualSubuh, prefs.sudutManualIsya, prefs.ikhDzuhur, prefs.ikhAshar, prefs.ikhMaghrib, prefs.ikhIsya, prefs.ikhSubuh, prefs.ikhImsak, prefs.ikhTerbit, prefs.ikhDhuha, prefs.metodeAsharSyafii, prefs.pembulatanIndex, prefs.is24HourFormat
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
                ikhImsak = prefs.ikhImsak,
                ikhSubuh = prefs.ikhSubuh,
                ikhTerbit = prefs.ikhTerbit,
                ikhDhuha = prefs.ikhDhuha,
                ikhDzuhur = prefs.ikhDzuhur,
                ikhAshar = prefs.ikhAshar,
                ikhMaghrib = prefs.ikhMaghrib,
                ikhIsya = prefs.ikhIsya,
                pembulatan = modePembulatan,
                gunakanElevasi = true,
                faktorAshar = asharFactor,
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
        LaunchedEffect(tomorrow.get(Calendar.DAY_OF_MONTH), lat, lon, elev, tz, kriteria, prefs.is24HourFormat) {
            jadwalTomorrow = withContext(Dispatchers.Default) {
                MesinWaktuShalat.hitung(
                    konteks = context,
                    tahun = tomorrow.get(Calendar.YEAR),
                    bulan = tomorrow.get(Calendar.MONTH) + 1,
                    hari = tomorrow.get(Calendar.DAY_OF_MONTH),
                    lintang = lat, bujur = lon, elevasi = elev, zonaWaktu = tz, kriteria = kriteria,
                    ikhImsak = prefs.ikhImsak, ikhSubuh = prefs.ikhSubuh, ikhTerbit = prefs.ikhTerbit,
                    ikhDhuha = prefs.ikhDhuha, ikhDzuhur = prefs.ikhDzuhur, ikhAshar = prefs.ikhAshar,
                    ikhMaghrib = prefs.ikhMaghrib, ikhIsya = prefs.ikhIsya,
                    pembulatan = modePembulatan, gunakanElevasi = true, faktorAshar = asharFactor,
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
    
    var hijri by remember { mutableStateOf(Triple(1, 1, 1)) }
    LaunchedEffect(currentJd, lat, lon, tz) {
        val h = withContext(Dispatchers.Default) {
            try {
                CalendarFunctions.getCorrectedHijri(currentJd, lat, lon, elev, tz)
            } catch (e: Exception) {
                CalendarFunctions.jdeToHijri(currentJd)
            }
        }
        hijri = h
    }
    
    val mName = CalendarFunctions.HIJRI_MONTH_NAMES.getOrNull(hijri.second - 1) ?: ""
    val hijriDateStr = "${hijri.third} $mName ${hijri.first} H"
    val fullDateStr = "$gregorianDateStr / $hijriDateStr"

    val locName = if (lokasiOtomatisState) (if (locationData != null && locationData.address != "Mencari Lokasi...") locationData.address else "Mencari Lokasi...") else prefs.manualLokasiNama
    val isLoadingLocation = lokasiOtomatisState && locationData == null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GreenPrimary),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(30.dp))

        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 12.dp, end = 12.dp, bottom = 1.dp),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 15.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(15.dp))

                Box(
                    modifier = Modifier.fillMaxWidth().height(65.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.logo_falakpro),
                        contentDescription = "FalakPro Logo",
                        modifier = Modifier.requiredSize(190.dp)
                    )
                }

                Spacer(modifier = Modifier.height(5.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { 
                        lokasiOtomatisState = true
                        onRefreshLocation() 
                    }
                ) {
                    Icon(Icons.Filled.LocationOn, contentDescription = null, tint = GreenPrimary, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isLoadingLocation) stringResource(R.string.searching_location) else "$locName ${stringResource(R.string.update_location)}",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                Text(
                    text = if (isLoadingLocation) "---" else "\u202A" + formatToDms(lat, true) + ", " + formatToDms(lon, false) + ", ${elev.toInt()} m" + "\u202C",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(top = 2.dp),
                    style = TextStyle(textDirection = TextDirection.Ltr)
                )

                Column(
                    modifier = Modifier.padding(top = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = nextPrayerName,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp
                    )

                    Row(
                        modifier = Modifier.clickable { onNavigateToJadwalShalat() },
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(text = nextPrayerTime, fontSize = 20.sp, fontWeight = FontWeight.Black, color = GreenPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = tzLabel, fontSize = 20.sp, fontWeight = FontWeight.Black, color = GreenPrimary)
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Text(
                        text = "\u202A- $countdownStr\u202C", 
                        color = RedAccent, 
                        fontSize = 12.sp, 
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(textDirection = TextDirection.Ltr)
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    AnalogClock(modifier = Modifier.size(160.dp))
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = fullDateStr,
                    color = GreenPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

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
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    userScrollEnabled = true
                ) {
                    items(menuItems) { menu ->
                        CircularMenuItem(menu)
                    }
                }
            }
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
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2, size.height / 2)
        val radius = size.minDimension / 2
        
        drawCircle(color = GreenPrimary, radius = radius, style = Stroke(width = 3.dp.toPx()))

        for (i in 0 until 60) {
            val angleDeg = i * 6f - 90f
            val angleRad = Math.toRadians(angleDeg.toDouble())
            val isHour = i % 5 == 0
            val lineLength = if (isHour) 12.dp.toPx() else 6.dp.toPx()
            val strokeWidth = if (isHour) 2.dp.toPx() else 1.dp.toPx()
            val color = if (isHour) GreenPrimary else onSurfaceVariantColor.copy(alpha = 0.5f)
            val outerPoint = Offset(center.x + radius * cos(angleRad).toFloat(), center.y + radius * sin(angleRad).toFloat())
            val innerPoint = Offset(center.x + (radius - lineLength) * cos(angleRad).toFloat(), center.y + (radius - lineLength) * sin(angleRad).toFloat())
            drawLine(color = color, start = innerPoint, end = outerPoint, strokeWidth = strokeWidth, cap = StrokeCap.Round)
        }
        
        drawCircle(color = GreenPrimary, radius = 3.dp.toPx())

        val hours = time.get(Calendar.HOUR)
        val minutes = time.get(Calendar.MINUTE)
        val seconds = time.get(Calendar.SECOND)
        val hourAngle = (hours + minutes / 60f) * 30f - 90f
        val minAngle = minutes * 6f - 90f
        val secAngle = seconds * 6f - 90f

        drawLine(color = onSurfaceColor, start = center, end = Offset(center.x + radius * 0.45f * cos(Math.toRadians(hourAngle.toDouble())).toFloat(), center.y + radius * 0.45f * sin(Math.toRadians(hourAngle.toDouble())).toFloat()), strokeWidth = 5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = onSurfaceColor, start = center, end = Offset(center.x + radius * 0.7f * cos(Math.toRadians(minAngle.toDouble())).toFloat(), center.y + radius * 0.7f * sin(Math.toRadians(minAngle.toDouble())).toFloat()), strokeWidth = 3.5.dp.toPx(), cap = StrokeCap.Round)
        drawLine(color = Color.Red, start = center, end = Offset(center.x + radius * 0.85f * cos(Math.toRadians(secAngle.toDouble())).toFloat(), center.y + radius * 0.85f * sin(Math.toRadians(secAngle.toDouble())).toFloat()), strokeWidth = 1.5.dp.toPx(), cap = StrokeCap.Round)
    }
}

@Composable
fun CircularMenuItem(menu: MenuData) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().clickable { menu.onClick() }
    ) {
        Box(
            modifier = Modifier.size(52.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = menu.icon, 
                contentDescription = menu.title, 
                tint = Color.Unspecified,
                modifier = Modifier.size(32.dp)
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = menu.title, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
    }
}

data class MenuData(val title: String, val icon: ImageVector, val onClick: () -> Unit)


