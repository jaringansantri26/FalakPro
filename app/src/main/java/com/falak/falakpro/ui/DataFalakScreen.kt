package com.falak.falakpro.ui

import android.content.Context
import android.print.PrintAttributes
import android.print.PrintManager
import android.util.Base64
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.falak.falakpro.R
import com.falak.falakpro.premium.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import kotlin.math.*

val NuGreen = Color(0xFF006633)
val NuGold  = Color(0xFFD4AF37)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataFalakScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var year by remember { mutableStateOf("2026") }
    var day by remember { mutableStateOf("17") }
    
    val monthNames = listOf(
        "Januari", "Februari", "Maret", "April", "Mei", "Juni",
        "Juli", "Agustus", "September", "Oktober", "November", "Desember"
    )
    var expandedMonth by remember { mutableStateOf(false) }
    var selectedMonthIndex by remember { mutableStateOf(4) } // Default Mei
    
    var activeMode by remember { mutableStateOf(0) } // 0: Ephemeris, 1: Almanak Nautical
    
    var isLoading by remember { mutableStateOf(false) }
    var ephemerisData by remember { mutableStateOf<EphemerisGenerator.DayEphemeris?>(null) }
    
    var almanacDataList by remember { mutableStateOf<List<EphemerisGenerator.DayEphemeris>?>(null) }
    var moonHiResData by remember { mutableStateOf<List<List<AlmanacGenerator.MoonPoint>>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    var printWebView by remember { mutableStateOf<WebView?>(null) } // For Android printing
    val nuLogoDataUri = remember(context) { loadDrawableDataUri(context, R.drawable.logo_nu) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Falak Ephemeris & Almanak", fontWeight = FontWeight.Bold) },
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
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Tab Row for selection
                    TabRow(
                        selectedTabIndex = activeMode,
                        containerColor = Color.Transparent,
                        contentColor = NuGreen
                    ) {
                        Tab(
                            selected = activeMode == 0,
                            onClick = { activeMode = 0 },
                            text = { Text("Ephemeris Harian", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = activeMode == 1,
                            onClick = { activeMode = 1 },
                            text = { Text("Almanak Nautika", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = day, onValueChange = { day = it },
                            label = { Text("Tgl", fontSize = 11.sp) }, modifier = Modifier.weight(0.7f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                        )

                        Box(modifier = Modifier.weight(1.8f)) {
                            OutlinedTextField(
                                value = monthNames[selectedMonthIndex],
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Bulan", fontSize = 11.sp) },
                                modifier = Modifier.fillMaxWidth().clickable { expandedMonth = true }
                            )
                            DropdownMenu(
                                expanded = expandedMonth,
                                onDismissRequest = { expandedMonth = false },
                                modifier = Modifier.heightIn(max = 240.dp)
                            ) {
                                monthNames.forEachIndexed { index, name ->
                                    DropdownMenuItem(
                                        text = { Text(name) },
                                        onClick = {
                                            selectedMonthIndex = index
                                            expandedMonth = false
                                        }
                                    )
                                }
                            }
                            Box(modifier = Modifier.matchParentSize().clickable { expandedMonth = true }.background(Color.Transparent))
                        }

                        OutlinedTextField(
                            value = year, onValueChange = { year = it },
                            label = { Text("Tahun", fontSize = 11.sp) }, modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        scope.launch {
                            val y = year.toIntOrNull() ?: 2026
                            val m = selectedMonthIndex + 1
                            val dVal = day.toIntOrNull() ?: 17
                            isLoading = true
                            errorMessage = null
                            
                            val result = withContext(Dispatchers.Default) {
                                runCatching {
                                context.assets.open("mpp02_core.bin").use { ElpDataProvider.initialize(it) }
                                context.assets.open("earth_vsop87d.bin").use { Vsop87SolarEngine.initialize(it) }

                                if (activeMode == 0) {
                                    val data = EphemerisGenerator.computeDay(y, m, dVal, context)
                                        DataFalakResult.Ephemeris(data)
                                } else {
                                    val jd1 = Julian.fromCalendar(y, m, dVal.toDouble())
                                        val date1 = Julian.toCalendar(jd1)
                                        val date2 = Julian.toCalendar(jd1 + 1.0)
                                        val date3 = Julian.toCalendar(jd1 + 2.0)
                                        
                                        val data1 = EphemerisGenerator.computeDay(date1.year, date1.month, date1.day, context)
                                        val data2 = EphemerisGenerator.computeDay(date2.year, date2.month, date2.day, context)
                                        val data3 = EphemerisGenerator.computeDay(date3.year, date3.month, date3.day, context)
                                        val almanacDays = listOf(data1, data2, data3)
                                        
                                        val elpMoon = ElpFactory.createMoon(context)
                                        val earth = VsopFactory.createEarth(context)
                                        val nutEngine = NutationIAU2000A(context)
                                        val hiResData = mutableListOf<List<AlmanacGenerator.MoonPoint>>()
                                        
                                        for (dayOff in 0..2) {
                                            val dayPoints = mutableListOf<AlmanacGenerator.MoonPoint>()
                                            val baseJd = jd1 + dayOff.toDouble()
                                            for (step in 0..48) {
                                                val hourFrac = step * 0.5
                                                val jdStep = baseJd + hourFrac / 24.0
                                                val jdeStep = jdStep + DeltaT.estimate(y.toDouble()) / 86400.0
                                                
                                                val moon = MoonEngine.compute(jdeStep, elpMoon, context)
                                                val sun = SunEngine.compute(jdeStep, earth, context)
                                                val nut = nutEngine.compute(jdeStep)
                                                val gst = SiderealTime.apparentGreenwich(
                                                    jdStep, Math.toDegrees(nut.deltaPsi) * 3600.0, sun.trueObliquity
                                                )
                                                val moonGha = Angle.normalizeDegrees(gst - moon.rightAscension)
                                                val moonHp = Math.toDegrees(asin(6378.14 / moon.distanceKm))
                                                dayPoints.add(AlmanacGenerator.MoonPoint(
                                                    hour = hourFrac, dec = moon.declination, gha = moonGha, hp = moonHp
                                                ))
                                            }
                                            hiResData.add(dayPoints)
                                        }
                                        DataFalakResult.Almanac(almanacDays, hiResData)
                                }
                            }
                            }

                            result.fold(
                                onSuccess = { data ->
                                    when (data) {
                                        is DataFalakResult.Ephemeris -> {
                                            ephemerisData = data.data
                                            almanacDataList = null
                                            moonHiResData = emptyList()
                                        }
                                        is DataFalakResult.Almanac -> {
                                            ephemerisData = null
                                            almanacDataList = data.days
                                            moonHiResData = data.moonHiRes
                                        }
                                    }
                                },
                                onFailure = { throwable ->
                                    throwable.printStackTrace()
                                    errorMessage = throwable.message ?: throwable::class.java.simpleName
                                }
                            )
                            isLoading = false
                        }
                    },
                    modifier = Modifier.weight(1f).height(52.dp),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NuGreen)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(if (activeMode == 0) "PROSES EPHEMERIS" else "PROSES ALMANAK", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }

                if ((activeMode == 0 && ephemerisData != null) || (activeMode == 1 && almanacDataList != null)) {
                    Button(
                        onClick = {
                            printWebView?.let { webView ->
                                val printManager = context.getSystemService(Context.PRINT_SERVICE) as PrintManager
                                val docName = if (activeMode == 0) "Ephemeris_${day}_${monthNames[selectedMonthIndex]}_${year}" else "NauticalAlmanac_${day}_${monthNames[selectedMonthIndex]}_${year}"
                                val printAdapter = webView.createPrintDocumentAdapter(docName)
                                printManager.print("$docName PDF", printAdapter, PrintAttributes.Builder().build())
                            }
                        },
                        modifier = Modifier.weight(1f).height(52.dp),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NuGold, contentColor = Color.White)
                    ) {
                        Text("CETAK PDF", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    }
                }
            }

            errorMessage?.let { message ->
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        text = "Gagal memproses data: $message",
                        modifier = Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if ((activeMode == 0 && ephemerisData != null) || (activeMode == 1 && almanacDataList != null)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    val currentHtml = remember(ephemerisData, almanacDataList, moonHiResData, activeMode) {
                        if (activeMode == 0) {
                            EphemerisGenerator.generateStandardHtml(ephemerisData!!, nuLogoDataUri)
                        } else {
                            AlmanacGenerator.generateAlmanacHtml(almanacDataList!!, moonHiResData)
                        }
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { ctx ->
                            WebView(ctx).apply {
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.useWideViewPort = true
                                settings.loadWithOverviewMode = true
                                webViewClient = object : WebViewClient() {
                                    override fun onPageFinished(view: WebView?, url: String?) {
                                        super.onPageFinished(view, url)
                                        printWebView = view
                                    }
                                }
                            }
                        },
                        update = { view ->
                            view.loadDataWithBaseURL(null, currentHtml, "text/html", "UTF-8", null)
                            printWebView = view
                        }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private sealed interface DataFalakResult {
    data class Ephemeris(val data: EphemerisGenerator.DayEphemeris) : DataFalakResult
    data class Almanac(
        val days: List<EphemerisGenerator.DayEphemeris>,
        val moonHiRes: List<List<AlmanacGenerator.MoonPoint>>
    ) : DataFalakResult
}

private fun loadDrawableDataUri(context: Context, resId: Int): String {
    return runCatching {
        context.resources.openRawResource(resId).use { input ->
            val base64 = Base64.encodeToString(input.readBytes(), Base64.NO_WRAP)
            "data:image/png;base64,$base64"
        }
    }.getOrDefault("")
}

// ============================================================================
// ADAPTER & ENGINE IMPLEMENTATIONS
// ============================================================================

object Julian {
    fun fromCalendar(year: Int, month: Int, day: Double): Double {
        val dInt = floor(day).toInt()
        val hour = (day - dInt) * 24.0
        return AstroTime.kmjd(dInt, month, year, hour, 0.0)
    }
    
    data class CalDate(val year: Int, val month: Int, val day: Int)
    
    fun toCalendar(jd: Double): CalDate {
        val z = floor(jd + 0.5).toLong()
        val w = floor((z - 1867216.25) / 36524.25).toLong()
        val x = floor(w / 4.0).toLong()
        val a = z + 1 + w - x
        val b = a + 1524
        val c = floor((b - 122.1) / 365.25).toLong()
        val d = floor(365.25 * c).toLong()
        val e = floor((b - d) / 30.6001).toLong()
        val day = b - d - floor(30.6001 * e).toLong()
        val month = if (e < 14) e - 1 else e - 13
        val year = if (month > 2) c - 4716 else c - 4715
        return CalDate(year.toInt(), month.toInt(), day.toInt())
    }

    fun dayOfWeek(jd: Double): Int {
        val z = floor(jd + 1.5).toLong()
        return (z % 7).toInt()
    }
}

object DeltaT {
    fun estimate(y: Double): Double {
        val jd = AstroTime.kmjd(1, 7, y.toInt(), 0.0, 0.0)
        return DynamicalTimeEngine.deltaT(jd)
    }
}

object Angle {
    fun normalizeDegrees(deg: Double): Double {
        return AstroMath.mod(deg, 360.0)
    }
}

class NutationIAU2000A(val context: Context) {
    data class NutResult(val deltaPsi: Double, val deltaEps: Double)
    fun compute(jde: Double): NutResult {
        val nut = AstroDataUtils.calculateNutation(jde)
        return NutResult(Math.toRadians(nut.first), Math.toRadians(nut.second))
    }
}

object SiderealTime {
    fun apparentGreenwich(jd: Double, nutLonArcsec: Double, trueObliqDeg: Double): Double {
        return AstroDataUtils.calculateGAST(jd)
    }
}

object SunEngine {
    data class SunPos(
        val rightAscension: Double,
        val declination: Double,
        val distanceAU: Double,
        val apparentLongitude: Double,
        val apparentLatitude: Double,
        val trueObliquity: Double,
        val equationOfTime: Double
    )
    fun compute(jde: Double, earth: Any?, context: Context): SunPos {
        val s = Vsop87SolarEngine.compute(jde)
        val obliq = AstroDataUtils.calculateTrueObliquity(jde)
        val eot = AstroSolarEngine.getEquationOfTime(jde) * 60.0 // in minutes
        return SunPos(
            rightAscension = s.ra,
            declination = s.dec,
            distanceAU = s.distanceAU,
            apparentLongitude = s.longitudeEcliptic,
            apparentLatitude = s.latitudeEcliptic,
            trueObliquity = obliq,
            equationOfTime = eot
        )
    }
}

object MoonEngine {
    data class MoonPos(
        val rightAscension: Double,
        val declination: Double,
        val distanceKm: Double,
        val apparentLongitude: Double,
        val apparentLatitude: Double,
        val semiDiameterDeg: Double
    )
    fun compute(jde: Double, elpMoon: Any?, context: Context): MoonPos {
        val m = ElpMpp02LunarEngine.computeGeometric(jde)
        val distKm = m.distanceAU * 149597870.7
        return MoonPos(
            rightAscension = m.ra,
            declination = m.dec,
            distanceKm = distKm,
            apparentLongitude = m.longitudeEcliptic,
            apparentLatitude = m.latitudeEcliptic,
            semiDiameterDeg = m.semidiameter
        )
    }
}

object MoonPhase {
    data class PhaseResult(val illuminatedFraction: Double)
    fun compute(jd: Double, elpMoon: Any?, earth: Any?, context: Context): PhaseResult {
        val jde = jd + DynamicalTimeEngine.deltaT(jd) / 86400.0
        val sun = Vsop87SolarEngine.compute(jde)
        val moon = ElpMpp02LunarEngine.computeGeometric(jde)
        val illum = LunarFunctions.moonIllumination(
            sun.ra, sun.dec, sun.distanceAU,
            moon.ra, moon.dec, moon.distanceAU * 149597870.7
        )
        return PhaseResult(illum.illuminatedFraction)
    }
}

object ElpFactory { fun createMoon(context: Context): Any? = null }
object VsopFactory { fun createEarth(context: Context): Any? = null }

object EphemerisGenerator {

    data class EphemerisRow(
        val day: Int,
        val hourUt: Int,
        val ariesGha: Double,
        
        // Sun
        val sunAppLong: Double,
        val sunAppLat: Double,
        val sunAppRa: Double,
        val sunAppDec: Double,
        val sunGha: Double,
        val sunDistAU: Double,
        val sunSd: Double,
        val trueObliquity: Double,
        val eqOfTimeMins: Double, // in minutes

        // Moon
        val moonAppLong: Double,
        val moonAppLat: Double,
        val moonAppRa: Double,
        val moonAppDec: Double,
        val moonGha: Double,
        val moonHp: Double,
        val moonSd: Double,
        val moonBrightLimb: Double,
        val moonIllumPercent: Double, // 0 to 100
        val moonV: Double, // Nautical Almanac v factor
        val moonD: Double  // Nautical Almanac d factor
    )

    data class DayEphemeris(
        val year: Int,
        val month: Int,
        val day: Int,
        val rows: List<EphemerisRow>
    )

    private fun dmsRoundStr(deg: Double): String {
        val sign = if (deg < 0) "-" else ""
        val v = kotlin.math.abs(deg)
        val d = v.toInt()
        val mFloat = (v - d) * 60.0
        val m = mFloat.toInt()
        val s = kotlin.math.round((mFloat - m) * 60.0).toInt()
        
        // Handle roll-over from rounding
        var finalS = s
        var finalM = m
        var finalD = d
        if (finalS >= 60) { finalS -= 60; finalM += 1 }
        if (finalM >= 60) { finalM -= 60; finalD += 1 }
        
        return String.format(Locale.US, "%s%02d° %02d' %02d\"", sign, finalD, finalM, finalS)
    }

    private fun dmStr(deg: Double): String {
        val sign = if (deg < 0) "-" else ""
        val v = kotlin.math.abs(deg)
        val d = v.toInt()
        val m = kotlin.math.round((v - d) * 60.0).toInt()
        
        var finalM = m
        var finalD = d
        if (finalM >= 60) { finalM -= 60; finalD += 1 }
        
        return String.format(Locale.US, "%s%02d° %02d'", sign, finalD, finalM)
    }

    private fun msStr(deg: Double): String {
        val totalSec = kotlin.math.abs(deg) * 3600.0
        val m = (totalSec / 60.0).toInt()
        val s = totalSec % 60.0
        return String.format(Locale.US, "%02d' %05.2f\"", m, s).replace('.', ',')
    }

    private fun secStr(deg: Double): String {
        val sign = if (deg < 0) "-" else ""
        val arcsec = kotlin.math.abs(deg) * 3600.0
        return String.format(Locale.US, "%s%04.2f\"", sign, arcsec).replace('.', ',')
    }

    private fun eqtStr(mins: Double): String {
        val sign = if (mins < 0) "-" else ""
        val a = kotlin.math.abs(mins)
        val m = kotlin.math.floor(a).toInt()
        val s = kotlin.math.round((a - m) * 60.0).toInt()
        
        var finalS = s
        var finalM = m
        if (finalS >= 60) { finalS -= 60; finalM += 1 }
        
        return String.format(Locale.US, "%s%02dm %02ds", sign, finalM, finalS)
    }

    private fun fracStr(f: Double): String {
        return String.format(Locale.US, "%.4f", f).replace('.', ',')
    }

    private fun distStr(d: Double): String {
        return String.format(Locale.US, "%.7f", d).replace('.', ',')
    }

    private fun illumStr(p: Double): String {
        return String.format(Locale.US, "%.2f%%", p).replace('.', ',')
    }

    private fun ghaStr(deg: Double): String {
        val v = Angle.normalizeDegrees(deg)
        val d = v.toInt()
        val m = (v - d) * 60.0
        return String.format(Locale.US, "%03d° %04.1f'", d, m).replace('.', ',')
    }

    private fun decAlmanacStr(deg: Double): String {
        val sign = if (deg >= 0) "N" else "S"
        val v = kotlin.math.abs(deg)
        val d = v.toInt()
        val m = (v - d) * 60.0
        return String.format(Locale.US, "%s %02d° %04.1f'", sign, d, m).replace('.', ',')
    }

    private fun factorStr(f: Double): String {
        return String.format(Locale.US, "%+.1f", f).replace('.', ',')
    }

    suspend fun computeDay(year: Int, month: Int, day: Int, context: Context): DayEphemeris {
        return withContext(Dispatchers.Default) {
            val elpMoon = ElpFactory.createMoon(context)
            val earth = VsopFactory.createEarth(context)
            val rows = mutableListOf<EphemerisRow>()
            
            val jds = (0..25).map { hour -> Julian.fromCalendar(year, month, day.toDouble() + hour / 24.0) }
            val deltaT = DeltaT.estimate(year.toDouble())
            val jdes = jds.map { it + deltaT / 86400.0 }

            val suns = jdes.map { SunEngine.compute(it, earth, context) }
            val moons = jdes.map { MoonEngine.compute(it, elpMoon, context) }
            
            val nutEngine = NutationIAU2000A(context)
            val nuts = jdes.map { nutEngine.compute(it) }

            for (hour in 0..24) {
                val jd = jds[hour]
                val sun = suns[hour]
                val moon = moons[hour]
                val nut = nuts[hour]
                
                val gst = SiderealTime.apparentGreenwich(jd, Math.toDegrees(nut.deltaPsi) * 3600.0, sun.trueObliquity)
                val sunGha = Angle.normalizeDegrees(gst - sun.rightAscension)
                val moonGha = Angle.normalizeDegrees(gst - moon.rightAscension)
                val ariesGha = gst

                val nextMoon = moons[hour + 1]
                val nextSun = suns[hour + 1]
                val nextGst = SiderealTime.apparentGreenwich(jds[hour+1], Math.toDegrees(nuts[hour+1].deltaPsi) * 3600.0, nextSun.trueObliquity)
                val nextMoonGha = Angle.normalizeDegrees(nextGst - nextMoon.rightAscension)
                
                var diffGha = nextMoonGha - moonGha
                if (diffGha < 0) diffGha += 360.0
                val moonV = (diffGha - 14.316666666) * 60.0 
                val moonD = (nextMoon.declination - moon.declination) * 60.0

                val sunSd = (959.63 / sun.distanceAU) / 3600.0
                val phase = MoonPhase.compute(jd, elpMoon, earth, context)
                val moonHp = Math.toDegrees(asin(6378.14 / moon.distanceKm))
                
                val raS = Math.toRadians(sun.rightAscension)
                val decS = Math.toRadians(sun.declination)
                val raM = Math.toRadians(moon.rightAscension)
                val decM = Math.toRadians(moon.declination)
                val chi = atan2(
                    cos(decS) * sin(raS - raM),
                    sin(decS) * cos(decM) - cos(decS) * sin(decM) * cos(raS - raM)
                )
                val chiDeg = Math.toDegrees(chi)
                val finalChi = if (chiDeg < 0) chiDeg + 360.0 else chiDeg

                rows.add(
                    EphemerisRow(
                        day = day,
                        hourUt = hour,
                        ariesGha = ariesGha,
                        sunAppLong = sun.apparentLongitude,
                        sunAppLat = sun.apparentLatitude,
                        sunAppRa = sun.rightAscension,
                        sunAppDec = sun.declination,
                        sunGha = sunGha,
                        sunDistAU = sun.distanceAU,
                        sunSd = sunSd,
                        trueObliquity = sun.trueObliquity,
                        eqOfTimeMins = sun.equationOfTime,
                        
                        moonAppLong = moon.apparentLongitude,
                        moonAppLat = moon.apparentLatitude,
                        moonAppRa = moon.rightAscension,
                        moonAppDec = moon.declination,
                        moonGha = moonGha,
                        moonHp = moonHp,
                        moonSd = moon.semiDiameterDeg,
                        moonBrightLimb = finalChi,
                        moonIllumPercent = phase.illuminatedFraction * 100.0,
                        moonV = moonV,
                        moonD = moonD
                    )
                )
            }
            DayEphemeris(year, month, day, rows)
        }
    }

    fun generateStandardHtml(ephemeris: DayEphemeris, nuLogoDataUri: String = ""): String {
        val monthNames = listOf("", "Januari", "Februari", "Maret", "April", "Mei", "Juni", "Juli", "Agustus", "September", "Oktober", "November", "Desember")
        val moName = monthNames[ephemeris.month]
        val logoHtml = if (nuLogoDataUri.isNotBlank()) {
            "<img class=\"nu-logo\" src=\"$nuLogoDataUri\" alt=\"Logo NU\" />"
        } else {
            "<div class=\"nu-logo-fallback\">NU</div>"
        }
        
        val sb = java.lang.StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    @page { size: A4 portrait; margin: 0; }
                    html, body {
                        width: 210mm;
                        height: 297mm;
                    }
                    body { 
                        font-family: 'Times New Roman', Times, serif; 
                        color: #000; 
                        margin: 0; 
                        padding: 0; 
                        line-height: 1; 
                        background-color: #fff;
                    }
                    
                    .page {
                        box-sizing: border-box;
                        width: 210mm;
                        height: 297mm;
                        overflow: hidden;
                        padding: 0 0 7mm 0;
                    }

                    .print-header {
                        text-align: center;
                        margin: 0 0 0.8mm 0;
                    }
                    .ornament-band {
                        position: relative;
                        height: 18mm;
                        overflow: hidden;
                        margin: 0 0 5mm 0;
                        background:
                            radial-gradient(circle at 5mm 5mm, transparent 2.1mm, #9c9c9c 2.2mm, #9c9c9c 2.55mm, transparent 2.65mm),
                            radial-gradient(circle at 12mm 5mm, transparent 2.1mm, #9c9c9c 2.2mm, #9c9c9c 2.55mm, transparent 2.65mm),
                            radial-gradient(circle at 8.5mm 10mm, transparent 2.1mm, #9c9c9c 2.2mm, #9c9c9c 2.55mm, transparent 2.65mm);
                        background-size: 17mm 10mm;
                    }
                    .ornament-band::after {
                        content: "";
                        position: absolute;
                        left: 0;
                        right: 0;
                        bottom: 0;
                        border-bottom: 0.45mm solid #9c9c9c;
                    }
                    .ornament-badge {
                        position: absolute;
                        left: 50%;
                        top: 8mm;
                        transform: translateX(-50%);
                        min-width: 48mm;
                        height: 10mm;
                        box-sizing: border-box;
                        background: #fff;
                        border: 0.35mm solid #9c9c9c;
                        border-radius: 6mm;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        gap: 1.5mm;
                        padding: 0 4mm;
                        color: #000;
                        font-size: 7.6pt;
                        font-weight: bold;
                        white-space: nowrap;
                    }
                    .nu-logo {
                        width: 6.5mm;
                        height: 6.5mm;
                        object-fit: contain;
                        display: inline-block;
                        margin: 0;
                    }
                    .nu-logo-fallback {
                        width: 6.5mm;
                        height: 6.5mm;
                        margin: 0;
                        border: 0.35mm solid #000;
                        border-radius: 50%;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        font-size: 5.2pt;
                        font-weight: bold;
                    }
                    .institution {
                        font-size: 10.24pt;
                        font-weight: bold;
                        letter-spacing: 0.2pt;
                        text-transform: uppercase;
                        margin: 0 0 4.4mm 0;
                    }
                    .date-title {
                        text-align: center;
                        font-size: 19pt;
                        font-weight: bold;
                        margin: 0 0 8.4mm 0;
                        letter-spacing: 0.4pt;
                    }
                    
                    .section-title {
                        text-align: center;
                        font-family: 'Times New Roman', Times, serif;
                        font-size: 10.24pt;
                        font-weight: bold;
                        margin: 3.4mm 0 0.8mm 0;
                        letter-spacing: 0.35pt;
                    }
                    
                    table { 
                        width: 189.4mm; 
                        border-collapse: collapse; 
                        margin: 0 auto; 
                        table-layout: fixed; 
                        border: 1.1px solid #000; 
                    }
                    th { 
                        border: 1px solid #333;
                        box-sizing: border-box;
                        height: 14.8mm;
                        padding: 0 0.35mm;
                        background-color: #f4f4f4;
                        background-image: radial-gradient(#cfcfcf 0.55px, transparent 0.55px);
                        background-size: 2.1px 2.1px;
                        font-weight: bold; 
                        font-family: 'Times New Roman', Times, serif;
                        font-size: 9.22pt; 
                        text-align: center; 
                        vertical-align: middle;
                        line-height: 0.92;
                    }
                    td { 
                        border: 1px solid #333;
                        box-sizing: border-box;
                        height: 4mm;
                        padding: 0 0.35mm;
                        text-align: center; 
                        vertical-align: middle;
                        font-family: 'Times New Roman', Times, serif;
                        font-size: 8.20pt; 
                        white-space: nowrap; 
                        line-height: 1.03;
                    }
                    .sun-hour,
                    .moon-hour { width: 10.2mm; }
                    .sun-ecl,
                    .sun-ra,
                    .sun-dec,
                    .sun-dist,
                    .sun-small,
                    .sun-eqt,
                    .moon-col { width: 22.4mm; }
                    .footnote {
                        width: 189.4mm;
                        margin: 0.5mm auto 0 auto;
                        font-family: 'Times New Roman', Times, serif;
                        font-size: 7.8pt;
                    }
                    .page-number {
                        text-align: center;
                        margin-top: 5.8mm;
                        font-family: 'Times New Roman', Times, serif;
                        font-size: 11pt;
                    }

                    @media print {
                        body { -webkit-print-color-adjust: exact; print-color-adjust: exact; }
                    }
                </style>
            </head>
            <body>
                <div class="page">
                <div class="print-header">
                    <div class="ornament-band">
                        <div class="ornament-badge">$logoHtml<span>Ephemeris Hisab Rukyat ${ephemeris.year}</span></div>
                    </div>
                    <div class="institution">LEMBAGA FALAKIYAH PWNU JAWA BARAT</div>
                    <div class="date-title">${ephemeris.day} $moName ${ephemeris.year}</div>
                </div>
                
                <div class="section-title">DATA MATAHARI</div>
                <table>
                    <thead>
                        <tr>
                            <th class="sun-hour">Jam</th>
                            <th class="sun-ecl">Ecliptic<br>Longitude<br>*)</th>
                            <th class="sun-ecl">Ecliptic<br>Latitude<br>*)</th>
                            <th class="sun-ra">Apparent<br>Right<br>Ascension</th>
                            <th class="sun-dec">Apparent<br>Declination</th>
                            <th class="sun-dist">True<br>Geocentric<br>Distance</th>
                            <th class="sun-small">Semi<br>Diameter</th>
                            <th class="sun-small">True<br>Obliquity</th>
                            <th class="sun-eqt">Equation<br>Of<br>Time</th>
                        </tr>
                    </thead>
                    <tbody>
        """.trimIndent())

        for (r in ephemeris.rows) {
            sb.append("<tr>")
            sb.append("<td>${r.hourUt}</td>")
            sb.append("<td>${dmsRoundStr(r.sunAppLong)}</td>")
            sb.append("<td>${secStr(r.sunAppLat)}</td>")
            sb.append("<td>${dmsRoundStr(r.sunAppRa)}</td>")
            sb.append("<td>${dmsRoundStr(r.sunAppDec)}</td>")
            sb.append("<td>${distStr(r.sunDistAU)}</td>")
            sb.append("<td>${msStr(r.sunSd)}</td>")
            sb.append("<td>${dmsRoundStr(r.trueObliquity)}</td>")
            sb.append("<td>${eqtStr(r.eqOfTimeMins)}</td>")
            sb.append("</tr>\n")
        }

        sb.append("""
                    </tbody>
                </table>
                <div class="footnote">*) for mean equinox of date</div>

                <div class="section-title">DATA BULAN</div>
                <table>
                    <thead>
                        <tr>
                            <th class="moon-hour">Jam</th>
                            <th class="moon-col">Apparent<br>Longitude</th>
                            <th class="moon-col">Apparent<br>Latitude</th>
                            <th class="moon-col">Apparent<br>Right<br>Ascension</th>
                            <th class="moon-col">Apparent<br>Declination</th>
                            <th class="moon-col">Horizontal<br>Parallax</th>
                            <th class="moon-col">Semi<br>Diameter</th>
                            <th class="moon-col">Angle<br>Bright<br>Limb</th>
                            <th class="moon-col">Fraction<br>Illumination</th>
                        </tr>
                    </thead>
                    <tbody>
        """.trimIndent())

        for (r in ephemeris.rows) {
            sb.append("<tr>")
            sb.append("<td>${r.hourUt}</td>")
            sb.append("<td>${dmsRoundStr(r.moonAppLong)}</td>")
            sb.append("<td>${dmsRoundStr(r.moonAppLat)}</td>")
            sb.append("<td>${dmsRoundStr(r.moonAppRa)}</td>")
            sb.append("<td>${dmsRoundStr(r.moonAppDec)}</td>")
            sb.append("<td>${dmStr(r.moonHp)}</td>")
            sb.append("<td>${msStr(r.moonSd)}</td>")
            sb.append("<td>${dmsRoundStr(r.moonBrightLimb)}</td>")
            sb.append("<td>${illumStr(r.moonIllumPercent)}</td>")
            sb.append("</tr>\n")
        }

        sb.append("""
                    </tbody>
                </table>
                <div class="page-number">41</div>
                </div>
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }

    fun generateNauticalHtml(ephemeris: DayEphemeris): String {
        val monthNames = listOf("", "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        val moName = monthNames[ephemeris.month]
        
        val sb = java.lang.StringBuilder()
        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    @page { size: A4; margin: 1cm; }
                    body { font-family: 'Courier New', Courier, monospace; color: #000; margin: 0; padding: 0; line-height: 1.1; font-size: 11px; }
                    
                    .doc-header { text-align: center; border-bottom: 2px solid #000; padding-bottom: 5px; margin-bottom: 8px; }
                    .doc-header h1 { font-size: 15px; margin: 0; text-transform: uppercase; font-weight: bold; }
                    
                    .doc-info { text-align: center; margin-bottom: 10px; border: 1px solid #ddd; padding: 5px; background-color: #fdfdfd; }
                    .doc-info .date-line { font-size: 13px; font-weight: bold; text-decoration: underline; }
                    
                    table { width: 100%; border-collapse: collapse; margin-bottom: 10px; table-layout: fixed; border: 1px solid #000; }
                    th { border: 1px solid #000; padding: 4px 2px; background-color: #f2f2f2; font-weight: bold; font-size: 9px; text-align: center; }
                    td { border: 1px solid #000; padding: 3px 2px; text-align: right; font-size: 11px; white-space: nowrap; font-weight: bold; }
                    
                    .highlight-row { background-color: #f8f8f8; }
                    .col-hour { width: 1%; font-weight: bold; background-color: #fafafa; text-align: center !important; }
                    .divider-bold { border-left: 2px solid #000; }
                    
                    .footer { font-size: 10px; margin-top: 10px; border-top: 1px solid #ccc; padding-top: 5px; }
                </style>
            </head>
            <body>
                <div class="doc-header">
                    <h1>LEMBAGA FALAKIYAH PWNU JAWA BARAT</h1>
                    <div style="font-weight: bold; font-size: 14px;">DAILY NAUTICAL ALMANAC DATA</div>
                </div>
                <div class="doc-info">
                    <div class="date-line">DATE: ${ephemeris.day} ${moName.take(3)} ${ephemeris.year} (UT)</div>
                </div>

                <table>
                    <thead>
                        <tr>
                            <th rowspan="2" class="col-hour" style="background:#eee;">UT</th>
                            <th rowspan="2" style="width: 85px;">ARIES GHA</th>
                            <th colspan="2" class="divider-bold">THE SUN</th>
                            <th colspan="5" class="divider-bold">THE MOON</th>
                        </tr>
                        <tr>
                            <th style="width:90px;">G.H.A.</th>
                            <th style="width:90px;">DEC.</th>
                            <th style="width:90px;" class="divider-bold">G.H.A.</th>
                            <th style="width:40px;">v</th>
                            <th style="width:90px;">DEC.</th>
                            <th style="width:40px;">d</th>
                            <th style="width:70px;">H.P.</th>
                        </tr>
                    </thead>
                    <tbody>
        """.trimIndent())

        for (i in 0..23) {
            val r = ephemeris.rows[i]
            val rowClass = if (i % 3 == 0) "highlight-row" else ""
            sb.append("<tr class=\"$rowClass\">")
            sb.append("<td class=\"col-hour\">${String.format(Locale.US, "%02d", r.hourUt)}</td>")
            sb.append("<td style=\"text-align:center;\">${ghaStr(r.ariesGha)}</td>")
            sb.append("<td class=\"divider-bold\">${ghaStr(r.sunGha)}</td>")
            sb.append("<td>${decAlmanacStr(r.sunAppDec)}</td>")
            sb.append("<td class=\"divider-bold\">${ghaStr(r.moonGha)}</td>")
            sb.append("<td style=\"text-align:center; color:#555; font-size:9px;\">${factorStr(r.moonV)}</td>")
            sb.append("<td>${decAlmanacStr(r.moonAppDec)}</td>")
            sb.append("<td style=\"text-align:center; color:#555; font-size:9px;\">${factorStr(r.moonD)}</td>")
            sb.append("<td>${String.format(Locale.US, "%04.1f'", r.moonHp * 60.0)}</td>")
            sb.append("</tr>\n")
        }

        sb.append("""
                    </tbody>
                </table>
                
                <div class="footer">
                    <div style="display: flex; justify-content: space-between;">
                        <div>
                            v = change in GHA - 14° 19.0'<br>
                            d = hourly change in DEC (arcmin)
                        </div>
                        <div>
                            Sun Mer. Pass: ${String.format(Locale.US, "%02d:%02d", (12 * 60 - ephemeris.rows[12].eqOfTimeMins).toInt() / 60, (12 * 60 - ephemeris.rows[12].eqOfTimeMins).toInt() % 60)} UT<br>
                            EqT (00:12h): ${eqtStr(ephemeris.rows[0].eqOfTimeMins)} / ${eqtStr(ephemeris.rows[12].eqOfTimeMins)}
                        </div>
                        <div style="text-align: right;">
                            Printed: ${java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.US).format(java.util.Date())}
                        </div>
                    </div>
                </div>
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }
}

object AlmanacGenerator {

    private fun dmsParts(deg: Double): DoubleArray {
        val v = abs(deg)
        val d = v.toInt()
        val mFloat = (v - d) * 60.0
        val m = mFloat.toInt()
        val s = (mFloat - m) * 60.0
        return doubleArrayOf(d.toDouble(), m.toDouble(), s)
    }

    private fun decAlmanacStr(decSec: Double): String {
        val sign = if (decSec < 0) "-" else ""
        val d = dmsParts(abs(decSec))
        return String.format(Locale.US, "%s%d°%04.1f", sign, d[0].toInt(), d[1] + d[2] / 60.0)
    }

    private fun ghaStr(deg: Double): String {
        val d = dmsParts(deg)
        return String.format(Locale.US, "%d°%04.1f", d[0].toInt(), d[1] + d[2] / 60.0)
    }

    private fun factorStr(f: Double): String {
        val sign = if (f < 0) "-" else ""
        return String.format(Locale.US, "%s%.1f", sign, abs(f))
    }

    // High-res Moon data point (computed every 10 min or 30 min from ELP2000)
    data class MoonPoint(val hour: Double, val dec: Double, val gha: Double, val hp: Double)

    fun generateAlmanacHtml(
        days: List<EphemerisGenerator.DayEphemeris>,
        moonHiRes: List<List<MoonPoint>> = emptyList() // 3 days of high-res data
    ): String {
        val sb = java.lang.StringBuilder()

        sb.append("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    @page { size: A4 portrait; margin: 14mm 5mm 10mm 5mm; }
                    html, body {
                        width: 210mm;
                        height: 297mm;
                    }
                    body {
                        font-family: Arial, Helvetica, sans-serif;
                        font-size: 7pt;
                        color: #000;
                        margin: 0;
                        padding: 0;
                        line-height: 1;
                    }
                    .page-title {
                        text-align: right;
                        font-weight: bold;
                        font-size: 9pt;
                        margin: 0 5mm 4.6mm 0;
                    }
                    .container {
                        display: grid;
                        grid-template-columns: 111mm 87mm;
                        column-gap: 2mm;
                        width: 200mm;
                        margin: 0 auto;
                        align-items: start;
                    }
                    .left-col {
                        width: 111mm;
                    }
                    .right-col {
                        width: 87mm;
                    }
                    table {
                        width: 100%;
                        border-collapse: collapse;
                        margin: 0 0 2.2mm 0;
                        table-layout: fixed;
                        border: 0.22mm solid #000;
                    }
                    th, td {
                        border-left: 0.18mm solid #000;
                        border-right: 0.18mm solid #000;
                        border-top: 0;
                        border-bottom: 0;
                        box-sizing: border-box;
                        height: 2.55mm;
                        padding: 0 0.4mm;
                        text-align: right;
                        white-space: nowrap;
                        overflow: hidden;
                        font-family: Arial, Helvetica, sans-serif;
                        font-size: 7pt;
                        line-height: 1;
                        font-weight: normal;
                    }
                    th {
                        text-align: center;
                        font-weight: bold;
                        background-color: #fff;
                        font-size: 7pt;
                        border-top: 0.22mm solid #000;
                        border-bottom: 0.22mm solid #000;
                    }
                    tbody th {
                        font-weight: normal;
                        border-top: 0;
                        border-bottom: 0;
                    }
                    tbody tr:last-child td,
                    tbody tr:last-child th {
                        border-top: 0.22mm solid #000;
                    }
                    .col-h {
                        text-align: center;
                        width: 6.5mm;
                    }
                    .col-gha { width: 19mm; }
                    .col-dec { width: 18mm; }
                    .col-v, .col-d { width: 8mm; }
                    .col-hp { width: 10.5mm; }
                    .group-row th {
                        border-left-color: transparent;
                        border-right-color: transparent;
                        border-top-color: transparent;
                        height: 4.2mm;
                        font-size: 9pt;
                        line-height: 1;
                    }
                    .day-title {
                        font-weight: bold;
                        text-align: center;
                    }
                    .section-sep { border-right-width: 0.35mm !important; }
                    .right-table th,
                    .right-table td {
                        height: 2.6mm;
                        padding: 0 0.35mm;
                    }
                    .right-table .lat-col { width: 7mm; text-align: center; }
                    .summary-table { margin-top: 3.2mm; }
                    .summary-table th,
                    .summary-table td {
                        height: 2.8mm;
                    }
                    .footer-brand {
                        position: fixed;
                        left: 0;
                        right: 0;
                        bottom: 4mm;
                        text-align: center;
                        font-family: Arial, Helvetica, sans-serif;
                        font-size: 7pt;
                        font-weight: normal;
                    }
                </style>
            </head>
            <body>
        """.trimIndent())

        if (days.size >= 3) {
            val d1 = days[0]
            val d2 = days[1]
            val d3 = days[2]
            
            val dayNamesShort = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
            val monthNamesShort = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val monthNamesFull = listOf("", "January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")

            val titleStart = "${d1.year} ${monthNamesFull[d1.month]} ${String.format(Locale.US, "%02d", d1.day)}"
            val titleEnd = "${monthNamesShort[d3.month]}. ${String.format(Locale.US, "%02d", d3.day)}"
            
            sb.append("<div class=\"page-title\">$titleStart to $titleEnd</div>\n")
            sb.append("<div class=\"container\">\n")
            
            // ================= LEFT COLUMN =================
            sb.append("<div class=\"left-col\">\n")

            for ((dayIndex, dayEphemeris) in days.withIndex()) {
                val jd = Julian.fromCalendar(dayEphemeris.year, dayEphemeris.month, dayEphemeris.day.toDouble())
                val dayOfWeek = Julian.dayOfWeek(jd)
                val dayStr = dayNamesShort[dayOfWeek]
                
                sb.append("<table class=\"main-day-table\">\n")
                sb.append("<colgroup>")
                sb.append("<col class=\"col-h\"><col class=\"col-gha\"><col class=\"col-dec\"><col class=\"col-gha\"><col class=\"col-v\"><col class=\"col-dec\"><col class=\"col-d\"><col class=\"col-hp\">")
                sb.append("</colgroup>\n")
                sb.append("<thead>\n")
                if (dayIndex == 0) {
                    sb.append("<tr class=\"group-row\"><th>h</th><th colspan=\"2\" class=\"section-sep\">Sun</th><th colspan=\"5\">Moon</th></tr>\n")
                }
                sb.append("<tr>")
                sb.append("<th class=\"day-title\">$dayStr</th>")
                sb.append("<th>GHA</th>")
                sb.append("<th class=\"section-sep\">Dec</th>")
                sb.append("<th>GHA</th>")
                sb.append("<th>v</th>")
                sb.append("<th>Dec</th>")
                sb.append("<th>d</th>")
                sb.append("<th>HP</th>")
                sb.append("</tr>\n")
                sb.append("</thead><tbody>\n")

                for (i in 0..23) {
                    val r = dayEphemeris.rows[i]
                    sb.append("<tr>")
                    sb.append("<td class=\"col-h\">${r.hourUt}</td>")
                    sb.append("<td>${ghaStr(r.sunGha)}</td>")
                    sb.append("<td class=\"section-sep\">${decAlmanacStr(r.sunAppDec)}</td>")
                    sb.append("<td>${ghaStr(r.moonGha)}</td>")
                    sb.append("<td>${factorStr(r.moonV)}</td>")
                    sb.append("<td>${decAlmanacStr(r.moonAppDec)}</td>")
                    sb.append("<td>${factorStr(r.moonD)}</td>")
                    sb.append("<td>${String.format(Locale.US, "%.1f", r.moonHp * 60.0)}</td>")
                    sb.append("</tr>\n")
                }
                
                val r12 = dayEphemeris.rows[12]
                val sunSD = String.format(Locale.US, "%.1f", r12.sunSd * 60.0)
                val sunDStr = if (dayEphemeris.rows.size > 13) {
                     factorStr((dayEphemeris.rows[13].sunAppDec - dayEphemeris.rows[12].sunAppDec) * 60.0)
                } else "0.0"
                val moonSD = String.format(Locale.US, "%.1f", r12.moonSd * 60.0)

                sb.append("<tr>")
                sb.append("<td class=\"col-h\"></td>")
                sb.append("<td colspan=\"2\" class=\"section-sep\" style=\"text-align:center;\">SD.=$sunSD&nbsp;&nbsp;&nbsp;&nbsp;d=$sunDStr</td>")
                sb.append("<td colspan=\"5\" style=\"text-align:center;\">S.D.=$moonSD</td>")
                sb.append("</tr>\n")
                sb.append("</tbody></table>\n")
            }
            sb.append("</div>\n")
            
            // ================= RIGHT COLUMN =================
            sb.append("<div class=\"right-col\">\n")
            
            val latitudes = listOf(
                72.0, 70.0, 68.0, 66.0, 64.0, 62.0, 60.0, 58.0, 56.0, 54.0, 52.0, 50.0,
                45.0, 40.0, 35.0, 30.0, 20.0, 10.0, 0.0, -10.0, -20.0, -30.0, -35.0,
                -40.0, -45.0, -50.0, -52.0, -54.0, -56.0, -58.0, -60.0
            )
            
            // ---- Helper: hour angle rise/set in LMT ----
            val daysArr = arrayOf(d1, d2, d3)
            
            // Compute rise/set LMT using hour angle formula
            // Returns Pair(riseLMT, setLMT) in hours, or -1 if no event
            fun computeH(lat: Double, decDeg: Double, h0: Double): Double {
                val l = Math.toRadians(lat)
                val d = Math.toRadians(decDeg)
                val ho = Math.toRadians(h0)
                val cosH = (Math.sin(ho) - Math.sin(l) * Math.sin(d)) / (Math.cos(l) * Math.cos(d))
                if (cosH < -1.0) return -1.0 // Always above horizon
                if (cosH > 1.0) return -2.0  // Always below horizon
                return Math.toDegrees(Math.acos(cosH)) / 15.0 // H in hours
            }

            // Interpolate Sun Dec from the middle day's hourly data
            fun interpSunDec(hour: Double): Double {
                val h = hour.coerceIn(0.0, 23.0)
                val h0i = h.toInt().coerceIn(0, 22)
                val frac = h - h0i
                val dec0 = d2.rows[h0i].sunAppDec
                val dec1 = d2.rows[h0i + 1].sunAppDec
                return dec0 + (dec1 - dec0) * frac
            }

            // 2-iteration rise/set with interpolated Dec
            fun lmtRiseSet(lat: Double, h0: Double, eqtMins: Double): Pair<Double, Double> {
                // EqT convention (Meeus): negative in January → Sun transits after noon
                // Transit LMT = 12 - EqT/60
                val transit = 12.0 - eqtMins / 60.0

                // First estimate with noon Dec
                val H1 = computeH(lat, d2.rows[12].sunAppDec, h0)
                if (H1 < 0) return Pair(H1, H1)
                val riseEst = transit - H1
                val setEst = transit + H1

                // Second iteration: interpolate Dec to estimated times
                val decRise = interpSunDec(riseEst.coerceIn(0.0, 23.0))
                val decSet = interpSunDec(setEst.coerceIn(0.0, 23.0))
                val H_rise = computeH(lat, decRise, h0)
                val H_set = computeH(lat, decSet, h0)
                if (H_rise < 0 || H_set < 0) return Pair(H_rise, H_set)

                return Pair(transit - H_rise, transit + H_set)
            }

            fun formatHM(hours: Double): String {
                if (hours < 0 || hours >= 24) return "-:-"
                val h = hours.toInt()
                val m = kotlin.math.round((hours - h) * 60.0).toInt()
                var finalH = h
                var finalM = m
                if (finalM >= 60) { finalH++; finalM -= 60 }
                if (finalH >= 24) return "-:-"
                return String.format(Locale.US, "%02d:%02d", finalH, finalM)
            }

            // Sun declination and EqT at noon of middle day
            val midSunDec = d2.rows[12].sunAppDec  // already in degrees!
            val midEqT = d2.rows[12].eqOfTimeMins  // in minutes

            // A) TWILIGHT & SUNRISE/SET
            sb.append("""
                <table class="right-table">
                    <colgroup><col class="lat-col"><col span="6"></colgroup>
                    <thead>
                        <tr>
                            <th rowspan="2">Lat.</th>
                            <th colspan="3">Twilight</th>
                            <th colspan="3">Twilight</th>
                        </tr>
                        <tr><th>Naut.</th><th>Civil</th><th>Sunrise</th><th>Sunset</th><th>Civil</th><th>Naut.</th></tr>
                    </thead>
                    <tbody>
            """.trimIndent())
            
            for (lat in latitudes) {
                val srss = lmtRiseSet(lat, -0.8333, midEqT)
                val civ = lmtRiseSet(lat, -6.0, midEqT)
                val naut = lmtRiseSet(lat, -12.0, midEqT)
                
                val latStr = if (lat == 0.0) "0" else lat.toInt().toString()
                
                sb.append("<tr>")
                sb.append("<th>$latStr</th>")
                sb.append("<td>${formatHM(naut.first)}</td>")
                sb.append("<td>${formatHM(civ.first)}</td>")
                sb.append("<td>${formatHM(srss.first)}</td>")
                sb.append("<td>${formatHM(srss.second)}</td>")
                sb.append("<td>${formatHM(civ.second)}</td>")
                sb.append("<td>${formatHM(naut.second)}</td>")
                sb.append("</tr>\n")
            }
            sb.append("</tbody></table>\n")

            // B) MOONRISE / MOONSET
            // For Moon: h0 = 0.125° (includes parallax correction roughly)
            // Moon declination at noon for each of the 3 days
            val mDec1 = d1.rows[12].moonAppDec  // already in degrees
            val mDec2 = d2.rows[12].moonAppDec
            val mDec3 = d3.rows[12].moonAppDec

            val d1Name = dayNamesShort[Julian.dayOfWeek(Julian.fromCalendar(d1.year, d1.month, d1.day.toDouble()))]
            val d2Name = dayNamesShort[Julian.dayOfWeek(Julian.fromCalendar(d2.year, d2.month, d2.day.toDouble()))]
            val d3Name = dayNamesShort[Julian.dayOfWeek(Julian.fromCalendar(d3.year, d3.month, d3.day.toDouble()))]

            sb.append("""
                <table class="right-table">
                    <colgroup><col class="lat-col"><col span="6"></colgroup>
                    <thead>
                        <tr>
                            <th rowspan="2">Lat.</th>
                            <th colspan="3">Moonrise</th>
                            <th colspan="3">Moonset</th>
                        </tr>
                        <tr>
                            <th>$d1Name</th><th>$d2Name</th><th>$d3Name</th>
                            <th>$d1Name</th><th>$d2Name</th><th>$d3Name</th>
                        </tr>
                    </thead>
                    <tbody>
            """.trimIndent())

            // Helper: Find Moon transit (GHA crosses 0/360)
            fun moonTransit(dayData: EphemerisGenerator.DayEphemeris): Double {
                for (h in 0..22) {
                    val g1 = dayData.rows[h].moonGha
                    val g2 = dayData.rows[h + 1].moonGha
                    if (g1 > 300 && g2 < 60) {
                        val frac = (360.0 - g1) / (360.0 - g1 + g2)
                        return h.toDouble() + frac
                    }
                }
                return -1.0
            }

            // Helper: Interpolate Moon Dec from hourly data
            fun interpMoonDec(dayData: EphemerisGenerator.DayEphemeris, hour: Double): Double {
                val h = hour.coerceIn(0.0, 23.0)
                val hi = h.toInt().coerceIn(0, 22)
                val frac = h - hi
                return dayData.rows[hi].moonAppDec + (dayData.rows[hi + 1].moonAppDec - dayData.rows[hi].moonAppDec) * frac
            }

            // Helper: Interpolate Moon HP from hourly data
            fun interpMoonHP(dayData: EphemerisGenerator.DayEphemeris, hour: Double): Double {
                val h = hour.coerceIn(0.0, 23.0)
                val hi = h.toInt().coerceIn(0, 22)
                val frac = h - hi
                return dayData.rows[hi].moonHp + (dayData.rows[hi + 1].moonHp - dayData.rows[hi].moonHp) * frac
            }

            // Moonrise/Moonset: scan high-res ELP2000 data (10-min or 30-min intervals)
            fun findMoonEventHiRes(lat: Double, moonData: List<MoonPoint>, isRise: Boolean): Double {
                val l = Math.toRadians(lat)
                var prevAlt = -999.0
                var prevHour = 0.0
                for (mp in moonData) {
                    val h0 = 0.7275 * mp.hp - 0.5667
                    val dec = Math.toRadians(mp.dec)
                    val gha = Math.toRadians(mp.gha)
                    val sinAlt = Math.sin(l) * Math.sin(dec) + Math.cos(l) * Math.cos(dec) * Math.cos(gha)
                    val alt = Math.toDegrees(Math.asin(sinAlt))
                    if (prevAlt != -999.0) {
                        val prevH0 = h0 // approximately same h0
                        if (isRise && prevAlt <= prevH0 && alt > h0) {
                            val frac = (h0 - prevAlt) / (alt - prevAlt)
                            return prevHour + (mp.hour - prevHour) * frac
                        } else if (!isRise && prevAlt >= prevH0 && alt < h0) {
                            val frac = (h0 - prevAlt) / (alt - prevAlt)
                            return prevHour + (mp.hour - prevHour) * frac
                        }
                    }
                    prevAlt = alt
                    prevHour = mp.hour
                }
                return -1.0
            }

            // Fallback to hour-angle method if no high-res data
            fun moonRiseSetFallback(lat: Double, dayData: EphemerisGenerator.DayEphemeris): Pair<Double, Double> {
                val transit = moonTransit(dayData)
                if (transit < 0) return Pair(-1.0, -1.0)
                val hpT = interpMoonHP(dayData, transit)
                val h0 = 0.7275 * hpT - 0.5667
                val decT = interpMoonDec(dayData, transit)
                val H1 = computeH(lat, decT, h0)
                if (H1 < 0) return Pair(H1, H1)
                var rE = transit - H1; var sE = transit + H1
                // 2nd iteration
                val dR = interpMoonDec(dayData, rE.coerceIn(0.0, 23.0))
                val dS = interpMoonDec(dayData, sE.coerceIn(0.0, 23.0))
                val hR = computeH(lat, dR, 0.7275 * interpMoonHP(dayData, rE.coerceIn(0.0, 23.0)) - 0.5667)
                val hS = computeH(lat, dS, 0.7275 * interpMoonHP(dayData, sE.coerceIn(0.0, 23.0)) - 0.5667)
                rE = if (hR >= 0) transit - hR else -1.0
                sE = if (hS >= 0) transit + hS else -1.0
                return Pair(rE, sE)
            }

            for (lat in latitudes) {
                val results = mutableListOf<Double>()
                for (dayIdx in 0..2) {
                    if (moonHiRes.size > dayIdx && moonHiRes[dayIdx].isNotEmpty()) {
                        // Use high-res ELP data
                        val mr = findMoonEventHiRes(lat, moonHiRes[dayIdx], true)
                        val ms = findMoonEventHiRes(lat, moonHiRes[dayIdx], false)
                        results.add(mr)
                        results.add(ms)
                    } else {
                        // Fallback
                        val rs = moonRiseSetFallback(lat, daysArr[dayIdx])
                        results.add(rs.first)
                        results.add(rs.second)
                    }
                }
                val latStr = if (lat == 0.0) "0" else lat.toInt().toString()
                sb.append("<tr>")
                sb.append("<th>$latStr</th>")
                sb.append("<td>${formatHM(results[0])}</td>") // rise day1
                sb.append("<td>${formatHM(results[2])}</td>") // rise day2
                sb.append("<td>${formatHM(results[4])}</td>") // rise day3
                sb.append("<td>${formatHM(results[1])}</td>") // set day1
                sb.append("<td>${formatHM(results[3])}</td>") // set day2
                sb.append("<td>${formatHM(results[5])}</td>") // set day3
                sb.append("</tr>\n")
            }
            sb.append("</tbody></table>\n")

            // C) SUMMARY matching Nautical Almanac
            // Upper transit: Moon GHA crosses 0°/360°
            fun moonMerPassUpper(dayData: EphemerisGenerator.DayEphemeris): Double {
                for (h in 0..22) {
                    val g1 = dayData.rows[h].moonGha
                    val g2 = dayData.rows[h + 1].moonGha
                    if (g1 > 300 && g2 < 60) {
                        val frac = (360.0 - g1) / (360.0 - g1 + g2)
                        return h.toDouble() + frac
                    }
                }
                return -1.0
            }
            // Lower transit: Moon GHA crosses 180°
            fun moonMerPassLower(dayData: EphemerisGenerator.DayEphemeris): Double {
                for (h in 0..22) {
                    val g1 = dayData.rows[h].moonGha
                    val g2 = dayData.rows[h + 1].moonGha
                    // GHA crosses 180° (going from <180 to >180)
                    if (g1 < 180 && g2 >= 180) {
                        val frac = (180.0 - g1) / (g2 - g1)
                        return h.toDouble() + frac
                    }
                }
                return -1.0
            }

            fun fmtEqT(mins: Double): String {
                val totalSecs = kotlin.math.round(abs(mins) * 60.0).toInt()
                val m = totalSecs / 60
                val s = totalSecs % 60
                return String.format(Locale.US, "%02d:%02d", m, s)
            }

            sb.append("""
                <table class="right-table summary-table">
                    <colgroup><col class="lat-col"><col span="6"></colgroup>
                    <thead>
                        <tr>
                            <th rowspan="3"></th>
                            <th colspan="3">Sun</th>
                            <th colspan="3">Moon</th>
                        </tr>
                        <tr>
                            <th colspan="2">Eqn.of Time</th>
                            <th rowspan="2">Mer.<br>Pass</th>
                            <th colspan="2">Mer.Pass.</th>
                            <th rowspan="2">Age</th>
                        </tr>
                        <tr>
                            <th>00h</th>
                            <th>12h</th>
                            <th>Upper</th>
                            <th>Lower</th>
                        </tr>
                    </thead>
                    <tbody>
            """.trimIndent())

            val namesArr = arrayOf(d1Name, d2Name, d3Name)
            for (k in 0..2) {
                val dd = daysArr[k]
                val eq0 = fmtEqT(dd.rows[0].eqOfTimeMins)
                val eq12 = fmtEqT(dd.rows[12].eqOfTimeMins)
                val mpSunHours = 12.0 - dd.rows[12].eqOfTimeMins / 60.0
                val mpSunStr = formatHM(mpSunHours)

                val mpUpper = moonMerPassUpper(dd)
                val mpUpperStr = formatHM(mpUpper)
                val mpLower = moonMerPassLower(dd)
                val mpLowerStr = formatHM(mpLower)

                val jdCurr = Julian.fromCalendar(dd.year, dd.month, dd.day.toDouble())
                val k = LunarFunctions.approximateK(dd.year, dd.month, LunarFunctions.LunarPhase.NEW_MOON)
                var jdeNM = LunarFunctions.lunarPhaseJde(k)
                if (jdeNM > jdCurr) {
                    jdeNM = LunarFunctions.lunarPhaseJde(k - 1.0)
                }
                val ageDays = kotlin.math.floor(jdCurr - jdeNM).toInt()
                val illumPct = kotlin.math.round(dd.rows[12].moonIllumPercent).toInt()
                val ageStr = "${ageDays}(${illumPct}%)"

                sb.append("<tr>")
                sb.append("<th>${dd.day}</th>")
                sb.append("<td>$eq0</td>")
                sb.append("<td>$eq12</td>")
                sb.append("<td>$mpSunStr</td>")
                sb.append("<td>$mpUpperStr</td>")
                sb.append("<td>$mpLowerStr</td>")
                sb.append("<td>$ageStr</td>")
                sb.append("</tr>\n")
            }
            sb.append("</tbody></table>\n")
            sb.append("</div>\n</div>\n")
            sb.append("<div class=\"footer-brand\">@FalakPro LF PWNU Jawa Barat</div>\n")
        }

        sb.append("""
            </body>
            </html>
        """.trimIndent())

        return sb.toString()
    }
}

