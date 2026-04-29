package com.fekete.bangdemo.data

import kotlinx.serialization.Serializable

/**
 * Class used by the [com.fekete.bangdemo.MyApp.settingsService] to store user preferences.
 *
 * @param language used language
 * @param fontSize font size values, used for scaling text of other fragments and activities
 * @param realtimeModel key from the [com.fekete.cvlibg.detection.detectors.DetectorRegistry] for the realtime model
 * @param precisionModel key from the [com.fekete.cvlibg.detection.detectors.DetectorRegistry] for the quality model
 * @param showMetrics whenever metrics should be shown
 * @param verboseMetrics whenever verbose, more detailed metrics should be shown
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
@Serializable
data class UserPreferences(
    var language: Language = Language.ENGLISH,
    var fontSize: Int = 13,
    var realtimeModel: String = "",
    var precisionModel: String = "",
    var showMetrics: Boolean = false,
    var verboseMetrics: Boolean = false,
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