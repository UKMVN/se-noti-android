package com.senoti.app.service

import android.util.Log
import com.senoti.app.BuildConfig
import com.senoti.app.data.NotificationEntity
import com.senoti.app.data.PushSettings
import com.senoti.app.data.PublishLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

object ApiPublisher {

    private const val TAG = "ApiPublisher"

    suspend fun publish(notification: NotificationEntity, settings: PushSettings): PublishResult {
        if (!settings.pushApiEnabled) {
            return PublishResult(
                isSuccess = true,
                requestBody = "{}",
                durationMs = 0,
                errorMessage = "API push disabled"
            )
        }

        val endpoint = settings.customApiUrl.ifBlank { BuildConfig.CUSTOM_API_URL }
        if (endpoint.isBlank()) {
            return PublishResult(
                isSuccess = false,
                requestBody = "{}",
                durationMs = 0,
                errorMessage = "CUSTOM_API_URL is empty"
            )
        }

        return withContext(Dispatchers.IO) {
            val startTime = System.currentTimeMillis()
            try {
                val data = buildMessageData(notification, settings)
                val bodyString = data.toString()

                val url = URL(endpoint)
                val connection = url.openConnection() as HttpURLConnection

                connection.apply {
                    requestMethod = "POST"
                    setRequestProperty("Content-Type", "application/json")
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
                } catch (e: Exception) {
                    ""
                }

                connection.disconnect()
                val duration = System.currentTimeMillis() - startTime

                if (responseCode in 200..299) {
                    Log.d(TAG, "Pushed to API [${duration}ms]: ${notification.title}")
                    PublishResult(
                        isSuccess = true,
                        httpCode = responseCode,
                        requestBody = data.toString(2),
                        durationMs = duration
                    )
                } else {
                    val errorMsg = "HTTP $responseCode: $responseBody"
                    Log.e(TAG, "API error: $errorMsg")
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
                Log.e(TAG, "Failed to push to API", e)
                PublishResult(
                    isSuccess = false,
                    errorMessage = "${e.javaClass.simpleName}: ${e.message}",
                    requestBody = "{}",
                    durationMs = duration
                )
            }
        }
    }

    fun createLogEntry(
        result: PublishResult,
        notification: NotificationEntity
    ): PublishLogEntity {
        return PublishLogEntity(
            channelName = "CUSTOM_API",
            eventName = "custom_api_push",
            isSuccess = result.isSuccess,
            httpCode = result.httpCode,
            errorMessage = result.errorMessage,
            requestBody = result.requestBody,
            notificationTitle = notification.title,
            notificationAppName = notification.appName,
            durationMs = result.durationMs
        )
    }

    private fun buildMessageData(notification: NotificationEntity, settings: PushSettings): JSONObject {
        // Reuse the same JSON payload logic as AblyPublisher
        val data = JSONObject()

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

        // Custom fields
        settings.customFields.forEach { (key, value) ->
            if (key.isNotBlank()) {
                data.put(key, value)
            }
        }

        return data
    }
}

