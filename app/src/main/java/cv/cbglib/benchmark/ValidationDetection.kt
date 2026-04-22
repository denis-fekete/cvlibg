package cv.cbglib.benchmark

import cv.cbglib.detection.Detection

data class ValidationDetection(
    var truth: Detection,
    var detection: Detection? = null,
    var iou: Float = 0.0f
)
