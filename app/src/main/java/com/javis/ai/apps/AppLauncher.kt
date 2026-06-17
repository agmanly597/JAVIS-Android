package com.javis.ai.apps

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppAction(
    val packageName: String,
    val appName: String,
    val searchQuery: String? = null,
    val intentAction: String? = null,
    val intentUri: String? = null
)

@Singleton
class AppLauncher @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val pm: PackageManager = context.packageManager

    private val knownApps = mapOf(
        listOf("youtube", "yt") to "com.google.android.youtube",
        listOf("whatsapp", "whats app", "wa") to "com.whatsapp",
        listOf("chrome", "browser", "google chrome") to "com.android.chrome",
        listOf("tiktok", "tik tok") to "com.zhiliaoapp.musically",
        listOf("instagram", "ig") to "com.instagram.android",
        listOf("facebook", "fb") to "com.facebook.katana",
        listOf("twitter", "x") to "com.twitter.android",
        listOf("telegram") to "org.telegram.messenger",
        listOf("camera") to "android.media.action.IMAGE_CAPTURE",
        listOf("settings") to "com.android.settings",
        listOf("contacts") to "com.android.contacts",
        listOf("gallery", "photos", "pictures") to "com.android.gallery3d",
        listOf("files", "file manager", "my files") to "com.android.documentsui",
        listOf("calculator", "calc") to "com.android.calculator2",
        listOf("clock", "alarm") to "com.android.deskclock",
        listOf("maps", "google maps", "navigation") to "com.google.android.apps.maps",
        listOf("gmail", "email") to "com.google.android.gm",
        listOf("spotify", "music") to "com.spotify.music",
        listOf("netflix") to "com.netflix.mediaclient",
        listOf("phone", "dialer") to "com.android.dialer",
        listOf("sms", "messages", "text") to "com.android.mms",
        listOf("playstore", "play store", "store") to "com.android.vending"
    )

    fun resolveApp(query: String): String? {
        val q = query.lowercase().trim()
        for ((aliases, pkg) in knownApps) {
            if (aliases.any { q.contains(it) }) {
                return pkg
            }
        }
        return findInstalledApp(q)
    }

    private fun findInstalledApp(query: String): String? {
        return try {
            val apps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
            apps.firstOrNull { app ->
                val label = pm.getApplicationLabel(app).toString().lowercase()
                label.contains(query) || app.packageName.lowercase().contains(query)
            }?.packageName
        } catch (e: Exception) { null }
    }

    fun launchApp(packageName: String): Boolean {
        return try {
            val intent = pm.getLaunchIntentForPackage(packageName)
                ?: return launchByAction(packageName)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("AppLauncher", "Failed to launch $packageName", e)
            false
        }
    }

    private fun launchByAction(action: String): Boolean {
        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun launchYouTubeSearch(query: String): Boolean {
        return try {
            val intent = Intent(Intent.ACTION_SEARCH).apply {
                setPackage("com.google.android.youtube")
                putExtra("query", query)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            val uri = Uri.parse("https://www.youtube.com/results?search_query=${Uri.encode(query)}")
            launchBrowser(uri.toString())
        }
    }

    fun launchGoogleSearch(query: String): Boolean {
        return try {
            val uri = Uri.parse("https://www.google.com/search?q=${Uri.encode(query)}")
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.android.chrome")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            launchBrowser("https://www.google.com/search?q=${Uri.encode(query)}")
        }
    }

    fun launchBrowser(url: String): Boolean {
        return try {
            val fullUrl = if (url.startsWith("http")) url else "https://$url"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(fullUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun openWhatsAppChat(contactName: String): Boolean {
        return try {
            val intent = pm.getLaunchIntentForPackage("com.whatsapp")?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            } ?: return false
            context.startActivity(intent)
            true
        } catch (e: Exception) { false }
    }

    fun getInstalledApps(): List<Pair<String, String>> {
        return try {
            pm.getInstalledApplications(PackageManager.GET_META_DATA)
                .filter { pm.getLaunchIntentForPackage(it.packageName) != null }
                .map { Pair(it.packageName, pm.getApplicationLabel(it).toString()) }
                .sortedBy { it.second }
        } catch (e: Exception) { emptyList() }
    }

    fun isAppInstalled(packageName: String): Boolean {
        return try {
            pm.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) { false }
    }
}
