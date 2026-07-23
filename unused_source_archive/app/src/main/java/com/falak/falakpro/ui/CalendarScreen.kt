package com.falak.falakpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.falak.falakpro.location.LocationData
import com.falak.falakpro.premium.AstroAssetPreloader
import com.falak.falakpro.premium.CalendarFunctions
import com.falak.falakpro.premium.ElpDataProvider
import com.falak.falakpro.premium.Vsop87SolarEngine
import com.falak.falakpro.premium.HilalEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.floor
import kotlin.math.round

private val DarkGreenColor = Color(0xFF2E574B)
private val RedSun    = Color(0xFFCC0000)
private val HeaderBg  = Color(0xFF2E574B)
private val DimGray   = Color(0xFF9CA3AF)

private val DAY_ROWS = listOf(
    "AHAD"   to "RADITE",
    "SENIN"  to "SOMA",
    "SELASA" to "ANGGARA",
    "RABU"   to "BUDA",
    "KAMIS"  to "RESPATI",
    "JUM'AT" to "SUKRA",
    "SABTU"  to "TUMPEK"
)

private fun isTodayDate(y: Int, m: Int, d: Int): Boolean {
    val t = Calendar.getInstance()
    return t.get(Calendar.YEAR) == y && t.get(Calendar.MONTH) == m && t.get(Calendar.DAY_OF_MONTH) == d
}
private fun isSelDate(sel: Calendar, y: Int, m: Int, d: Int) =
    sel.get(Calendar.YEAR) == y && sel.get(Calendar.MONTH) == m && sel.get(Calendar.DAY_OF_MONTH) == d

private fun Int.toArabic(): String {
    val ar = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
    return this.toString().map { if (it.isDigit()) ar[it.digitToInt()] else it.toString() }.joinToString("")
}

data class CalendarDay(
    val year: Int,
    val month: Int, // 0-based
    val day: Int,
    val isCurrentMonth: Boolean
)

