package com.falak.falakpro

import android.content.Context
import androidx.core.content.pm.PackageInfoCompat
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

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

    suspend fun check(context: Context): AppUpdateInfo? = withContext(Dispatchers.IO) {
        suspendCancellableCoroutine { continuation ->
            try {
                val appUpdateManager = AppUpdateManagerFactory.create(context)
                val appUpdateInfoTask = appUpdateManager.appUpdateInfo

                appUpdateInfoTask.addOnSuccessListener { info ->
                    if (info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                        val availableVersionCode = info.availableVersionCode().toLong()
                        val defaultPlayStoreUrl = "https://play.google.com/store/apps/details?id=${context.packageName}"

                        val updateInfo = AppUpdateInfo(
                            versionCode = availableVersionCode,
                            versionName = "Versi Baru",
                            apkUrl = defaultPlayStoreUrl,
                            releasePage = defaultPlayStoreUrl,
                            changelog = "Pembaruan fitur & peningkatan stabilitas aplikasi FalakPro sudah resmi tersedia di Google Play Store.",
                            forceUpdate = false
                        )
                        if (continuation.isActive) continuation.resume(updateInfo)
                    } else {
                        if (continuation.isActive) continuation.resume(null)
                    }
                }.addOnFailureListener { e ->
                    android.util.Log.e("AppUpdateChecker", "Google Play Update check failed: ${e.message}")
                    if (continuation.isActive) continuation.resume(null)
                }
            } catch (e: Exception) {
                android.util.Log.e("AppUpdateChecker", "Google Play Update check exception: ${e.message}")
                if (continuation.isActive) continuation.resume(null)
            }
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

    fun Context.currentVersionCode(): Long {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return PackageInfoCompat.getLongVersionCode(packageInfo)
    }

    fun Context.currentVersionName(): String {
        val packageInfo = packageManager.getPackageInfo(packageName, 0)
        return packageInfo.versionName ?: "1.0.0"
    }
}
