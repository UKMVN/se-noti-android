package com.senoti.app.service

import android.util.Base64
import android.util.Log
import com.senoti.app.data.NotificationEntity
import com.senoti.app.data.PublishLogEntity
import com.senoti.app.data.PushSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Result of an Ably publish attempt, used to create log entries.
 */
data class PublishResult(
    val isSuccess: Boolean,
    val httpCode: Int? = null,
    val errorMessage: String? = null,
    val requestBody: String,
    val durationMs: Long
)

/**
 * Publishes notification data to Ably via REST API.
 * No external SDK needed - uses built-in HttpURLConnection.
 */
object AblyPublisher {

    private const val TAG = "AblyPublisher"
    private const val ABLY_REST_URL = "https://rest.ably.io/channels/%s/messages"
    private const val DEFAULT_CUSTOMER_ID = "SE2029"
    private const val FALLBACK_CHANNEL_NAME = "channel-none"

    /**
     * Publish a notification to Ably channel based on user settings.
     * Returns PublishResult with full details for logging.
     */
    suspend fun publish(notification: NotificationEntity, settings: PushSettings): PublishResult {
        if (!settings.isEnabled) {
            return PublishResult(
                isSuccess = true,
                requestBody = "{}",
                durationMs = 0,
                errorMessage = "Push disabled"
            )
        }
        if (settings.ablyApiKey.isBlank()) {
            return PublishResult(
                isSuccess = false,
                requestBody = "{}",
                durationMs = 0,
                errorMessage = "Ably API key is empty"
            )
        }

        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val data = buildMessageData(notification, settings)
                val effectiveChannelName = getEffectiveChannelName(settings.channelName)
                val body = JSONObject().apply {
                    put("name", settings.eventName)
                    put("data", data.toString())
                }
                val bodyString = body.toString()

                val channelEncoded = java.net.URLEncoder.encode(effectiveChannelName, "UTF-8")
                val url = URL(String.format(ABLY_REST_URL, channelEncoded))
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Basic ${encodeApiKey(settings.ablyApiKey)}")
                    settings.clientId.trim().takeIf { it.isNotEmpty() }?.let {
                        setRequestProperty("X-Ably-ClientId", encodeClientId(it))
                    }
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(bodyString)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val responseBody = try {
                    val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                    stream?.let {
                        BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() }
                    } ?: ""
                } catch (e: Exception) { "" }

                connection.disconnect()
                val duration = System.currentTimeMillis() - startTime

