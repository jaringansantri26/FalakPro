package com.falak.falakpro.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falak.falakpro.ui.theme.GreenLightBg
import com.falak.falakpro.ui.theme.GreenPrimary
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
    var shiftActive by remember { mutableStateOf(false) }
    var memory by remember { mutableDoubleStateOf(0.0) }
    var ans    by remember { mutableDoubleStateOf(0.0) }
    val history = remember { mutableStateListOf<CalculatorHistory>() }
    var showHelp by remember { mutableStateOf(false) }

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
        shiftActive = false
    }

    // ----- SHIFT+DMS: decimal → D° M' S" -----
    fun convertToDMS() {
        val value = ans
        val sign = if (value < 0) "-" else ""
        val abs = kotlin.math.abs(value)
        val d = abs.toInt()
        val mFull = (abs - d) * 60.0
        val m = mFull.toInt()
        val s = (mFull - m) * 60.0
        resultText = "$sign${d}° ${m}' ${String.format(java.util.Locale.US, "%.4f", s)}\""
        shiftActive = false
    }

    // ----- SHIFT+HMS: decimal → H h M m S s -----
    fun convertToHMS() {
        val value = ans
        val sign = if (value < 0) "-" else ""
        val abs = kotlin.math.abs(value)
        val h = abs.toInt()
        val mFull = (abs - h) * 60.0
        val m = mFull.toInt()
        val s = (mFull - m) * 60.0
        resultText = "$sign${h}j ${m}m ${String.format(java.util.Locale.US, "%.4f", s)}d"
        shiftActive = false
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
                    Text("TUTUP")
                }
            },
            title = { Text("Panduan Kalkulator", fontWeight = FontWeight.Bold) },
            text = {
                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val guides = listOf(
                        "◄ / ►" to "Geser kursor kiri/kanan untuk edit ekspresi",
                        "SHIFT" to "Aktifkan fungsi alternatif (label kuning di atas tombol)",
                        "DMS" to "Masukkan derajat ke desimal: dms(6,18,48)",
                        "SHIFT+DMS" to "Konversi hasil desimal → D° M' S\" (DMS)",
                        "HMS" to "Masukkan jam ke desimal: hms(21,30,0)",
                        "SHIFT+HMS" to "Konversi hasil desimal → Jj Mm Ss (HMS)",
                        "sin/cos/tan" to "Fungsi trig — mode DEG/RAD sesuai status atas",
                        "sind/cosd/tand" to "Selalu pakai derajat meski mode RAD",
                        "sin⁻¹/cos⁻¹/tan⁻¹" to "Fungsi invers trig",
                        "√x" to "Akar kuadrat: sqrt(9) = 3",
                        "xʸ" to "Pangkat: 2^10 = 1024",
                        "Log / Ln" to "Logaritma basis 10 / natural",
                        "Abs" to "Nilai mutlak: abs(-5) = 5",
                        "Mod" to "Sisa bagi: mod(10,3) = 1",
                        "Int / Frac" to "Bagian bulat / desimal",
                        "Floor / Ceil" to "Pembulatan ke bawah / ke atas",
                        "Rnd" to "Pembulatan biasa",
                        "Norm" to "Normalisasi 0–360°",
                        "Norm-" to "Normalisasi -180° s.d. 180°",
                        "nPr / nCr" to "Permutasi / Kombinasi",
                        "SHIFT+nPr" to "GCD(a,b) — faktor persekutuan terbesar",
                        "SHIFT+nCr" to "LCM(a,b) — kelipatan persekutuan terkecil",
                        "Pol(x,y)" to "Polar: hitung r dari x,y",
                        "Rec(r,θ)" to "Rectangular: hitung x dari r,θ",
                        "Ran# / RanInt" to "Bilangan acak",
                        "FACT" to "Faktorial: fact(5) = 120",
                        "Ans" to "Gunakan hasil terakhir",
                        "M+ / M- / MS" to "Memori tambah/kurang/simpan",
                        "M (kunci MR)" to "Gunakan nilai memori dalam ekspresi",
                        "pi / e" to "Konstanta π dan e",
                        "Exp" to "Notasi ilmiah: 1.5e3 = 1500",
                        "atan2(y,x)" to "Azimut/sudut arah dari koordinat",
                        "Contoh Kiblat" to "atan2(sind(L2-L1), cosd(B)*tand(LK)-sind(B)*cosd(L2-L1))"
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
                            Text(
                                desc,
                                fontSize = 12.sp,
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
                shiftActive = shiftActive,
                memory = memory,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.22f)
            )
            FxCalculatorToolbar(
                palette = palette,
                onNavigateBack = onNavigateBack,
                shiftActive = shiftActive,
                onToggleShift = { shiftActive = !shiftActive },
                onToggleMode = { degreesMode = !degreesMode },
                onMemoryRecall = { append("M") },
                onMemoryClear = { memory = 0.0 },
                onShowHelp = { showHelp = true }
            )
            FxCalculatorKeypad(
                palette = palette,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(0.78f),
                shiftActive = shiftActive,
                onInsert = ::insertAt,
                onShiftConsumed = { shiftActive = false },
                onEvaluate = ::evaluateCurrent,
                onClear = {
                    expression = ""
                    cursorPos = 0
                    resultText = "0"
                    errorText = null
                    shiftActive = false
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
            pageBg = Color(0xFF0B0B0B),
            displayBg = Color(0xFF101010),
            toolbarBg = Color(0xFF151515),
            keypadBg = Color(0xFF101010),
            keyBg = Color(0xFF1B1B1B),
            keyAltBg = Color(0xFF242424),
            keyActionBg = Color(0xFF181818),
            keyBorder = Color(0xFF303030),
            primaryText = Color(0xFFEDEDED),
            numberText = Color(0xFFD8D8D8),
            displayText = Color(0xFF00D9FF),
            statusText = Color(0xFF00D9FF),
            shiftText = Color(0xFFC98400),
            alphaText = Color(0xFFE53935),
            operatorText = Color(0xFF00D9FF),
            buttonBorder = Color(0xFF00A9CC),
            clearText = Color(0xFFC98400)
        )
    } else {
        FxPalette(
            pageBg = Color(0xFFE7E7E7),
            displayBg = Color(0xFFF3F3F3),
            toolbarBg = Color(0xFFCFCFCF),
            keypadBg = Color(0xFFD8D8D8),
            keyBg = Color(0xFFDCDCDC),
            keyAltBg = Color(0xFFF3F3F3),
            keyActionBg = Color(0xFFE9E9E9),
            keyBorder = Color.White,
            primaryText = Color(0xFF333333),
            numberText = Color(0xFF333333),
            displayText = Color(0xFF222222),
            statusText = Color(0xFF3B3B3B),
            shiftText = Color(0xFFC47A00),
            alphaText = Color(0xFFC47A00),
            operatorText = Color(0xFF333333),
            buttonBorder = Color(0xFF777777),
            clearText = Color(0xFFC47A00)
        )
    }
}

