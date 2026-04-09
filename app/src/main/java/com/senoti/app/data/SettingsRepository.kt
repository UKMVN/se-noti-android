package com.senoti.app.data

import android.content.Context
import android.content.SharedPreferences
import com.senoti.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("push_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<PushSettings> = _settings.asStateFlow()

    private fun loadSettings(): PushSettings {
        return PushSettings(
            isEnabled = prefs.getBoolean(KEY_ENABLED, BuildConfig.DEFAULT_PUSH_ENABLED),
            ablyApiKey = prefs.getString(KEY_API_KEY, PushSettings().ablyApiKey) ?: PushSettings().ablyApiKey,
            clientId = DEFAULT_CLIENT_ID,
            channelName = prefs.getString(KEY_CHANNEL, DEFAULT_CHANNEL_NAME) ?: DEFAULT_CHANNEL_NAME,
            eventName = prefs.getString(KEY_EVENT_NAME, "new-notification") ?: "new-notification",
            autoDeleteMinutes = prefs.getInt(KEY_AUTO_DELETE_MINUTES, 2),
            autoDeleteImmediately = prefs.getBoolean(KEY_AUTO_DELETE_IMMEDIATELY, false),
            pushApiEnabled = prefs.getBoolean(KEY_PUSH_API_ENABLED, BuildConfig.DEFAULT_PUSH_API_ENABLED),
            customApiUrl = prefs.getString(KEY_CUSTOM_API_URL, BuildConfig.CUSTOM_API_URL) ?: BuildConfig.CUSTOM_API_URL,
            sendAppName = prefs.getBoolean(KEY_SEND_APP_NAME, true),
            sendPackageName = prefs.getBoolean(KEY_SEND_PACKAGE_NAME, true),
            sendTitle = prefs.getBoolean(KEY_SEND_TITLE, true),
            sendText = prefs.getBoolean(KEY_SEND_TEXT, true),
            sendSubText = prefs.getBoolean(KEY_SEND_SUB_TEXT, true),
            sendTimestamp = prefs.getBoolean(KEY_SEND_TIMESTAMP, true),
            customFields = loadCustomFields()
        )
    }

    fun saveSettings(settings: PushSettings) {
        prefs.edit().apply {
            putBoolean(KEY_ENABLED, settings.isEnabled)
            putString(KEY_API_KEY, settings.ablyApiKey)
            putString(KEY_CHANNEL, settings.channelName)
            putString(KEY_EVENT_NAME, settings.eventName)
            putInt(KEY_AUTO_DELETE_MINUTES, settings.autoDeleteMinutes)
            putBoolean(KEY_AUTO_DELETE_IMMEDIATELY, settings.autoDeleteImmediately)
            putBoolean(KEY_PUSH_API_ENABLED, settings.pushApiEnabled)
            putString(KEY_CUSTOM_API_URL, settings.customApiUrl)
            putBoolean(KEY_SEND_APP_NAME, settings.sendAppName)
            putBoolean(KEY_SEND_PACKAGE_NAME, settings.sendPackageName)
            putBoolean(KEY_SEND_TITLE, settings.sendTitle)
            putBoolean(KEY_SEND_TEXT, settings.sendText)
            putBoolean(KEY_SEND_SUB_TEXT, settings.sendSubText)
            putBoolean(KEY_SEND_TIMESTAMP, settings.sendTimestamp)
            saveCustomFields(this, settings.customFields)
            apply()
        }
        _settings.value = settings
    }

    fun getCurrentSettings(): PushSettings = _settings.value

    private fun loadCustomFields(): Map<String, String> {
        val json = prefs.getString(KEY_CUSTOM_FIELDS, null)
        if (json.isNullOrBlank()) {
            return defaultCustomFields()
        }
        return try {
            val jsonObj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            jsonObj.keys().forEach { key ->
                map[key] = jsonObj.getString(key)
            }
            if (!map.containsKey(DEFAULT_CUSTOMER_ID_KEY)) {
                map[DEFAULT_CUSTOMER_ID_KEY] = DEFAULT_CUSTOMER_ID_VALUE
            }
            map
        } catch (e: Exception) {
            defaultCustomFields()
        }
    }

    private fun defaultCustomFields(): Map<String, String> {
        return mapOf(DEFAULT_CUSTOMER_ID_KEY to DEFAULT_CUSTOMER_ID_VALUE)
    }

    private fun saveCustomFields(editor: SharedPreferences.Editor, fields: Map<String, String>) {
        val jsonObj = JSONObject()
        fields.forEach { (key, value) ->
            jsonObj.put(key, value)
        }
        editor.putString(KEY_CUSTOM_FIELDS, jsonObj.toString())
    }

    companion object {
        private const val KEY_ENABLED = "push_enabled"
        private const val KEY_API_KEY = "ably_api_key"
        private const val KEY_CHANNEL = "channel_name"
        private const val KEY_EVENT_NAME = "event_name"
        private const val KEY_AUTO_DELETE_MINUTES = "auto_delete_minutes"
        private const val KEY_AUTO_DELETE_IMMEDIATELY = "auto_delete_immediately"
        private const val KEY_PUSH_API_ENABLED = "push_api_enabled"
        private const val KEY_CUSTOM_API_URL = "custom_api_url"
        private const val KEY_SEND_APP_NAME = "send_app_name"
        private const val KEY_SEND_PACKAGE_NAME = "send_package_name"
        private const val KEY_SEND_TITLE = "send_title"
        private const val KEY_SEND_TEXT = "send_text"
        private const val KEY_SEND_SUB_TEXT = "send_sub_text"
        private const val KEY_SEND_TIMESTAMP = "send_timestamp"
        private const val KEY_CUSTOM_FIELDS = "custom_fields"
        private const val DEFAULT_CUSTOMER_ID_KEY = "customerID"
        private val DEFAULT_CHANNEL_NAME = BuildConfig.DEFAULT_CHANNEL_NAME
        private val DEFAULT_CLIENT_ID = BuildConfig.DEFAULT_CLIENT_ID
        private val DEFAULT_CUSTOMER_ID_VALUE = BuildConfig.DEFAULT_CUSTOMER_ID_VALUE
    }
}
