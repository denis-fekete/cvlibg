package com.fekete.cvlibg.detection

import android.graphics.Bitmap
import com.fekete.cvlibg.metrics.MetricsValue
import com.fekete.cvlibg.utils.TimerResult

/**
 * Data class containing results of the [Detector] image analysis run.
 *
 * @param detections list of [Detection] objects
 * @param imageDetails used for scaling detections if they were not scaled by the [Detector]
 * @param timeMetrics map of [TimerResult] values with [String] keys
 * @param metrics list of metrics using the [MetricsValue]
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
data class DetectorResult(
    val detections: List<Detection> = emptyList(),
    val imageDetails: ImageDetails = ImageDetails(),
    val inputImage: Bitmap? = null,
    val timeMetrics: Map<String, TimerResult> = emptyMap(),
    val metrics: List<MetricsValue> = emptyList(),
) {
    fun copyFrom(
        detections: List<Detection>? = null,
        imageDetails: ImageDetails? = null,
        inputImage: Bitmap? = null,
        timeMetrics: Map<String, TimerResult>? = null,
        metrics: List<MetricsValue>? = null
    ): DetectorResult {
        return DetectorResult(
            detections ?: this.detections,
            imageDetails ?: this.imageDetails,
            inputImage,
            timeMetrics ?: this.timeMetrics,
            metrics ?: this.metrics
        )
    }
}