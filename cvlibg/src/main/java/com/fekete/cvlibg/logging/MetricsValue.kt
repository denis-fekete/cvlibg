package com.fekete.cvlibg.logging

/**
 * Simple data class containing logging values to be displayed in [MetricsOverlay].
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
data class MetricsValue(
    val prefix: String = "",
    val value: Double = 0.0,
    val suffix: String = "",
)
