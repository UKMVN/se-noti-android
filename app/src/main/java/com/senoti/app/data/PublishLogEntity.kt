package com.senoti.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "publish_logs")
data class PublishLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val channelName: String,
    val eventName: String,
    val isSuccess: Boolean,
    val httpCode: Int? = null,
    val errorMessage: String? = null,
    val requestBody: String,       // JSON data đã gửi
    val notificationTitle: String,  // Tiêu đề notification gốc
    val notificationAppName: String, // App gốc
    val durationMs: Long = 0       // Thời gian request (ms)
)
