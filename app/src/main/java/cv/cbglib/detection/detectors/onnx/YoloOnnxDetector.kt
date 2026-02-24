package cv.cbglib.detection.detectors.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.util.Log
import cv.cbglib.detection.Detection
import cv.cbglib.detection.detectors.AbstractYoloDetector
import cv.cbglib.detection.detectors.DetectorResult
import cv.cbglib.logging.MetricsValue
import cv.cbglib.services.AssetService
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.FloatBuffer

/**
 * Abstract class for all Yolo based detectors that use Onnx Runtime, containing common functions.
 */
open class YoloOnnxDetector(
    modelPath: String,
    confThreshold: Float = 0.6f,
    applyNMS: Boolean = true,
    nmsThreshold: Float = 0.5f

) : AbstractYoloDetector(modelPath, confThreshold, applyNMS, nmsThreshold) {
    protected lateinit var ortSession: OrtSession
    protected var ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    protected lateinit var inputName: String
    protected val modelInputWidth = 640

    override fun runtimeSetup(assetService: AssetService) {
        val modelBytes = assetService.getModel(modelPath)

        // try to use Nnapi for hardware accelerated detection, on fail use CPU
        try {
            val sessionOptions = OrtSession.SessionOptions()
            sessionOptions.addNnapi()
            ortSession = ortEnvironment.createSession(modelBytes, sessionOptions)
            Log.i(javaClass.simpleName, "Loaded NNAPI for OnnxRuntime")
        } catch (e: Exception) {
            Log.e(javaClass.simpleName, "Failed to use NNAPI for OnnxRuntime, using CPU instead", e)
            ortSession = ortEnvironment.createSession(modelBytes)
        }

        inputName = ortSession.inputNames.first()
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
        val (tensor, timeTensor) = measureTime(showMetrics) { matToTensor(letterBoxMat) }

        if (tensor == null) {
            return DetectorResult(emptyList(), imageDetails, false, emptyList())
        }

        // run model on tensor, and get result
        val (results, timeDetection) = measureTime(showMetrics) { ortSession.run(mapOf(inputName to tensor)) }

        // convert flat outputs into an 3D array
        val result3D = results[0].value as Array<Array<FloatArray>> // [batch, values, detections]

        // extract bounding boxes [Detection] objects from results that
        val (detections, timeExtractDetections) = measureTime(showMetrics) { thresholdingFilter(result3D) }

        // apply NMS onto results
        val (filteredDetections, timeNMS) = measureTime(showMetrics) { nmsFilter(detections) }

        results.close()
        tensor.close()

        val metricsList = if (showMetrics && verboseMetrics) {
            listOf(
                MetricsValue("LetterBox", timeLetterboxing),
                MetricsValue("Tensor", timeTensor),
                MetricsValue("Detection", timeDetection),
                MetricsValue("Extract detections", timeExtractDetections),
                MetricsValue("NMS", timeNMS),
            )
        } else { // show metrics but only basic (total)
            emptyList()
        }

        return DetectorResult(
            filteredDetections,
            imageDetails,
            showMetrics = showMetrics,
            metricsList
        )
    }


    /**
     * Extracts list of [cv.cbglib.detection.Detection] objects from OrtSession result. Value for thresholding is
     * Results are in format `[batch, values, detections]` where the values are:
     * x, y, w, h, class0 confidence, class1 confidence, class2 confidence...
     *
     * By default, the [threshold] value is determined by the [confThreshold] (see [cv.cbglib.detection.detectors.Detector]).
     *
     * @param results Onnx runtime results in array format that will be filtered.
     * @return List of [cv.cbglib.detection.Detection] that pass the [confThreshold] confidence score threshold
     */
    protected open fun thresholdingFilter(
        results: Array<Array<FloatArray>>,
        threshold: Float = confThreshold
    ): List<Detection> {
        // remove batch dimension as model only outputs one batch
        val rawDetections = results[0] // [values, detections]

        // transpose from [values, detections] into more user friendly [detections, values]
        val transposedDetections = transpose(rawDetections)

        val detections = mutableListOf<Detection>()

        for (value in transposedDetections) {
            val classScores = value.sliceArray(4 until value.size)

            var bestScore = 0f
            var bestClass = -1

            for (i in classScores.indices) {
                val score = classScores[i]
                if (score > bestScore) {
                    bestScore = score
                    bestClass = i
                }
            }

            if (bestScore < threshold)
                continue

            val x = value[0]
            val y = value[1]
            val w = value[2]
            val h = value[3]

            detections.add(Detection(x, y, w, h, bestClass, bestScore))
        }

        return detections
    }

    /**
     * Converts OpenCV Mat containing input image into an OnnxTensor that can be put into OnnxSession for object
     * detection. OpenCV uses HWC format, where the ONNX expects and CHW format, for that and image has to converted.
     *
     * @param mat input OpenCV [Mat] that will transform into [OnnxTensor]
     * @return [OnnxTensor] that can be put as an input to OnnxRuntime model
     */
    protected fun matToTensor(mat: Mat): OnnxTensor? {
        if (mat.empty())
            return null

        // convert from RGB Alpha into RGB
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_RGBA2RGB)

        // convert RGB to normalized values <0-1>
        rgbMat.convertTo(floatMat, CvType.CV_32FC3, 1.0 / 255.0)

        // separate channels
        val channels = ArrayList<Mat>()
        Core.split(floatMat, channels)

        // onnx model expects an CHW format in its tensor, CHW: [(R1, R2, R3, ...), [G1, G2, ...], [B1, ... )]
        val height = mat.rows()
        val width = mat.cols()
        val chw = FloatArray(3 * height * width)

        for (i in 0..2) {
            val channelData = FloatArray(height * width)
            channels[i].get(0, 0, channelData)
            System.arraycopy(channelData, 0, chw, i * height * width, height * width)
        }

        // batch size, channels, heigh, width == batch size of 1 for CHW image
        val shape = longArrayOf(1, 3, height.toLong(), width.toLong())

        // create tensor, wrap used as a View, no copying, better performance
        return OnnxTensor.createTensor(ortEnvironment, FloatBuffer.wrap(chw), shape)
    }
}