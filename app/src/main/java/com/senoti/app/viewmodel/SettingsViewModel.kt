package com.senoti.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.senoti.app.data.PushSettings
import com.senoti.app.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    private val _settings = MutableStateFlow(repository.getCurrentSettings())
    val settings: StateFlow<PushSettings> = _settings.asStateFlow()

    // Temporary state for a new custom field being added
    private val _newFieldKey = MutableStateFlow("")
    val newFieldKey: StateFlow<String> = _newFieldKey.asStateFlow()

    private val _newFieldValue = MutableStateFlow("")
    val newFieldValue: StateFlow<String> = _newFieldValue.asStateFlow()

    fun updateEnabled(enabled: Boolean) {
        updateAndSave(_settings.value.copy(isEnabled = enabled))
    }

    fun updateApiKey(apiKey: String) {
        updateAndSave(_settings.value.copy(ablyApiKey = apiKey))
    }

    fun updateChannelName(channel: String) {
        updateAndSave(_settings.value.copy(channelName = channel))
    }

    fun updateEventName(eventName: String) {
        updateAndSave(_settings.value.copy(eventName = eventName))
    }

    fun updateAutoDeleteMinutes(minutesInput: String) {
        val minutes = minutesInput.toIntOrNull()?.coerceAtLeast(0) ?: 0
        updateAndSave(_settings.value.copy(autoDeleteMinutes = minutes))
    }

    fun updateAutoDeleteImmediately(enabled: Boolean) {
        updateAndSave(_settings.value.copy(autoDeleteImmediately = enabled))
    }

    fun updateSendAppName(send: Boolean) {
        updateAndSave(_settings.value.copy(sendAppName = send))
    }

    fun updateSendPackageName(send: Boolean) {
        updateAndSave(_settings.value.copy(sendPackageName = send))
    }

    fun updateSendTitle(send: Boolean) {
        updateAndSave(_settings.value.copy(sendTitle = send))
    }

    fun updateSendText(send: Boolean) {
        updateAndSave(_settings.value.copy(sendText = send))
    }

    fun updateSendSubText(send: Boolean) {
        updateAndSave(_settings.value.copy(sendSubText = send))
    }

    fun updateSendTimestamp(send: Boolean) {
        updateAndSave(_settings.value.copy(sendTimestamp = send))
    }

    fun updateNewFieldKey(key: String) {
        _newFieldKey.value = key
    }

    fun updateNewFieldValue(value: String) {
        _newFieldValue.value = value
    }

    fun addCustomField() {
        val key = _newFieldKey.value.trim()
        val value = _newFieldValue.value.trim()
        if (key.isNotBlank()) {
            val updatedFields = _settings.value.customFields.toMutableMap()
            updatedFields[key] = value
            updateAndSave(_settings.value.copy(customFields = updatedFields))
            _newFieldKey.value = ""
            _newFieldValue.value = ""
        }
    }

    fun removeCustomField(key: String) {
        val updatedFields = _settings.value.customFields.toMutableMap()
        updatedFields.remove(key)
        updateAndSave(_settings.value.copy(customFields = updatedFields))
    }

    fun updateCustomField(oldKey: String, newKey: String, newValue: String) {
        val trimmedOldKey = oldKey.trim()
        val trimmedNewKey = newKey.trim()
        val trimmedValue = newValue.trim()
        if (trimmedOldKey.isBlank() || trimmedNewKey.isBlank()) return

        val updatedFields = _settings.value.customFields.toMutableMap()
        if (trimmedOldKey != trimmedNewKey) {
            updatedFields.remove(trimmedOldKey)
        }
        updatedFields[trimmedNewKey] = trimmedValue
        updateAndSave(_settings.value.copy(customFields = updatedFields))
    }

    private fun updateAndSave(newSettings: PushSettings) {
        _settings.value = newSettings
        repository.saveSettings(newSettings)
    }

    class Factory(private val repository: SettingsRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
