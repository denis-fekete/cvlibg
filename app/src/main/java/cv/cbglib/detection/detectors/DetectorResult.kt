package cv.cbglib.detection.detectors

import cv.cbglib.detection.Detection
import cv.cbglib.detection.ImageDetails
import cv.cbglib.logging.MetricsValue
import cv.cbglib.utils.TimerResult

data class DetectorResult(
    val detections: List<Detection>,
    val details: ImageDetails,
    val showMetrics: Boolean = false,

    val performanceMetrics: Map<String, TimerResult> = emptyMap(),
    val otherMetrics: Map<String, MetricsValue> = emptyMap(),
)