package com.senoti.app.data

data class PushSettings(
    val isEnabled: Boolean = true,
    val ablyApiKey: String = "lUyPUA.nmXh1A:8Cn23EhlVidxoB_45qIP4L41NqbWf8a9e4tKdZ2ZN4s",
    val clientId: String = "",
    val channelName: String = "notifications",
    val eventName: String = "new-notification",

    // Toggles for which data fields to include
    val sendAppName: Boolean = true,
    val sendPackageName: Boolean = false,
    val sendTitle: Boolean = true,
    val sendText: Boolean = true,
    val sendSubText: Boolean = false,
    val sendTimestamp: Boolean = true,

    // Custom key-value fields added by user
    val customFields: Map<String, String> = emptyMap()
)
