package cv.cbglib.detection.detectors.onnx

import cv.cbglib.detection.Detection

/**
 * Open class for Yolo model version 26
 */
open class Yolo26OnnxDetector(
    modelPath: String,
    confThreshold: Float = 0.6f,
) : YoloOnnxDetector(
    modelPath,
    confThreshold,
    applyNMS = false
) {
    override fun thresholdingFilter(
        results: Array<Array<FloatArray>>,
        threshold: Float
    ): List<Detection> {
        // remove batch dimension as model only outputs one batch
        val rawDetections = results[0] // [values, detections]

        val detections = mutableListOf<Detection>()

        for (value in rawDetections) {
            val score = value[4]
            if (score < confThreshold)
                continue

            val bestClass = value[5].toInt()
            val left = value[0]
            val top = value[1]
            val right = value[2]
            val bottom = value[3]
            val width = right - left
            val height = bottom - top

            // Yolo 26 is not mid centered output is not mid centered, but uses corners instead
            detections.add(Detection(left + width / 2, top + height / 2, width, height, bestClass, score))
        }

        return detections
    }
}