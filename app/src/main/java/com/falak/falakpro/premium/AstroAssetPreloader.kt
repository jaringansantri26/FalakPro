package com.falak.falakpro.premium

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AstroAssetPreloader {
    private val lock = Any()
    private const val NUTATION_ASSET = "iau2000a_nutation.bin"
    private const val LUNAR_ASSET = "mpp02_core.bin"
    private const val SOLAR_ASSET = "earth_vsop87d.bin"

    suspend fun ensureCore(context: Context) {
        withContext(Dispatchers.IO) {
            ensureCoreBlocking(context.applicationContext)
        }
    }

    suspend fun ensureSolar(context: Context) {
        withContext(Dispatchers.IO) {
            ensureSolarBlocking(context.applicationContext)
        }
    }

    fun ensureCoreBlocking(context: Context?) {
        if (context == null) return
        if (ElpDataProvider.isInitialized && Vsop87SolarEngine.isInitialized && Iau2006Nutation.isInitialized) return
        synchronized(lock) {
            ensureNutationLoaded(context)
            ensureLunarLoaded(context)
            ensureSolarLoaded(context)
        }
    }

    fun ensureSolarBlocking(context: Context?) {
        if (context == null || (Vsop87SolarEngine.isInitialized && Iau2006Nutation.isInitialized)) return
        synchronized(lock) {
            ensureNutationLoaded(context)
            ensureSolarLoaded(context)
        }
    }

    private fun ensureNutationLoaded(context: Context) {
        if (!Iau2006Nutation.isInitialized) {
            context.assets.open(NUTATION_ASSET).use { Iau2006Nutation.initialize(it) }
        }
    }

    private fun ensureLunarLoaded(context: Context) {
        if (!ElpDataProvider.isInitialized) {
            context.assets.open(LUNAR_ASSET).use { ElpDataProvider.initialize(it) }
        }
    }

    private fun ensureSolarLoaded(context: Context) {
        if (!Vsop87SolarEngine.isInitialized) {
            context.assets.open(SOLAR_ASSET).use { Vsop87SolarEngine.initialize(it) }
        }
    }
}
