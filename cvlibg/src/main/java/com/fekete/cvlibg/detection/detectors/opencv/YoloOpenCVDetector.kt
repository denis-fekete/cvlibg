package com.fekete.cvlibg.detection.detectors.opencv

import android.graphics.Bitmap
import android.util.Size
import com.fekete.cvlibg.detection.Detection
import com.fekete.cvlibg.detection.detectors.AbstractYoloDetector
import com.fekete.cvlibg.detection.detectors.DetectorResult
import com.fekete.cvlibg.utils.AssetLoader
import com.fekete.cvlibg.utils.Timer
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfByte
import org.opencv.core.Scalar
import org.opencv.dnn.Dnn
import org.opencv.dnn.Net
import org.opencv.imgproc.Imgproc

/**
 * Class implementing abstract [com.fekete.cvlibg.detection.detectors.Detector] and [AbstractYoloDetector]. This class uses
 * OpenCV's [org.opencv.dnn.Net] as an inference runtime/engine.
 *
 * This detector scales [Detection] objects to input image resolution.
 *
 * @param modelPath path to the ONNX model in assets
 * @param confThreshold threshold used for filtering detections
 * @param applyNMS use or not use Non-Maximum Suppression
 * @param nmsThreshold Intersection over Union threshold for Non-Maximum Suppression
 * @param inputDataSize expected size for model loaded from the [modelPath]
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
open class YoloOpenCVDetector(
    modelPath: String,
    confThreshold: Float = 0.6f,
    applyNMS: Boolean = true,
    nmsThreshold: Float = 0.5f,
    inputDataSize: Size = Size(640, 640)
) : AbstractYoloDetector(modelPath, confThreshold, applyNMS, nmsThreshold, inputDataSize) {
    override val detectorName = "YoloOpenCVDetector"
    protected var results = Mat()
    protected lateinit var inferenceRuntime: Net

    override fun runtimeSetup(assetLoader: AssetLoader) {
        val modelBytes = assetLoader.loadModel(modelPath)
        val modelMat = MatOfByte(*modelBytes)

        inferenceRuntime = Dnn.readNetFromONNX(modelMat)
        inferenceRuntime.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV)
        inferenceRuntime.setPreferableTarget(Dnn.DNN_TARGET_CPU)
    }


    override fun detect(image: Bitmap): DetectorResult {
        // convert Bitmap => OpenCV.Mat
        Utils.bitmapToMat(
            image,
            bitmapMat
        )
        // resize image into expected size for model, apply letterboxing if needed
        val (letterBoxMat, letterboxingTime) = Timer.measure(showMetrics) {
            resizeAndLetterBox(bitmapMat, inputDataSize.width)
        }

        // create tensor from Mat
        val (blob, blobConversion) = Timer.measure(showMetrics) {
            Imgproc.cvtColor(letterBoxMat, rgbMat, Imgproc.COLOR_RGBA2RGB)

            Dnn.blobFromImage(
                rgbMat,
                1.0 / 255.0,
                org.opencv.core.Size(rgbMat.cols().toDouble(), rgbMat.rows().toDouble()),
                Scalar(0.0, 0.0, 0.0),
                false,
                false,
                CvType.CV_32F
            )
        }

        inferenceRuntime.setInput(blob)
        // run opencv net runtime and get results
        val (_, inferenceTime) = Timer.measure(showMetrics) {
            results = inferenceRuntime.forward()
        }

        // extract bounding boxes [Detection] objects from results that
        val (detections, extractionTime) = Timer.measure(showMetrics) { thresholdingFilter(results) }

        // apply NMS onto results
        val (nsmFilteredDetections, nmsTime) = Timer.measure(showMetrics) { opencvNmsFilterByClass(detections) }

        val performanceMetrics = if (showMetrics && verboseMetrics) {
            mapOf(
                METRICS_LETTERBOX_KEY to letterboxingTime,
                METRICS_CONVERSION_KEY to blobConversion,
                METRICS_INTERFACE_KEY to inferenceTime,
                METRICS_EXTRACT_KEY to extractionTime,
                METRICS_NMS_KEY to nmsTime,
            )
        } else {
            emptyMap()
        }

        return DetectorResult(
            nsmFilteredDetections,
            imageDetails,
            performanceMetrics = performanceMetrics,
        )
    }


    /**
     * Extracts list of [com.fekete.cvlibg.detection.Detection] objects from OpenCV [Net] result. Value for thresholding is
     * Results are in format `[batch, values, detections]` where the values are:
     * x, y, w, h, class0 confidence, class1 confidence, class2 confidence...
     *
     * By default, the [threshold] value is determined by the [confThreshold] (see [com.fekete.cvlibg.detection.detectors.Detector]).
     *
     * @param input [Mat] containing all detections from the runtime.
     * @return List of [com.fekete.cvlibg.detection.Detection] that pass the [confThreshold] confidence score threshold
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