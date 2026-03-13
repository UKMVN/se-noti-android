package com.senoti.app.data

import kotlinx.coroutines.flow.Flow

class NotificationRepository(private val dao: NotificationDao) {
    companion object {
        private const val RETENTION_PERIOD_MS = 3L * 24 * 60 * 60 * 1000
    }

    val allNotifications: Flow<List<NotificationEntity>> = dao.getAllNotifications()
    val unreadCount: Flow<Int> = dao.getUnreadCount()

    fun getNotificationById(id: Long): Flow<NotificationEntity?> = dao.getNotificationById(id)

    fun getNotificationsByPackage(packageName: String): Flow<List<NotificationEntity>> =
        dao.getNotificationsByPackage(packageName)

    fun getDistinctApps(): Flow<List<AppInfo>> = dao.getDistinctApps()

    fun searchNotifications(query: String): Flow<List<NotificationEntity>> =
        dao.searchNotifications(query)

    suspend fun insert(notification: NotificationEntity): Long = dao.insert(notification)

    suspend fun deleteExpiredNotifications(now: Long = System.currentTimeMillis()) {
        val cutoff = now - RETENTION_PERIOD_MS
        dao.deleteOlderThan(cutoff)
    }

    suspend fun update(notification: NotificationEntity) = dao.update(notification)

    suspend fun delete(notification: NotificationEntity) = dao.delete(notification)

    suspend fun deleteById(id: Long) = dao.deleteById(id)

    suspend fun deleteAll() = dao.deleteAll()

    suspend fun markAsRead(id: Long) = dao.markAsRead(id)

    suspend fun markAllAsRead() = dao.markAllAsRead()
}
