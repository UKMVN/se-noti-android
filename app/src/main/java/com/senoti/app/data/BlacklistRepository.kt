package com.senoti.app.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class BlacklistRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("notification_blacklist", Context.MODE_PRIVATE)

    suspend fun refreshBlacklistFromRemote() = withContext(Dispatchers.IO) {
        try {
            val connection = (URL(BLACKLIST_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
            }

            connection.inputStream.bufferedReader().use { reader ->
                val rawLines = reader.readLines()
                val parsed = rawLines
                    .map { it.trim() }
                    .filter { it.isNotBlank() }
                    .filterNot { it.startsWith("#") }
                    .toSet()

                prefs.edit().putStringSet(KEY_BLACKLIST_PACKAGES, parsed).apply()
                Log.d(TAG, "Blacklist updated: ${parsed.size} packages")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to refresh blacklist from remote", e)
        }
    }

    fun isPackageBlacklisted(packageName: String): Boolean {
        val cached = prefs.getStringSet(KEY_BLACKLIST_PACKAGES, emptySet()).orEmpty()
        return packageName in cached
    }

    companion object {
        private const val TAG = "BlacklistRepository"
        private const val KEY_BLACKLIST_PACKAGES = "blacklist_packages"
        private const val BLACKLIST_URL =
            "https://gist.githubusercontent.com/khongluadao/1961b734997d7a38e23e2cac6b1ab98a/raw/Blacklist-noti.md"
    }
}
