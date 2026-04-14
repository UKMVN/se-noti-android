package com.senoti.app

import android.app.Application
import com.senoti.app.data.AppDatabase
import com.senoti.app.data.BlacklistRepository
import com.senoti.app.data.NotificationRepository
import com.senoti.app.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SeNotiApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { NotificationRepository(database.notificationDao()) }
    val settingsRepository by lazy { SettingsRepository(this) }
    val blacklistRepository by lazy { BlacklistRepository(this) }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            blacklistRepository.refreshBlacklistFromRemote()
        }
    }
}
