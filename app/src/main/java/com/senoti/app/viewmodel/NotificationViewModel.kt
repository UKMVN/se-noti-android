package com.senoti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.senoti.app.data.AppInfo
import com.senoti.app.data.NotificationEntity
import com.senoti.app.data.NotificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NotificationUiState(
    val searchQuery: String = "",
    val selectedApp: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationViewModel(private val repository: NotificationRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationUiState())
    val uiState: StateFlow<NotificationUiState> = _uiState.asStateFlow()

    val notifications: StateFlow<List<NotificationEntity>> = _uiState
        .flatMapLatest { state ->
            when {
                state.searchQuery.isNotBlank() -> repository.searchNotifications(state.searchQuery)
                state.selectedApp != null -> repository.getNotificationsByPackage(state.selectedApp)
                else -> repository.allNotifications
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distinctApps: StateFlow<List<AppInfo>> = repository.getDistinctApps()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadCount: StateFlow<Int> = repository.unreadCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    init {
        viewModelScope.launch {
            repository.deleteExpiredNotifications()
        }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query, selectedApp = null)
    }

    fun onAppSelected(packageName: String?) {
        _uiState.value = _uiState.value.copy(selectedApp = packageName, searchQuery = "")
    }

    fun markAsRead(id: Long) {
        viewModelScope.launch {
            repository.markAsRead(id)
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            repository.markAllAsRead()
        }
    }

    fun deleteNotification(id: Long) {
        viewModelScope.launch {
            repository.deleteById(id)
        }
    }

    fun deleteAllNotifications() {
        viewModelScope.launch {
            repository.deleteAll()
        }
    }

    fun getNotificationById(id: Long) = repository.getNotificationById(id)

    class Factory(private val repository: NotificationRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotificationViewModel::class.java)) {
                return NotificationViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
