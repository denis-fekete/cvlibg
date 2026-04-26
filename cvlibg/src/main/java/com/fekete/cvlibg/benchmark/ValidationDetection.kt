package com.fekete.cvlibg.benchmark

import com.fekete.cvlibg.detection.Detection

data class ValidationDetection(
    var truth: Detection,
    var detection: Detection? = null,
    var iou: Float = 0.0f
)
