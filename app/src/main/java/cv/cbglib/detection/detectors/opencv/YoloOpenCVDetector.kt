package cv.cbglib.detection.detectors.opencv

import android.graphics.Bitmap
import cv.cbglib.detection.Detection
import cv.cbglib.detection.detectors.AbstractYoloDetector
import cv.cbglib.detection.detectors.DetectorResult
import cv.cbglib.logging.MetricsValue
import cv.cbglib.services.AssetService
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc

open class YoloOpenCVDetector(
    modelPath: String,
    confThreshold: Float = 0.6f,
    applyNMS: Boolean = true,
    nmsThreshold: Float = 0.5f
) : AbstractYoloDetector(modelPath, confThreshold, applyNMS, nmsThreshold) {
    protected val modelInputWidth = 640

    protected var results = Mat()
    protected lateinit var inferenceEngine: Net

    override fun runtimeSetup(assetService: AssetService) {
        val modelBytes = assetService.getModel(modelPath)
        val modelMat = MatOfByte(*modelBytes)

        inferenceEngine = Dnn.readNetFromONNX(modelMat)
        inferenceEngine.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV)
        inferenceEngine.setPreferableTarget(Dnn.DNN_TARGET_CPU)
    }

    override fun detect(image: Bitmap): DetectorResult {
        // convert Bitmap => OpenCV.Mat
        Utils.bitmapToMat(
            image,
            bitmapMat
        )
        // resize image into expected size for model, apply letterboxing if needed
        val (letterBoxMat, timeLetterboxing) = measureTime(showMetrics) {
            resizeAndLetterBox(bitmapMat, modelInputWidth)
        }

        // create tensor from Mat
        Imgproc.cvtColor(letterBoxMat, rgbMat, Imgproc.COLOR_RGBA2RGB)
        val (blob, timeBlob) = measureTime(showMetrics) {
            Dnn.blobFromImage(
                rgbMat,
                1.0 / 255.0,
                Size(rgbMat.cols().toDouble(), rgbMat.rows().toDouble()),
                Scalar(0.0, 0.0, 0.0),
                false,
                false,
                CvType.CV_32F
            )
        }

        inferenceEngine.setInput(blob)
        // run opencv net runtime and get results
        val (_, timeDetection) = measureTime(showMetrics) {
            results = inferenceEngine.forward()
        }

        // extract bounding boxes [Detection] objects from results that
        val (detections, timeExtractDetections) = measureTime(showMetrics) { thresholdingFilter(results) }

        // apply NMS onto results
        val (nsmFilteredDetections, timeNMS) = measureTime(showMetrics) { opencvNmsFilterByClass(detections) }

        val metricsList = if (showMetrics && verboseMetrics) {
            listOf(
                MetricsValue("LetterBox", timeLetterboxing / 1_000_000f, "ms"),
                MetricsValue("Blob", timeBlob / 1_000_000f, "ms"),
                MetricsValue("Detection", timeDetection / 1_000_000f, "ms"),
                MetricsValue("Extract detections", timeExtractDetections / 1_000_000f, "ms"),
                MetricsValue("NMS", timeNMS / 1_000_000f, "ms"),
                MetricsValue("Detections", nsmFilteredDetections.size.toFloat()),
            )
        } else {
            emptyList()
        }

        return DetectorResult(
            nsmFilteredDetections,
            imageDetails,
            showMetrics = verboseMetrics,
            metrics = metricsList,
        )
    }

    /**
     * Extracts list of [cv.cbglib.detection.Detection] objects from OpenCV [Net] result. Value for thresholding is
     * Results are in format `[batch, values, detections]` where the values are:
     * x, y, w, h, class0 confidence, class1 confidence, class2 confidence...
     *
     * By default, the [threshold] value is determined by the [confThreshold] (see [cv.cbglib.detection.detectors.Detector]).
     *
     * @param input [Mat] containing all detections from the runtime.
     * @return List of [cv.cbglib.detection.Detection] that pass the [confThreshold] confidence score threshold
     */
    protected open fun thresholdingFilter(
        input: Mat,
        threshold: Float = confThreshold
    ): List<Detection> {
        val numOfAttributes = input.size(1) // x,y,w,h, class_conf1, class_conf2, ...
        val numOfDetections = input.size(2) // detections

        // remove batch dimension
        val flattened = input.reshape(1, intArrayOf(numOfAttributes, numOfDetections))

        val values = FloatArray(numOfAttributes * numOfDetections)
        flattened.get(0, 0, values)

        return extractDetectionResults(
            values,
            numOfDetections,
            numOfAttributes,
            threshold
        )
    }

    override fun destroy() {
        super.destroy()

        results.release()
    }
}