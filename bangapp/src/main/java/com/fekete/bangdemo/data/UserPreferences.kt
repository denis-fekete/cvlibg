package com.fekete.bangdemo.data

import kotlinx.serialization.Serializable

@Serializable
data class UserPreferences(
    var language: Language = Language.ENGLISH,
    var fontSize: Int = 13,
    var realtimeModel: String = "",
    var precisionModel: String = "",
    var showMetrics: Boolean = false,
    var verboseMetrics: Boolean = false,
    var framesToSkip: Int = 5,
)

@Serializable
enum class Language() {
    ENGLISH,
    CZECH,
    SLOVAK
}

fun Language.uiLabel(): String {
    return when (this) {
        Language.ENGLISH -> "English"
        Language.CZECH -> "Čeština"
        Language.SLOVAK -> "Slovenčina"
    }
}