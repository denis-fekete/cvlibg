package cv.cbglib.logging

/**
 * Simple data class containing values to be displayed in [MetricsOverlay].
 */
data class MetricsValue(
    val prefix: String,
    val value: Float,
    val suffix: String = "",
)
