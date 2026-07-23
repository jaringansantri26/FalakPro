package com.falak.falakpro.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falak.falakpro.location.LocationData
import com.falak.falakpro.premium.AstroAssetPreloader
import com.falak.falakpro.premium.CalendarFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Locale
import android.widget.NumberPicker
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog

private val AstroDarkGreenColor = Color(0xFF00897B)
private val AstroRed = Color(0xFFD00000)
private val AstroHeaderGreen = Color(0xFF0B6B35)
private val AstroGridLine = Color(0xFFCBE7E0)
private val AstroDim = Color(0xFF9CA3AF)
private val AstroOrange = Color(0xFFD97706)
private val AstroBrown = Color(0xFF9A3F0C)

private val ASTRO_DAY_ROWS = listOf(
    AstroDayLabel("AHAD", "Radite", AstroRed),
    AstroDayLabel("SENIN", "Soma", AstroDarkGreenColor),
    AstroDayLabel("SELASA", "Anggara", AstroDarkGreenColor),
    AstroDayLabel("RABU", "Buda", AstroDarkGreenColor),
    AstroDayLabel("KAMIS", "Wraspati", AstroDarkGreenColor),
    AstroDayLabel("JUMAT", "Sukra", Color(0xFF078A43)),
    AstroDayLabel("SABTU", "Tumpek", AstroDarkGreenColor)
)

private data class AstroDayLabel(
    val indonesia: String,
    val sunda: String,
    val color: Color
)

private data class AstroCalendarDay(
    val year: Int,
    val month: Int,
    val day: Int,
    val isCurrentMonth: Boolean
)



private enum class AstroMainCalendarSystem(
    val label: String
) {
    MASEHI("Masehi"),
    HIJRIYAH("Hijriyah"),
    SUNDA("Sunda"),
    JAWA("Jawa")
}

private data class AstroCalendarModel(
    val weekColumns: List<Array<AstroCalendarDay>>,
    val cellDataMap: Map<String, AstroCellData>
)

private data class AstroCellData(
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
    val sundaPasaran: String,
    val jawaPasaran: String,
    val wukuName: String,
    val dayName: String,
    val sundaDayName: String,
    val pranotoDay: Int,
    val pranotoName: String,
    val pranotoYear: Int,
    val isCurrentMonth: Boolean
)

private fun Int.toArabicDigitsAstro(): String {
    val arabic = arrayOf("٠", "١", "٢", "٣", "٤", "٥", "٦", "٧", "٨", "٩")
    return toString().map { if (it.isDigit()) arabic[it.digitToInt()] else it.toString() }.joinToString("")
}

private fun isAstroTodayDate(y: Int, m: Int, d: Int): Boolean {
    val today = Calendar.getInstance()
    return today.get(Calendar.YEAR) == y &&
            today.get(Calendar.MONTH) == m &&
            today.get(Calendar.DAY_OF_MONTH) == d
}

