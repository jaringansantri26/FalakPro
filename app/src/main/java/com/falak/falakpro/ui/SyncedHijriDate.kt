package com.falak.falakpro.ui

import android.content.Context
import com.falak.falakpro.premium.AstroAssetPreloader
import com.falak.falakpro.premium.CalendarFunctions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun calculateSyncedHijriDate(
    context: Context,
    jde: Double,
    criteria: String
): Triple<Int, Int, Int> = withContext(Dispatchers.Default) {
    AstroAssetPreloader.ensureCore(context)
    CalendarFunctions.getIndonesianCalendarHijri(jde, criteria)
}
