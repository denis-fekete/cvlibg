package com.fekete.cvlibg.benchmark

import com.fekete.cvlibg.detection.Detection

/**
 * Data class storing [Detection] ground truth and matched [Detection] object
 *
 * @param truth a ground truth object
 * @param detection [Detection] object that passes the IoU threshold
 * @param iou IoU of the matched [detection] object
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
data class ValidationDetection(
    var truth: Detection,
    var detection: Detection? = null,
    var iou: Float = 0.0f
)
