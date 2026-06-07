package com.falak.falakpro.ui

import androidx.compose.material3.MaterialTheme
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falak.falakpro.R
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.ElpDataProvider
import com.falak.falakpro.premium.HilalEngine
import com.falak.falakpro.premium.HilalVisibilityMapEngine
import com.falak.falakpro.premium.HilalVisibilityMapMode
import com.falak.falakpro.premium.HilalVisibilityMapResult
import com.falak.falakpro.premium.HilalVisibilityRenderer
import com.falak.falakpro.premium.HilalVisibilityZone
import com.falak.falakpro.premium.VisibilityMapRequest
import com.falak.falakpro.premium.Vsop87SolarEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

private val VisibilityBg = Color(0xFFD8F4F1)
private val VisibilityPrimary = Color(0xFF008F7E)
private val VisibilityText = Color(0xFF304057)
private val DefaultShadeBounds = MapShadeBounds(topBandFraction = 0.16f, bottomBandFraction = 0.14f)
private const val ShadeRowCoverageThreshold = 0.01f

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisibilityMapScreen(
    request: VisibilityMapRequest? = null,
    onNavigateBack: () -> Unit
) {
    if (request != null) {
        ForceLandscape()
    }
    val context = LocalContext.current
    val mapResult by produceState<HilalVisibilityMapResult?>(initialValue = null, request) {
        value = null
        val req = request ?: return@produceState
        withContext(Dispatchers.IO) {
            context.assets.open("mpp02_core.bin").use { ElpDataProvider.initialize(it) }
            context.assets.open("earth_vsop87d.bin").use { Vsop87SolarEngine.initialize(it) }
        }
        value = withContext(Dispatchers.Default) {
            HilalVisibilityMapEngine.buildFastMap(
                ijtimaGeoJde = req.ijtimaGeoJde,
                mode = req.mode,
                latStep = 5.0,
                lonStep = 5.0,
                dayOffset = 0,
                baseDateJdUtOverride = localIjtimaDateMidnightUt(req)
            )
        }
    }

    Scaffold(
        containerColor = Color(0xFF9EDAF0)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Color(0xFF9EDAF0))
        ) {
            val req = request
            if (req == null) {
                MissingVisibilityRequest(onNavigateBack = onNavigateBack)
            } else {
                FullScreenVisibilityMap(request = req, result = mapResult)
            }
            IconButton(
                onClick = onNavigateBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(10.dp)
                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(24.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color(0xFF071226))
            }
        }
    }
}

@Composable
private fun MissingVisibilityRequest(onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Peta visibilitas dibuka dari Hisab Awal Bulan.",
            color = Color(0xFF071226),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onNavigateBack) {
            Text("Kembali ke Hisab Awal Bulan", color = Color(0xFF071226))
        }
    }
}

@Composable
private fun FullScreenVisibilityMap(
    request: VisibilityMapRequest,
    result: HilalVisibilityMapResult?
) {
    val context = LocalContext.current
    val baseMap by produceState<Bitmap?>(initialValue = null) {
        value = withContext(Dispatchers.Default) {
            BitmapFactory.decodeResource(context.resources, R.drawable.peta)
        }
    }
    val overlay by produceState<Bitmap?>(initialValue = null, result) {
        value = null
        value = result?.let { withContext(Dispatchers.Default) { buildOverlayBitmap(it) } }
    }
    val shadeBounds by produceState<MapShadeBounds>(initialValue = DefaultShadeBounds, overlay) {
        value = overlay?.let { withContext(Dispatchers.Default) { detectShadeBounds(it) } } ?: DefaultShadeBounds
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        val mapModifier = if (maxWidth / maxHeight >= 2f) {
            Modifier
                .fillMaxHeight()
                .aspectRatio(2f)
                .align(Alignment.Center)
        } else {
            Modifier
                .fillMaxWidth()
                .aspectRatio(2f)
                .align(Alignment.Center)
        }
        Box(
            modifier = mapModifier
                .background(Color(0xFF9EDAF0))
        ) {
            baseMap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
            overlay?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (result == null) {
                Text(
                    "Menghitung peta...",
                    color = Color(0xFF071226),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                )
            }
            MapHeader(
                request = request,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(shadeBounds.topBandFraction)
            )
            MapLegendOverlay(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(shadeBounds.bottomBandFraction)
            )
        }
    }
}

