package com.falak.falakpro.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

import com.falak.falakpro.R

@Composable
fun FalakProSplashScreen(onSplashFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    // Fade in
    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "alpha"
    )

    // Scale: zoom dari kecil ke normal
    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.75f,
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "scale"
    )

    // Tagline fade in (delayed)
    val taglineAlpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 800, delayMillis = 600, easing = LinearEasing),
        label = "taglineAlpha"
    )

    // Loading indicator progress
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val loadingProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "loadingProgress"
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(3000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        contentAlignment = Alignment.Center
    ) {
        // Konten utama: logo + teks
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .scale(scaleAnim)
                .alpha(alphaAnim)
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.logo_falakpro),
                contentDescription = "Logo FalakPro",
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .wrapContentHeight()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Garis dekoratif tipis berwarna teal
            Box(
                modifier = Modifier
                    .width(120.dp)
                    .height(2.dp)
                    .background(Color(0xFF1D6E71).copy(alpha = 0.4f))
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Tagline
            Text(
                text = "LF PWNU JAWA BARAT",
                color = Color(0xFF1D6E71),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.alpha(taglineAlpha)
            )
        }

        // Loading bar di bagian bawah
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 72.dp)
                .width(160.dp)
                .height(3.dp)
                .background(Color(0xFFE0E0E0))
        ) {
            // Indikator bergerak
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.35f)
                    .offset(x = (160 * loadingProgress).dp - (160 * 0.35f * loadingProgress).dp)
                    .background(Color(0xFF1D6E71))
            )
        }

        // Copyright
        Text(
            text = "LF PWNU Jawa Barat © 2026",
            color = Color(0xFF9E9E9E),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 40.dp)
                .alpha(taglineAlpha)
        )
    }
}

