package com.senoti.app.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PublishLogDao {

    @Query("SELECT * FROM publish_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<PublishLogEntity>>

    @Query("SELECT * FROM publish_logs WHERE isSuccess = 1 ORDER BY timestamp DESC")
    fun getSuccessLogs(): Flow<List<PublishLogEntity>>

    @Query("SELECT * FROM publish_logs WHERE isSuccess = 0 ORDER BY timestamp DESC")
    fun getFailedLogs(): Flow<List<PublishLogEntity>>

    @Query("SELECT * FROM publish_logs WHERE id = :id")
    fun getLogById(id: Long): Flow<PublishLogEntity?>

    @Query("SELECT COUNT(*) FROM publish_logs")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM publish_logs WHERE isSuccess = 1")
    fun getSuccessCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM publish_logs WHERE isSuccess = 0")
    fun getFailedCount(): Flow<Int>

    @Insert
    suspend fun insert(log: PublishLogEntity): Long

    @Query("DELETE FROM publish_logs WHERE timestamp < :cutoffTimestamp")
    suspend fun deleteOlderThan(cutoffTimestamp: Long)

    @Query("DELETE FROM publish_logs")
    suspend fun deleteAll()

    @Query("DELETE FROM publish_logs WHERE id = :id")
    suspend fun deleteById(id: Long)
}
