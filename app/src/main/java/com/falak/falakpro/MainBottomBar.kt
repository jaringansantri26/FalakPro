package com.falak.falakpro

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.falak.falakpro.ui.theme.GreenLightBg
import com.falak.falakpro.ui.theme.GreenPrimary

@Composable
fun MainBottomBar(
    currentScreen: Screen,
    onNavigate: (Screen) -> Unit
) {
    if (!currentScreen.showsBottomBar()) return

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp
    ) {
        NavigationBarItem(
            selected = currentScreen is Screen.Home,
            onClick = { onNavigate(Screen.Home) },
            icon = {
                Icon(
                    imageVector = if (currentScreen is Screen.Home) {
                        Icons.Filled.Home
                    } else {
                        Icons.Outlined.Home
                    },
                    contentDescription = stringResource(R.string.menu_beranda),
                    tint = if (currentScreen is Screen.Home) GreenPrimary else Color.Gray
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.menu_beranda),
                    color = if (currentScreen is Screen.Home) GreenPrimary else Color.Gray
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = GreenLightBg)
        )

        NavigationBarItem(
            selected = currentScreen is Screen.KalenderAstronomis,
            onClick = { onNavigate(Screen.KalenderAstronomis) },
            icon = {
                Icon(
                    imageVector = if (currentScreen is Screen.KalenderAstronomis) {
                        Icons.Filled.DateRange
                    } else {
                        Icons.Outlined.DateRange
                    },
                    contentDescription = stringResource(R.string.menu_kalender),
                    tint = if (currentScreen is Screen.KalenderAstronomis) GreenPrimary else Color.Gray
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.menu_kalender),
                    color = if (currentScreen is Screen.KalenderAstronomis) GreenPrimary else Color.Gray
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = GreenLightBg)
        )

        NavigationBarItem(
            selected = currentScreen is Screen.Kiblat,
            onClick = { onNavigate(Screen.Kiblat) },
            icon = {
                Icon(
                    imageVector = if (currentScreen is Screen.Kiblat) {
                        Icons.Filled.Explore
                    } else {
                        Icons.Outlined.Explore
                    },
                    contentDescription = stringResource(R.string.menu_kiblat),
                    tint = if (currentScreen is Screen.Kiblat) GreenPrimary else Color.Gray
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.menu_kiblat),
                    color = if (currentScreen is Screen.Kiblat) GreenPrimary else Color.Gray
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = GreenLightBg)
        )

        NavigationBarItem(
            selected = currentScreen is Screen.Settings,
            onClick = { onNavigate(Screen.Settings) },
            icon = {
                Icon(
                    imageVector = if (currentScreen is Screen.Settings) {
                        Icons.Filled.Settings
                    } else {
                        Icons.Outlined.Settings
                    },
                    contentDescription = stringResource(R.string.menu_pengaturan),
                    tint = if (currentScreen is Screen.Settings) GreenPrimary else Color.Gray
                )
            },
            label = {
                Text(
                    text = stringResource(R.string.menu_pengaturan),
                    color = if (currentScreen is Screen.Settings) GreenPrimary else Color.Gray
                )
            },
            colors = NavigationBarItemDefaults.colors(indicatorColor = GreenLightBg)
        )
    }
}

private fun Screen.showsBottomBar(): Boolean {
    return this is Screen.Home ||
        this is Screen.KalenderAstronomis ||
        this is Screen.Kiblat ||
        this is Screen.Settings
}