private data class MapShadeBounds(
    val topBandFraction: Float,
    val bottomBandFraction: Float
)

@Composable
private fun ForceLandscape() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = previous
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun MapHeader(request: VisibilityMapRequest, modifier: Modifier = Modifier) {
    val ijtimaDate = formatMapDate(request.ijtimaLocalJd)
    val magribTime = request.magribLocalTimeText.ifBlank { timezoneLabel(request.timezone) }
    Column(
        modifier = modifier
            .clipToBounds()
            .background(Color(0xF4FFFFFF))
            .padding(start = 58.dp, end = 14.dp, top = 2.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Peta Visibilitas Hilal Awal Bulan ${hijriMonthName(request.hijriMonth)} ${request.hijriYear} H - Kriteria ${request.mode.label}",
            color = Color.Black,
            fontSize = 13.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Saat Magrib: $ijtimaDate, $magribTime",
            color = Color(0xFFB00000),
            fontSize = 10.sp,
            lineHeight = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Ijtimak: $ijtimaDate, ${formatMapTime(request.ijtimaLocalJd, request.timezone)}",
            color = Color(0xFF00006E),
            fontSize = 9.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun MapLegendOverlay(modifier: Modifier = Modifier) {
    val items = listOf(
        AL_HABIB_EASY to "Mudah terlihat mata",
        AL_HABIB_CLEAR to "Jika cuaca cerah",
        AL_HABIB_OPTICAL to "Perlu alat optis",
        AL_HABIB_TELESCOPE to "Melalui teleskop",
        AL_HABIB_GLOBAL_ACCEPTED to "Diterima global (KGHT)",
        AL_HABIB_SPECIAL_GRAY to "Tenggelam/belum ijtimak"
    )
    Row(
        modifier = modifier
            .clipToBounds()
            .background(Color(0xF4FFFFFF))
            .padding(horizontal = 8.dp, vertical = 1.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (color, label) ->
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Box(
                    Modifier
                        .size(7.dp)
                        .background(composeArgb(color), RoundedCornerShape(2.dp))
                        .border(1.dp, Color(0x44000000), RoundedCornerShape(2.dp))
                )
                Text(label, color = Color(0xFF565656), fontSize = 7.sp, lineHeight = 8.sp)
            }
        }
    }
}

private fun detectShadeBounds(bitmap: Bitmap): MapShadeBounds {
    val width = bitmap.width
    val height = bitmap.height
    val pixels = IntArray(width)
    var firstShadeY = height
    var lastShadeY = -1

    for (y in 0 until height) {
        bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
        var shadedPixels = 0
        for (x in 0 until width) {
            if ((pixels[x] ushr 24) != 0) {
                shadedPixels++
            }
        }
        val shaded = shadedPixels.toFloat() >= width * ShadeRowCoverageThreshold
        if (shaded) {
            if (firstShadeY == height) firstShadeY = y
            lastShadeY = y
        }
    }

    if (lastShadeY < firstShadeY) return DefaultShadeBounds
    val top = (firstShadeY.toFloat() / height.toFloat()).coerceIn(0f, 1f)
    val bottom = ((height - lastShadeY - 1).toFloat() / height.toFloat()).coerceIn(0f, 1f)
    return MapShadeBounds(topBandFraction = top, bottomBandFraction = bottom)
}

private fun localIjtimaDateMidnightUt(request: VisibilityMapRequest): Double {
    val offsetHours = if (request.timezone in -12.0..14.0) request.timezone else request.longitude / 15.0
    val ijtimaUt = request.ijtimaLocalJd - offsetHours / 24.0
    val utcDate = floor(ijtimaUt + 0.5) - 0.5
    val localDate = floor(ijtimaUt + offsetHours / 24.0 + 0.5) - 0.5
    return utcDate + (localDate - utcDate).roundToInt()
}

private fun composeArgb(color: Int): Color =
    Color(color.toLong() and 0xffffffffL)

private fun hijriMonthName(month: Int): String =
    CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse((month - 1).coerceIn(0, 11)) { "Hijriah" }

private fun formatMapDate(jdLocal: Double): String {
    val (year, month, dayDouble) = CalendarFunctions.jdeToGregorian(jdLocal)
    val date = LocalDate.of(year, month, floor(dayDouble).toInt().coerceIn(1, 31))
    return "${dayName(date)}, ${date.dayOfMonth} ${gregorianMonthName(date.monthValue)} ${date.year}"
}

private fun formatMapTime(jdLocal: Double, timezone: Double): String {
    val fraction = (jdLocal + 0.5) - floor(jdLocal + 0.5)
    var totalSeconds = (fraction * 86400.0).roundToInt()
    if (totalSeconds >= 86400) totalSeconds -= 86400
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format(java.util.Locale.US, "%02d:%02d:%02d %s", h, m, s, timezoneLabel(timezone))
}

private fun timezoneLabel(timezone: Double): String = when (timezone) {
    7.0 -> "WIB"
    8.0 -> "WITA"
    9.0 -> "WIT"
    else -> "LT"
}

private fun dayName(date: LocalDate): String = when (date.dayOfWeek) {
    java.time.DayOfWeek.SUNDAY -> "Minggu"
    java.time.DayOfWeek.MONDAY -> "Senin"
    java.time.DayOfWeek.TUESDAY -> "Selasa"
    java.time.DayOfWeek.WEDNESDAY -> "Rabu"
    java.time.DayOfWeek.THURSDAY -> "Kamis"
    java.time.DayOfWeek.FRIDAY -> "Jum'at"
    java.time.DayOfWeek.SATURDAY -> "Sabtu"
}

private fun gregorianMonthName(month: Int): String = when (month) {
    1 -> "Januari"
    2 -> "Februari"
    3 -> "Maret"
    4 -> "April"
    5 -> "Mei"
    6 -> "Juni"
    7 -> "Juli"
    8 -> "Agustus"
    9 -> "September"
    10 -> "Oktober"
    11 -> "November"
    12 -> "Desember"
    else -> ""
}

@Composable
private fun MapPreview(result: HilalVisibilityMapResult?) {
    val context = LocalContext.current
    val baseMap by produceState<Bitmap?>(initialValue = null) {
        value = withContext(Dispatchers.Default) {
            BitmapFactory.decodeResource(context.resources, R.drawable.peta)
        }
    }
    val overlay by produceState<Bitmap?>(initialValue = null, result) {
        value = null
        value = result?.let { withContext(Dispatchers.Default) { buildOverlayBitmap(it) } }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF9EDAF0))
    ) {
        baseMap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
        overlay?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}



@Composable
private fun ModeDropdown(mode: HilalVisibilityMapMode, onModeChange: (HilalVisibilityMapMode) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(VisibilityBg)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(mode.label, modifier = Modifier.weight(1f), color = Color(0xFF003F39), fontWeight = FontWeight.Bold, fontSize = 22.sp)
            IconButton(onClick = { expanded = true }) {
                Icon(Icons.Filled.ArrowDropDown, contentDescription = "Pilih kriteria", tint = Color(0xFF003F39))
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            HilalVisibilityMapMode.entries.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item.label) },
                    onClick = {
                        onModeChange(item)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun Legend() {
    val items = listOf(
        HilalVisibilityZone.EASY_NAKED_EYE to "Mudah terlihat dengan mata telanjang",
        HilalVisibilityZone.POSSIBLE_NAKED_EYE to "Terlihat dengan mata telanjang (cuaca cerah)",
        HilalVisibilityZone.OPTICAL_AID_TO_FIND to "Mungkin perlu alat optis untuk menemukan",
        HilalVisibilityZone.OPTICAL_AID to "Perlu alat bantu optis",
        HilalVisibilityZone.TELESCOPE_ONLY to "Tidak terlihat dengan teleskop biasa",
        HilalVisibilityZone.NOT_VISIBLE to "Tidak terlihat - di bawah batas Danjon",
        HilalVisibilityZone.MOON_SET_BEFORE_SUN to "Bulan tenggelam lebih dulu / belum ijtimak"
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        items.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                rowItems.forEach { (zone, label) ->
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            Modifier
                                .size(14.dp)
                                .background(HilalVisibilityRenderer.legendColor(zone), RoundedCornerShape(4.dp))
                                .border(1.dp, Color(0x33000000), RoundedCornerShape(4.dp))
                        )
                        Text(label, color = VisibilityText, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

private fun buildOverlayBitmap(result: HilalVisibilityMapResult): Bitmap {
    val width = 1080
    val height = 540
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val lonSpan = result.lonMax - result.lonMin
    val fullLatMax = 90.0
    val fullLatSpan = 180.0

    val scanLatMin = result.points.minOfOrNull { it.latitude } ?: return bitmap
    val scanLatMax = result.points.maxOfOrNull { it.latitude } ?: return bitmap
    val latCount = ((scanLatMax - scanLatMin) / result.latStep).roundToInt() + 1
    val lonCount = ((result.lonMax - result.lonMin) / result.lonStep).roundToInt() + 1
    val scores = DoubleArray(latCount * lonCount) { TRANSPARENT_SCORE }
    val sunsetAges = DoubleArray(latCount * lonCount) { SAFE_POSITIVE_HOURS }
    val moonLags = DoubleArray(latCount * lonCount) { SAFE_POSITIVE_HOURS }

    result.points.forEach { point ->
        val latIndex = ((point.latitude - scanLatMin) / result.latStep).roundToInt().coerceIn(0, latCount - 1)
        val lonIndex = ((point.longitude - result.lonMin) / result.lonStep).roundToInt().coerceIn(0, lonCount - 1)
        val index = latIndex * lonCount + lonIndex
        sunsetAges[index] = if (point.sunsetAgeHours.isFinite()) point.sunsetAgeHours else point.ageHours
        moonLags[index] = if (point.moonLagHours.isFinite()) point.moonLagHours else SAFE_POSITIVE_HOURS
        scores[index] = when {
            point.zone == HilalVisibilityZone.NO_EVENT -> TRANSPARENT_SCORE
            point.zone == HilalVisibilityZone.GLOBAL_ACCEPTED -> GLOBAL_ACCEPTED_SCORE
            else -> point.score
        }
    }

    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        // Map y ke latitude penuh: atas=90, bawah=-90
        val lat = fullLatMax - (y + 0.5) / height * fullLatSpan
        // Wilayah polar di luar scan - biarkan transparan
        if (lat < scanLatMin || lat > scanLatMax) continue
        val gy = (lat - scanLatMin) / result.latStep
        val y0 = floor(gy).toInt().coerceIn(0, latCount - 1)
        val y1 = min(y0 + 1, latCount - 1)
        val fy = (gy - y0).coerceIn(0.0, 1.0)
        for (x in 0 until width) {
            val lon = result.lonMin + (x + 0.5) / width * lonSpan
            val gx = (lon - result.lonMin) / result.lonStep
            val x0 = floor(gx).toInt().coerceIn(0, lonCount - 1)
            val x1 = min(x0 + 1, lonCount - 1)
            val fx = (gx - x0).coerceIn(0.0, 1.0)
            val i00 = y0 * lonCount + x0
            val i10 = y0 * lonCount + x1
            val i01 = y1 * lonCount + x0
            val i11 = y1 * lonCount + x1

            val moonLag = bilinearValue(
                moonLags[i00],
                moonLags[i10],
                moonLags[i01],
                moonLags[i11],
                fx,
                fy
            )
            val sunsetAge = bilinearValue(
                sunsetAges[i00],
                sunsetAges[i10],
                sunsetAges[i01],
                sunsetAges[i11],
                fx,
                fy
            )
            if (moonLag < 0.0 || sunsetAge < 0.0) {
                pixels[y * width + x] = AL_HABIB_SPECIAL_GRAY
                continue
            }

            val score = bilinearValue(scores[i00], scores[i10], scores[i01], scores[i11], fx, fy)
            pixels[y * width + x] = colorForSample(result.mode, score)
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

private fun bilinearValue(v00: Double, v10: Double, v01: Double, v11: Double, fx: Double, fy: Double): Double {
    val top = v00 + (v10 - v00) * fx
    val bottom = v01 + (v11 - v01) * fx
    return top + (bottom - top) * fy
}

private fun colorForSample(
    mode: HilalVisibilityMapMode,
    score: Double
): Int {
    if (score == GLOBAL_ACCEPTED_SCORE) return AL_HABIB_GLOBAL_ACCEPTED
    return when (mode) {
        HilalVisibilityMapMode.YALLOP -> alHabibYallopColor(score)
        HilalVisibilityMapMode.ODEH -> alHabibOdehColor(score)
        else -> alHabibCriteriaColor(score)
    }
}

private const val AL_HABIB_ALPHA = 153
private const val TRANSPARENT_SCORE = -100.0
private const val GLOBAL_ACCEPTED_SCORE = -99.0   // sentinel: diterima global tapi bukan lokal
private const val SAFE_POSITIVE_HOURS = 24.0
private val AL_HABIB_EASY = argb(AL_HABIB_ALPHA, 131, 199, 2)
private val AL_HABIB_CLEAR = argb(AL_HABIB_ALPHA, 112, 154, 8)
private val AL_HABIB_OPTICAL_INITIAL = argb(AL_HABIB_ALPHA, 65, 97, 0)
private val AL_HABIB_OPTICAL = argb(AL_HABIB_ALPHA, 255, 215, 0)
private val AL_HABIB_TELESCOPE = argb(AL_HABIB_ALPHA, 255, 165, 0)
private val AL_HABIB_GLOBAL_ACCEPTED = argb(120, 144, 238, 144)  // hijau muda semi-transparan
private val AL_HABIB_SPECIAL_GRAY = argb(AL_HABIB_ALPHA, 98, 109, 114)
private val AL_HABIB_TRANSPARENT = argb(0, 0, 0, 0)

private fun alHabibYallopColor(q: Double): Int = when {
    q > 0.216 -> AL_HABIB_EASY
    q > -0.014 -> AL_HABIB_CLEAR
    q > -0.160 -> AL_HABIB_OPTICAL_INITIAL
    q > -0.232 -> AL_HABIB_OPTICAL
    q > -0.293 -> AL_HABIB_TELESCOPE
    else -> AL_HABIB_TRANSPARENT
}

private fun alHabibOdehColor(v: Double): Int = when {
    v >= 5.65 -> AL_HABIB_EASY
    v >= 2.0 -> AL_HABIB_OPTICAL_INITIAL
    v >= -0.96 -> AL_HABIB_TELESCOPE
    else -> AL_HABIB_TRANSPARENT
}

private fun alHabibCriteriaColor(score: Double): Int = when {
    score >= 8.0 -> AL_HABIB_EASY
    score >= 3.0 -> AL_HABIB_CLEAR
    score >= 0.0 -> AL_HABIB_OPTICAL_INITIAL
    else -> AL_HABIB_TRANSPARENT
}

private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
    return (alpha.coerceIn(0, 255) shl 24) or
        (red.coerceIn(0, 255) shl 16) or
        (green.coerceIn(0, 255) shl 8) or
        blue.coerceIn(0, 255)
}

private fun defaultVisibilityTarget(): Pair<Int, Int> {
    val now = LocalDate.now()
    val jde = CalendarFunctions.gregorianToJde(now.year, now.monthValue, now.dayOfMonth.toDouble())
    val (hYear, hMonth, hDay) = CalendarFunctions.jdeToHijri(jde)
    return if (hDay <= 10) {
        hYear to hMonth
    } else if (hMonth == 12) {
        hYear + 1 to 1
    } else {
        hYear to hMonth + 1
    }
}