private fun isAstroSelectedDate(selected: Calendar, y: Int, m: Int, d: Int): Boolean {
    return selected.get(Calendar.YEAR) == y &&
            selected.get(Calendar.MONTH) == m &&
            selected.get(Calendar.DAY_OF_MONTH) == d
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AstronomicalCalendarScreen(
    onNavigateBack: () -> Unit = {},
    locationData: LocationData = LocationData()
) {
    val context = LocalContext.current
    var calendar by remember { mutableStateOf(Calendar.getInstance()) }
    var selectedDate by remember { mutableStateOf(Calendar.getInstance()) }

    var showIslamicHoly by remember { mutableStateOf(true) }
    var showNationalLib by remember { mutableStateOf(true) }
    var showSundaPasar by remember { mutableStateOf(true) }
    var hijriCriteria by remember { mutableStateOf("Mabims Baru") }
    var showSettings by remember { mutableStateOf(false) }
    var showMonthYearPicker by remember { mutableStateOf(false) }

    var mainCalendarSystem by remember {
        mutableStateOf(AstroMainCalendarSystem.MASEHI)
    }

    val year = calendar.get(Calendar.YEAR)
    val month = calendar.get(Calendar.MONTH)
    val tz = 7.0

    val selectedBaseYear = selectedDate.get(Calendar.YEAR)
    val selectedBaseMonth = selectedDate.get(Calendar.MONTH)
    val selectedBaseDay = selectedDate.get(Calendar.DAY_OF_MONTH)

    val calendarModel by produceState<AstroCalendarModel>(
        initialValue = AstroCalendarModel(emptyList(), emptyMap()),
        year, month, selectedBaseYear, selectedBaseMonth, selectedBaseDay,
        mainCalendarSystem, hijriCriteria
    ) {
        value = withContext(Dispatchers.Default) {
            AstroAssetPreloader.ensureCore(context)

            fun keyOf(y: Int, m0: Int, d: Int): String = "$y-${m0 + 1}-$d"

            val selectedCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, selectedBaseYear)
                set(Calendar.MONTH, selectedBaseMonth)
                set(Calendar.DAY_OF_MONTH, selectedBaseDay)
            }

            val broadStart = (selectedCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, -70)
            }

            val broadEnd = (selectedCal.clone() as Calendar).apply {
                add(Calendar.DAY_OF_MONTH, 70)
            }

            val jdCenter = CalendarFunctions.gregorianToJde(
                selectedBaseYear,
                selectedBaseMonth + 1,
                selectedBaseDay.toDouble()
            )

            val approx = CalendarFunctions.jdeToHijri(jdCenter)
            val estHY = approx.first
            val estHM = approx.second

            val anchors = mutableListOf<Pair<Triple<Int, Int, Int>, Double>>()
            for (off in -5..5) {
                var hy = estHY
                var hm = estHM + off
                while (hm > 12) {
                    hm -= 12
                    hy++
                }
                while (hm < 1) {
                    hm += 12
                    hy--
                }

                val startJde = CalendarFunctions.getStartJdeOfIndonesianHijriMonth(
                    hy, hm, hijriCriteria
                )

                anchors.add(Triple(hy, hm, 1) to startJde)
            }
            anchors.sortBy { it.second }

            fun buildCellData(cell: AstroCalendarDay): AstroCellData {
                val jde = CalendarFunctions.gregorianToJde(
                    cell.year,
                    cell.month + 1,
                    cell.day.toDouble()
                )

                var hY = 0
                var hM = 0
                var hD = 0

                CalendarFunctions.getHijriDateFromMonthAnchors(jde, tz, anchors)?.let {
                    hY = it.first
                    hM = it.second
                    hD = it.third
                }

                if (hY == 0) {
                    val fallback = CalendarFunctions.getIndonesianCalendarHijri(jde, hijriCriteria)
                    hY = fallback.first
                    hM = fallback.second
                    hD = fallback.third
                }

                val caka = CalendarFunctions.getCakaSundaCandra(jde)
                val jawa = CalendarFunctions.getJavaneseDate(jde)
                val pranoto = CalendarFunctions.getPranotoMongso(
                    cell.year,
                    cell.month + 1,
                    cell.day
                )
                val holiday = CalendarFunctions.getHoliday(
                    cell.year,
                    cell.month + 1,
                    cell.day,
                    hM,
                    hD,
                    jde
                )

                return AstroCellData(
                    hijriDay = hD,
                    hijriMonth = hM,
                    hijriYear = hY,
                    cakaDay = caka.day,
                    cakaIsPoek = caka.isPoek,
                    cakaMonth = caka.month,
                    cakaYear = caka.year,
                    jawaDay = jawa.third,
                    jawaMonth = jawa.second,
                    jawaYear = jawa.first,
                    holidayName = holiday,
                    sundaPasaran = CalendarFunctions.pasaranSundaName(jde, tz),
                    jawaPasaran = CalendarFunctions.pasaranName(jde),
                    wukuName = CalendarFunctions.getWukuName(jde),
                    dayName = CalendarFunctions.dayName(jde),
                    sundaDayName = CalendarFunctions.sundaDayName(jde),
                    pranotoDay = pranoto.day,
                    pranotoName = pranoto.name,
                    pranotoYear = pranoto.year,
                    isCurrentMonth = cell.isCurrentMonth
                )
            }

            val broadCells = mutableListOf<AstroCalendarDay>()
            val cursor = broadStart.clone() as Calendar

            while (!cursor.after(broadEnd)) {
                broadCells.add(
                    AstroCalendarDay(
                        year = cursor.get(Calendar.YEAR),
                        month = cursor.get(Calendar.MONTH),
                        day = cursor.get(Calendar.DAY_OF_MONTH),
                        isCurrentMonth = false
                    )
                )
                cursor.add(Calendar.DAY_OF_MONTH, 1)
            }

            val broadData = HashMap<String, AstroCellData>(broadCells.size)
            broadCells.forEach { cell ->
                broadData[keyOf(cell.year, cell.month, cell.day)] = buildCellData(cell)
            }

            val selectedKey = keyOf(selectedBaseYear, selectedBaseMonth, selectedBaseDay)
            val selectedData = broadData[selectedKey]

            val targetCells = broadCells.filter { cell ->
                val data = broadData[keyOf(cell.year, cell.month, cell.day)]

                when (mainCalendarSystem) {
                    AstroMainCalendarSystem.MASEHI ->
                        cell.year == year && cell.month == month

                    AstroMainCalendarSystem.HIJRIYAH ->
                        selectedData != null &&
                                data?.hijriDay != null &&
                                data.hijriMonth == selectedData.hijriMonth &&
                                data.hijriYear == selectedData.hijriYear

                    AstroMainCalendarSystem.SUNDA ->
                        selectedData != null &&
                                data?.cakaDay != null &&
                                data.cakaMonth == selectedData.cakaMonth &&
                                data.cakaYear == selectedData.cakaYear

                    AstroMainCalendarSystem.JAWA ->
                        selectedData != null &&
                                data?.jawaDay != null &&
                                data.jawaMonth == selectedData.jawaMonth &&
                                data.jawaYear == selectedData.jawaYear
                }
            }

            val safeTargetCells = targetCells.ifEmpty {
                broadCells.filter { it.year == year && it.month == month }
            }

            val firstMain = safeTargetCells.first()
            val lastMain = safeTargetCells.last()

            val firstCal = Calendar.getInstance().apply {
                set(firstMain.year, firstMain.month, firstMain.day)
            }

            val lastCal = Calendar.getInstance().apply {
                set(lastMain.year, lastMain.month, lastMain.day)
            }

            val gridStart = (firstCal.clone() as Calendar).apply {
                val dow = get(Calendar.DAY_OF_WEEK) - 1
                add(Calendar.DAY_OF_MONTH, -dow)
            }

            val gridEnd = (lastCal.clone() as Calendar).apply {
                val dow = get(Calendar.DAY_OF_WEEK) - 1
                add(Calendar.DAY_OF_MONTH, 6 - dow)
            }

            val targetKeys = safeTargetCells
                .map { keyOf(it.year, it.month, it.day) }
                .toSet()

            val result = HashMap<String, AstroCellData>(56)
            val columns = mutableListOf<Array<AstroCalendarDay>>()
            val gridCursor = gridStart.clone() as Calendar

            while (!gridCursor.after(gridEnd)) {
                val week = Array(7) {
                    val y0 = gridCursor.get(Calendar.YEAR)
                    val m0 = gridCursor.get(Calendar.MONTH)
                    val d0 = gridCursor.get(Calendar.DAY_OF_MONTH)
                    val key = keyOf(y0, m0, d0)

                    val cell = AstroCalendarDay(
                        year = y0,
                        month = m0,
                        day = d0,
                        isCurrentMonth = targetKeys.contains(key)
                    )

                    val data = broadData[key] ?: buildCellData(cell)
                    result[key] = data.copy(isCurrentMonth = cell.isCurrentMonth)

                    gridCursor.add(Calendar.DAY_OF_MONTH, 1)

                    cell
                }

                columns.add(week)
            }

            AstroCalendarModel(
                weekColumns = columns,
                cellDataMap = result
            )
        }
    }

    val weekColumns = calendarModel.weekColumns
    val cellDataMap = calendarModel.cellDataMap

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstData = cellDataMap["$year-${month + 1}-1"]
    val lastData = cellDataMap["$year-${month + 1}-$daysInMonth"]

    val selectedYear = selectedDate.get(Calendar.YEAR)
    val selectedMonth = selectedDate.get(Calendar.MONTH)
    val selectedDay = selectedDate.get(Calendar.DAY_OF_MONTH)
    val selectedHeaderData = cellDataMap["$selectedYear-${selectedMonth + 1}-$selectedDay"] ?: firstData

    val scroll = rememberScrollState()

    val astroBg = MaterialTheme.colorScheme.background
    val astroCellBg = MaterialTheme.colorScheme.surface
    val astroDarkGreen = MaterialTheme.colorScheme.onSurface

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(astroBg)
    ) {
        AstroHeader(
            calendar = calendar,
            weekColumns = weekColumns,
            cellDataMap = cellDataMap,
            firstData = firstData,
            lastData = lastData,
            selectedData = selectedHeaderData,
            mainCalendarSystem = mainCalendarSystem,
            onPrev = {
                val newDate = astroNavigateMainCalendar(
                    calendar = calendar,
                    selectedDate = selectedDate,
                    weekColumns = weekColumns,
                    cellDataMap = cellDataMap,
                    mainCalendarSystem = mainCalendarSystem,
                    forward = false
                )

                calendar = (newDate.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }

                selectedDate = newDate
            },
            onNext = {
                val newDate = astroNavigateMainCalendar(
                    calendar = calendar,
                    selectedDate = selectedDate,
                    weekColumns = weekColumns,
                    cellDataMap = cellDataMap,
                    mainCalendarSystem = mainCalendarSystem,
                    forward = true
                )

                calendar = (newDate.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }

                selectedDate = newDate
            },
            onSettings = { showSettings = true },
            onPickMonthYear = { showMonthYearPicker = true }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll)
                .padding(bottom = 4.dp)
        ) {
            AstroWukuHeader(weekColumns, cellDataMap)
            AstroCalendarGrid(
                weekColumns = weekColumns,
                cellDataMap = cellDataMap,
                selectedDate = selectedDate,
                mainCalendarSystem = mainCalendarSystem,
                showIslamicHoly = showIslamicHoly,
                showNationalLib = showNationalLib,
                showSundaPasar = showSundaPasar,
                onSelect = { selectedDate = it },
                astroDarkGreen = astroDarkGreen,
                astroCellBg = astroCellBg
            )
            AstroSelectedDetail(selectedDate, cellDataMap, astroDarkGreen)
        }
    }
    if (showMonthYearPicker) {
        AstroMonthYearPickerDialog(
            mainCalendarSystem = mainCalendarSystem,
            currentYear = astroPickerYear(
                calendar = calendar,
                selectedData = selectedHeaderData,
                mainCalendarSystem = mainCalendarSystem
            ),
            currentMonth = astroPickerMonth(
                calendar = calendar,
                selectedData = selectedHeaderData,
                mainCalendarSystem = mainCalendarSystem
            ),
            onDismiss = { showMonthYearPicker = false },
            onApply = { y, m ->
                val newDate = astroFindFirstDateOfMainMonth(
                    baseDate = selectedDate,
                    selectedYear = y,
                    selectedMonthZero = m,
                    mainCalendarSystem = mainCalendarSystem,
                    cellDataMap = cellDataMap
                )

                calendar = (newDate.clone() as Calendar).apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                }

                selectedDate = newDate
                showMonthYearPicker = false
            }
        )
    }

    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .padding(15.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Pengaturan Kalender", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = astroDarkGreen)
                AstroSettingToggle("Hari Besar Islam", showIslamicHoly) { showIslamicHoly = it }
                AstroSettingToggle("Hari Libur Nasional", showNationalLib) { showNationalLib = it }
                AstroSettingToggle("Pasaran Sunda", showSundaPasar) { showSundaPasar = it }
                HorizontalDivider()

                HorizontalDivider()

                Text(
                    "Tanggal Utama",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                AstroMainCalendarSystem.entries.forEach { system ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mainCalendarSystem = system },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = mainCalendarSystem == system,
                            onClick = { mainCalendarSystem = system }
                        )

                        Text(
                            system.label,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                val hijriCriteriaOptions = listOf(
                    "Mabims Baru",
                    "Mabims Lama",
                    "KGHT Turki",
                    "KGHT Muhammadiyah",
                    "Wujudul Hilal",
                    "LAPAN",
                    "ODEH",
                    "Danjon Limit"
                )

                Text(
                    "Kriteria Awal Bulan",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )

                hijriCriteriaOptions.forEach { criteria ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { hijriCriteria = criteria },
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = hijriCriteria == criteria, onClick = { hijriCriteria = criteria })
                        Text(criteria, fontSize = 14.sp, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}


@Composable
private fun AstroHeader(
    calendar: Calendar,
    weekColumns: List<Array<AstroCalendarDay>>,
    cellDataMap: Map<String, AstroCellData>,
    firstData: AstroCellData?,
    lastData: AstroCellData?,
    selectedData: AstroCellData?,
    mainCalendarSystem: AstroMainCalendarSystem,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSettings: () -> Unit,
    onPickMonthYear: () -> Unit
) {

    val headerRangeCells = remember(calendar, weekColumns, cellDataMap, selectedData, mainCalendarSystem) {
        astroHeaderRangeCells(
            calendar = calendar,
            weekColumns = weekColumns,
            cellDataMap = cellDataMap,
            selectedData = selectedData,
            mainCalendarSystem = mainCalendarSystem
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(AstroHeaderGreen)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // BULAN UTAMA
        Text(
            text = astroMainHeaderText(
                calendar = calendar,
                firstData = firstData,
                lastData = lastData,
                selectedData = selectedData,
                mainCalendarSystem = mainCalendarSystem
            ),
            modifier = Modifier.clickable { onPickMonthYear() },
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            textAlign = TextAlign.Center,
            maxLines = 1
        )

        Spacer(Modifier.height(2.dp))


        if (mainCalendarSystem != AstroMainCalendarSystem.MASEHI) {
            Text(
                text = astroMasehiRangeFromCells(headerRangeCells),
                color = Color.White.copy(alpha = 0.74f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        if (mainCalendarSystem != AstroMainCalendarSystem.HIJRIYAH) {
            Text(
                text = astroHijriRangeFromCells(headerRangeCells),
                color = Color.White.copy(alpha = 0.74f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        if (mainCalendarSystem != AstroMainCalendarSystem.SUNDA) {
            Text(
                text = astroCakaRangeFromCells(headerRangeCells),
                color = Color.White.copy(alpha = 0.74f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        if (mainCalendarSystem != AstroMainCalendarSystem.JAWA) {
            Text(
                text = astroJawaRangeFromCells(headerRangeCells),
                color = Color.White.copy(alpha = 0.74f),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }

        Spacer(Modifier.height(6.dp))


        // NAVIGASI + SETTINGS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = onPrev) {
                Icon(
                    Icons.Default.ChevronLeft,
                    contentDescription = "Bulan sebelumnya",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onSettings) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Pengaturan",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            IconButton(onClick = onNext) {
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Bulan berikutnya",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}



private data class AstroHeaderRangeCell(
    val date: AstroCalendarDay,
    val data: AstroCellData
)

private fun astroHeaderRangeCells(
    calendar: Calendar,
    weekColumns: List<Array<AstroCalendarDay>>,
    cellDataMap: Map<String, AstroCellData>,
    selectedData: AstroCellData?,
    mainCalendarSystem: AstroMainCalendarSystem
): List<AstroHeaderRangeCell> {
    // UNIVERSAL SMART RANGE:
    // Berlaku untuk semua sistem utama: Masehi, Hijriyah, Sunda, dan Jawa.
    // Semua subtitle dihitung dari tanggal yang benar-benar masuk
    // ke bulan kalender utama yang sedang dipilih.
    // Jadi subtitle hanya dibuat rentang kalau memang berubah bulan/tahun.
    val allCells = weekColumns
        .flatMap { week -> week.toList() }
        .mapNotNull { day ->
            val data = cellDataMap["${day.year}-${day.month + 1}-${day.day}"]
            if (data == null) null else AstroHeaderRangeCell(day, data)
        }

    if (allCells.isEmpty()) return emptyList()

    val filtered = when (mainCalendarSystem) {
        AstroMainCalendarSystem.MASEHI -> {
            val y = calendar.get(Calendar.YEAR)
            val m = calendar.get(Calendar.MONTH)
            allCells.filter { it.date.year == y && it.date.month == m }
        }

        AstroMainCalendarSystem.HIJRIYAH -> {
            val selected = selectedData
            if (selected == null) emptyList()
            else allCells.filter {
                it.data.hijriMonth == selected.hijriMonth &&
                        it.data.hijriYear == selected.hijriYear
            }
        }

        AstroMainCalendarSystem.SUNDA -> {
            val selected = selectedData
            if (selected == null) emptyList()
            else allCells.filter {
                it.data.cakaMonth == selected.cakaMonth &&
                        it.data.cakaYear == selected.cakaYear
            }
        }

        AstroMainCalendarSystem.JAWA -> {
            val selected = selectedData
            if (selected == null) emptyList()
            else allCells.filter {
                it.data.jawaMonth == selected.jawaMonth &&
                        it.data.jawaYear == selected.jawaYear
            }
        }
    }

    return filtered.ifEmpty { allCells }
}

private fun astroMasehiRangeFromCells(cells: List<AstroHeaderRangeCell>): String {
    if (cells.isEmpty()) return "Memuat Masehi..."

    val first = cells.first().date
    val last = cells.last().date

    val firstCal = Calendar.getInstance().apply { set(first.year, first.month, first.day) }
    val lastCal = Calendar.getInstance().apply { set(last.year, last.month, last.day) }

    val m1 = firstCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id", "ID"))
        ?.replaceFirstChar { it.uppercase() } ?: ""
    val m2 = lastCal.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id", "ID"))
        ?.replaceFirstChar { it.uppercase() } ?: ""

    val y1 = first.year
    val y2 = last.year

    return when {
        first.month == last.month && y1 == y2 -> "$m1 $y1 M"
        y1 == y2 -> "$m1 – $m2 $y1 M"
        else -> "$m1 $y1 – $m2 $y2 M"
    }
}

private fun astroHijriRangeFromCells(cells: List<AstroHeaderRangeCell>): String {
    if (cells.isEmpty()) return "Memuat Hijriyah..."
    val first = cells.first().data
    val last = cells.last().data

    val m1 = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(first.hijriMonth - 1) { "" }
    val m2 = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(last.hijriMonth - 1) { "" }

    return when {
        first.hijriMonth == last.hijriMonth && first.hijriYear == last.hijriYear ->
            "$m1 ${first.hijriYear} H"
        first.hijriYear == last.hijriYear ->
            "$m1 – $m2 ${first.hijriYear} H"
        else ->
            "$m1 ${first.hijriYear} – $m2 ${last.hijriYear} H"
    }
}

private fun astroCakaRangeFromCells(cells: List<AstroHeaderRangeCell>): String {
    if (cells.isEmpty()) return "Memuat Caka Sunda..."
    val first = cells.first().data
    val last = cells.last().data

    val m1 = CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse(first.cakaMonth - 1) { "" }
    val m2 = CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse(last.cakaMonth - 1) { "" }

    return when {
        first.cakaMonth == last.cakaMonth && first.cakaYear == last.cakaYear ->
            "$m1 ${first.cakaYear} CS"
        first.cakaYear == last.cakaYear ->
            "$m1 – $m2 ${first.cakaYear} CS"
        else ->
            "$m1 ${first.cakaYear} – $m2 ${last.cakaYear} CS"
    }
}

private fun astroJawaRangeFromCells(cells: List<AstroHeaderRangeCell>): String {
    if (cells.isEmpty()) return "Memuat Jawa..."
    val first = cells.first().data
    val last = cells.last().data

    val m1 = CalendarFunctions.JAVANESE_MONTH_NAMES.getOrElse(first.jawaMonth - 1) { "" }
    val m2 = CalendarFunctions.JAVANESE_MONTH_NAMES.getOrElse(last.jawaMonth - 1) { "" }

    return when {
        first.jawaMonth == last.jawaMonth && first.jawaYear == last.jawaYear ->
            "$m1 ${first.jawaYear} Jawa"
        first.jawaYear == last.jawaYear ->
            "$m1 – $m2 ${first.jawaYear} Jawa"
        else ->
            "$m1 ${first.jawaYear} – $m2 ${last.jawaYear} Jawa"
    }
}



private fun astroNavigateMainCalendar(
    calendar: Calendar,
    selectedDate: Calendar,
    weekColumns: List<Array<AstroCalendarDay>>,
    cellDataMap: Map<String, AstroCellData>,
    mainCalendarSystem: AstroMainCalendarSystem,
    forward: Boolean
): Calendar {

    fun keyOf(cal: Calendar): String {
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.MONTH) + 1}-${cal.get(Calendar.DAY_OF_MONTH)}"
    }

    fun keyOf(day: AstroCalendarDay): String {
        return "${day.year}-${day.month + 1}-${day.day}"
    }

    if (mainCalendarSystem == AstroMainCalendarSystem.MASEHI) {
        return (calendar.clone() as Calendar).apply {
            add(Calendar.MONTH, if (forward) 1 else -1)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    val selectedData = cellDataMap[keyOf(selectedDate)]
        ?: return selectedDate

    val visibleMainCells = weekColumns
        .flatMap { it.toList() }
        .filter { cell ->
            val data = cellDataMap[keyOf(cell)] ?: return@filter false

            when (mainCalendarSystem) {
                AstroMainCalendarSystem.HIJRIYAH ->
                    data.hijriMonth == selectedData.hijriMonth &&
                            data.hijriYear == selectedData.hijriYear

                AstroMainCalendarSystem.SUNDA ->
                    data.cakaMonth == selectedData.cakaMonth &&
                            data.cakaYear == selectedData.cakaYear

                AstroMainCalendarSystem.JAWA ->
                    data.jawaMonth == selectedData.jawaMonth &&
                            data.jawaYear == selectedData.jawaYear

                AstroMainCalendarSystem.MASEHI ->
                    false
            }
        }
        .sortedWith(
            compareBy<AstroCalendarDay> { it.year }
                .thenBy { it.month }
                .thenBy { it.day }
        )

    val anchorCell = if (forward) {
        visibleMainCells.lastOrNull()
    } else {
        visibleMainCells.firstOrNull()
    }

    if (anchorCell != null) {
        return Calendar.getInstance().apply {
            set(Calendar.YEAR, anchorCell.year)
            set(Calendar.MONTH, anchorCell.month)
            set(Calendar.DAY_OF_MONTH, anchorCell.day)
            add(Calendar.DAY_OF_MONTH, if (forward) 1 else -1)
        }
    }

    // fallback kasar kalau data belum siap
    return (selectedDate.clone() as Calendar).apply {
        add(Calendar.DAY_OF_MONTH, if (forward) 30 else -30)
    }
}


private fun astroMainHeaderText(
    calendar: Calendar,
    firstData: AstroCellData?,
    lastData: AstroCellData?,
    selectedData: AstroCellData?,
    mainCalendarSystem: AstroMainCalendarSystem
): String {

    val year = calendar.get(Calendar.YEAR)

    val monthName = calendar.getDisplayName(
        Calendar.MONTH,
        Calendar.LONG,
        Locale("id", "ID")
    )?.replaceFirstChar { it.uppercase() } ?: ""

    return when (mainCalendarSystem) {

        AstroMainCalendarSystem.MASEHI ->
            "$monthName $year M"

        AstroMainCalendarSystem.HIJRIYAH -> {
            val data = selectedData ?: firstData
            if (data == null) {
                "Memuat Hijriyah..."
            } else {
                val m = CalendarFunctions.HIJRI_MONTH_NAMES
                    .getOrElse(data.hijriMonth - 1) { "" }

                "$m ${data.hijriYear} H"
            }
        }

        AstroMainCalendarSystem.SUNDA -> {
            val data = selectedData ?: firstData
            if (data == null) {
                "Memuat Sunda..."
            } else {
                val m = CalendarFunctions.CAKA_SUNDA_MONTHS
                    .getOrElse(data.cakaMonth - 1) { "" }

                "$m ${data.cakaYear} CS"
            }
        }

        AstroMainCalendarSystem.JAWA -> {
            val data = selectedData ?: firstData
            if (data == null) {
                "Memuat Jawa..."
            } else {
                val m = CalendarFunctions.JAVANESE_MONTH_NAMES
                    .getOrElse(data.jawaMonth - 1) { "" }

                "$m ${data.jawaYear} Jawa"
            }
        }
    }
}

private fun astroMasehiRangeCompact(calendar: Calendar): String {

    val currentMonth = calendar.getDisplayName(
        Calendar.MONTH,
        Calendar.LONG,
        Locale("id", "ID")
    )?.replaceFirstChar { it.uppercase() } ?: ""

    val nextCal = (calendar.clone() as Calendar).apply {
        add(Calendar.MONTH, 1)
    }

    val nextMonth = nextCal.getDisplayName(
        Calendar.MONTH,
        Calendar.LONG,
        Locale("id", "ID")
    )?.replaceFirstChar { it.uppercase() } ?: ""

    val year = calendar.get(Calendar.YEAR)

    return if (currentMonth == nextMonth) {
        "$currentMonth $year M"
    } else {
        "$currentMonth – $nextMonth $year M"
    }
}


private fun astroHijriRangeCompact(first: AstroCellData?, last: AstroCellData?): String {
    if (first == null || last == null) return "Memuat Hijriyah..."
    val m1 = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(first.hijriMonth - 1) { "" }
    val m2 = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(last.hijriMonth - 1) { "" }

    return when {
        first.hijriMonth == last.hijriMonth && first.hijriYear == last.hijriYear ->
            "$m1 ${first.hijriYear} H"

        first.hijriYear == last.hijriYear ->
            "$m1 – $m2 ${first.hijriYear} H"

        else ->
            "$m1 ${first.hijriYear} – $m2 ${last.hijriYear} H"
    }
}

private fun astroCakaRangeCompact(first: AstroCellData?, last: AstroCellData?): String {
    if (first == null || last == null) return "Memuat Caka Sunda..."
    val m1 = CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse(first.cakaMonth - 1) { "" }
    val m2 = CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse(last.cakaMonth - 1) { "" }

    return when {
        first.cakaMonth == last.cakaMonth && first.cakaYear == last.cakaYear ->
            "$m1 ${first.cakaYear} CS"

        first.cakaYear == last.cakaYear ->
            "$m1 – $m2 ${first.cakaYear} CS"

        else ->
            "$m1 ${first.cakaYear} – $m2 ${last.cakaYear} CS"
    }
}

private fun astroJawaRangeCompact(first: AstroCellData?, last: AstroCellData?): String {
    if (first == null || last == null) return "Memuat Jawa..."
    val m1 = CalendarFunctions.JAVANESE_MONTH_NAMES.getOrElse(first.jawaMonth - 1) { "" }
    val m2 = CalendarFunctions.JAVANESE_MONTH_NAMES.getOrElse(last.jawaMonth - 1) { "" }

    return when {
        first.jawaMonth == last.jawaMonth && first.jawaYear == last.jawaYear ->
            "$m1 ${first.jawaYear} Jawa"

        first.jawaYear == last.jawaYear ->
            "$m1 – $m2 ${first.jawaYear} Jawa"

        else ->
            "$m1 ${first.jawaYear} – $m2 ${last.jawaYear} Jawa"
    }
}
@Composable
private fun AstroWukuHeader(
    weekColumns: List<Array<AstroCalendarDay>>,
    cellDataMap: Map<String, AstroCellData>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(38.dp)
            .background(AstroHeaderGreen.copy(alpha = 0.88f))
    ) {
        Box(
            modifier = Modifier
                .width(56.dp)
                .fillMaxHeight()
                .border(0.6.dp, AstroGridLine),
            contentAlignment = Alignment.Center
        ) {
            Text("WUKU", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
        weekColumns.forEach { week ->
            val sun = week[0]
            val wuku = cellDataMap["${sun.year}-${sun.month + 1}-${sun.day}"]?.wukuName.orEmpty()
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(0.6.dp, AstroGridLine),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = wuku,
                    color = Color.White,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun AstroCalendarGrid(
    weekColumns: List<Array<AstroCalendarDay>>,
    cellDataMap: Map<String, AstroCellData>,
    selectedDate: Calendar,
    mainCalendarSystem: AstroMainCalendarSystem,
    showIslamicHoly: Boolean,
    showNationalLib: Boolean,
    showSundaPasar: Boolean,
    onSelect: (Calendar) -> Unit,
    astroDarkGreen: Color,
    astroCellBg: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        for (dow in 0..6) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(65.dp)
            ) {
                AstroDayNameCell(ASTRO_DAY_ROWS[dow])

                weekColumns.forEach { week ->
                    val cDay = week[dow]
                    val data = cellDataMap["${cDay.year}-${cDay.month + 1}-${cDay.day}"]
                    val isLib = data?.holidayName != null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) {
                        AstroAlmanacDayCell(
                            masehi = cDay.day,
                            hijriDay = data?.hijriDay ?: 0,
                            cakaDay = data?.cakaDay ?: 0,
                            cakaIsPoek = data?.cakaIsPoek ?: false,
                            sundaPasaran = if (showSundaPasar) data?.sundaPasaran.orEmpty() else "",
                            jawaDay = data?.jawaDay ?: 0,
                            jawaPasaran = data?.jawaPasaran.orEmpty(),
                            mainCalendarSystem = mainCalendarSystem,
                            isSunday = dow == 0,
                            isHoliday = isLib && ((data?.holidayName?.contains("Idul") == true && showIslamicHoly) || showNationalLib),
                            isToday = isAstroTodayDate(cDay.year, cDay.month, cDay.day),
                            isSelected = isAstroSelectedDate(selectedDate, cDay.year, cDay.month, cDay.day),
                            isCurrentMonth = cDay.isCurrentMonth,
                            isLoading = data == null,
                            onClick = {
                                val selected = Calendar.getInstance()
                                selected.set(cDay.year, cDay.month, cDay.day)
                                onSelect(selected)
                            },
                            astroDarkGreen = astroDarkGreen,
                            astroCellBg = astroCellBg
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AstroDayNameCell(label: AstroDayLabel) {
    Box(
        modifier = Modifier
            .width(56.dp)
            .fillMaxHeight()
            .background(label.color)
            .border(0.6.dp, AstroGridLine),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label.indonesia, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(label.sunda, color = Color.White.copy(alpha = 0.86f), fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
        }
    }
}

@Composable
private fun AstroAlmanacDayCell(
    masehi: Int,
    hijriDay: Int,
    cakaDay: Int,
    cakaIsPoek: Boolean,
    sundaPasaran: String,
    jawaDay: Int,
    jawaPasaran: String,
    mainCalendarSystem: AstroMainCalendarSystem,
    isSunday: Boolean,
    isHoliday: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isCurrentMonth: Boolean,
    isLoading: Boolean = false,
    onClick: () -> Unit,
    astroDarkGreen: Color,
    astroCellBg: Color
) {
    val baseMainColor = when {
        !isCurrentMonth -> AstroDim.copy(alpha = 0.3f)
        isSunday || isHoliday -> AstroRed
        else -> astroDarkGreen
    }
    val baseSecColor = when {
        !isCurrentMonth -> AstroDim.copy(alpha = 0.3f)
        isSunday || isHoliday -> AstroRed
        else -> astroDarkGreen
    }
    val bgColor = when {
        isSelected -> astroDarkGreen.copy(alpha = 0.13f)
        isToday -> astroDarkGreen.copy(alpha = 0.08f)
        else -> astroCellBg
    }
    val borderMod = when {
        isSelected -> Modifier.border(1.5.dp, astroDarkGreen, RoundedCornerShape(2.dp))
        isToday -> Modifier.border(1.dp, astroDarkGreen.copy(alpha = 0.35f), RoundedCornerShape(2.dp))
        else -> Modifier.border(0.5.dp, AstroGridLine)
    }

    val cakaColor = if (cakaIsPoek) Color(0xFF6B7280) else AstroOrange

    val sundaSymbol = if (cakaIsPoek) "k" else "s"

    val mainText = when (mainCalendarSystem) {
        AstroMainCalendarSystem.MASEHI -> masehi.toString()
        AstroMainCalendarSystem.HIJRIYAH -> if (hijriDay > 0) hijriDay.toArabicDigitsAstro() else ""
        AstroMainCalendarSystem.SUNDA -> if (cakaDay > 0) "${cakaDay}${sundaSymbol}" else ""
        AstroMainCalendarSystem.JAWA -> if (jawaDay > 0) jawaDay.toString() else ""
    }

    val topLeftText = when (mainCalendarSystem) {
        AstroMainCalendarSystem.SUNDA,
        AstroMainCalendarSystem.JAWA -> masehi.toString()

        else -> ""
    }

    val topRightText = when (mainCalendarSystem) {
        AstroMainCalendarSystem.MASEHI -> if (hijriDay > 0) hijriDay.toArabicDigitsAstro() else ""
        AstroMainCalendarSystem.HIJRIYAH -> masehi.toString()
        AstroMainCalendarSystem.SUNDA -> if (hijriDay > 0) hijriDay.toArabicDigitsAstro() else ""
        AstroMainCalendarSystem.JAWA -> if (hijriDay > 0) hijriDay.toArabicDigitsAstro() else ""
    }

    val firstBottomText = when (mainCalendarSystem) {
        AstroMainCalendarSystem.MASEHI,
        AstroMainCalendarSystem.HIJRIYAH -> {
            if (cakaDay > 0) "${cakaDay}${sundaSymbol} ${sundaPasaran.uppercase()}" else ""
        }

        AstroMainCalendarSystem.SUNDA -> sundaPasaran.uppercase()

        AstroMainCalendarSystem.JAWA -> jawaPasaran.uppercase()
    }

    val firstBottomColor = when (mainCalendarSystem) {
        AstroMainCalendarSystem.JAWA -> AstroBrown
        else -> cakaColor
    }

    val secondBottomText = when (mainCalendarSystem) {
        AstroMainCalendarSystem.MASEHI,
        AstroMainCalendarSystem.HIJRIYAH,
        AstroMainCalendarSystem.SUNDA -> {
            if (jawaDay > 0) "$jawaDay ${jawaPasaran.uppercase()}" else ""
        }

        AstroMainCalendarSystem.JAWA -> {
            if (cakaDay > 0) "${cakaDay}${sundaSymbol} ${sundaPasaran.uppercase()}" else ""
        }
    }

    val secondBottomColor = when (mainCalendarSystem) {
        AstroMainCalendarSystem.JAWA -> cakaColor
        else -> AstroBrown
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .then(borderMod)
            .clickable { onClick() }
    ) {

        if (isCurrentMonth && !isLoading && topLeftText.isNotBlank()) {
            Text(
                text = topLeftText,
                color = baseSecColor,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 3.dp, top = 2.dp),
                maxLines = 1
            )
        }

        if (isCurrentMonth && !isLoading && topRightText.isNotBlank()) {
            Text(
                text = topRightText,
                color = baseSecColor,
                fontSize = 9.sp,
                fontFamily = FontFamily.Default,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 3.dp, top = 2.dp),
                maxLines = 1
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 1.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            Spacer(Modifier.height(6.dp))

            Text(
                text = mainText,
                color = baseMainColor,
                fontSize = 20.sp,
                fontFamily = if (mainCalendarSystem == AstroMainCalendarSystem.HIJRIYAH) FontFamily.Default else null,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            if (isCurrentMonth && !isLoading) {
                Text(
                    text = firstBottomText,
                    color = firstBottomColor,
                    fontSize = 6.8.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    lineHeight = 7.sp
                )

                Text(
                    text = secondBottomText,
                    color = secondBottomColor,
                    fontSize = 6.6.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    lineHeight = 7.sp
                )
            }
        }

        if (isToday) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .align(Alignment.BottomStart)
                    .background(AstroRed, CircleShape)
            )
        }
    }
}

@Composable
private fun AstroSelectedDetail(
    selectedDate: Calendar,
    cellDataMap: Map<String, AstroCellData>,
    astroDarkGreen: Color
) {
    val sy = selectedDate.get(Calendar.YEAR)
    val sm0 = selectedDate.get(Calendar.MONTH)
    val sd = selectedDate.get(Calendar.DAY_OF_MONTH)
    val sel = cellDataMap["$sy-${sm0 + 1}-$sd"]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(astroDarkGreen),
                contentAlignment = Alignment.Center
            ) {
                Text(sd.toString(), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.width(10.dp))
            if (sel == null) {
                Text("Menghitung...", fontSize = 12.sp, color = AstroDim)
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        "${sel.dayName} (${sel.sundaDayName}) ${sel.sundaPasaran}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = astroDarkGreen
                    )
                    val masehiMonth = selectedDate.getDisplayName(Calendar.MONTH, Calendar.LONG, Locale("id", "ID")) ?: ""
                    val hijriMonth = CalendarFunctions.HIJRI_MONTH_NAMES.getOrElse(sel.hijriMonth - 1) { "" }
                    Text(
                        "$sd $masehiMonth $sy  •  ${sel.hijriDay.toArabicDigitsAstro()} $hijriMonth ${sel.hijriYear.toArabicDigitsAstro()} H",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Default,
                        color = Color(0xFF64748B)
                    )
                    val jawaMonth = CalendarFunctions.JAVANESE_MONTH_NAMES.getOrElse(sel.jawaMonth - 1) { "" }
                    val cakaMonth = CalendarFunctions.CAKA_SUNDA_MONTHS.getOrElse(sel.cakaMonth - 1) { "" }
                    Text("Jawa: ${sel.jawaDay} $jawaMonth ${sel.jawaYear} ${sel.jawaPasaran}", fontSize = 12.sp, color = AstroBrown, fontWeight = FontWeight.Bold)
                    Text("Caka: ${sel.cakaDay} $cakaMonth ${sel.cakaYear} ${sel.sundaPasaran}", fontSize = 12.sp, color = astroDarkGreen, fontWeight = FontWeight.Bold)
                    Text("Saka: ${sel.pranotoDay} ${sel.pranotoName} ${sel.pranotoYear}", fontSize = 12.sp, color = AstroOrange)
                    sel.holidayName?.let {
                        Text(it, fontSize = 11.sp, color = AstroRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AstroSettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
private fun astroPickerYear(
    calendar: Calendar,
    selectedData: AstroCellData?,
    mainCalendarSystem: AstroMainCalendarSystem
): Int {
    return when (mainCalendarSystem) {
        AstroMainCalendarSystem.MASEHI -> calendar.get(Calendar.YEAR)
        AstroMainCalendarSystem.HIJRIYAH -> selectedData?.hijriYear ?: calendar.get(Calendar.YEAR)
        AstroMainCalendarSystem.SUNDA -> selectedData?.cakaYear ?: calendar.get(Calendar.YEAR)
        AstroMainCalendarSystem.JAWA -> selectedData?.jawaYear ?: calendar.get(Calendar.YEAR)
    }
}

private fun astroPickerMonth(
    calendar: Calendar,
    selectedData: AstroCellData?,
    mainCalendarSystem: AstroMainCalendarSystem
): Int {
    return when (mainCalendarSystem) {
        AstroMainCalendarSystem.MASEHI -> calendar.get(Calendar.MONTH)
        AstroMainCalendarSystem.HIJRIYAH -> (selectedData?.hijriMonth ?: 1) - 1
        AstroMainCalendarSystem.SUNDA -> (selectedData?.cakaMonth ?: 1) - 1
        AstroMainCalendarSystem.JAWA -> (selectedData?.jawaMonth ?: 1) - 1
    }.coerceIn(0, 11)
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AstroMonthYearPickerDialog(
    mainCalendarSystem: AstroMainCalendarSystem,
    currentYear: Int,
    currentMonth: Int,
    onDismiss: () -> Unit,
    onApply: (Int, Int) -> Unit
) {
    var selectedYear by remember { mutableStateOf(currentYear) }
    var selectedMonth by remember { mutableStateOf(currentMonth.coerceIn(0, 11)) }

    val title = when (mainCalendarSystem) {
        AstroMainCalendarSystem.MASEHI -> "INPUT MASEHI"
        AstroMainCalendarSystem.HIJRIYAH -> "INPUT HIJRIYAH"
        AstroMainCalendarSystem.SUNDA -> "INPUT SUNDA"
        AstroMainCalendarSystem.JAWA -> "INPUT JAWA"
    }

    val monthNames = when (mainCalendarSystem) {
        AstroMainCalendarSystem.MASEHI -> listOf(
            "Januari", "Februari", "Maret", "April", "Mei", "Juni",
            "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        )
        AstroMainCalendarSystem.HIJRIYAH -> CalendarFunctions.HIJRI_MONTH_NAMES
        AstroMainCalendarSystem.SUNDA -> CalendarFunctions.CAKA_SUNDA_MONTHS
        AstroMainCalendarSystem.JAWA -> CalendarFunctions.JAVANESE_MONTH_NAMES
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = Color(0xFF0F6F68),
            tonalElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF0B837C))
                        .padding(vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(MaterialTheme.colorScheme.surface),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AstroNumberWheel(
                        modifier = Modifier.weight(1f),
                        value = selectedYear,
                        minValue = currentYear - 120,
                        maxValue = currentYear + 120,
                        displayedValues = null,
                        onValueChange = { selectedYear = it }
                    )

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(Color.DarkGray.copy(alpha = 0.6f))
                    )

                    AstroNumberWheel(
                        modifier = Modifier.weight(1f),
                        value = selectedMonth,
                        minValue = 0,
                        maxValue = 11,
                        displayedValues = monthNames.toTypedArray(),
                        onValueChange = { selectedMonth = it }
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    Button(
                        onClick = { onApply(selectedYear, selectedMonth) },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("SET", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.LightGray,
                            contentColor = Color.Black
                        )
                    ) {
                        Text("BATAL", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AstroNumberWheel(
    modifier: Modifier = Modifier,
    value: Int,
    minValue: Int,
    maxValue: Int,
    displayedValues: Array<String>?,
    onValueChange: (Int) -> Unit
) {
    AndroidView(
        modifier = modifier.fillMaxHeight(),
        factory = { context ->
            NumberPicker(context).apply {
                wrapSelectorWheel = false
                descendantFocusability = NumberPicker.FOCUS_BLOCK_DESCENDANTS
            }
        },
        update = { picker ->
            picker.minValue = minValue
            picker.maxValue = maxValue
            picker.displayedValues = null
            picker.displayedValues = displayedValues
            picker.value = value.coerceIn(minValue, maxValue)
            picker.setOnValueChangedListener { _, _, newVal ->
                onValueChange(newVal)
            }
        }
    )
}
private fun astroFindFirstDateOfMainMonth(
    baseDate: Calendar,
    selectedYear: Int,
    selectedMonthZero: Int,
    mainCalendarSystem: AstroMainCalendarSystem,
    cellDataMap: Map<String, AstroCellData>
): Calendar {
    if (mainCalendarSystem == AstroMainCalendarSystem.MASEHI) {
        return (baseDate.clone() as Calendar).apply {
            set(Calendar.YEAR, selectedYear)
            set(Calendar.MONTH, selectedMonthZero)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    }

    // Cari di cellDataMap (biasanya mencakup +/- 70 hari)
    val target = cellDataMap.entries.find { (_, data) ->
        when (mainCalendarSystem) {
            AstroMainCalendarSystem.HIJRIYAH ->
                data.hijriYear == selectedYear && data.hijriMonth == selectedMonthZero + 1 && data.hijriDay == 1
            AstroMainCalendarSystem.SUNDA ->
                data.cakaYear == selectedYear && data.cakaMonth == selectedMonthZero + 1 && data.cakaDay == 1
            AstroMainCalendarSystem.JAWA ->
                data.jawaYear == selectedYear && data.jawaMonth == selectedMonthZero + 1 && data.jawaDay == 1
            else -> false
        }
    }

    if (target != null) {
        val parts = target.key.split("-")
        return Calendar.getInstance().apply {
            set(parts[0].toInt(), parts[1].toInt() - 1, parts[2].toInt())
        }
    }

    // Fallback universal: konversi langsung ke Gregorian tanpa bergantung cellDataMap
    val jde = when (mainCalendarSystem) {
        AstroMainCalendarSystem.HIJRIYAH ->
            CalendarFunctions.hijriToJde(selectedYear, selectedMonthZero + 1, 1)
        AstroMainCalendarSystem.SUNDA ->
            CalendarFunctions.cakaSundaToJde(selectedYear, selectedMonthZero + 1)
        AstroMainCalendarSystem.JAWA ->
            CalendarFunctions.javaToJde(selectedYear, selectedMonthZero + 1)
        else -> return (baseDate.clone() as Calendar)
    }

    val greg = CalendarFunctions.jdeToGregorian(jde)
    return Calendar.getInstance().apply {
        set(greg.first, greg.second - 1, greg.third.toInt())
    }
}

