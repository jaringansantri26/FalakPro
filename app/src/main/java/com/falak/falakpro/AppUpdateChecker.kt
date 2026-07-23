package com.falak.falakpro

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppUpdateInfo(
    val versionCode: Long,
    val versionName: String,
    val apkUrl: String,
    val releasePage: String,
    val changelog: String,
    val forceUpdate: Boolean
) {
    val downloadUrl: String
        get() = apkUrl.ifBlank { releasePage }
}

object AppUpdateChecker {
    private const val PRIMARY_UPDATE_URL = "https://jaringansantri26.github.io/FalakPro/update.json"
    private const val FALLBACK_UPDATE_URL = "https://raw.githubusercontent.com/jaringansantri26/FalakPro/main/update.json"
    private const val CONNECT_TIMEOUT_MS = 7000
    private const val READ_TIMEOUT_MS = 7000

    suspend fun check(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        if (!context.hasInternetConnection()) return@withContext null

        val json = runCatching { fetchJson(PRIMARY_UPDATE_URL) }
            .getOrElse { runCatching { fetchJson(FALLBACK_UPDATE_URL) }.getOrNull() }
            ?: return@withContext null

        val updateInfo = parseUpdateInfo(json)
        val localVersionCode = context.currentVersionCode()

        if (updateInfo.versionCode > localVersionCode) {
            updateInfo
        } else {
            null
        }
    }

    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        try {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("market://details?id=$packageName")
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = android.content.Intent(
                android.content.Intent.ACTION_VIEW,
                android.net.Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
            ).apply {
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    private fun fetchJson(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            useCaches = false
        }

        return connection.use {
            if (responseCode !in 200..299) {
                throw IllegalStateException("Update check failed: HTTP $responseCode")
            }
            JSONObject(inputStream.bufferedReader().use { reader -> reader.readText() })
        }
    }

    private fun parseUpdateInfo(json: JSONObject): AppUpdateInfo {
        val defaultPlayStoreUrl = "https://play.google.com/store/apps/details?id=com.falak.falakpro"
        return AppUpdateInfo(
            versionCode = json.optLong("versionCode", 0L),
            versionName = json.optString("versionName", "1.0.0"),
            apkUrl = json.optString("apkUrl", defaultPlayStoreUrl),
            releasePage = json.optString("releasePage", defaultPlayStoreUrl),
            changelog = json.optString("changelog", "Versi terbaru FalakPro sudah tersedia di Google Play Store."),
            forceUpdate = json.optBoolean("forceUpdate", false)
        )
    }

    fun Context.currentVersionCode(): Long {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return PackageInfoCompat.getLongVersionCode(packageInfo)
    }

    fun Context.currentVersionName(): String {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return packageInfo.versionName ?: "1.0.0"
    }

    private fun Context.hasInternetConnection(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private inline fun <T : HttpURLConnection, R> T.use(block: T.() -> R): R {
        return try {
            block()
        } finally {
            disconnect()
        }
    }
}
