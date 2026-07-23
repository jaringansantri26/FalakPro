package com.falak.falakpro

import android.content.Context
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.falak.falakpro.premium.PreferencesHelper
import com.falak.falakpro.ui.theme.FalakProTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferencesHelper(newBase)
        val lang = prefs.appLanguage
        if (lang == "system") {
            super.attachBaseContext(newBase)
            return
        }

        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawable(ColorDrawable(Color(0xFF12161A).toArgb()))

        setContent {
            val prefs = remember { PreferencesHelper(this) }
            var appTheme by remember { mutableIntStateOf(prefs.appTheme) }
            val useDarkTheme = when (appTheme) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            FalakProTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainApp(
                        prefs = prefs,
                        onThemeChanged = { appTheme = prefs.appTheme }
                    )
                }
            }
        }
    }
}
