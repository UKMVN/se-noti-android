package com.senoti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.senoti.app.data.PublishLogDao
import com.senoti.app.data.PublishLogEntity
import com.senoti.app.data.SettingsRepository
import com.senoti.app.service.AblyPublisher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class LogFilter {
    ALL, SUCCESS, FAILED
}

class PublishLogViewModel(
    private val dao: PublishLogDao,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    companion object {
        private const val PUBLISH_LOG_RETENTION_MS = 7L * 24 * 60 * 60 * 1000
    }

    private val _filter = MutableStateFlow(LogFilter.ALL)
    val filter: StateFlow<LogFilter> = _filter.asStateFlow()

    // Selected log for detail view
    private val _selectedLogId = MutableStateFlow<Long?>(null)
    val selectedLogId: StateFlow<Long?> = _selectedLogId.asStateFlow()

    @Suppress("OPT_IN_USAGE")
    val logs: StateFlow<List<PublishLogEntity>> = _filter
        .flatMapLatest { filter ->
            when (filter) {
                LogFilter.ALL -> dao.getAllLogs()
                LogFilter.SUCCESS -> dao.getSuccessLogs()
                LogFilter.FAILED -> dao.getFailedLogs()
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalCount: StateFlow<Int> = dao.getTotalCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val successCount: StateFlow<Int> = dao.getSuccessCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val failedCount: StateFlow<Int> = dao.getFailedCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun setFilter(filter: LogFilter) {
        _filter.value = filter
    }

    fun selectLog(id: Long?) {
        _selectedLogId.value = id
    }

    fun getLogById(id: Long) = dao.getLogById(id)

    fun deleteAll() {
        viewModelScope.launch {
            dao.deleteAll()
        }
    }

    fun deleteById(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
        }
    }

    fun republish(log: PublishLogEntity) {
        viewModelScope.launch {
            val settings = settingsRepository.getCurrentSettings()
            val result = AblyPublisher.republish(log, settings.ablyApiKey, settings.clientId)
            val cutoff = System.currentTimeMillis() - PUBLISH_LOG_RETENTION_MS
            dao.deleteOlderThan(cutoff)
            dao.insert(
                PublishLogEntity(
                    channelName = log.channelName,
                    eventName = log.eventName,
                    isSuccess = result.isSuccess,
                    httpCode = result.httpCode,
                    errorMessage = result.errorMessage,
                    requestBody = result.requestBody,
                    notificationTitle = log.notificationTitle,
                    notificationAppName = log.notificationAppName,
                    durationMs = result.durationMs
                )
            )
        }
    }

    class Factory(
        private val dao: PublishLogDao,
        private val settingsRepository: SettingsRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(PublishLogViewModel::class.java)) {
                return PublishLogViewModel(dao, settingsRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
