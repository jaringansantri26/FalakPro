package com.falak.falakpro.ui

import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import com.falak.falakpro.premium.PreferencesHelper

@Composable
internal fun rememberKiblatShowSunMoonState(prefs: PreferencesHelper): MutableState<Boolean> {
    val showSunMoon = remember { mutableStateOf(prefs.kiblatShowSunMoon) }

    LaunchedEffect(prefs) {
        showSunMoon.value = prefs.kiblatShowSunMoon
    }

    DisposableEffect(prefs) {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == "kiblatShowSunMoon") {
                showSunMoon.value = prefs.kiblatShowSunMoon
            }
        }
        prefs.registerListener(listener)
        onDispose { prefs.unregisterListener(listener) }
    }

    return showSunMoon
}
