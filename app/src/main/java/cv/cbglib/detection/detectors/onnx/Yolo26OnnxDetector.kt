package cv.cbglib.detection.detectors.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import cv.cbglib.detection.Detection

/**
 * Open class for Yolo model version 26
 */
open class Yolo26OnnxDetector(
    modelPath: String,
    confThreshold: Float = 0.6f,
    useNNAPI: Boolean = false,
) : YoloOnnxDetector(
    modelPath,
    confThreshold,
    applyNMS = false,
    0f,
    useNNAPI,
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

    override fun thresholdingFilter(
        results: OrtSession.Result,
        threshold: Float
    ): MutableList<Detection> {
        if (results.size() < 1)
            return mutableListOf<Detection>()

        val tensor = results[0] as OnnxTensor // get first result of the model
        val buffer = tensor.floatBuffer // access the tensor as a FloatBuffer
        val shape = tensor.info.shape

        // expected shape of tensor 3D array [batch, attributes, detections]
        val numOfDetections = shape[2].toInt()
        val data = FloatArray(buffer.remaining()) // data size = attributes * detections
        buffer.get(data)

        val detections = mutableListOf<Detection>()

        for (detection in 0 until numOfDetections) {
            val index = numOfDetections * detection
            val score = data[index + 4]
            if (score < confThreshold)
                continue

            val bestClass = data[index + 5].toInt()
            val left = data[index + 0]
            val top = data[index + 1]
            val right = data[index + 2]
            val bottom = data[index + 3]
            val width = right - left
            val height = bottom - top

            // Yolo 26 is not mid centered output is not mid centered, but uses corners instead
            detections.add(Detection(left + width / 2, top + height / 2, width, height, bestClass, score))
        }

        return detections
    }


}