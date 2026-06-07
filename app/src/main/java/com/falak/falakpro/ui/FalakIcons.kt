package com.falak.falakpro.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

object FalakIcons {

    val HomeColor: ImageVector
        get() = ImageVector.Builder(
            name = "HomeColor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color(0xFF008B7D))) {
                moveTo(10f, 20f)
                lineTo(10f, 14f)
                lineTo(14f, 14f)
                lineTo(14f, 20f)
                lineTo(19f, 20f)
                lineTo(19f, 12f)
                lineTo(22f, 12f)
                lineTo(12f, 3f)
                lineTo(2f, 12f)
                lineTo(5f, 12f)
                lineTo(5f, 20f)
                close()
            }
            path(fill = SolidColor(Color(0xFFFF9800))) {
                moveTo(17f, 7f)
                curveToRelative(1.1f, 0f, 2f, -0.9f, 2f, -2f)
                reflectiveCurveToRelative(-0.9f, -2f, -2f, -2f)
                reflectiveCurveToRelative(-2f, 0.9f, -2f, 2f)
                reflectiveCurveToRelative(0.9f, 2f, 2f, 2f)
                close()
            }
        }.build()

    val SettingsColor: ImageVector
        get() = ImageVector.Builder(
            name = "SettingsColor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(fill = SolidColor(Color.Gray)) {
                moveTo(19.14f, 12.94f)
                curveToRelative(0.04f, -0.3f, 0.06f, -0.61f, 0.06f, -0.94f)
                curveToRelative(0f, -0.32f, -0.02f, -0.64f, -0.06f, -0.94f)
                lineToRelative(2.03f, -1.58f)
                curveToRelative(0.18f, -0.14f, 0.23f, -0.39f, 0.12f, -0.59f)
                lineToRelative(-1.92f, -3.32f)
                curveToRelative(-0.12f, -0.2f, -0.36f, -0.27f, -0.57f, -0.19f)
                lineToRelative(-2.39f, 0.96f)
                curveToRelative(-0.5f, -0.38f, -1.03f, -0.7f, -1.62f, -0.94f)
                lineToRelative(-0.36f, -2.54f)
                curveToRelative(-0.04f, -0.22f, -0.24f, -0.38f, -0.46f, -0.38f)
                lineTo(11.45f, 2f)
                lineTo(7.61f, 2f)
                curveToRelative(-0.22f, 0f, -0.42f, 0.17f, -0.46f, 0.38f)
                lineToRelative(-0.36f, 2.54f)
                curveToRelative(-0.59f, 0.24f, -1.13f, 0.56f, -1.62f, 0.94f)
                lineToRelative(-2.39f, -0.96f)
                curveToRelative(-0.22f, -0.08f, -0.45f, 0f, -0.57f, 0.19f)
                lineTo(2.31f, 8.91f)
                curveToRelative(-0.11f, 0.2f, -0.06f, 0.45f, 0.12f, 0.59f)
                lineToRelative(2.03f, 1.58f)
                curveToRelative(-0.04f, 0.3f, -0.06f, 0.61f, -0.06f, 0.94f)
                curveToRelative(0f, 0.32f, 0.02f, 0.64f, -0.06f, 0.94f)
                lineToRelative(-2.03f, 1.58f)
                curveToRelative(-0.18f, 0.14f, -0.23f, 0.39f, -0.12f, 0.59f)
                lineToRelative(1.92f, 3.32f)
                curveToRelative(0.12f, 0.2f, 0.36f, 0.27f, 0.57f, 0.19f)
                lineToRelative(2.39f, -0.96f)
                curveToRelative(0.5f, 0.38f, 1.03f, 0.7f, 1.62f, 0.94f)
                lineToRelative(0.36f, 2.54f)
                curveToRelative(0.04f, 0.22f, 0.24f, 0.38f, 0.46f, 0.38f)
                lineTo(12.55f, 22f)
                lineTo(16.39f, 22f)
                curveToRelative(0.22f, 0f, 0.42f, -0.17f, 0.46f, -0.38f)
                lineToRelative(0.36f, -2.54f)
                curveToRelative(0.59f, -0.24f, 1.13f, -0.56f, 1.62f, -0.94f)
                lineToRelative(2.39f, 0.96f)
                curveToRelative(0.22f, 0.08f, 0.45f, 0f, 0.57f, -0.19f)
                lineToRelative(1.92f, -3.32f)
                curveToRelative(0.12f, -0.2f, 0.07f, -0.45f, -0.12f, -0.59f)
                lineToRelative(-2.03f, -1.58f)
                close()
            }
            path(fill = SolidColor(Color(0xFF2196F3))) {
                moveTo(12f, 15f)
                curveToRelative(-1.66f, 0f, -3f, -1.34f, -3f, -3f)
                reflectiveCurveToRelative(1.34f, -3f, 3f, -3f)
                reflectiveCurveToRelative(3f, 1.34f, 3f, 3f)
                reflectiveCurveToRelative(-1.34f, 3f, -3f, 3f)
                close()
            }
        }.build()

    val HisabColor: ImageVector
        get() = ImageVector.Builder(
            name = "HisabColor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Crescent Moon
            path(fill = SolidColor(Color(0xFFFFB300))) {
                moveTo(12.7f, 2.2f)
                curveToRelative(-4.8f, 0f, -8.7f, 3.9f, -8.7f, 8.7f)
                curveToRelative(0f, 4.8f, 3.9f, 8.7f, 8.7f, 8.7f)
                curveToRelative(0.7f, 0f, 1.4f, -0.1f, 2.1f, -0.3f)
                curveToRelative(-3.4f, -1.3f, -5.8f, -4.7f, -5.8f, -8.6f)
                curveToRelative(0f, -3.8f, 2.3f, -7.1f, 5.5f, -8.5f)
                curveToRelative(-0.6f, -0.1f, -1.2f, -0.1f, -1.8f, -0.1f)
                close()
            }
            // Star
            path(fill = SolidColor(Color(0xFFFFA000))) {
                moveTo(17f, 4f)
                lineToRelative(1.2f, 2.4f)
                lineToRelative(2.6f, 0.4f)
                lineToRelative(-1.9f, 1.8f)
                lineToRelative(0.4f, 2.6f)
                lineToRelative(-2.3f, -1.2f)
                lineToRelative(-2.3f, 1.2f)
                lineToRelative(0.4f, -2.6f)
                lineToRelative(-1.9f, -1.8f)
                lineToRelative(2.6f, -0.4f)
                close()
            }
        }.build()

    val CalendarColor: ImageVector
        get() = ImageVector.Builder(
            name = "CalendarColor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Calendar Body (White)
            path(fill = SolidColor(Color.White)) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                lineTo(20f, 20f)
                curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
                lineTo(6f, 22f)
                curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
                close()
            }
            // Calendar Header (Red)
            path(fill = SolidColor(Color(0xFFE53935))) {
                moveTo(4f, 6f)
                curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
                lineTo(18f, 4f)
                curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
                lineTo(20f, 9f)
                lineTo(4f, 9f)
                close()
            }
            // Binding Rings
            path(fill = SolidColor(Color(0xFF616161))) {
                moveTo(7f, 2f)
                lineTo(7f, 5f)
                lineTo(9f, 5f)
                lineTo(9f, 2f)
                close()
                moveTo(15f, 2f)
                lineTo(15f, 5f)
                lineTo(17f, 5f)
                lineTo(17f, 2f)
                close()
            }
            // Date Dot
            path(fill = SolidColor(Color(0xFF1E88E5))) {
                moveTo(12f, 15f)
                curveToRelative(1.6f, 0f, 3f, 1.3f, 3f, 3f)
                reflectiveCurveToRelative(-1.3f, 3f, -3f, 3f)
                reflectiveCurveToRelative(-3f, -1.3f, -3f, -3f)
                reflectiveCurveToRelative(1.3f, -3f, 3f, -3f)
                close()
            }
        }.build()

    val KiblatColor: ImageVector
        get() = ImageVector.Builder(
            name = "KiblatColor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Compass Outer Rim
            path(fill = SolidColor(Color(0xFF90A4AE))) {
                moveTo(12f, 1f)
                curveToRelative(-6.1f, 0f, -11f, 4.9f, -11f, 11f)
                reflectiveCurveToRelative(4.9f, 11f, 11f, 11f)
                reflectiveCurveToRelative(11f, -4.9f, 11f, -11f)
                reflectiveCurveToRelative(-4.9f, -11f, -11f, -11f)
                close()
            }
            // Compass Inner Face
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 3f)
                curveToRelative(-5f, 0f, -9f, 4f, -9f, 9f)
                reflectiveCurveToRelative(4f, 9f, 9f, 9f)
                reflectiveCurveToRelative(9f, -4f, 9f, -9f)
                reflectiveCurveToRelative(-4f, -9f, -9f, -9f)
                close()
            }
            // Red Needle
            path(fill = SolidColor(Color(0xFFE53935))) {
                moveTo(12f, 12f)
                lineToRelative(2.5f, 0f)
                lineToRelative(-2.5f, -7.5f)
                lineToRelative(-2.5f, 7.5f)
                close()
            }
            // Blue Needle
            path(fill = SolidColor(Color(0xFF1E88E5))) {
                moveTo(12f, 12f)
                lineToRelative(2.5f, 0f)
                lineToRelative(-2.5f, 7.5f)
                lineToRelative(-2.5f, -7.5f)
                close()
            }
            // Center Dot
            path(fill = SolidColor(Color(0xFF37474F))) {
                moveTo(12f, 11f)
                curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                reflectiveCurveToRelative(-0.4f, 1f, -1f, 1f)
                reflectiveCurveToRelative(-1f, -0.4f, -1f, -1f)
                reflectiveCurveToRelative(0.4f, -1f, 1f, -1f)
                close()
            }
        }.build()

    val EclipseColor: ImageVector
        get() = ImageVector.Builder(
            name = "EclipseColor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Sun (Orange)
            path(fill = SolidColor(Color(0xFFFF9800))) {
                moveTo(12f, 3f)
                curveToRelative(-5f, 0f, -9f, 4f, -9f, 9f)
                reflectiveCurveToRelative(4f, 9f, 9f, 9f)
                reflectiveCurveToRelative(9f, -4f, 9f, -9f)
                reflectiveCurveToRelative(-4f, -9f, -9f, -9f)
                close()
            }
            // Moon (Dark Gray overlapping)
            path(fill = SolidColor(Color(0xFF424242))) {
                moveTo(15f, 3.5f)
                curveToRelative(-4.7f, 0f, -8.5f, 3.8f, -8.5f, 8.5f)
                reflectiveCurveToRelative(3.8f, 8.5f, 8.5f, 8.5f)
                reflectiveCurveToRelative(8.5f, -3.8f, 8.5f, -8.5f)
                reflectiveCurveToRelative(-3.8f, -8.5f, -8.5f, -8.5f)
                close()
            }
        }.build()

    val PrayerColor: ImageVector
        get() = ImageVector.Builder(
            name = "PrayerColor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Clock Outer Rim (Teal)
            path(fill = SolidColor(Color(0xFF009688))) {
                moveTo(12f, 2f)
                curveToRelative(-5.5f, 0f, -10f, 4.5f, -10f, 10f)
                reflectiveCurveToRelative(4.5f, 10f, 10f, 10f)
                reflectiveCurveToRelative(10f, -4.5f, 10f, -10f)
                reflectiveCurveToRelative(-4.5f, -10f, -10f, -10f)
                close()
            }
            // Clock Inner Face (White)
            path(fill = SolidColor(Color.White)) {
                moveTo(12f, 4f)
                curveToRelative(-4.4f, 0f, -8f, 3.6f, -8f, 8f)
                reflectiveCurveToRelative(3.6f, 8f, 8f, 8f)
                reflectiveCurveToRelative(8f, -3.6f, 8f, -8f)
                reflectiveCurveToRelative(-3.6f, -8f, -8f, -8f)
                close()
            }
            // Hour Hand (Dark Gray)
            path(fill = SolidColor(Color(0xFF424242))) {
                moveTo(11.2f, 7f)
                lineTo(12.8f, 7f)
                lineTo(12.8f, 13f)
                lineTo(11.2f, 13f)
                close()
            }
            // Minute Hand (Red)
            path(fill = SolidColor(Color(0xFFD32F2F))) {
                moveTo(12f, 11.2f)
                lineToRelative(4.5f, 4.5f)
                lineToRelative(-1f, 1f)
                lineToRelative(-4.5f, -4.5f)
                close()
            }
            // Center Dot
            path(fill = SolidColor(Color(0xFF424242))) {
                moveTo(12f, 11f)
                curveToRelative(0.6f, 0f, 1f, 0.4f, 1f, 1f)
                reflectiveCurveToRelative(-0.4f, 1f, -1f, 1f)
                reflectiveCurveToRelative(-1f, -0.4f, -1f, -1f)
                reflectiveCurveToRelative(0.4f, -1f, 1f, -1f)
                close()
            }
        }.build()

    val DataColor: ImageVector
        get() = ImageVector.Builder(
            name = "DataColor",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            // Folder Back (Orange)
            path(fill = SolidColor(Color(0xFFFFB300))) {
                moveTo(2f, 6f)
                curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
                lineTo(9f, 4f)
                lineToRelative(2f, 2f)
                lineTo(20f, 6f)
                curveToRelative(1.1f, 0f, 2f, 0.9f, 2f, 2f)
                lineTo(22f, 18f)
                curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
                lineTo(4f, 20f)
                curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
                close()
            }
            // Document (White)
            path(fill = SolidColor(Color.White)) {
                moveTo(6f, 8f)
                lineTo(18f, 8f)
                lineTo(18f, 18f)
                lineTo(6f, 18f)
                close()
            }
            // Charts (Blue and Green)
            path(fill = SolidColor(Color(0xFF2196F3))) {
                moveTo(8f, 14f)
                lineTo(10f, 14f)
                lineTo(10f, 16f)
                lineTo(8f, 16f)
                close()
                moveTo(14f, 10f)
                lineTo(16f, 10f)
                lineTo(16f, 16f)
                lineTo(14f, 16f)
                close()
            }
            path(fill = SolidColor(Color(0xFF4CAF50))) {
                moveTo(11f, 12f)
                lineTo(13f, 12f)
                lineTo(13f, 16f)
                lineTo(11f, 16f)
                close()
            }
            // Folder Front (Yellow)
            path(fill = SolidColor(Color(0xFFFFCA28))) {
                moveTo(2f, 10f)
                lineTo(22f, 10f)
                lineTo(22f, 18f)
                curveToRelative(0f, 1.1f, -0.9f, 2f, -2f, 2f)
                lineTo(4f, 20f)
                curveToRelative(-1.1f, 0f, -2f, -0.9f, -2f, -2f)
                close()
            }
        }.build()
}

