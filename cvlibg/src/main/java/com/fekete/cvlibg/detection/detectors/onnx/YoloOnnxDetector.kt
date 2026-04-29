package com.fekete.cvlibg.detection.detectors.onnx

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.util.Log
import android.util.Size
import com.fekete.cvlibg.detection.Detection
import com.fekete.cvlibg.detection.detectors.AbstractYoloDetector
import com.fekete.cvlibg.detection.detectors.DetectorResult
import com.fekete.cvlibg.utils.AssetLoader
import com.fekete.cvlibg.utils.Timer
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.FloatBuffer


/**
 * Class implementing abstract [com.fekete.cvlibg.detection.detectors.Detector] and [AbstractYoloDetector]. This class uses
 * ONNX's runtime as an inference runtime/engine. This detector supports YOLO version from 8 to 11.
 *
 * This detector scales [Detection] objects to input image resolution.
 *
 * @param modelPath path to the ONNX model in assets
 * @param confThreshold threshold used for filtering detections
 * @param applyNMS use or not use Non-Maximum Suppression
 * @param nmsThreshold Intersection over Union threshold for Non-Maximum Suppression
 * @param inputDataSize expected size for model loaded from the [modelPath]
 * @param useNNAPI whenever to use ONNX's NNAPI for accelerated inference
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
open class YoloOnnxDetector(
    modelPath: String,
    confThreshold: Float = 0.6f,
    applyNMS: Boolean = true,
    nmsThreshold: Float = 0.5f,
    inputDataSize: Size = Size(640, 640),
    private val useNNAPI: Boolean = false,
) : AbstractYoloDetector(modelPath, confThreshold, applyNMS, nmsThreshold, inputDataSize) {
    override val detectorName = "YoloOnnxDetector"
    protected lateinit var inferenceRuntime: OrtSession
    protected var ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    protected lateinit var inputName: String
    protected val modelInputWidth = 640

    override fun runtimeSetup(assetLoader: AssetLoader) {
        val modelBytes = assetLoader.loadModel(modelPath)
        var runtimeInitialized = false
        if (useNNAPI) {
            try {
                val sessionOptions = OrtSession.SessionOptions()
                sessionOptions.addNnapi()

                inferenceRuntime = ortEnvironment.createSession(modelBytes, sessionOptions)
                Log.i(javaClass.simpleName, "Loaded NNAPI for OnnxRuntime")
                runtimeInitialized = true
            } catch (e: Exception) {
                Log.e(javaClass.simpleName, "Failed to use NNAPI for OnnxRuntime:", e)
            }
        }

        if (!runtimeInitialized) {
            inferenceRuntime = ortEnvironment.createSession(modelBytes)
            Log.i(javaClass.simpleName, "Loaded default runtime for OnnxRuntime")
        }

        inputName = inferenceRuntime.inputNames.first()
    }

    override fun detect(image: Bitmap): DetectorResult {
        // convert Bitmap => OpenCV.Mat
        Utils.bitmapToMat(
            image,
            bitmapMat
        )
        // resize image into expected size for model, apply letterboxing if needed
        val (letterBoxMat, letterboxingTime) = Timer.measure(showMetrics) {
            resizeAndLetterBox(bitmapMat, modelInputWidth)
        }

        // create tensor from Mat
        val (tensor, tensorConversion) = Timer.measure(showMetrics) { matToTensor(letterBoxMat) }

        if (tensor == null) {
            return DetectorResult(emptyList(), imageDetails, false)
        }

        // run model on tensor, and get result
        val (results, inferenceTime) = Timer.measure(showMetrics) { inferenceRuntime.run(mapOf(inputName to tensor)) }

        // extract bounding boxes [Detection] objects from results that
        val (detections, extractionTime) = Timer.measure(showMetrics) { thresholdingFilter(results) }

        // apply NMS onto results
        val (nsmFilteredDetections, nmsTime) = Timer.measure(showMetrics) { ilibgNmsFilterByClass(detections) }

        results.close()
        tensor.close()


        val performanceMetrics = if (showMetrics && verboseMetrics) {
            mapOf(
                METRICS_LETTERBOX_KEY to letterboxingTime,
                METRICS_CONVERSION_KEY to tensorConversion,
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
     * Extracts list of [com.fekete.cvlibg.detection.Detection] objects from OrtSession result. Value for thresholding is
     * Results are in format `[batch, values, detections]` where the values are:
     * x, y, w, h, class0 confidence, class1 confidence, class2 confidence...
     *
     * By default, the [threshold] value is determined by the [confThreshold] (see [com.fekete.cvlibg.detection.detectors.Detector]).
     *
     * @see <a href="https://onnxruntime.ai/docs/api/java/ai/onnxruntime/OnnxTensor.html">OnnxTensor Api Documentation
     *
     * @param results Onnx runtime results in array format that will be filtered.
     * @return List of [com.fekete.cvlibg.detection.Detection] that pass the [confThreshold] confidence score threshold
     */
    protected open fun thresholdingFilter(
        results: OrtSession.Result,
        threshold: Float = confThreshold
    ): MutableList<Detection> {
        if (results.size() < 1)
            return mutableListOf()

        val tensor = results[0] as OnnxTensor // get first result of the model
        val buffer = tensor.floatBuffer // access the tensor as a FloatBuffer
        val shape = tensor.info.shape

        // expected shape of tensor 3D array [batch, attributes, detections]
        val numOfAttributes = shape[1].toInt()
        val numOfDetections = shape[2].toInt()

        val data = FloatArray(buffer.remaining()) // data size = attributes * detections
        buffer.get(data)

        return extractDetectionResults(data, numOfAttributes, numOfDetections, threshold)
    }

    /**
     * Converts OpenCV Mat containing input image into an OnnxTensor that can be put into OnnxSession for object
     * detection. OpenCV uses HWC format, where the ONNX expects and CHW format, for that and image has to converted.
     *
     * @param mat input OpenCV [Mat] that will transform into [OnnxTensor]
     * @return [OnnxTensor] that can be put as an input to OnnxRuntime model
     */
    protected open fun matToTensor(mat: Mat): OnnxTensor? {
        if (mat.empty())
            return null

        // convert from RGB Alpha into RGB
        Imgproc.cvtColor(mat, rgbMat, Imgproc.COLOR_RGBA2RGB)

        // convert RGB to normalized values <0-1>
        rgbMat.convertTo(floatMat, CvType.CV_32FC3, 1.0 / 255.0)

        // separate channels
        val channels = ArrayList<Mat>()
        Core.split(floatMat, channels)

        if (channels.size != 3)
            return null

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

    override fun toString(): String {
        return "$detectorName(" +
                "modelPath=$modelPath, " +
                "confThreshold=$confThreshold, " +
                "nmsThreshold=$nmsThreshold, " +
                "applyNMS=$applyNMS," +
                "inputDataSize=(${inputDataSize.width},${inputDataSize.height})," +
                "useNNAPI=${useNNAPI})"
    }

    override fun toCsvString(): String {
        return "$detectorName," +
                "$modelPath," +
                "(confThreshold=$confThreshold;" +
                "nmsThreshold=$nmsThreshold;" +
                "applyNMS=$applyNMS;" +
                "inputDataSize=(${inputDataSize.width},${inputDataSize.height});" +
                "useNNAPI=${useNNAPI})"
    }
}