private data class FxPalette(
    val pageBg: Color,
    val displayBg: Color,
    val toolbarBg: Color,
    val keypadBg: Color,
    val keyBg: Color,
    val keyAltBg: Color,
    val keyActionBg: Color,
    val keyBorder: Color,
    val primaryText: Color,
    val numberText: Color,
    val displayText: Color,
    val statusText: Color,
    val shiftText: Color,
    val alphaText: Color,
    val operatorText: Color,
    val buttonBorder: Color,
    val clearText: Color
)

@Composable
private fun FxCalculatorDisplay(
    palette: FxPalette,
    expression: String,
    cursorPos: Int,
    resultText: String,
    errorText: String?,
    degreesMode: Boolean,
    shiftActive: Boolean,
    memory: Double,
    modifier: Modifier = Modifier
) {
    // Tampilkan ekspresi dengan kursor blok |
    val displayExpr = buildString {
        val pos = cursorPos.coerceIn(0, expression.length)
        append(expression.substring(0, pos))
        append("│")   // karakter cursor
        append(expression.substring(pos))
    }

    Column(
        modifier = modifier
            .background(palette.displayBg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                if (shiftActive) "SHIFT" else "",
                if (degreesMode) "DEG" else "RAD",
                if (memory == 0.0) "" else "M"
            )
                .filter { it.isNotBlank() }
                .forEach { item ->
                    Text(
                        text = item,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = palette.statusText,
                        modifier = Modifier.padding(start = 10.dp)
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
            fontSize = 16.sp,
            lineHeight = 20.sp,
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
            fontWeight = FontWeight.SemiBold,
            fontSize = if (errorText == null) 22.sp else 12.sp,
            color = if (errorText == null) palette.displayText else MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun FxCalculatorToolbar(
    palette: FxPalette,
    onNavigateBack: () -> Unit,
    shiftActive: Boolean,
    onToggleShift: () -> Unit,
    onToggleMode: () -> Unit,
    onMemoryRecall: () -> Unit,
    onMemoryClear: () -> Unit,
    onShowHelp: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .background(palette.toolbarBg)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(
            onClick = onNavigateBack,
            modifier = Modifier
                .height(36.dp)
                .weight(1.2f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, palette.buttonBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.statusText)
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("BACK", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }
        OutlinedButton(
            onClick = onToggleShift,
            modifier = Modifier.height(36.dp).weight(1f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, if (shiftActive) palette.shiftText else palette.buttonBorder),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.Transparent,
                contentColor = if (shiftActive) palette.shiftText else palette.statusText
            )
        ) {
            Text("SHIFT", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }
        OutlinedButton(
            onClick = onShowHelp,
            modifier = Modifier.height(36.dp).weight(1f),
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, palette.buttonBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = palette.statusText)
        ) {
            Icon(Icons.Filled.History, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("HELP", fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun FxSmallCommand(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (active) Color(0xFFC47A00) else Color(0xFF777777)),
        contentPadding = ButtonDefaults.ContentPadding,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (active) Color(0xFFFFF3D4) else Color.Transparent,
            contentColor = if (active) Color(0xFFC47A00) else Color(0xFF333333)
        )
    ) {
        Text(label, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, fontSize = 11.sp)
    }
}

@Composable
private fun FxCalculatorKeypad(
    palette: FxPalette,
    modifier: Modifier = Modifier,
    shiftActive: Boolean,
    onInsert: (String) -> Unit,
    onShiftConsumed: () -> Unit,
    onEvaluate: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onCursorLeft: () -> Unit,
    onCursorRight: () -> Unit,
    onMemoryAdd: () -> Unit,
    onMemorySubtract: () -> Unit,
    onMemoryStore: () -> Unit,
    onUseAns: () -> Unit,
    onConvertToDMS: () -> Unit,
    onConvertToHMS: () -> Unit
) {
    val rows = listOf(
        listOf(
            FxKey("ALPHA", "", "", "", FxKeyKind.Action),
            FxKey("◄", "", "", "", FxKeyKind.Action, action = onCursorLeft),
            FxKey("►", "", "", "", FxKeyKind.Action, action = onCursorRight),
            FxKey("2nd", "", "", "", FxKeyKind.Action),
            FxKey("MODE", "", "", "", FxKeyKind.Action),
            FxKey("MATH", "", "", "", FxKeyKind.Action)
        ),
        listOf(
            FxKey("sin", "sin(", "sinD", "sind("),
            FxKey("cos", "cos(", "cosD", "cosd("),
            FxKey("tan", "tan(", "tanD", "tand("),
            FxKey("sin⁻¹", "asin(", "asinD", "asind("),
            FxKey("cos⁻¹", "acos(", "acosD", "acosd("),
            FxKey("tan⁻¹", "atan(", "atanD", "atand(")
        ),
        listOf(
            FxKey("√x", "sqrt(", "x²", "^2"),
            FxKey("xʸ", "^", "pow", "pow("),
            FxKey("Log", "log(", "10ˣ", "10^"),
            FxKey("Ln", "ln(", "eˣ", "e^"),
            FxKey("Abs", "abs(", "|x|", "abs("),
            FxKey("Mod", "mod(", "", "")
        ),
        listOf(
            // DMS: normal=masukkan dms(, SHIFT=konversi hasil ke DMS
            FxKey("DMS", "dms(", "→DMS", "", shiftAction = onConvertToDMS),
            // HMS: normal=masukkan hms(, SHIFT=konversi hasil ke HMS
            FxKey("HMS", "hms(", "→HMS", "", shiftAction = onConvertToHMS),
            FxKey("Int", "int(", "", ""),
            FxKey("Frac", "frac(", "", ""),
            FxKey("Floor", "floor(", "", ""),
            FxKey("Ceil", "ceil(", "", "")
        ),
        listOf(
            FxKey("x⁻¹", "^-1", "x!", "!"),
            FxKey("x³", "^3", "³√x", "cbrt("),
            FxKey("x√y", "root(", "", ""),
            FxKey("hyp", "", "", ""),
            FxKey("nPr", "npr(", "GCD", "gcd("),
            FxKey("nCr", "ncr(", "LCM", "lcm(")
        ),
        listOf(
            FxKey("Rnd", "round(", "", ""),
            FxKey("Norm", "norm360(", "", ""),
            FxKey("Norm-", "norm180(", "", ""),
            FxKey("%", "%", "", ""),
            FxKey("(", "(", "", ""),
            FxKey(")", ")", "", "")
        ),
        listOf(
            FxKey("Pol", "pol(", "", ""),
            FxKey("Rec", "rec(", "", ""),
            FxKey("Ran#", "ran(", "", ""),
            FxKey("RanInt", "ranint(", "", ""),
            FxKey("FACT", "fact(", "", ""),
            FxKey("ENG", "", "", "")
        ),
        listOf(
            FxKey("7", "7", "CONST", "", FxKeyKind.Number),
            FxKey("8", "8", "CONV", "", FxKeyKind.Number),
            FxKey("9", "9", "", "", FxKeyKind.Number),
            FxKey("DEL", "", "", "", FxKeyKind.Action, onBackspace),
            FxKey("CLR", "", "ALL", "", FxKeyKind.Clear, onClear),
            FxKey("/", "/", "", "", FxKeyKind.Operator)
        ),
        listOf(
            FxKey("4", "4", "", "", FxKeyKind.Number),
            FxKey("5", "5", "", "", FxKeyKind.Number),
            FxKey("6", "6", "", "", FxKeyKind.Number),
            FxKey("*", "*", "", "", FxKeyKind.Operator),
            FxKey(",", ",", "", "", FxKeyKind.Operator),
            FxKey("M+", "", "", "", FxKeyKind.Memory, onMemoryAdd)
        ),
        listOf(
            FxKey("1", "1", "", "", FxKeyKind.Number),
            FxKey("2", "2", "", "", FxKeyKind.Number),
            FxKey("3", "3", "", "", FxKeyKind.Number),
            FxKey("+", "+", "", "", FxKeyKind.Operator),
            FxKey("-", "-", "", "", FxKeyKind.Operator),
            FxKey("M-", "", "", "", FxKeyKind.Memory, onMemorySubtract)
        ),
        listOf(
            FxKey("0", "0", "", "", FxKeyKind.Number),
            FxKey(".", ".", "Ran#", "", FxKeyKind.Number),
            FxKey("Exp", "e", "π", "pi"),
            FxKey("Ans", "", "PreAns", "", FxKeyKind.Memory, onUseAns),
            FxKey("MS", "", "", "", FxKeyKind.Memory, onMemoryStore),
            FxKey("=", "", "History", "", FxKeyKind.Equals, onEvaluate)
        )
    )

    Column(modifier = modifier.background(palette.keypadBg)) {
        rows.forEach { row ->
            Row(modifier = Modifier.weight(1f)) {
                row.forEach { key ->
                    FxCalculatorKey(
                        palette = palette,
                        key = key,
                        shiftActive = shiftActive,
                        modifier = Modifier.weight(1f),
                        onClick = {
                            when {
                                // SHIFT aktif dan ada shiftAction khusus → jalankan
                                shiftActive && key.shiftAction != null -> {
                                    key.shiftAction.invoke()
                                }
                                // SHIFT aktif dan ada shiftInsert → sisipkan teks
                                shiftActive && key.shiftInsert.isNotBlank() -> {
                                    onInsert(key.shiftInsert)
                                    onShiftConsumed()
                                }
                                // Normal action (DEL, CLR, cursor, dll)
                                key.action != null -> {
                                    key.action.invoke()
                                    if (shiftActive) onShiftConsumed()
                                }
                                // Insert teks biasa
                                key.insert.isNotBlank() -> {
                                    onInsert(key.insert)
                                    if (shiftActive) onShiftConsumed()
                                }
                            }
                        }
                    )
                }
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
        FxKeyKind.Number -> palette.keyAltBg
        FxKeyKind.Operator -> palette.keyBg
        FxKeyKind.Function -> palette.keyBg
        FxKeyKind.Memory -> palette.keyBg
        FxKeyKind.Action -> palette.keyActionBg
        FxKeyKind.Clear -> palette.keyAltBg
        FxKeyKind.Equals -> palette.keyActionBg
    }

    // Warna lebih gelap saat ditekan
    val pressedBg = if (isPressed) bg.copy(alpha = (bg.alpha * 0.6f).coerceAtLeast(0.15f)) else bg

    Button(
        onClick = onClick,
        modifier = modifier.fillMaxSize(),
        interactionSource = interactionSource,
        shape = RoundedCornerShape(0.dp),
        border = BorderStroke(
            width = if (isPressed) 1.2.dp else 0.6.dp,
            color = if (isPressed) palette.buttonBorder else palette.keyBorder
        ),
        colors = ButtonDefaults.buttonColors(
            containerColor = pressedBg,
            contentColor = palette.primaryText
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(1.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (key.shiftLabel.isNotBlank()) {
                Text(
                    text = key.shiftLabel,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 2.dp, start = 2.dp, end = 2.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    fontSize = 7.5.sp,
                    lineHeight = 8.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (shiftActive) palette.alphaText else palette.shiftText
                )
            }
            val labelLen = key.label.length
            val fontSize = when {
                key.kind == FxKeyKind.Number    -> 24.sp
                key.kind == FxKeyKind.Operator  -> 23.sp
                key.kind == FxKeyKind.Equals    -> 24.sp
                key.kind == FxKeyKind.Clear     -> 16.sp
                key.kind == FxKeyKind.Action    -> 12.sp
                key.kind == FxKeyKind.Memory    -> 17.sp
                labelLen <= 1                   -> 22.sp
                labelLen <= 2                   -> 20.sp
                labelLen <= 3                   -> 18.sp
                labelLen <= 4                   -> 16.sp
                labelLen <= 5                   -> 14.sp
                labelLen <= 6                   -> 12.sp
                else                            -> 10.sp
            }
            Text(
                text = key.label,
                modifier = Modifier
                    .align(if (key.shiftLabel.isBlank()) Alignment.Center else Alignment.BottomCenter)
                    .padding(
                        start = 1.dp,
                        end = 1.dp,
                        bottom = if (key.shiftLabel.isBlank()) 0.dp else 4.dp
                    ),
                maxLines = 1,
                overflow = TextOverflow.Clip,
                fontSize = fontSize,
                lineHeight = fontSize,
                fontFamily = FontFamily.SansSerif,
                fontWeight = when (key.kind) {
                    FxKeyKind.Number -> FontWeight.Medium
                    FxKeyKind.Operator, FxKeyKind.Memory -> FontWeight.SemiBold
                    else -> FontWeight.Bold
                },
                textAlign = TextAlign.Center,
                color = when (key.kind) {
                    FxKeyKind.Clear -> palette.clearText
                    FxKeyKind.Operator, FxKeyKind.Equals -> palette.operatorText
                    FxKeyKind.Number -> palette.numberText
                    else -> palette.primaryText
                }
            )
        }
    }
}

@Composable
private fun CalculatorDisplay(
    expression: String,
    resultText: String,
    errorText: String?,
    degreesMode: Boolean,
    memory: Double,
    onExpressionChange: (String) -> Unit,
    onToggleMode: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onUseAns: () -> Unit,
    onMemoryRecall: () -> Unit,
    onMemoryClear: () -> Unit,
    onCopyResult: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(onClick = onToggleMode, label = { Text(if (degreesMode) "DEG" else "RAD") })
                AssistChip(onClick = onUseAns, label = { Text("Ans") })
                AssistChip(onClick = onMemoryRecall, label = { Text("MR") })
                AssistChip(onClick = onMemoryClear, label = { Text("MC") })
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "M=${formatCalculatorNumber(memory)}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            OutlinedTextField(
                value = expression,
                onValueChange = onExpressionChange,
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                label = { Text("Ekspresi") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 18.sp
                )
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = GreenLightBg,
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = resultText,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.End,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 26.sp,
                        color = GreenPrimary
                    )
                    if (errorText != null) {
                        Text(
                            text = errorText,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("AC")
                }
                OutlinedButton(onClick = onBackspace, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Backspace, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("DEL")
                }
                OutlinedButton(onClick = onCopyResult, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("USE")
                }
            }
        }
    }
}

@Composable
private fun CalculatorShortcutPanel(
    onInsert: (String) -> Unit,
    onSetExpression: (String) -> Unit
) {
    val shortcuts = listOf(
        "DMS" to "dms(",
        "HMS" to "hms(",
        "JD" to "jd(",
        "Delta T" to "dt(",
        "Mod" to "mod(",
        "Abs" to "abs(",
        "Int" to "int(",
        "Frac" to "frac(",
        "Floor" to "floor(",
        "Ceil" to "ceil(",
        "Rnd" to "round(",
        "Norm" to "norm360(",
        "Norm-" to "norm180(",
        "sinD" to "sind(",
        "cosD" to "cosd(",
        "tanD" to "tand("
    )
    val examples = listOf(
        "Azimut kiblat" to "atan2(sind(39.8262-107.3191), cosd(-6.3133)*tand(21.4225)-sind(-6.3133)*cosd(39.8262-107.3191))",
        "JD hari ini" to "jd(2026,6,6)",
        "Delta T 2026" to "dt(jd(2026,6,6))",
        "DMS ke derajat" to "dms(6,18,48)"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Fungsi falak cepat", fontWeight = FontWeight.Bold)
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 154.dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(shortcuts) { shortcut ->
                    OutlinedButton(
                        onClick = { onInsert(shortcut.second) },
                        modifier = Modifier.height(40.dp),
                        contentPadding = ButtonDefaults.ContentPadding
                    ) {
                        Text(shortcut.first, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                examples.take(2).forEach { example ->
                    Button(
                        onClick = { onSetExpression(example.second) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text(example.first, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                examples.drop(2).forEach { example ->
                    Button(
                        onClick = { onSetExpression(example.second) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text(example.first, fontSize = 11.sp, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun CalculatorKeypad(
    onInsert: (String) -> Unit,
    onEvaluate: () -> Unit,
    onClear: () -> Unit,
    onBackspace: () -> Unit,
    onMemoryAdd: () -> Unit,
    onMemorySubtract: () -> Unit,
    onMemoryStore: () -> Unit
) {
    val keys = listOf(
        CalcKey("sin", "sin(", CalcKeyKind.Function),
        CalcKey("cos", "cos(", CalcKeyKind.Function),
        CalcKey("tan", "tan(", CalcKeyKind.Function),
        CalcKey("ln", "ln(", CalcKeyKind.Function),
        CalcKey("log", "log(", CalcKeyKind.Function),
        CalcKey("sqrt", "sqrt(", CalcKeyKind.Function),
        CalcKey("x^y", "^", CalcKeyKind.Operator),
        CalcKey("x!", "!", CalcKeyKind.Operator),
        CalcKey("%", "%", CalcKeyKind.Operator),
        CalcKey("mod", "mod(", CalcKeyKind.Function),
        CalcKey("pi", "pi", CalcKeyKind.Function),
        CalcKey("e", "e", CalcKeyKind.Function),
        CalcKey("7", "7", CalcKeyKind.Number),
        CalcKey("8", "8", CalcKeyKind.Number),
        CalcKey("9", "9", CalcKeyKind.Number),
        CalcKey("/", "/", CalcKeyKind.Operator),
        CalcKey("4", "4", CalcKeyKind.Number),
        CalcKey("5", "5", CalcKeyKind.Number),
        CalcKey("6", "6", CalcKeyKind.Number),
        CalcKey("*", "*", CalcKeyKind.Operator),
        CalcKey("1", "1", CalcKeyKind.Number),
        CalcKey("2", "2", CalcKeyKind.Number),
        CalcKey("3", "3", CalcKeyKind.Number),
        CalcKey("-", "-", CalcKeyKind.Operator),
        CalcKey("0", "0", CalcKeyKind.Number),
        CalcKey(".", ".", CalcKeyKind.Number),
        CalcKey(",", ",", CalcKeyKind.Operator),
        CalcKey("+", "+", CalcKeyKind.Operator),
        CalcKey("M+", "", CalcKeyKind.Memory, onMemoryAdd),
        CalcKey("M-", "", CalcKeyKind.Memory, onMemorySubtract),
        CalcKey("MS", "", CalcKeyKind.Memory, onMemoryStore),
        CalcKey("=", "", CalcKeyKind.Equals, onEvaluate)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(448.dp),
                userScrollEnabled = false,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(keys) { key ->
                    CalculatorKeyButton(
                        key = key,
                        onClick = {
                            key.action?.invoke() ?: onInsert(key.insert)
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f)) { Text("AC") }
                OutlinedButton(onClick = onBackspace, modifier = Modifier.weight(1f)) { Text("DEL") }
            }
        }
    }
}

@Composable
private fun CalculatorKeyButton(
    key: CalcKey,
    onClick: () -> Unit
) {
    val colors = when (key.kind) {
        CalcKeyKind.Number -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
        CalcKeyKind.Operator -> ButtonDefaults.buttonColors(
            containerColor = GreenLightBg,
            contentColor = GreenPrimary
        )
        CalcKeyKind.Function -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
        CalcKeyKind.Memory -> ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
        CalcKeyKind.Equals -> ButtonDefaults.buttonColors(
            containerColor = GreenPrimary,
            contentColor = Color.White
        )
    }

    Button(
        onClick = onClick,
        modifier = Modifier.height(48.dp),
        shape = RoundedCornerShape(10.dp),
        colors = colors,
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(key.label, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun HistoryRow(
    item: CalculatorHistory,
    onUseExpression: () -> Unit,
    onUseResult: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.expression, fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                Text(
                    item.result,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GreenPrimary
                )
            }
            OutlinedButton(onClick = onUseExpression) { Text("EXPR") }
            Button(onClick = onUseResult, colors = ButtonDefaults.buttonColors(containerColor = GreenPrimary)) {
                Text("ANS")
            }
        }
    }
}

private data class CalculatorHistory(val expression: String, val result: String)

private data class FxKey(
    val label: String,
    val insert: String,
    val shiftLabel: String = "",
    val shiftInsert: String = "",
    val kind: FxKeyKind = FxKeyKind.Function,
    val action: (() -> Unit)? = null,
    val shiftAction: (() -> Unit)? = null  // aksi khusus saat SHIFT aktif
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

private data class CalcKey(
    val label: String,
    val insert: String,
    val kind: CalcKeyKind,
    val action: (() -> Unit)? = null
)

private enum class CalcKeyKind {
    Number,
    Operator,
    Function,
    Memory,
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
            var value = parsePower()
            while (true) {
                skipSpaces()
                value = when {
                    consume('*') -> value * parsePower()
                    consume('/') -> value / parsePower()
                    else -> return value
                }
            }
        }

        private fun parsePower(): Double {
            var value = parseUnary()
            skipSpaces()
            if (consume('^')) {
                value = value.pow(parsePower())
            }
            return value
        }

        private fun parseUnary(): Double {
            skipSpaces()
            return when {
                consume('+') -> parseUnary()
                consume('-') -> -parseUnary()
                else -> parsePostfix()
            }
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
            "atan2" -> {
                val (y, x) = two()
                if (degreesMode) Math.toDegrees(kotlin.math.atan2(y, x)) else kotlin.math.atan2(y, x)
            }
            "sqrt" -> sqrt(one())
            "cbrt" -> Math.cbrt(one())
            "ln" -> ln(one())
            "log" -> log10(one())
            "exp" -> exp(one())
            "abs" -> abs(one())
            "int" -> truncate(one())
            "trunc", "truncate" -> truncate(one())
            "normal" -> round(one())
            "keatas" -> ceil(one())
            "kebawah" -> floor(one())
            "floor" -> floor(one())
            "ceil" -> ceil(one())
            "round" -> round(one())
            "rad" -> Math.toRadians(one())
            "deg" -> Math.toDegrees(one())
            "frac" -> one() - floor(one())
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

