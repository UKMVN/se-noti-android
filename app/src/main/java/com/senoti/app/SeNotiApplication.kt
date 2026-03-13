package com.senoti.app

import android.app.Application
import com.senoti.app.data.AppDatabase
import com.senoti.app.data.NotificationRepository
import com.senoti.app.data.SettingsRepository

class SeNotiApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { NotificationRepository(database.notificationDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
}
