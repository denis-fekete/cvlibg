package cv.cbglib.detection.detectors.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import android.util.Size
import cv.cbglib.detection.Detection
import cv.cbglib.detection.detectors.AbstractYoloDetector

/**
 * Class implementing abstract [cv.cbglib.detection.detectors.Detector] and [AbstractYoloDetector]. This class uses
 * OpenCV's [org.opencv.dnn.Net] as an inference runtime/engine. This detector supports YOLO 26 version.
 *
 * This detector scales [Detection] objects to input image resolution.
 *
 * @param modelPath path to the ONNX model in assets
 * @param confThreshold threshold used for filtering detections
 * @param applyNMS use or not use Non-Maximum Suppression
 * @param nmsThreshold Intersection over Union threshold for Non-Maximum Suppression
 * @param inputDataSize expected size for model loaded from the [modelPath]
 * @param useNNAPI whenever to use ONNX's NNAPI for accelerated inference
 */
open class Yolo26OnnxDetector(
    modelPath: String,
    confThreshold: Float = 0.6f,
    inputDataSize: Size = Size(640, 640),
    useNNAPI: Boolean = false,
) : YoloOnnxDetector(
    modelPath,
    confThreshold,
    applyNMS = false,
    0f,
    inputDataSize,
    useNNAPI,
) {
    override val detectorName = "Yolo26OnnxDetector"
    
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

            val w = right - left
            val h = bottom - top

            val sX = ((left + w / 2) - imageDetails.padX) / imageDetails.scale
            val sY = ((top + h / 2) - imageDetails.padY) / imageDetails.scale
            val sW = w / imageDetails.scale
            val sH = h / imageDetails.scale

            // Yolo 26 is not mid centered output is not mid centered, but uses corners instead
            detections.add(Detection(sX, sY, sW, sH, bestClass, score))
        }

        return detections
    }
}