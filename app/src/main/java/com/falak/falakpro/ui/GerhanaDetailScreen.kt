package com.falak.falakpro.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.falak.falakpro.ui.components.EclipseDetailContent
import com.falak.falakpro.ui.components.LunarDetailContent
import com.falak.falakpro.ui.components.CombinedSolarEclipseDetailContent
import com.falak.falakpro.ui.components.FalakHeaderBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GerhanaDetailScreen(
    jdeApprox: Double,
    isSolar: Boolean,
    typology: String,
    lat: Double,
    lon: Double,
    elev: Double,
    timezone: Double,
    locName: String = "",
    viewModel: GerhanaDetailViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val solarDetail by viewModel.solarDetail.collectAsState()
    val localSolarDetail by viewModel.localSolarDetail.collectAsState()
    val lunarDetail by viewModel.lunarDetail.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    val tealPrimary = Color(0xFF00897B)
    val bgWhite = MaterialTheme.colorScheme.background

    LaunchedEffect(jdeApprox, isSolar, typology, lat, lon, elev, timezone, locName) {
        viewModel.calculate(jdeApprox, context, isSolar, typology, lat, lon, elev, timezone, locName)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgWhite)
    ) {
        FalakHeaderBar(
            title = if (isSolar) "Detail Gerhana Matahari" else "Detail Gerhana Bulan",
            onBack = onBack
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = tealPrimary)
            }
        } else {
            if (isSolar) {
                val global = solarDetail
                if (typology == "Global") {
                    if (global != null) {
                        EclipseDetailContent(detail = global)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = tealPrimary)
                        }
                    }
                } else {
                    val local = localSolarDetail
                    if (local != null && global != null) {
                        CombinedSolarEclipseDetailContent(local = local, global = global)
                    } else {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = tealPrimary)
                        }
                    }
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    lunarDetail?.let { d ->
                        LunarDetailContent(detail = d)
                    }
                }
            }
        }
    }
}

@Composable
fun ShadowRuleChip(label: String, isSelected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.clickable { onClick() },
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = if (isSelected) Color(0xFF00897B) else Color(0xFFF5F5F5),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) Color(0xFF00897B) else Color.LightGray)
    ) {
        Box(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (isSelected) Color.White else Color.Black, fontWeight = FontWeight.Medium, fontSize = 12.sp)
        }
    }
}