/** Semua data astronomi yang dihitung background untuk satu sel */
data class CellData(
    val hijriDay: Int,
    val hijriMonth: Int,
    val hijriYear: Int,
    val cakaDay: Int,
    val cakaIsPoek: Boolean,
    val cakaMonth: Int,
    val cakaYear: Int,
    val jawaDay: Int,
    val jawaMonth: Int,
    val jawaYear: Int,
    val holidayName: String?,
    val pasaran: String,
    val wukuName: String,
    val dayName: String,
    val sundaDayName: String,
    val pranotoDay: Int,
    val pranotoName: String,
    val pranotoYear: Int,
    val isCurrentMonth: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    onNavigateBack: () -> Unit = {},
    locationData: LocationData = LocationData()
) {
    val context = LocalContext.current
    var calendar     by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    var showIslamicHoly by remember { mutableStateOf(true) }
    var showNationalLib by remember { mutableStateOf(true) }
    var showSundaPasar  by remember { mutableStateOf(true) }
    var hijriCriteria   by remember { mutableStateOf("Mabims Baru") } 
    var showSettings    by remember { mutableStateOf(false) }

    val year  = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)

    val lat  = locationData.latitude
    val lon  = locationData.longitude
    val elev = locationData.altitude
    val tz   = 7.0

    val weekColumns: List<Array<CalendarDay>> = remember(year, month) {
        val c = calendar.clone() as Calendar
        c.set(Calendar.DAY_OF_MONTH, 1)
        val firstDow = c.get(Calendar.DAY_OF_WEEK) - 1
        val gridStart = c.clone() as Calendar
        gridStart.add(Calendar.DAY_OF_MONTH, -firstDow)
        val cols = mutableListOf<Array<CalendarDay>>()
        repeat(6) {
            val week = Array(7) {
                val day = CalendarDay(
                    year = gridStart.get(Calendar.YEAR),
                    month = gridStart.get(Calendar.MONTH),
                    day = gridStart.get(Calendar.DAY_OF_MONTH),
                    isCurrentMonth = gridStart.get(Calendar.MONTH) == month
                )
                gridStart.add(Calendar.DAY_OF_MONTH, 1)
                day
            }
            if (week.any { it.isCurrentMonth } || cols.size < 4) cols.add(week)
        }
        cols
    }

    // Optimize: Snap location to 0.1 degree and 10m elevation to prevent recalculation on every GPS jitter
    val snappedLat  = remember(lat)  { round(lat * 10) / 10.0 }
    val snappedLon  = remember(lon)  { round(lon * 10) / 10.0 }
    val snappedElev = remember(elev) { round(elev / 10.0) * 10.0 }

    val cellDataMap by produceState<Map<String, CellData>>(
        initialValue = emptyMap(), year, month, snappedLat, snappedLon, snappedElev, hijriCriteria
    ) {
        value = withContext(Dispatchers.Default) {
            AstroAssetPreloader.ensureCore(context)

            val result = HashMap<String, CellData>(42)

            // 2. LOGIKA JANGKAR (ANCHOR) - Diperluas ke -2..2 untuk keamanan grid
            val jdCenter = CalendarFunctions.gregorianToJde(year, month + 1, 15.0)
            val approx   = CalendarFunctions.jdeToHijri(jdCenter)
            val estHY    = approx.first
            val estHM    = approx.second
            
            val anchors = mutableListOf<Pair<Triple<Int, Int, Int>, Double>>() // ((Y,M,D), StartJd)
            for (off in -2..2) {
                var ty = estHY; var tm = estHM + off
                while (tm > 12) { tm -= 12; ty++ }
                while (tm < 1) { tm += 12; ty-- }
                val sJde = CalendarFunctions.getStartJdeOfHijriMonth(ty, tm, snappedLat, snappedLon, snappedElev, tz, hijriCriteria)
                anchors.add(Triple(ty, tm, 1) to sJde)
            }
            anchors.sortBy { it.second } 

            for (week in weekColumns) {
                for (cell in week) {
                    val jde = CalendarFunctions.gregorianToJde(cell.year, cell.month + 1, cell.day.toDouble())
                    
                    // Tentukan tanggal Hijriah dari awal hari lokal agar anchor KHGT tidak geser.
                    var hY = 0; var hM = 0; var hD = 0
                    CalendarFunctions.getHijriDateFromMonthAnchors(jde, tz, anchors)?.let {
                        hY = it.first
                        hM = it.second
                        hD = it.third
                    }
                    if (hY == 0) { // Deep fallback
                        val fb = CalendarFunctions.getCorrectedHijri(jde, snappedLat, snappedLon, snappedElev, tz)
                        hY=fb.first; hM=fb.second; hD=fb.third
                    }

                    val caka    = CalendarFunctions.getCakaSundaCandra(jde)
                    val jawa    = CalendarFunctions.getJavaneseDate(jde)
                    // Update: Kirim 'jde' untuk deteksi libur siklus (Paskah, Galungan, dsb)
                    val holiday = CalendarFunctions.getHoliday(cell.year, cell.month + 1, cell.day, hM, hD, jde)
                    val pasaran = CalendarFunctions.pasaranSundaName(jde)
                    val wuku    = CalendarFunctions.getWukuName(jde)
                    val dName   = CalendarFunctions.dayName(jde)
                    val sName   = CalendarFunctions.sundaDayName(jde)
                    val pranoto = CalendarFunctions.getPranotoMongso(cell.year, cell.month + 1, cell.day)
                    
                    val key = "${cell.year}-${cell.month + 1}-${cell.day}"
                    result[key] = CellData(
                        hijriDay = hD, hijriMonth = hM, hijriYear = hY,
                        cakaDay = caka.day, cakaIsPoek = caka.isPoek,
                        cakaMonth = caka.month, cakaYear = caka.year,
                        jawaDay = jawa.third, jawaMonth = jawa.second, jawaYear = jawa.first,
                        holidayName = holiday,
                        pasaran = pasaran, wukuName = wuku,
                        dayName = dName, sundaDayName = sName,
                        pranotoDay = pranoto.day, pranotoName = pranoto.name, pranotoYear = pranoto.year,
                        isCurrentMonth = cell.isCurrentMonth
                    )
                }
            }
            result
        }
    }

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstData   = cellDataMap["$year-${month + 1}-1"]
    val lastData    = cellDataMap["$year-${month + 1}-$daysInMonth"]

    val hijriMonthLeft  = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse((firstData?.hijriMonth ?: 1) - 1) { "" }
    val hijriMonthRight = if (firstData != null && lastData != null && firstData.hijriMonth != lastData.hijriMonth)
        CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(lastData.hijriMonth - 1) { "" } else null
    val cakaMonthLeft   = CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse((firstData?.cakaMonth ?: 1) - 1) { "" }
    val cakaMonthRight  = if (firstData != null && lastData != null && firstData.cakaMonth != lastData.cakaMonth)
        CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse(lastData.cakaMonth - 1) { "" } else null

    val subHeader = buildString {
        append("\u200E") // LTR mark to prevent Bidi RTL reversing
        if (firstData != null) append("$hijriMonthLeft ${firstData.hijriYear} H")
        if (hijriMonthRight != null && lastData != null) append(" - $hijriMonthRight ${lastData.hijriYear} H")
    }
    val subHeaderCaka = buildString {
        append("\u200E") // LTR mark
        if (firstData != null) append("$cakaMonthLeft ${firstData.cakaYear}")
        if (cakaMonthRight != null && lastData != null) append(" - $cakaMonthRight ${lastData.cakaYear}")
    }

    val bgGreen = MaterialTheme.colorScheme.background
    val cellBg = MaterialTheme.colorScheme.surface
    val darkGreenText = MaterialTheme.colorScheme.onSurface

    Column(Modifier.fillMaxSize().background(bgGreen)) {

        Column(
            Modifier.fillMaxWidth().background(HeaderBg).padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    val c = calendar.clone() as Calendar; c.add(Calendar.MONTH, -1); calendar = c
                }) { Icon(Icons.Default.ChevronLeft, null, tint = Color.White) }

                Box(Modifier.weight(1f).padding(horizontal = 4.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        val monthNameGreg = calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id","ID"))
                            ?.replaceFirstChar { it.uppercase() } ?: ""
                        Text("$monthNameGreg $year", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        
                        var hRange = ""
                        var cRange = ""
                        if (cellDataMap.isNotEmpty()) {
                            val first = cellDataMap.values.filter { it.isCurrentMonth }.firstOrNull()
                            val last = cellDataMap.values.filter { it.isCurrentMonth }.lastOrNull()
                            if (first != null && last != null) {
                                // Hijri
                                val h1 = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(first.hijriMonth-1) { "" }
                                val h2 = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(last.hijriMonth-1) { "" }
                                hRange = if (first.hijriMonth == last.hijriMonth) "\u200E$h1 ${first.hijriYear} H"
                                         else "\u200E$h1 - $h2 ${last.hijriYear} H"
                                
                                // Caka Sunda
                                val c1 = CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse(first.cakaMonth-1) { "" }
                                val c2 = CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse(last.cakaMonth-1) { "" }
                                cRange = if (first.cakaMonth == last.cakaMonth) "\u200E$c1 ${first.cakaYear} C"
                                         else "\u200E$c1 - $c2 ${last.cakaYear} C"
                            }
                        }
                        if (hRange.isNotEmpty()) {
                            Text(hRange, color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
                        }
                        if (cRange.isNotEmpty()) {
                            Text(cRange, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, maxLines = 2, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
                        }
                    }
                }

                IconButton(onClick = { showSettings = true }) {
                    Icon(Icons.Default.Settings, "Settings", tint = Color.White)
                }
                
                IconButton(onClick = {
                    val next = (calendar.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    calendar = next
                }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", tint = Color.White)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().background(HeaderBg.copy(alpha = 0.85f)).padding(horizontal = 1.dp),
            horizontalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Box(Modifier.width(52.dp).padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
                Text("WUKU", color = Color.White, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
            for (w in weekColumns.indices) {
                val sun  = weekColumns[w][0]
                val wuku = cellDataMap["${sun.year}-${sun.month + 1}-${sun.day}"]?.wukuName ?: ""
                Box(Modifier.weight(1f).padding(vertical = 3.dp), contentAlignment = Alignment.Center) {
                    Text(wuku.uppercase(), color = Color.White, fontSize = 6.sp,
                        fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }

        Column(Modifier.weight(1f).padding(horizontal = 1.dp).padding(bottom = 1.dp)) {
            for (dow in 0..6) {
                Row(
                    Modifier.fillMaxWidth().weight(1f).padding(top = if (dow == 0) 2.dp else 1.dp),
                    horizontalArrangement = Arrangement.spacedBy(1.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isSun = dow == 0
                    Box(
                        Modifier.width(52.dp).fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSun) RedSun else HeaderBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(DAY_ROWS[dow].first, color = Color.White, fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center)
                            Text(DAY_ROWS[dow].second, color = Color.White.copy(alpha = 0.8f),
                                fontSize = 6.5.sp, textAlign = TextAlign.Center)
                        }
                    }

                    for (w in weekColumns.indices) {
                        val cDay = weekColumns[w][dow]
                        val key  = "${cDay.year}-${cDay.month + 1}-${cDay.day}"
                        val data = cellDataMap[key]
                        Box(Modifier.weight(1f).fillMaxHeight()) {
                            val isLib = data?.holidayName != null
                            val isIslamHoliday = data?.holidayName?.let {
                                it.contains("Idulfitri") || it.contains("Iduladha") ||
                                it.contains("Maulid") || it.contains("Isra") || it.contains("Muharam")
                            } == true
                            DayCell(
                                masehi         = cDay.day,
                                hijriDay       = data?.hijriDay ?: 0,
                                cakaDay        = data?.cakaDay ?: 0,
                                cakaIsPoek     = data?.cakaIsPoek ?: false,
                                pasaran        = if (showSundaPasar) (data?.pasaran ?: "") else "",
                                isSunday       = isSun,
                                isHoliday      = isLib && ((isIslamHoliday && showIslamicHoly) || (!isIslamHoliday && showNationalLib)),
                                isToday        = isTodayDate(cDay.year, cDay.month, cDay.day),
                                isSelected     = isSelDate(selectedDate, cDay.year, cDay.month, cDay.day),
                                isCurrentMonth = cDay.isCurrentMonth,
                                isLoading      = data == null,
                                onClick        = {
                                    val s = Calendar.getInstance()
                                    s.set(cDay.year, cDay.month, cDay.day)
                                    selectedDate = s
                                },
                                darkGreenText = darkGreenText,
                                cellBg = cellBg
                            )
                        }
                    }
                }
            }
        }

        val sy     = selectedDate.get(Calendar.YEAR)
        val sm     = selectedDate.get(Calendar.MONTH) + 1
        val sd     = selectedDate.get(Calendar.DAY_OF_MONTH)
        val selKey = "$sy-$sm-$sd"
        val sel    = cellDataMap[selKey]

        Card(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(50.dp).clip(RoundedCornerShape(10.dp)).background(HeaderBg),
                    contentAlignment = Alignment.Center
                ) {
                    Text(sd.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(10.dp))
                if (sel == null) {
                    Text("Menghitung...", fontSize = 12.sp, color = DimGray)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("${sel.dayName} (${sel.sundaDayName}) ${sel.pasaran}",
                            fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = darkGreenText)
                        val mStr = "$sd ${selectedDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id","ID"))} $sy"
                        val hStr = "${sel.hijriDay.toArabic()} ${CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(sel.hijriMonth - 1) { "" }} ${sel.hijriYear.toArabic()} H"
                        Text("$mStr  •  $hStr", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        
                        val jStr = "Jawa: ${sel.jawaDay} ${CalendarFunctions.JAVANESE_MONTH_NAMES.getOrElse(sel.jawaMonth-1){""}} ${sel.jawaYear}  •  Caka Sunda: ${sel.cakaDay} ${CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse(sel.cakaMonth-1){""}} ${sel.cakaYear} (${CalendarFunctions.getCakaSundaYearName(sel.cakaYear)})"
                        Text(jStr, fontSize = 10.sp, color = darkGreenText, fontWeight = FontWeight.Medium)
                        
                        Text("Saka Sunda: ${sel.pranotoDay} ${sel.pranotoName} ${sel.pranotoYear}", fontSize = 10.sp, color = Color(0xFFB45309))
                        
                        if (sel.holidayName != null) {
                            val isIslam = sel.holidayName.contains("Idulfitri") ||
                                sel.holidayName.contains("Iduladha") ||
                                sel.holidayName.contains("Maulid") ||
                                sel.holidayName.contains("Isra") ||
                                sel.holidayName.contains("Muharam")
                            if ((isIslam && showIslamicHoly) || (!isIslam && showNationalLib)) {
                                Text(sel.holidayName, fontSize = 10.sp, color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        if (showSettings) {
            ModalBottomSheet(
                onDismissRequest = { showSettings = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(Modifier.padding(20.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Pengaturan Kalender", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = darkGreenText)
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Tampilan", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        SettingToggle("Hari Besar Islam", showIslamicHoly) { showIslamicHoly = it }
                        SettingToggle("Hari Libur Nasional", showNationalLib) { showNationalLib = it }
                        SettingToggle("Pasaran Sunda", showSundaPasar) { showSundaPasar = it }
                    }
                    
                    HorizontalDivider()
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Kriteria Awal Bulan (Hisab)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
                        val crits = listOf("Mabims Baru", "Mabims Lama", "KGHT Turki", "KGHT Muhammadiyah", "Wujudul Hilal", "LAPAN", "ODEH", "Danjon Limit")
                        crits.forEach { crit ->
                            Row(
                                Modifier.fillMaxWidth().clickable { hijriCriteria = crit },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = hijriCriteria == crit, onClick = { hijriCriteria = crit })
                                Text(crit, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                            }
                        }
                    }
                    Spacer(Modifier.height(20.dp))
                }
            }
        }
    }
}

@Composable
fun SettingToggle(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun DayCell(
    masehi: Int, hijriDay: Int, cakaDay: Int, cakaIsPoek: Boolean,
    pasaran: String, isSunday: Boolean, isHoliday: Boolean, isToday: Boolean,
    isSelected: Boolean, isCurrentMonth: Boolean,
    isLoading: Boolean = false, onClick: () -> Unit,
    darkGreenText: Color, cellBg: Color
) {
    val masehiColor = when {
        !isCurrentMonth -> DimGray.copy(alpha = 0.3f)
        isSunday || isHoliday -> RedSun
        else -> darkGreenText
    }
    val hijriColor = when {
        !isCurrentMonth -> DimGray.copy(alpha = 0.3f)
        isSunday || isHoliday -> RedSun
        else -> darkGreenText
    }
    val cakaColor      = if (cakaIsPoek) Color(0xFF6B7280) else Color(0xFFD97706)
    val finalCakaColor = if (isCurrentMonth) cakaColor else cakaColor.copy(alpha = 0.3f)

    val bgColor = when {
        isSelected -> darkGreenText.copy(alpha = 0.13f)
        isToday    -> darkGreenText.copy(alpha = 0.08f)
        else       -> cellBg
    }
    val borderMod = when {
        isSelected -> Modifier.border(1.5.dp, darkGreenText, RoundedCornerShape(2.dp))
        isToday    -> Modifier.border(1.dp, darkGreenText.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
        else       -> Modifier
    }

    Box(
        Modifier.fillMaxSize().clip(RoundedCornerShape(2.dp))
            .background(bgColor).then(borderMod).clickable { onClick() }
    ) {
        Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            // Hijriyah (atas kanan)
            Box(
                Modifier.fillMaxWidth().weight(0.30f).padding(horizontal = 3.dp),
                contentAlignment = Alignment.TopEnd
            ) {
                if (isCurrentMonth && !isLoading && hijriDay > 0)
                    Text(
                        hijriDay.toArabic(),
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Default,
                        color = hijriColor,
                        fontWeight = FontWeight.Bold
                    )
            }
            // Masehi (tengah)
            Box(Modifier.fillMaxWidth().weight(0.40f), contentAlignment = Alignment.Center) {
                Text(masehi.toString(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = masehiColor)
            }
            // Caka + Pasaran (bawah)
            Box(Modifier.fillMaxWidth().weight(0.30f), contentAlignment = Alignment.TopCenter) {
                if (isCurrentMonth && !isLoading && cakaDay > 0) {
                    val sym = if (cakaIsPoek) "k" else "s"
                    Text("${cakaDay}${sym} ${pasaran.uppercase()}",
                        fontSize = 8.sp, fontWeight = FontWeight.ExtraBold,
                        color = finalCakaColor, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        }
        // Titik merah hari ini
        if (isToday)
            Box(Modifier.size(5.dp).align(Alignment.TopStart).background(RedSun, CircleShape))
    }
}

