package com.fekete.cvlibg.detection.detectors

import com.fekete.cvlibg.detection.Detection
import com.fekete.cvlibg.detection.ImageDetails
import com.fekete.cvlibg.logging.MetricsValue
import com.fekete.cvlibg.utils.TimerResult

/**
 * Data class containing results of the [Detector] image analysis run.
 *
 * @param detections list of [Detection] objects
 * @param details used for scaling detections if they were not scaled by the [Detector]
 * @param performanceMetrics map of [TimerResult] values
 * @param otherMetrics list of metrics using the [MetricsValue]
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
data class DetectorResult(
    val detections: List<Detection>,
    val details: ImageDetails,
    val showMetrics: Boolean = false,

    val performanceMetrics: Map<String, TimerResult> = emptyMap(),
    val otherMetrics: List<MetricsValue> = emptyList(),
)