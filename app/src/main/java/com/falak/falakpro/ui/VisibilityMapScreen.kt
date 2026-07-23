package com.falak.falakpro.ui

import androidx.compose.material3.MaterialTheme
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Canvas
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Download
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
import com.falak.falakpro.premium.AstroAssetPreloader
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.DynamicalTimeEngine
import com.falak.falakpro.premium.ElpDataProvider
import com.falak.falakpro.premium.HilalEngine
import com.falak.falakpro.premium.HilalVisibilityMapEngine
import com.falak.falakpro.premium.HilalVisibilityMapMode
import com.falak.falakpro.premium.HilalVisibilityPoint
import com.falak.falakpro.premium.HilalVisibilityMapResult
import com.falak.falakpro.premium.HilalVisibilityRasterMapResult
import com.falak.falakpro.premium.HilalVisibilityRenderer
import com.falak.falakpro.premium.HilalVisibilityZone
import com.falak.falakpro.premium.VisibilityMapRequest
import com.falak.falakpro.premium.Vsop87SolarEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.util.LinkedHashMap
import java.util.Locale
import java.time.LocalDate
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

private val VisibilityBg = Color(0xFFD8F4F1)
private val VisibilityText = Color(0xFF304057)

private data class MapShadeBounds(
    val topBandFraction: Float,
    val bottomBandFraction: Float
)
private val DefaultShadeBounds = MapShadeBounds(topBandFraction = 0.166f, bottomBandFraction = 0.166f)
private const val ShadeRowCoverageThreshold = 0.01f
private const val VISIBILITY_MAP_LAT_STEP = 4.0
private const val VISIBILITY_MAP_LON_STEP = 4.0
private const val OVERLAY_BITMAP_WIDTH = 960
private const val OVERLAY_BITMAP_HEIGHT = 480
private const val OVERLAY_BITMAP_CACHE_SIZE = 8