                if (responseCode in 200..299) {
                    Log.d(TAG, "Published to Ably [${duration}ms]: ${notification.title} -> $effectiveChannelName")
                    PublishResult(
                        isSuccess = true,
                        httpCode = responseCode,
                        requestBody = data.toString(2),
                        durationMs = duration
                    )
                } else {
                    val errorMsg = "HTTP $responseCode: $responseBody"
                    Log.e(TAG, "Ably error: $errorMsg")
                    PublishResult(
                        isSuccess = false,
                        httpCode = responseCode,
                        errorMessage = errorMsg,
                        requestBody = data.toString(2),
                        durationMs = duration
                    )
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "Failed to publish to Ably", e)
                PublishResult(
                    isSuccess = false,
                    errorMessage = "${e.javaClass.simpleName}: ${e.message}",
                    requestBody = "{}",
                    durationMs = duration
                )
            }
        }
    }

    /**
     * Re-publish an existing payload from a publish log entry.
     */
    suspend fun republish(log: PublishLogEntity, apiKey: String, clientId: String): PublishResult {
        if (apiKey.isBlank()) {
            return PublishResult(
                isSuccess = false,
                requestBody = log.requestBody,
                durationMs = 0,
                errorMessage = "Ably API key is empty"
            )
        }

        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val effectiveChannelName = getEffectiveChannelName(log.channelName)
                val body = JSONObject().apply {
                    put("name", log.eventName)
                    put("data", log.requestBody)
                }
                val bodyString = body.toString()

                val channelEncoded = java.net.URLEncoder.encode(effectiveChannelName, "UTF-8")
                val url = URL(String.format(ABLY_REST_URL, channelEncoded))
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Basic ${encodeApiKey(apiKey)}")
                    clientId.trim().takeIf { it.isNotEmpty() }?.let {
                        setRequestProperty("X-Ably-ClientId", encodeClientId(it))
                    }
                    doOutput = true
                    connectTimeout = 10_000
                    readTimeout = 10_000
                }

                OutputStreamWriter(connection.outputStream).use { writer ->
                    writer.write(bodyString)
                    writer.flush()
                }

                val responseCode = connection.responseCode
                val responseBody = try {
                    val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
                    stream?.let {
                        BufferedReader(InputStreamReader(it)).use { reader -> reader.readText() }
                    } ?: ""
                } catch (e: Exception) { "" }

                connection.disconnect()
                val duration = System.currentTimeMillis() - startTime

                if (responseCode in 200..299) {
                    Log.d(TAG, "Republished to Ably [${duration}ms]: ${log.eventName} -> $effectiveChannelName")
                    PublishResult(
                        isSuccess = true,
                        httpCode = responseCode,
                        requestBody = log.requestBody,
                        durationMs = duration
                    )
                } else {
                    val errorMsg = "HTTP $responseCode: $responseBody"
                    Log.e(TAG, "Ably republish error: $errorMsg")
                    PublishResult(
                        isSuccess = false,
                        httpCode = responseCode,
                        errorMessage = errorMsg,
                        requestBody = log.requestBody,
                        durationMs = duration
                    )
                }
            } catch (e: Exception) {
                val duration = System.currentTimeMillis() - startTime
                Log.e(TAG, "Failed to republish to Ably", e)
                PublishResult(
                    isSuccess = false,
                    errorMessage = "${e.javaClass.simpleName}: ${e.message}",
                    requestBody = log.requestBody,
                    durationMs = duration
                )
            }
        }
    }

    /**
     * Create a PublishLogEntity from a PublishResult.
     */
    fun createLogEntry(
        result: PublishResult,
        notification: NotificationEntity,
        settings: PushSettings
    ): PublishLogEntity {
        val effectiveChannelName = getEffectiveChannelName(settings.channelName)
        return PublishLogEntity(
            channelName = effectiveChannelName,
            eventName = settings.eventName,
            isSuccess = result.isSuccess,
            httpCode = result.httpCode,
            errorMessage = result.errorMessage,
            requestBody = result.requestBody,
            notificationTitle = notification.title,
            notificationAppName = notification.appName,
            durationMs = result.durationMs
        )
    }

    /**
     * Build the JSON data payload based on user's toggle settings.
     */
    private fun buildMessageData(notification: NotificationEntity, settings: PushSettings): JSONObject {
        val data = JSONObject()
        data.put("customerID", DEFAULT_CUSTOMER_ID)

        if (settings.sendAppName) {
            data.put("appName", notification.appName)
        }
        if (settings.sendPackageName) {
            data.put("packageName", notification.packageName)
        }
        if (settings.sendTitle) {
            data.put("title", notification.title)
        }
        if (settings.sendText) {
            data.put("text", notification.text)
        }
        if (settings.sendSubText && !notification.subText.isNullOrBlank()) {
            data.put("subText", notification.subText)
        }
        if (settings.sendTimestamp) {
            data.put("timestamp", notification.timestamp)
        }

        // Add user-defined custom fields
        settings.customFields.forEach { (key, value) ->
            if (key.isNotBlank()) {
                data.put(key, value)
            }
        }

        return data
    }

    /**
     * Encode Ably API key to Base64 for Basic auth header.
     */
    private fun encodeApiKey(apiKey: String): String {
        return Base64.encodeToString(apiKey.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun encodeClientId(clientId: String): String {
        return Base64.encodeToString(clientId.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun getEffectiveChannelName(channelName: String): String {
        return channelName.trim().ifBlank { FALLBACK_CHANNEL_NAME }
    }
}
