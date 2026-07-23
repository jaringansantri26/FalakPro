package com.falak.falakpro.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.random.Random
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.truncate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificCalculatorScreen(
    onNavigateBack: () -> Unit
) {
    var expression by remember { mutableStateOf("") }
    var cursorPos  by remember { mutableStateOf(0) }
    var resultText by remember { mutableStateOf("0") }
    var errorText  by remember { mutableStateOf<String?>(null) }
    var degreesMode by remember { mutableStateOf(true) }
    var memory by remember { mutableDoubleStateOf(0.0) }
    var ans    by remember { mutableDoubleStateOf(0.0) }
    val history = remember { mutableStateListOf<CalculatorHistory>() }
    var showHelp by remember { mutableStateOf(false) }
    var shiftActive by remember { mutableStateOf(false) }

    // ----- cursor-aware insert / delete -----
    fun insertAt(text: String) {
        val pos = cursorPos.coerceIn(0, expression.length)
        expression = expression.substring(0, pos) + text + expression.substring(pos)
        cursorPos = pos + text.length
        errorText = null
    }
    fun backspaceAt() {
        val pos = cursorPos.coerceIn(0, expression.length)
        if (pos > 0) {
            expression = expression.removeRange(pos - 1, pos)
            cursorPos = pos - 1
        }
    }
    fun moveCursorLeft()  { cursorPos = (cursorPos - 1).coerceAtLeast(0) }
    fun moveCursorRight() { cursorPos = (cursorPos + 1).coerceAtMost(expression.length) }

    // ----- evaluasi -----
    fun evaluateCurrent() {
        val raw = expression.trim()
        if (raw.isEmpty()) return
        val result = runCatching {
            ScientificExpressionEvaluator(degreesMode = degreesMode, ans = ans, memory = memory)
                .evaluate(raw)
        }
        result.fold(
            onSuccess = { value ->
                if (value.isNaN() || value.isInfinite()) {
                    resultText = "Math ERROR"
                    errorText = null
                } else {
                    ans = value
                    resultText = formatCalculatorNumber(value)
                    errorText = null
                    history.add(0, CalculatorHistory(raw, resultText))
                    while (history.size > 12) history.removeAt(history.lastIndex)
                }
            },
            onFailure = { error ->
                val message = error.message ?: "Input belum valid"
                val complexResult = complexInverseTrigResult(raw, degreesMode)
                if (message == "Math ERROR" && complexResult != null) {
                    resultText = complexResult
                    errorText = null
                    history.add(0, CalculatorHistory(raw, resultText))
                    while (history.size > 12) history.removeAt(history.lastIndex)
                } else if (message == "Math ERROR") {
                    resultText = "Math ERROR"
                    errorText = null
                } else {
                    errorText = message
                }
            }
        )
    }

    // ----- DMS: decimal → D° M' S" -----
    fun convertToDMS() {
        val value = ans
        val sign = if (value < 0) "-" else ""
        val absVal = kotlin.math.abs(value)
        var d = absVal.toInt()
        val mFull = (absVal - d) * 60.0
        var m = mFull.toInt()
        var s = (mFull - m) * 60.0
        // Carry-over: round to 4 decimal places first, then check boundary
        val sRounded = kotlin.math.round(s * 10000.0) / 10000.0
        if (sRounded >= 60.0) {
            s = 0.0
            m += 1
            if (m >= 60) {
                m = 0
                d += 1
            }
        } else {
            s = sRounded
        }
        resultText = "$sign${d}° ${m}' ${String.format(java.util.Locale.US, "%.4f", s)}\""
    }

    // ----- HMS: decimal → H h M m S s -----
    fun convertToHMS() {
        val value = ans
        val sign = if (value < 0) "-" else ""
        val absVal = kotlin.math.abs(value)
        var h = absVal.toInt()
        val mFull = (absVal - h) * 60.0
        var m = mFull.toInt()
        var s = (mFull - m) * 60.0
        // Carry-over: round to 4 decimal places first, then check boundary
        val sRounded = kotlin.math.round(s * 10000.0) / 10000.0
        if (sRounded >= 60.0) {
            s = 0.0
            m += 1
            if (m >= 60) {
                m = 0
                h += 1
            }
        } else {
            s = sRounded
        }
        resultText = "$sign${h}j ${m}m ${String.format(java.util.Locale.US, "%.4f", s)}d"
    }

    fun append(text: String) = insertAt(text)
    fun replaceWith(text: String) {
        expression = text
        cursorPos = text.length
        errorText = null
    }

    val palette = fxPalette()

    // ----- Help Dialog -----
    if (showHelp) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showHelp = false },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showHelp = false }) {
                    Text("TUTUP", fontWeight = FontWeight.Bold)
                }
            },
            title = { Text("Panduan Lengkap Fungsi Tombol", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val guides = listOf(
                        "◄ / ► (Navigasi Kursor)" to "Geser kursor ke kiri atau kanan untuk menyisipkan angka atau fungsi.\nContoh: Ketik 'sin(30)' lalu tekan ◄ untuk mengubah angka di dalam kurung.",
                        "SHIFT (Fungsi Sekunder)" to "Aktifkan fungsi berlabel warna BIRU / EMAS di atas tombol.\nContoh: Tekan SHIFT lalu sin → menghasilkan sin⁻¹ (asin).",
                        "DEG / RAD (Mode Sudut)" to "Ubah mode antara DEG (Derajat) atau RAD (Radian) untuk fungsi trigonometri.\nContoh: Dalam mode DEG, sin(30) = 0.5.",
                        "sin, cos, tan" to "Fungsi trigonometri utama (sinus, kosinus, tangen).\nContoh: sin(30) = 0.5 (dalam mode DEG).",
                        "sin⁻¹, cos⁻¹, tan⁻¹ (SHIFT)" to "Invers trigonometri (Arcus). Menghasilkan sudut.\nContoh: asin(0.5) = 30°.",
                        "atan2 (Arctangent 2 Argumen)" to "Arctangent 2 Argumen atan2(y, x). Sangat penting untuk perhitungan Azimut Kiblat & arah kuadran presisi (0°–360°).\nContoh: atan2(y, x).",
                        "DMS & →DMS (Derajat Menit Detik)" to "Format dms(D, M, S) & Konversi desimal → DMS.\nContoh: dms(6, 18, 48) = 6.31333°. Tekan →DMS untuk mengubah 6.31333 kembali ke 6° 18' 48.00\".",
                        "HMS & →HMS (Jam Menit Detik)" to "Format hms(H, M, S) & Konversi desimal → HMS.\nContoh: hms(21, 30, 0) = 21.5 Jam. Tekan →HMS untuk mengubah 21.5 kembali ke 21j 30m 00s.",
                        "JD (Julian Day)" to "Hitung Nilai Julian Day dari tanggal: jd(Tahun, Bulan, Tanggal).\nContoh: jd(2026, 6, 6) = 2461198.5.",
                        "ΔT (Delta T - SHIFT JD)" to "Nilai Koreksi Selisih Waktu WUD/UT: dt(JD).\nContoh: dt(2461198.5) = 69.1 detik.",
                        "Norm & Norm- (Normalisasi Sudut)" to "Norm = Normalisasi sudut 0° s.d. 360°: norm360(sudut).\nNorm- = Normalisasi -180° s.d. +180°: norm180(sudut).\nContoh: norm360(-30) = 330°, norm180(200) = -160°.",
                        "x², x³, xʸ, √x, ³√x" to "Pangkat & Akar. x² (kuadrat), x³ (kubik), xʸ (pangkat y), √x (akar kuadrat), ³√x (akar kubik).\nContoh: 5^2 = 25, 2^3 = 8, 2^10 = 1024, sqrt(16) = 4, cbrt(27) = 3.",
                        "Log, Ln, 10ˣ, eˣ" to "Logaritma basis 10 log(x), natural ln(x), dan Eksponensial.\nContoh: log(100) = 2, ln(e) = 1, 10^2 = 100, e^1 = 2.71828.",
                        "Abs & Mod" to "Abs = Nilai Mutlak abs(x). Mod = Sisa Bagi mod(a, b).\nContoh: abs(-15) = 15, mod(10, 3) = 1.",
                        "Rnd & Int (Pembulatan)" to "Rnd = Pembulatan Normal (≥0.5 keatas, <0.5 kebawah): round(x).\nInt = Ambil bagian bulat saja: int(x).\nContoh: round(5.6) = 6, round(5.4) = 5, int(12.78) = 12.",
                        "Floor & Ceil (Batas Pembulatan)" to "Floor = Pembulatan kebawah: floor(x).\nCeil = Pembulatan keatas: ceil(x).\nContoh: floor(3.9) = 3, ceil(3.1) = 4.",
                        "nPr & nCr" to "Permutasi npr(n, r) & Kombinasi ncr(n, r).\nContoh: npr(5, 2) = 20, ncr(5, 2) = 10.",
                        "GCD & LCM" to "GCD = FPB (Faktor Persekutuan Terbesar) gcd(a, b).\nLCM = KPK (Kelipatan Persekutuan Terkecil) lcm(a, b).\nContoh: gcd(12, 18) = 6, lcm(4, 6) = 12.",
                        "Pol & Rec (Koordinat)" to "Pol = Konversi Rectangular (x,y) → Polar radius: pol(x, y).\nRec = Konversi Polar (r,θ) → Rectangular x: rec(r, θ).\nContoh: pol(3, 4) = 5.",
                        "% & ! (Persen & Faktorial)" to "% = Persen (x/100).\n! = Faktorial (n!).\nContoh: 50% = 0.5, 5! = 120.",
                        "π & e (Konstanta)" to "Konstanta Pi (π ≈ 3.14159) dan Euler (e ≈ 2.71828).",
                        "Ans & Memori (MS, MR, M+, M-, MC)" to "Ans = Hasil perhitungan terakhir.\nMS = Simpan memori. MR = Panggil memori.\nM+ / M- = Tambah / Kurang memori. MC = Hapus memori."
                    )
                    items(guides.size) { i ->
                        val (key, desc) = guides[i]
                        Column {
                            Text(
                                key,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                desc,
                                fontSize = 12.sp,
                                lineHeight = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (i < guides.size - 1) {
                                androidx.compose.material3.HorizontalDivider(
                                    modifier = Modifier.padding(top = 8.dp),
                                    thickness = 0.5.dp
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    Scaffold(
        containerColor = palette.pageBg
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .background(palette.pageBg)
        ) {
            FxCalculatorDisplay(
                palette = palette,
                expression = expression,
                cursorPos = cursorPos,
                resultText = resultText,
                errorText = errorText,
                degreesMode = degreesMode,
                memory = memory,
                shiftActive = shiftActive,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.20f)
            )
            FxCalculatorToolbar(
                palette = palette,
                onNavigateBack = onNavigateBack,
                degreesMode = degreesMode,
                onToggleMode = { degreesMode = !degreesMode },
                onShowHelp = { showHelp = true },
                shiftActive = shiftActive,
                onToggleShift = { shiftActive = !shiftActive }
            )
            FxCalculatorKeypad(
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.80f),
                shiftActive = shiftActive,
                onShiftUsed = { shiftActive = false },
                onInsert = ::insertAt,
                onEvaluate = ::evaluateCurrent,
                onClear = {
                    expression = ""
                    cursorPos = 0
                    resultText = "0"
                    errorText = null
                },
                onBackspace = ::backspaceAt,
                onCursorLeft = ::moveCursorLeft,
                onCursorRight = ::moveCursorRight,
                onMemoryAdd = {
                    evaluateCurrent()
                    memory += ans
                },
                onMemorySubtract = {
                    evaluateCurrent()
                    memory -= ans
                },
                onMemoryStore = {
                    evaluateCurrent()
                    memory = ans
                },
                onMemoryClear = { memory = 0.0 },
                onMemoryRecall = { append("M") },
                onUseAns = { insertAt("Ans") },
                onConvertToDMS = ::convertToDMS,
                onConvertToHMS = ::convertToHMS
            )
        }
    }
}

@Composable
private fun fxPalette(): FxPalette {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    return if (dark) {
        FxPalette(
            pageBg = Color(0xFF13171F),
            displayBg = Color(0xFF0D1117),
            toolbarBg = Color(0xFF1F2430),
            keypadBg = Color(0xFF13171F),
            keyBg = Color(0xFF282C37),
            keyNumBg = Color(0xFF3B404D),
            keyOpBg = Color(0xFF212631),
            keyActionBg = Color(0xFF282C37),
            keyClearBg = Color(0xFF991B1B),
            keyEqualsBg = Color(0xFF1D4ED8),
            keyBorder = Color(0xFF0F1218),
            primaryText = Color(0xFFF3F4F6),
            numberText = Color(0xFFFFFFFF),
            displayText = Color(0xFF38BDF8),
            statusText = Color(0xFF94A3B8),
            operatorText = Color(0xFF60A5FA),
            buttonBorder = Color(0xFF475569),
            shiftText = Color(0xFF60A5FA),
            shiftBg = Color(0xFF2563EB)
        )
    } else {
        FxPalette(
            pageBg = Color(0xFFDDE1E7),
            displayBg = Color(0xFFE2E8F0),
            toolbarBg = Color(0xFFCBD5E1),
            keypadBg = Color(0xFFDDE1E7),
            keyBg = Color(0xFF334155),
            keyNumBg = Color(0xFFFFFFFF),
            keyOpBg = Color(0xFF475569),
            keyActionBg = Color(0xFF334155),
            keyClearBg = Color(0xFFDC2626),
            keyEqualsBg = Color(0xFF2563EB),
            keyBorder = Color(0xFFCBD5E1),
            primaryText = Color(0xFFFFFFFF),
            numberText = Color(0xFF0F172A),
            displayText = Color(0xFF0F172A),
            statusText = Color(0xFF475569),
            operatorText = Color(0xFFFFFFFF),
            buttonBorder = Color(0xFF94A3B8),
            shiftText = Color(0xFF1E40AF),
            shiftBg = Color(0xFF1D4ED8)
        )
    }
}

private data class FxPalette(
    val pageBg: Color,
    val displayBg: Color,
    val toolbarBg: Color,
    val keypadBg: Color,
    val keyBg: Color,
    val keyNumBg: Color,
    val keyOpBg: Color,
    val keyActionBg: Color,
    val keyClearBg: Color,
    val keyEqualsBg: Color,
    val keyBorder: Color,
    val primaryText: Color,
    val numberText: Color,
    val displayText: Color,
    val statusText: Color,
    val operatorText: Color,
    val buttonBorder: Color,
    val shiftText: Color,
    val shiftBg: Color
)

@Composable
private fun FxCalculatorDisplay(
    palette: FxPalette,
    expression: String,
    cursorPos: Int,
    resultText: String,
    errorText: String?,
    degreesMode: Boolean,
    memory: Double,
    shiftActive: Boolean,
    modifier: Modifier = Modifier
) {
    val displayExpr = buildString {
        val pos = cursorPos.coerceIn(0, expression.length)
        append(expression.substring(0, pos))
        append("│")
        append(expression.substring(pos))
    }

    Column(
        modifier = modifier
            .background(palette.displayBg)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (shiftActive) {
                Text(
                    text = "SHIFT",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = palette.shiftText,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = if (degreesMode) "DEG" else "RAD",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = palette.statusText,
                modifier = Modifier.padding(start = 8.dp)
            )
            if (memory != 0.0) {
                Text(
                    text = "M",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = palette.statusText,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = if (expression.isBlank()) "│" else displayExpr,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            fontFamily = FontFamily.Monospace,
            fontSize = 17.sp,
            lineHeight = 21.sp,
            color = palette.displayText
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = errorText ?: resultText,
            modifier = Modifier.fillMaxWidth(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = if (errorText == null) 24.sp else 13.sp,
            color = if (errorText == null) palette.displayText else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun FxCalculatorToolbar(
    palette: FxPalette,
    onNavigateBack: () -> Unit,
    degreesMode: Boolean,
    onToggleMode: () -> Unit,
    onShowHelp: () -> Unit,
    shiftActive: Boolean,
    onToggleShift: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .background(palette.toolbarBg)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier.height(34.dp).weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            border = BorderStroke(1.dp, palette.buttonBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.statusText)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("BACK", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
        }
        // SHIFT button — Casio Deep Blue when normal, Amber Gold when active
        androidx.compose.material3.Button(
            onClick = onToggleShift,
            modifier = Modifier.height(34.dp).weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = if (shiftActive) Color(0xFFD97706) else palette.shiftBg,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "SHIFT",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                maxLines = 1
            )
        }
        OutlinedButton(
            onClick = onToggleMode,
            modifier = Modifier.height(34.dp).weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            border = BorderStroke(1.dp, palette.buttonBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.statusText)
        ) {
            Text(if (degreesMode) "DEG" else "RAD", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
        }
        OutlinedButton(
            onClick = onShowHelp,
            modifier = Modifier.height(34.dp).weight(1f),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 4.dp),
            border = BorderStroke(1.dp, palette.buttonBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.statusText)
        ) {
            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(3.dp))
            Text("HELP", fontWeight = FontWeight.Bold, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun FxCalculatorKeypad(
    palette: FxPalette,
    modifier: Modifier = Modifier,
    shiftActive: Boolean,
    onShiftUsed: () -> Unit,
    onInsert: (String) -> Unit,
    onEvaluate: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onCursorLeft: () -> Unit,
    onCursorRight: () -> Unit,
    onMemoryAdd: () -> Unit,
    onMemorySubtract: () -> Unit,
    onMemoryStore: () -> Unit,
    onMemoryClear: () -> Unit,
    onMemoryRecall: () -> Unit,
    onUseAns: () -> Unit,
    onConvertToDMS: () -> Unit,
    onConvertToHMS: () -> Unit
) {
    val rows = listOf(
        // Row 1: Navigation & Memory
        listOf(
            FxKey("◄", kind = FxKeyKind.Action, action = onCursorLeft),
            FxKey("►", kind = FxKeyKind.Action, action = onCursorRight),
            FxKey("(", "("),
            FxKey(")", ")"),
            FxKey("MS", action = onMemoryStore, kind = FxKeyKind.Memory,
                shiftLabel = "MC", shiftAction = onMemoryClear),
            FxKey("MR", action = onMemoryRecall, kind = FxKeyKind.Memory,
                shiftLabel = "M+", shiftAction = onMemoryAdd)
        ),
        // Row 2: Trig — primary & inverse via SHIFT + atan2/tan2
        listOf(
            FxKey("sin", "sin(",    shiftLabel = "sin⁻¹", shiftInsert = "asin("),
            FxKey("cos", "cos(",    shiftLabel = "cos⁻¹", shiftInsert = "acos("),
            FxKey("tan", "tan(",    shiftLabel = "tan⁻¹", shiftInsert = "atan("),
            FxKey("atan2", "atan2("),
            FxKey("√x",  "sqrt(",   shiftLabel = "³√x",   shiftInsert = "cbrt("),
            FxKey("Log", "log(",    shiftLabel = "Ln",    shiftInsert = "ln(")
        ),
        // Row 3: Power & Roots & Falak Conversions
        listOf(
            FxKey("x²",  "^2",    shiftLabel = "x³",   shiftInsert = "^3"),
            FxKey("xʸ",  "^",     shiftLabel = "10ˣ",  shiftInsert = "10^"),
            FxKey("DMS", "dms(",  shiftLabel = "→DMS", shiftAction = onConvertToDMS),
            FxKey("HMS", "hms(",  shiftLabel = "→HMS", shiftAction = onConvertToHMS),
            FxKey("JD",  "jd(",   shiftLabel = "ΔT",   shiftInsert = "dt("),
            FxKey("Abs", "abs(",  shiftLabel = "Mod",  shiftInsert = "mod(")
        ),
        // Row 4: Rounding & Combinatorics & Pol/Rec
        listOf(
            FxKey("Rnd",  "round(",  shiftLabel = "Int",   shiftInsert = "int("),
            FxKey("Floor","floor(",  shiftLabel = "Frac",  shiftInsert = "frac("),
            FxKey("Norm", "norm360(",shiftLabel = "Norm-", shiftInsert = "norm180("),
            FxKey("nPr",  "npr(",    shiftLabel = "nCr",   shiftInsert = "ncr("),
            FxKey("GCD",  "gcd(",    shiftLabel = "LCM",   shiftInsert = "lcm("),
            FxKey("Pol",  "pol(",    shiftLabel = "Rec",   shiftInsert = "rec(")
        ),
        // Row 5: Numbers 7-9 & Actions
        listOf(
            FxKey("7", "7", kind = FxKeyKind.Number),
            FxKey("8", "8", kind = FxKeyKind.Number),
            FxKey("9", "9", kind = FxKeyKind.Number),
            FxKey("DEL", kind = FxKeyKind.Clear, action = onBackspace),
            FxKey("AC",  kind = FxKeyKind.Clear, action = onClear),
            FxKey("/",   "/", kind = FxKeyKind.Operator)
        ),
        // Row 6: Numbers 4-6 & Operators
        listOf(
            FxKey("4", "4", kind = FxKeyKind.Number),
            FxKey("5", "5", kind = FxKeyKind.Number),
            FxKey("6", "6", kind = FxKeyKind.Number),
            FxKey("*", "*", kind = FxKeyKind.Operator),
            FxKey(",", ",", kind = FxKeyKind.Operator, shiftLabel = "%", shiftInsert = "%"),
            FxKey("M-", action = onMemorySubtract, kind = FxKeyKind.Memory)
        ),
        // Row 7: Numbers 1-3 & Memory
        listOf(
            FxKey("1", "1", kind = FxKeyKind.Number),
            FxKey("2", "2", kind = FxKeyKind.Number),
            FxKey("3", "3", kind = FxKeyKind.Number),
            FxKey("+", "+", kind = FxKeyKind.Operator),
            FxKey("-", "-", kind = FxKeyKind.Operator),
            FxKey("M+", action = onMemoryAdd, kind = FxKeyKind.Memory)
        ),
        // Row 8: 0 / . / Constants / Ans / Equals
        listOf(
            FxKey("0", "0", kind = FxKeyKind.Number),
            FxKey(".", ".", kind = FxKeyKind.Number),
            FxKey("e",   "e",  shiftLabel = "π", shiftInsert = "pi"),
            FxKey("Ans", action = onUseAns, kind = FxKeyKind.Memory),
            FxKey("(",   "(",  shiftLabel = "!", shiftInsert = "!"),
            FxKey("=", kind = FxKeyKind.Equals, action = onEvaluate)
        )
    )

    Column(
        modifier = modifier
            .background(palette.keypadBg)
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 4.dp),
    ) {
        rows.forEachIndexed { rowIdx, row ->
            // SHIFT label row — sits visually RIGHT ABOVE its button capsule (Casio style)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(13.dp),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentHeight(unbounded = true)
                            .padding(bottom = 1.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        if (key.shiftLabel.isNotBlank()) {
                            val shiftColor = if (shiftActive) Color(0xFFF59E0B) else palette.shiftText
                            Text(
                                text = key.shiftLabel,
                                fontSize = 9.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.SansSerif,
                                color = shiftColor,
                                style = androidx.compose.ui.text.TextStyle(
                                    fontSize = 9.sp,
                                    platformStyle = androidx.compose.ui.text.PlatformTextStyle(
                                        includeFontPadding = false
                                    )
                                )
                            )
                        }
                    }
                }
            }

            // Actual button capsule row
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                row.forEach { key ->
                    FxCalculatorKey(
                        palette = palette,
                        key = key,
                        shiftActive = shiftActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val hasShift = key.shiftInsert.isNotBlank() || key.shiftAction != null
                            if (shiftActive && hasShift) {
                                when {
                                    key.shiftAction != null -> key.shiftAction.invoke()
                                    key.shiftInsert.isNotBlank() -> onInsert(key.shiftInsert)
                                }
                                onShiftUsed()
                            } else {
                                when {
                                    key.action != null -> key.action.invoke()
                                    key.insert.isNotBlank() -> onInsert(key.insert)
                                }
                            }
                        }
                    )
                }
            }

            // Spacer between rows (except after last row)
            if (rowIdx < rows.lastIndex) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}

@Composable
private fun FxCalculatorKey(
    palette: FxPalette,
    key: FxKey,
    shiftActive: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val bg = when (key.kind) {
        FxKeyKind.Number   -> palette.keyNumBg
        FxKeyKind.Operator -> palette.keyOpBg
        FxKeyKind.Function -> palette.keyBg
        FxKeyKind.Memory   -> palette.keyBg
        FxKeyKind.Action   -> palette.keyActionBg
        FxKeyKind.Clear    -> palette.keyClearBg
        FxKeyKind.Equals   -> palette.keyEqualsBg
    }
    val pressedBg = if (isPressed) bg.copy(alpha = 0.75f) else bg

    // Only the button capsule — SHIFT label is rendered separately above in the label row
    Surface(
        modifier = modifier.fillMaxSize(),
        shape = RoundedCornerShape(6.dp),
        color = pressedBg,
        shadowElevation = 0.dp,
        border = BorderStroke(
            width = if (isPressed) 1.2.dp else 0.6.dp,
            color = if (isPressed) palette.buttonBorder else palette.keyBorder
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                ),
            contentAlignment = Alignment.Center
        ) {
            val fontSize = when {
                key.kind == FxKeyKind.Number   -> 19.sp
                key.kind == FxKeyKind.Equals   -> 19.sp
                key.kind == FxKeyKind.Operator -> 18.sp
                key.label.length <= 1 -> 17.sp
                key.label.length <= 2 -> 15.sp
                key.label.length <= 3 -> 13.sp
                key.label.length <= 4 -> 11.sp
                key.label.length <= 5 -> 10.sp
                else -> 9.sp
            }

            Text(
                text = key.label,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = fontSize,
                fontFamily = FontFamily.SansSerif,
                fontWeight = when (key.kind) {
                    FxKeyKind.Number -> FontWeight.Bold
                    FxKeyKind.Equals, FxKeyKind.Clear -> FontWeight.Bold
                    else -> FontWeight.SemiBold
                },
                color = when (key.kind) {
                    FxKeyKind.Number -> palette.numberText
                    FxKeyKind.Clear, FxKeyKind.Equals -> Color.White
                    else -> palette.primaryText
                }
            )
        }
    }
}


private data class CalculatorHistory(val expression: String, val result: String)

private data class FxKey(
    val label: String,
    val insert: String = "",
    val kind: FxKeyKind = FxKeyKind.Function,
    val action: (() -> Unit)? = null,
    val shiftLabel: String = "",
    val shiftInsert: String = "",
    val shiftAction: (() -> Unit)? = null
)

private enum class FxKeyKind {
    Number,
    Operator,
    Function,
    Memory,
    Action,
    Clear,
    Equals
}

private fun formatCalculatorNumber(value: Double): String {

    if (value.isNaN() || value.isInfinite()) return "Math ERROR"
    val absValue = abs(value)
    return when {
        absValue != 0.0 && (absValue >= 1.0e12 || absValue < 1.0e-8) ->
            String.format(Locale.US, "%.12e", value).trimTrailingZeros()
        floor(value) == value && absValue < 9.0e15 ->
            String.format(Locale.US, "%.0f", value)
        else ->
            String.format(Locale.US, "%.12f", value).trimTrailingZeros()
    }
}

private fun complexInverseTrigResult(input: String, degreesMode: Boolean): String? {
    val match = Regex(
        pattern = """^\s*(asin|acos|asind|acosd)\s*\(\s*([+-]?(?:\d+(?:\.\d*)?|\.\d+)(?:[eE][+-]?\d+)?)\s*\)\s*$""",
        option = RegexOption.IGNORE_CASE
    ).matchEntire(input) ?: return null

    val function = match.groupValues[1].lowercase(Locale.US)
    val x = match.groupValues[2].toDoubleOrNull() ?: return null
    if (abs(x) <= 1.0) return null

    val acosh = ln(abs(x) + sqrt(x * x - 1.0))
    val forceDegrees = function.endsWith("d")
    val useDegrees = forceDegrees || degreesMode
    val scale = if (useDegrees) 180.0 / PI else 1.0

    val (realRad, imagRad) = when {
        function.startsWith("asin") && x > 1.0 -> (PI / 2.0) to -acosh
        function.startsWith("asin") -> (-PI / 2.0) to acosh
        function.startsWith("acos") && x > 1.0 -> 0.0 to -acosh
        else -> PI to -acosh
    }

    val real = realRad * scale
    val imag = imagRad * scale
    val sign = if (imag < 0.0) "-" else "+"
    return "${formatCalculatorNumber(real)} $sign ${formatCalculatorNumber(abs(imag))}i"
}

private fun String.trimTrailingZeros(): String {
    return if (contains('e') || contains('E')) {
        replace(Regex("0+e"), "e").replace(Regex("\\.e"), "e")
    } else {
        replace(Regex("0+$"), "").replace(Regex("\\.$"), "")
    }
}

private class ScientificExpressionEvaluator(
    private val degreesMode: Boolean,
    private val ans: Double,
    private val memory: Double
) {
    fun evaluate(input: String): Double {
        val parser = Parser(input)
        val value = parser.parseExpression()
        parser.skipSpaces()
        if (!parser.isAtEnd()) {
            throw IllegalArgumentException("Karakter tidak dikenali: ${parser.peek()}")
        }
        if (value.isNaN() || value.isInfinite()) {
            throw IllegalArgumentException("Math ERROR")
        }
        return value
    }

    private inner class Parser(private val input: String) {
        private var index = 0

        fun parseExpression(): Double {
            var value = parseTerm()
            while (true) {
                skipSpaces()
                value = when {
                    consume('+') -> value + parseTerm()
                    consume('-') -> value - parseTerm()
                    else -> return value
                }
            }
        }

        private fun parseTerm(): Double {
            var value = parseUnary()
            while (true) {
                skipSpaces()
                value = when {
                    consume('*') -> value * parseUnary()
                    consume('/') -> value / parseUnary()
                    else -> return value
                }
            }
        }

        private fun parseUnary(): Double {
            skipSpaces()
            return when {
                consume('+') -> parseUnary()
                consume('-') -> -parseUnary()
                else -> parsePower()
            }
        }

        private fun parsePower(): Double {
            var value = parsePostfix()
            skipSpaces()
            if (consume('^')) {
                value = value.pow(parseUnary())
            }
            return value
        }

        private fun parsePostfix(): Double {
            var value = parsePrimary()
            while (true) {
                skipSpaces()
                value = when {
                    consume('!') -> factorial(value)
                    consume('%') -> value / 100.0
                    else -> return value
                }
            }
        }

        private fun parsePrimary(): Double {
            skipSpaces()
            if (consume('(')) {
                val value = parseExpression()
                expect(')')
                return value
            }

            if (peek()?.isDigit() == true || peek() == '.') {
                return parseNumber()
            }

            if (peek()?.isLetter() == true) {
                val name = parseIdentifier().lowercase(Locale.US)
                skipSpaces()
                if (consume('(')) {
                    val args = parseArguments()
                    return callFunction(name, args)
                }
                return when (name) {
                    "pi" -> PI
                    "e" -> E
                    "ans" -> ans
                    "m" -> memory
                    else -> throw IllegalArgumentException("Nama tidak dikenal: $name")
                }
            }

            throw IllegalArgumentException("Input belum lengkap")
        }

        private fun parseArguments(): List<Double> {
            val args = mutableListOf<Double>()
            skipSpaces()
            if (consume(')')) return args
            while (true) {
                args.add(parseExpression())
                skipSpaces()
                if (consume(')')) return args
                expect(',')
            }
        }

        private fun parseNumber(): Double {
            val start = index
            while (peek()?.isDigit() == true) index++
            if (peek() == '.') {
                index++
                while (peek()?.isDigit() == true) index++
            }
            if (peek() == 'e' || peek() == 'E') {
                index++
                if (peek() == '+' || peek() == '-') index++
                while (peek()?.isDigit() == true) index++
            }
            return input.substring(start, index).toDoubleOrNull()
                ?: throw IllegalArgumentException("Angka tidak valid")
        }

        private fun parseIdentifier(): String {
            val start = index
            while (peek()?.isLetterOrDigit() == true || peek() == '_') index++
            return input.substring(start, index)
        }

        fun skipSpaces() {
            while (peek()?.isWhitespace() == true) index++
        }

        fun isAtEnd(): Boolean = index >= input.length

        fun peek(): Char? = input.getOrNull(index)

        private fun consume(char: Char): Boolean {
            skipSpaces()
            if (peek() == char) {
                index++
                return true
            }
            return false
        }

        private fun expect(char: Char) {
            if (!consume(char)) throw IllegalArgumentException("Perlu '$char'")
        }
    }

    private fun callFunction(name: String, args: List<Double>): Double {
        fun one(): Double = args.singleOrNull() ?: throw IllegalArgumentException("$name perlu 1 argumen")
        fun two(): Pair<Double, Double> {
            if (args.size != 2) throw IllegalArgumentException("$name perlu 2 argumen")
            return args[0] to args[1]
        }
        fun three(): Triple<Double, Double, Double> {
            if (args.size != 3) throw IllegalArgumentException("$name perlu 3 argumen")
            return Triple(args[0], args[1], args[2])
        }
        fun angle(value: Double): Double = if (degreesMode) Math.toRadians(value) else value
        fun invAngle(value: Double): Double = if (degreesMode) Math.toDegrees(value) else value

        return when (name) {
            "sin" -> sin(angle(one()))
            "cos" -> cos(angle(one()))
            "tan" -> tan(angle(one()))
            "asin" -> invAngle(asin(one()))
            "acos" -> invAngle(acos(one()))
            "atan" -> invAngle(atan(one()))
            "sind" -> sin(Math.toRadians(one()))
            "cosd" -> cos(Math.toRadians(one()))
            "tand" -> tan(Math.toRadians(one()))
            "asind" -> Math.toDegrees(asin(one()))
            "acosd" -> Math.toDegrees(acos(one()))
            "atand" -> Math.toDegrees(atan(one()))
            "atan2", "tan2", "atan2d", "tan2d" -> {
                val (y, x) = two()
                if (degreesMode) Math.toDegrees(kotlin.math.atan2(y, x)) else kotlin.math.atan2(y, x)
            }
            "sqrt" -> sqrt(one())
            "cbrt" -> Math.cbrt(one())
            "ln" -> ln(one())
            "log" -> {
                if (args.size == 1) log10(args[0])
                else if (args.size == 2) kotlin.math.log(args[0], args[1])
                else throw IllegalArgumentException("log perlu 1 atau 2 argumen: log(x) atau log(x, base)")
            }
            "exp" -> exp(one())
            "abs" -> abs(one())
            "int" -> truncate(one())
            "trunc", "truncate" -> truncate(one())
            "round", "rnd", "normal" -> round(one())
            "rad" -> Math.toRadians(one())
            "deg" -> Math.toDegrees(one())
            "frac", "prac" -> one() - floor(one())
            "sign" -> kotlin.math.sign(one())
            "norm360" -> normalize360(one())
            "norm180" -> normalize180(one())
            "dms" -> {
                val (d, m, s) = three()
                val sign = if (d < 0.0 || m < 0.0 || s < 0.0) -1.0 else 1.0
                sign * (abs(d) + abs(m) / 60.0 + abs(s) / 3600.0)
            }
            "hms" -> {
                val (h, m, s) = three()
                val sign = if (h < 0.0 || m < 0.0 || s < 0.0) -1.0 else 1.0
                sign * (abs(h) + abs(m) / 60.0 + abs(s) / 3600.0)
            }
            "pow" -> {
                val (a, b) = two()
                a.pow(b)
            }
            "root" -> {
                val (x, y) = two()
                x.pow(1.0 / y)
            }
            "mod" -> {
                val (a, b) = two()
                a - b * floor(a / b)
            }
            "fact" -> factorial(one())
            "npr" -> {
                val (n, r) = two()
                permutation(n, r)
            }
            "ncr" -> {
                val (n, r) = two()
                combination(n, r)
            }
            "gcd" -> args.map { it.toLongChecked(name) }.reduce { a, b -> gcdLong(a, b) }.toDouble()
            "lcm" -> args.map { it.toLongChecked(name) }.reduce { a, b -> lcmLong(a, b) }.toDouble()
            "ran" -> Random.nextDouble()
            "ranint" -> {
                val (a, b) = two()
                val min = kotlin.math.min(a.toInt(), b.toInt())
                val max = kotlin.math.max(a.toInt(), b.toInt())
                Random.nextInt(min, max + 1).toDouble()
            }
            "pol" -> {
                val (x, y) = two()
                sqrt(x * x + y * y)
            }
            "rec" -> {
                val (r, theta) = two()
                r * cos(if (degreesMode) Math.toRadians(theta) else theta)
            }
            "floor", "kebawah" -> kotlin.math.floor(one())
            "ceil", "keatas" -> kotlin.math.ceil(one())
            "jd" -> {
                // Julian Day dari jd(tahun, bulan, hari)
                val (yr, mo, dy) = three()
                val y = yr.toInt(); val m = mo.toInt(); val d = dy.toInt()
                val a = (14 - m) / 12
                val ye = y + 4800 - a
                val mn = m + 12 * a - 3
                (d + (153 * mn + 2) / 5 + ye * 365 + ye / 4 - ye / 100 + ye / 400 - 32045).toDouble() - 0.5
            }
            "dt", "deltat" -> {
                // Delta T dari JD (perkiraan modern)
                val jd = one()
                val t = (jd - 2451545.0) / 36525.0
                val dt = 62.92 + 0.32217 * t + 0.005589 * t * t
                dt
            }
            "pi" -> Math.PI
            "e" -> Math.E
            "min" -> args.minOrNull() ?: throw IllegalArgumentException("min perlu argumen")
            "max" -> args.maxOrNull() ?: throw IllegalArgumentException("max perlu argumen")
            else -> throw IllegalArgumentException("Fungsi tidak dikenal: $name")
        }
    }

    private fun factorial(value: Double): Double {
        if (value < 0.0 || floor(value) != value || value > 170.0) {
            throw IllegalArgumentException("Faktorial hanya untuk bilangan bulat 0..170")
        }
        var result = 1.0
        for (i in 2..value.toInt()) result *= i
        return result
    }

    private fun permutation(nValue: Double, rValue: Double): Double {
        val n = nValue.toIntChecked("nPr")
        val r = rValue.toIntChecked("nPr")
        if (r < 0 || n < 0 || r > n) throw IllegalArgumentException("nPr perlu 0 <= r <= n")
        var result = 1.0
        for (i in 0 until r) result *= (n - i).toDouble()
        return result
    }

    private fun combination(nValue: Double, rValue: Double): Double {
        val n = nValue.toIntChecked("nCr")
        val rRaw = rValue.toIntChecked("nCr")
        if (rRaw < 0 || n < 0 || rRaw > n) throw IllegalArgumentException("nCr perlu 0 <= r <= n")
        val r = kotlin.math.min(rRaw, n - rRaw)
        var result = 1.0
        for (i in 1..r) {
            result = result * (n - r + i).toDouble() / i.toDouble()
        }
        return result
    }

    private fun Double.toIntChecked(name: String): Int {
        if (floor(this) != this || this < 0.0 || this > Int.MAX_VALUE) {
            throw IllegalArgumentException("$name perlu bilangan bulat non-negatif")
        }
        return toInt()
    }

    private fun Double.toLongChecked(name: String): Long {
        if (floor(this) != this) throw IllegalArgumentException("$name perlu bilangan bulat")
        return toLong()
    }

    private fun gcdLong(aValue: Long, bValue: Long): Long {
        var a = kotlin.math.abs(aValue)
        var b = kotlin.math.abs(bValue)
        while (b != 0L) {
            val r = a % b
            a = b
            b = r
        }
        return a
    }

    private fun lcmLong(a: Long, b: Long): Long {
        if (a == 0L || b == 0L) return 0L
        return kotlin.math.abs(a / gcdLong(a, b) * b)
    }

    private fun normalize360(value: Double): Double {
        val mod = value % 360.0
        return if (mod < 0.0) mod + 360.0 else mod
    }

    private fun normalize180(value: Double): Double {
        val normalized = normalize360(value)
        return if (normalized > 180.0) normalized - 360.0 else normalized
    }
}