private var cachedVisibilityBaseMap: Bitmap? = null
private val overlayBitmapCache = object : LinkedHashMap<String, Bitmap>(OVERLAY_BITMAP_CACHE_SIZE, 0.75f, true) {
    override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean {
        return size > OVERLAY_BITMAP_CACHE_SIZE
    }
}

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
    val mapResult by produceState<HilalVisibilityRasterMapResult?>(initialValue = null, request) {
        value = null
        val req = request ?: return@produceState
        value = withContext(Dispatchers.Default) {
            readRasterMapDiskCache(context, req)?.let { return@withContext it }
            AstroAssetPreloader.ensureCore(context)
            val computed = HilalVisibilityMapEngine.buildFastRasterMap(
                ijtimaGeoJde = req.ijtimaGeoJde,
                mode = req.mode,
                latStep = VISIBILITY_MAP_LAT_STEP,
                lonStep = VISIBILITY_MAP_LON_STEP,
                dayOffset = 0,
                baseDateJdUtOverride = localIjtimaDateMidnightUt(req)
            )
            writeRasterMapDiskCache(context, computed)
            computed
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
    result: HilalVisibilityRasterMapResult?
) {
    val context = LocalContext.current
    val baseMap by produceState<Bitmap?>(initialValue = null) {
        value = withContext(Dispatchers.Default) {
            cachedVisibilityBaseMap ?: BitmapFactory.decodeResource(context.resources, R.drawable.peta)
                .also { cachedVisibilityBaseMap = it }
        }
    }
    val overlay by produceState<Bitmap?>(initialValue = null, result) {
        value = null
        value = result?.let { withContext(Dispatchers.Default) { getOrBuildOverlayBitmap(context, it) } }
    }
    val shadeBounds = DefaultShadeBounds

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
                mode = request.mode,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .fillMaxHeight(shadeBounds.bottomBandFraction)
            )
            IconButton(
                onClick = {
                    val base = baseMap
                    val over = overlay
                    if (base != null && over != null && result != null) {
                        val saved = saveVisibilityMapImage(context, request, result, base, over)
                        Toast.makeText(
                            context,
                            if (saved) "Gambar peta disimpan di Galeri/FalakPro" else "Gagal menyimpan gambar peta",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                },
                enabled = baseMap != null && overlay != null && result != null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(10.dp)
                    .background(Color.White.copy(alpha = 0.82f), RoundedCornerShape(24.dp))
            ) {
                Icon(Icons.Filled.Download, contentDescription = "Unduh gambar peta", tint = Color(0xFF071226))
            }
        }
    }
}


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
private fun MapLegendOverlay(mode: HilalVisibilityMapMode, modifier: Modifier = Modifier) {
    val items = mutableListOf<Pair<Int, String>>()
    if (mode == HilalVisibilityMapMode.YALLOP) {
        items.add(AL_HABIB_EASY to "Mudah terlihat mata")
        items.add(AL_HABIB_CLEAR to "Jika cuaca cerah")
        items.add(AL_HABIB_OPTICAL_INITIAL to "Mungkin perlu alat optis")
        items.add(AL_HABIB_OPTICAL to "Perlu alat optis")
        items.add(AL_HABIB_TELESCOPE to "Hanya melalui teleskop")
        items.add(AL_HABIB_TRANSPARENT_LEGEND to "Tidak terlihat (Bawah Danjon)")
    } else if (mode == HilalVisibilityMapMode.ODEH) {
        items.add(AL_HABIB_EASY to "Mudah terlihat oleh mata")
        items.add(AL_HABIB_CLEAR to "Mungkin perlu bantuan alat optis")
        items.add(AL_HABIB_OPTICAL to "Hanya terlihat dengan alat optis")
        items.add(AL_HABIB_TRANSPARENT_LEGEND to "Bulan di atas ufuk, tidak kasat mata")
    } else {
        items.add(AL_HABIB_EASY to metCriteriaLegend(mode))
        items.add(AL_HABIB_TRANSPARENT_LEGEND to unmetCriteriaLegend(mode))
    }
    items.add(AL_HABIB_SPECIAL_GRAY to "Tenggelam/belum ijtimak")
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

    for (y in 0 until height step 2) {
        bitmap.getPixels(pixels, 0, width, 0, y, width, 1)
        var shadedPixels = 0
        for (x in 0 until width step 2) {
            if ((pixels[x] ushr 24) != 0) {
                shadedPixels++
            }
        }
        val sampledWidth = (width + 1) / 2
        val shaded = shadedPixels.toFloat() >= sampledWidth * ShadeRowCoverageThreshold
        if (shaded) {
            if (firstShadeY == height) firstShadeY = y
            lastShadeY = y
        }
    }

    if (lastShadeY < firstShadeY) return DefaultShadeBounds
    val topBandFraction = (firstShadeY.toFloat() / height).coerceIn(0.08f, 0.4f)
    val bottomBandFraction = (1f - (lastShadeY.toFloat() + 1f) / height).coerceIn(0.08f, 0.4f)
    return MapShadeBounds(topBandFraction, bottomBandFraction)
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
private fun MapPreview(result: HilalVisibilityRasterMapResult?) {
    val context = LocalContext.current
    val baseMap by produceState<Bitmap?>(initialValue = null) {
        value = withContext(Dispatchers.Default) {
            BitmapFactory.decodeResource(context.resources, R.drawable.peta)
        }
    }
    val overlay by produceState<Bitmap?>(initialValue = null, result) {
        value = null
        value = result?.let { withContext(Dispatchers.Default) { getOrBuildOverlayBitmap(context, it) } }
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

private fun getOrBuildOverlayBitmap(context: Context, result: HilalVisibilityRasterMapResult): Bitmap {
    synchronized(overlayBitmapCache) {
        overlayBitmapCache[result.cacheKey]?.let { return it }
    }
    readOverlayBitmapDiskCache(context, result.cacheKey)?.let { cached ->
        synchronized(overlayBitmapCache) {
            overlayBitmapCache[result.cacheKey] = cached
        }
        return cached
    }
    val bitmap = buildOverlayBitmap(result)
    writeOverlayBitmapDiskCache(context, result.cacheKey, bitmap)
    synchronized(overlayBitmapCache) {
        overlayBitmapCache[result.cacheKey] = bitmap
    }
    return bitmap
}

private fun visibilityCacheDir(context: Context): File =
    File(context.cacheDir, "visibility_maps").also { it.mkdirs() }

private fun rasterDiskCacheFile(context: Context, request: VisibilityMapRequest): File =
    File(visibilityCacheDir(context), "raster_${visibilityRasterCacheKey(request).hashCode()}.bin")

private fun overlayDiskCacheFile(context: Context, cacheKey: String): File =
    File(visibilityCacheDir(context), "overlay_${cacheKey.hashCode()}.png")

private fun visibilityRasterCacheKey(request: VisibilityMapRequest): String {
    val dtIjtima = DynamicalTimeEngine.deltaT(request.ijtimaGeoJde)
    val jdUtIjtima = request.ijtimaGeoJde - dtIjtima / 86400.0
    val defaultBaseDateJdUt = floor(jdUtIjtima + 0.5) - 0.5
    val baseDateJdUt = localIjtimaDateMidnightUt(request)
    return String.format(
        Locale.US,
        "RASTER_V7:%s:%.5f:%.5f:%.2f:%.2f:%d",
        request.mode.name,
        request.ijtimaGeoJde,
        baseDateJdUt.takeIf { it.isFinite() } ?: defaultBaseDateJdUt,
        VISIBILITY_MAP_LAT_STEP,
        VISIBILITY_MAP_LON_STEP,
        0
    )
}

private fun readRasterMapDiskCache(context: Context, request: VisibilityMapRequest): HilalVisibilityRasterMapResult? {
    val file = rasterDiskCacheFile(context, request)
    if (!file.exists() || file.length() <= 0L) return null
    return try {
        DataInputStream(BufferedInputStream(file.inputStream())).use { input ->
            if (input.readInt() != 0x46564D31) return null
            val cacheKey = input.readUTF()
            if (cacheKey != visibilityRasterCacheKey(request)) return null
            val mode = HilalVisibilityMapMode.valueOf(input.readUTF())
            val latMin = input.readDouble()
            val latMax = input.readDouble()
            val lonMin = input.readDouble()
            val lonMax = input.readDouble()
            val scanLatMin = input.readDouble()
            val scanLatMax = input.readDouble()
            val latStep = input.readDouble()
            val lonStep = input.readDouble()
            val latCount = input.readInt()
            val lonCount = input.readInt()
            val dayOffset = input.readInt()
            val baseDateJdUt = input.readDouble()
            val hasBest = input.readBoolean()
            val bestPoint = if (hasBest) {
                HilalVisibilityPoint(
                    latitude = input.readDouble(),
                    longitude = input.readDouble(),
                    zone = HilalVisibilityZone.valueOf(input.readUTF()),
                    score = input.readDouble(),
                    arcV = input.readDouble(),
                    arcL = input.readDouble(),
                    daz = input.readDouble(),
                    ageHours = input.readDouble(),
                    crescentWidthArcMin = input.readDouble(),
                    moonAltTopo = input.readDouble(),
                    moonAltGeo = input.readDouble(),
                    moonLagHours = input.readDouble(),
                    sunsetAgeHours = input.readDouble()
                )
            } else null
            val size = latCount * lonCount
            val scores = FloatArray(size) { input.readFloat() }
            val grayMargins = FloatArray(size) { input.readFloat() }
            val noEventMasks = ByteArray(size)
            input.readFully(noEventMasks)
            HilalVisibilityRasterMapResult(
                cacheKey = cacheKey,
                mode = mode,
                scores = scores,
                grayMargins = grayMargins,
                noEventMasks = noEventMasks,
                bestPoint = bestPoint,
                latMin = latMin,
                latMax = latMax,
                lonMin = lonMin,
                lonMax = lonMax,
                scanLatMin = scanLatMin,
                scanLatMax = scanLatMax,
                latStep = latStep,
                lonStep = lonStep,
                latCount = latCount,
                lonCount = lonCount,
                dayOffset = dayOffset,
                baseDateJdUt = baseDateJdUt
            )
        }
    } catch (_: Exception) {
        file.delete()
        null
    }
}

private fun writeRasterMapDiskCache(context: Context, result: HilalVisibilityRasterMapResult) {
    val file = File(visibilityCacheDir(context), "raster_${result.cacheKey.hashCode()}.bin")
    try {
        DataOutputStream(BufferedOutputStream(file.outputStream())).use { output ->
            output.writeInt(0x46564D31)
            output.writeUTF(result.cacheKey)
            output.writeUTF(result.mode.name)
            output.writeDouble(result.latMin)
            output.writeDouble(result.latMax)
            output.writeDouble(result.lonMin)
            output.writeDouble(result.lonMax)
            output.writeDouble(result.scanLatMin)
            output.writeDouble(result.scanLatMax)
            output.writeDouble(result.latStep)
            output.writeDouble(result.lonStep)
            output.writeInt(result.latCount)
            output.writeInt(result.lonCount)
            output.writeInt(result.dayOffset)
            output.writeDouble(result.baseDateJdUt)
            val best = result.bestPoint
            output.writeBoolean(best != null)
            if (best != null) {
                output.writeDouble(best.latitude)
                output.writeDouble(best.longitude)
                output.writeUTF(best.zone.name)
                output.writeDouble(best.score)
                output.writeDouble(best.arcV)
                output.writeDouble(best.arcL)
                output.writeDouble(best.daz)
                output.writeDouble(best.ageHours)
                output.writeDouble(best.crescentWidthArcMin)
                output.writeDouble(best.moonAltTopo)
                output.writeDouble(best.moonAltGeo)
                output.writeDouble(best.moonLagHours)
                output.writeDouble(best.sunsetAgeHours)
            }
            result.scores.forEach { output.writeFloat(it) }
            result.grayMargins.forEach { output.writeFloat(it) }
            output.write(result.noEventMasks)
        }
    } catch (_: Exception) {
        file.delete()
    }
}

private fun readOverlayBitmapDiskCache(context: Context, cacheKey: String): Bitmap? {
    val file = overlayDiskCacheFile(context, cacheKey)
    if (!file.exists() || file.length() <= 0L) return null
    return BitmapFactory.decodeFile(file.absolutePath)
}

private fun writeOverlayBitmapDiskCache(context: Context, cacheKey: String, bitmap: Bitmap) {
    val file = overlayDiskCacheFile(context, cacheKey)
    try {
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    } catch (_: Exception) {
        file.delete()
    }
}

private fun saveVisibilityMapImage(
    context: Context,
    request: VisibilityMapRequest,
    result: HilalVisibilityRasterMapResult,
    baseMap: Bitmap,
    overlay: Bitmap
): Boolean {
    return try {
        val bitmap = buildPrintableVisibilityBitmap(request, result, baseMap, overlay)
        val name = "Peta_Visibilitas_${request.hijriYear}_${request.hijriMonth}_${request.mode.name}_${System.currentTimeMillis()}.png"
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, name)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/FalakPro")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        bitmap.recycle()
        true
    } catch (_: Exception) {
        false
    }
}

private fun buildPrintableVisibilityBitmap(
    request: VisibilityMapRequest,
    result: HilalVisibilityRasterMapResult,
    baseMap: Bitmap,
    overlay: Bitmap
): Bitmap {
    val width = OVERLAY_BITMAP_WIDTH
    val mapHeight = OVERLAY_BITMAP_HEIGHT
    val headerHeight = 96
    val legendHeight = 78
    val output = Bitmap.createBitmap(width, headerHeight + mapHeight + legendHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(output)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    canvas.drawColor(android.graphics.Color.WHITE)
    paint.color = android.graphics.Color.rgb(245, 248, 250)
    canvas.drawRect(0f, 0f, width.toFloat(), headerHeight.toFloat(), paint)

    paint.color = android.graphics.Color.rgb(10, 31, 46)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.textSize = 30f
    canvas.drawText("Peta Visibilitas Hilal ${hijriMonthName(request.hijriMonth)} ${request.hijriYear} H", 28f, 38f, paint)
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 20f
    canvas.drawText("Kriteria ${request.mode.label} | ${formatMapDate(request.ijtimaLocalJd)} | Magrib ${request.magribLocalTimeText.ifBlank { timezoneLabel(request.timezone) }}", 28f, 70f, paint)

    canvas.drawBitmap(baseMap, null, RectF(0f, headerHeight.toFloat(), width.toFloat(), (headerHeight + mapHeight).toFloat()), paint)
    canvas.drawBitmap(overlay, null, RectF(0f, headerHeight.toFloat(), width.toFloat(), (headerHeight + mapHeight).toFloat()), paint)

    paint.color = android.graphics.Color.rgb(245, 248, 250)
    canvas.drawRect(0f, (headerHeight + mapHeight).toFloat(), width.toFloat(), output.height.toFloat(), paint)
    paint.textSize = 18f
    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    paint.color = android.graphics.Color.rgb(10, 31, 46)
    canvas.drawText(result.bestPoint?.let {
        String.format(Locale.US, "Titik terbaik: %.1f, %.1f | ARCV %.2f deg | ARCL %.2f deg | umur %.1f jam", it.latitude, it.longitude, it.arcV, it.arcL, it.ageHours)
    } ?: "Titik terbaik belum tersedia", 28f, (headerHeight + mapHeight + 28).toFloat(), paint)

    paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    paint.textSize = 16f
    val legend = legendTextForPrint(request.mode)
    canvas.drawText(legend, 28f, (headerHeight + mapHeight + 58).toFloat(), paint)
    return output
}

private fun legendTextForPrint(mode: HilalVisibilityMapMode): String = when (mode) {
    HilalVisibilityMapMode.YALLOP -> "Hijau: mudah terlihat, kuning/oranye: perlu kondisi/alat, merah: teleskop, abu-abu: belum ijtimak/bulan tenggelam."
    HilalVisibilityMapMode.ODEH -> "Hijau: mudah terlihat, kuning/oranye: perlu alat optis, transparan: belum kasat mata, abu-abu: belum ijtimak/bulan tenggelam."
    else -> "Hijau: memenuhi kriteria ${mode.label}, transparan: belum memenuhi, abu-abu: belum ijtimak/bulan tenggelam."
}

private fun buildOverlayBitmap(result: HilalVisibilityRasterMapResult): Bitmap {
    val width = OVERLAY_BITMAP_WIDTH
    val height = OVERLAY_BITMAP_HEIGHT
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    val lonSpan = result.lonMax - result.lonMin
    val fullLatMax = 90.0
    val fullLatSpan = 180.0
    val x0s = IntArray(width)
    val x1s = IntArray(width)
    val fxs = DoubleArray(width)
    for (x in 0 until width) {
        val lon = result.lonMin + (x + 0.5) / width * lonSpan
        val gx = (lon - result.lonMin) / result.lonStep
        val x0 = floor(gx).toInt().coerceIn(0, result.lonCount - 1)
        x0s[x] = x0
        x1s[x] = min(x0 + 1, result.lonCount - 1)
        fxs[x] = (gx - x0).coerceIn(0.0, 1.0)
    }

    for (y in 0 until height) {
        val lat = fullLatMax - (y + 0.5) / height * fullLatSpan
        if (lat < result.scanLatMin || lat > result.scanLatMax) continue
        val gy = (lat - result.scanLatMin) / result.latStep
        val y0 = floor(gy).toInt().coerceIn(0, result.latCount - 1)
        val y1 = min(y0 + 1, result.latCount - 1)
        val fy = (gy - y0).coerceIn(0.0, 1.0)
        val row = y * width
        val row0 = y0 * result.lonCount
        val row1 = y1 * result.lonCount

        for (x in 0 until width) {
            val x0 = x0s[x]
            val x1 = x1s[x]
            val fx = fxs[x]

            val i00 = row0 + x0
            val i10 = row0 + x1
            val i01 = row1 + x0
            val i11 = row1 + x1

            val noEventMask = bilinearValue(
                result.noEventMasks[i00].toDouble(),
                result.noEventMasks[i10].toDouble(),
                result.noEventMasks[i01].toDouble(),
                result.noEventMasks[i11].toDouble(),
                fx,
                fy
            )
            val grayMargin = bilinearValue(
                result.grayMargins[i00].toDouble(),
                result.grayMargins[i10].toDouble(),
                result.grayMargins[i01].toDouble(),
                result.grayMargins[i11].toDouble(),
                fx,
                fy
            )
            val score = bilinearValue(
                result.scores[i00].toDouble(),
                result.scores[i10].toDouble(),
                result.scores[i01].toDouble(),
                result.scores[i11].toDouble(),
                fx,
                fy
            )
            pixels[row + x] = colorForVisibilitySample(result.mode, noEventMask, grayMargin, score)
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

private fun buildOverlayBitmap(result: HilalVisibilityMapResult): Bitmap {
    val width = result.points
        .map { ((it.longitude - result.lonMin) / result.lonStep).roundToInt() }
        .maxOrNull()
        ?.plus(1)
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val height = result.points
        .map { ((it.latitude - (result.points.minOfOrNull { point -> point.latitude } ?: result.latMin)) / result.latStep).roundToInt() }
        .maxOrNull()
        ?.plus(1)
        ?: return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    val scanLatMin = result.points.minOfOrNull { it.latitude } ?: return bitmap
    result.points.forEach { point ->
        val latIndex = ((point.latitude - scanLatMin) / result.latStep).roundToInt().coerceIn(0, height - 1)
        val lonIndex = ((point.longitude - result.lonMin) / result.lonStep).roundToInt().coerceIn(0, width - 1)
        val y = height - 1 - latIndex
        pixels[y * width + lonIndex] = when (point.zone) {
            HilalVisibilityZone.BEFORE_CONJUNCTION,
            HilalVisibilityZone.MOON_SET_BEFORE_SUN -> AL_HABIB_SPECIAL_GRAY
            HilalVisibilityZone.NO_EVENT -> AL_HABIB_TRANSPARENT
            else -> colorForSample(result.mode, point.score)
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

private fun buildOverlayBitmapNearest(result: HilalVisibilityMapResult): Bitmap {
    val width = OVERLAY_BITMAP_WIDTH
    val height = OVERLAY_BITMAP_HEIGHT
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val lonSpan = result.lonMax - result.lonMin
    val fullLatMax = 90.0
    val fullLatSpan = 180.0

    val scanLatMin = result.points.minOfOrNull { it.latitude } ?: return bitmap
    val scanLatMax = result.points.maxOfOrNull { it.latitude } ?: return bitmap
    val latCount = ((scanLatMax - scanLatMin) / result.latStep).roundToInt() + 1
    val lonCount = ((result.lonMax - result.lonMin) / result.lonStep).roundToInt() + 1
    val colors = IntArray(latCount * lonCount) { AL_HABIB_TRANSPARENT }

    result.points.forEach { point ->
        val latIndex = ((point.latitude - scanLatMin) / result.latStep).roundToInt().coerceIn(0, latCount - 1)
        val lonIndex = ((point.longitude - result.lonMin) / result.lonStep).roundToInt().coerceIn(0, lonCount - 1)
        val index = latIndex * lonCount + lonIndex
        colors[index] = colorForPoint(result.mode, point)
    }

    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        val lat = fullLatMax - (y + 0.5) / height * fullLatSpan
        if (lat < scanLatMin || lat > scanLatMax) continue
        val gy = (lat - scanLatMin) / result.latStep
        val y0 = floor(gy).toInt().coerceIn(0, latCount - 1)
        val y1 = min(y0 + 1, latCount - 1)
        val fy = (gy - y0).coerceIn(0.0, 1.0)
        for (x in 0 until width) {
            val lon = result.lonMin + (x + 0.5) / width * lonSpan
            val gx = (lon - result.lonMin) / result.lonStep
            val nearestY = if (fy < 0.5) y0 else y1
            val nearestX = gx.roundToInt().coerceIn(0, lonCount - 1)
            pixels[y * width + x] = colors[nearestY * lonCount + nearestX]
        }
    }
    bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
    return bitmap
}

private fun unusedOldBilinearOverlay(result: HilalVisibilityMapResult): Bitmap {
    val width = OVERLAY_BITMAP_WIDTH
    val height = OVERLAY_BITMAP_HEIGHT
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val lonSpan = result.lonMax - result.lonMin
    val fullLatMax = 90.0
    val fullLatSpan = 180.0

    val scanLatMin = result.points.minOfOrNull { it.latitude } ?: return bitmap
    val scanLatMax = result.points.maxOfOrNull { it.latitude } ?: return bitmap
    val latCount = ((scanLatMax - scanLatMin) / result.latStep).roundToInt() + 1
    val lonCount = ((result.lonMax - result.lonMin) / result.lonStep).roundToInt() + 1
    val scores = DoubleArray(latCount * lonCount) { TRANSPARENT_SCORE }
    val specialMasks = DoubleArray(latCount * lonCount) { 0.0 }

    result.points.forEach { point ->
        val latIndex = ((point.latitude - scanLatMin) / result.latStep).roundToInt().coerceIn(0, latCount - 1)
        val lonIndex = ((point.longitude - result.lonMin) / result.lonStep).roundToInt().coerceIn(0, lonCount - 1)
        val index = latIndex * lonCount + lonIndex
        when (point.zone) {
            HilalVisibilityZone.BEFORE_CONJUNCTION,
            HilalVisibilityZone.MOON_SET_BEFORE_SUN -> specialMasks[index] = 1.0
            HilalVisibilityZone.NO_EVENT -> scores[index] = TRANSPARENT_SCORE
            else -> scores[index] = point.score
        }
    }

    val pixels = IntArray(width * height)

    for (y in 0 until height) {
        val lat = fullLatMax - (y + 0.5) / height * fullLatSpan
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

            val specialMask = bilinearValue(
                specialMasks[i00],
                specialMasks[i10],
                specialMasks[i01],
                specialMasks[i11],
                fx,
                fy
            )
            if (specialMask >= 0.5) {
                pixels[y * width + x] = AL_HABIB_SPECIAL_GRAY
                continue
            }

            val score = bilinearValue(
                scores[i00],
                scores[i10],
                scores[i01],
                scores[i11],
                fx,
                fy
            )
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
    return when (mode) {
        HilalVisibilityMapMode.YALLOP -> alHabibYallopColor(score)
        HilalVisibilityMapMode.ODEH -> alHabibOdehColor(score)
        else -> alHabibBinaryColor(score)
    }
}

private fun colorForVisibilitySample(
    mode: HilalVisibilityMapMode,
    noEventMask: Double,
    grayMargin: Double,
    score: Double
): Int {
    return when {
        noEventMask >= 0.5 -> AL_HABIB_TRANSPARENT
        grayMargin < 0.0 -> AL_HABIB_SPECIAL_GRAY
        else -> colorForSample(mode, score)
    }
}

private fun metCriteriaLegend(mode: HilalVisibilityMapMode): String = when (mode) {
    HilalVisibilityMapMode.MABIMS_BARU -> "Tinggi >=3°, elongasi >=6,4°"
    HilalVisibilityMapMode.MABIMS_LAMA -> "Tinggi >=2°, elongasi >=3°, umur >=8j"
    HilalVisibilityMapMode.WUJUDUL_HILAL -> "Ijtimak sebelum magrib, bulan di atas ufuk"
    HilalVisibilityMapMode.LAPAN -> "Tinggi >=2°, elongasi >=3°"
    HilalVisibilityMapMode.DANJON -> "Elongasi >=7°"
    HilalVisibilityMapMode.KGHT_TURKI -> "Toposentrik >=5°, elongasi geo >=8°"
    HilalVisibilityMapMode.KGHT_MUHAMMADIYAH -> "Geosentrik >=5°, elongasi geo >=8°"
    else -> "Memenuhi kriteria"
}

private fun unmetCriteriaLegend(mode: HilalVisibilityMapMode): String = when (mode) {
    HilalVisibilityMapMode.DANJON -> "Di bawah batas Danjon"
    else -> "Belum memenuhi kriteria"
}

private fun colorForPoint(mode: HilalVisibilityMapMode, point: HilalVisibilityPoint): Int {
    return when (point.zone) {
        HilalVisibilityZone.BEFORE_CONJUNCTION,
        HilalVisibilityZone.MOON_SET_BEFORE_SUN -> AL_HABIB_SPECIAL_GRAY
        HilalVisibilityZone.NO_EVENT -> AL_HABIB_TRANSPARENT
        else -> colorForSample(mode, point.score)
    }
}

private const val AL_HABIB_ALPHA = 153
private const val TRANSPARENT_SCORE = -100.0
private val AL_HABIB_EASY = argb(AL_HABIB_ALPHA, 50, 145, 69)
private val AL_HABIB_CLEAR = argb(AL_HABIB_ALPHA, 86, 176, 135)
private val AL_HABIB_OPTICAL_INITIAL = argb(AL_HABIB_ALPHA, 229, 205, 112)
private val AL_HABIB_OPTICAL = argb(AL_HABIB_ALPHA, 240, 133, 64)
private val AL_HABIB_TELESCOPE = argb(AL_HABIB_ALPHA, 205, 86, 72)
private val AL_HABIB_GLOBAL_ACCEPTED = argb(120, 144, 238, 144)  // hijau muda semi-transparan
private val AL_HABIB_SPECIAL_GRAY = argb(AL_HABIB_ALPHA, 98, 109, 114)
private val AL_HABIB_TRANSPARENT = argb(0, 0, 0, 0)
private val AL_HABIB_TRANSPARENT_LEGEND = argb(255, 255, 255, 255)

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
    v >= 2.0 -> AL_HABIB_CLEAR
    v >= -0.96 -> AL_HABIB_OPTICAL
    else -> AL_HABIB_TRANSPARENT
}

private fun alHabibCriteriaColor(score: Double): Int = when {
    score >= 8.0 -> AL_HABIB_EASY
    score >= 3.0 -> AL_HABIB_CLEAR
    score >= 0.0 -> AL_HABIB_OPTICAL_INITIAL
    else -> AL_HABIB_TRANSPARENT
}

private fun alHabibBinaryColor(score: Double): Int {
    if (score >= 8.0) return AL_HABIB_EASY
    return AL_HABIB_TRANSPARENT
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

