package com.fekete.cvlibg.detection.detectors

import com.fekete.cvlibg.detection.Detection
import com.fekete.cvlibg.detection.ImageDetails
import com.fekete.cvlibg.logging.MetricsValue
import com.fekete.cvlibg.utils.TimerResult

data class DetectorResult(
    val detections: List<Detection>,
    val details: ImageDetails,
    val showMetrics: Boolean = false,

    val performanceMetrics: Map<String, TimerResult> = emptyMap(),
    val otherMetrics: Map<String, MetricsValue> = emptyMap(),
)