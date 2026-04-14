package cv.cbglib.detection.detectors

import cv.cbglib.detection.Detection
import cv.cbglib.detection.ImageDetails
import cv.cbglib.logging.MetricsValue

data class DetectorResult(
    val detections: List<Detection>,
    val details: ImageDetails,
    val showMetrics: Boolean = false,
    val metrics: List<MetricsValue>
)