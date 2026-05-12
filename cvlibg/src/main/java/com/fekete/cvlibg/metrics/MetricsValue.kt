package com.fekete.cvlibg.metrics

/**
 * Simple data class containing logging values to be displayed in [com.fekete.cvlibg.ui.MetricsOverlay].
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
data class MetricsValue(
    val prefix: String = "",
    val value: Double? = null,
    val suffix: String = "",
)