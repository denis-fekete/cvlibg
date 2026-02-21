package cv.cbglib.detection.detectors.realtime

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import android.util.Log
import cv.cbglib.detection.Detection
import cv.cbglib.detection.detectors.DetectorResult
import cv.cbglib.logging.MetricsValue
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import java.nio.FloatBuffer

/**
 * Abstract class for all Yolo based detectors that use Onnx Runtime, containing common functions.
 */
abstract class AbstractYoloOnnxDetector(
    modelPath: String
) : AbstractYoloDetector(modelPath) {
    protected lateinit var ortSession: OrtSession
    protected var ortEnvironment: OrtEnvironment = OrtEnvironment.getEnvironment()
    protected lateinit var inputName: String
    protected val modelInputWidth = 640

    override fun afterModelLoaded() {
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

        // fixes running out of memory for low-end devices, frees heap
        this.modelBytes = null
    }

    private fun analysisFunction(
        image: Bitmap
    ): Pair<List<Detection>, List<MetricsValue>> {
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

        // run model on tensor, and get result
        val (results, timeDetection) = measureTime(showMetrics) { ortSession.run(mapOf(inputName to tensor)) }

        // convert flat outputs into an 3D array
        val result3D = results[0].value as Array<Array<FloatArray>> // [batch, values, detections]

        // extract bounding boxes [Detection] objects from results that
        val (detections, timeExtractDetections) = measureTime(showMetrics) { extractDetections(result3D, 0.6f) }

        // apply NMS onto results
        val (filteredDetections, timeNMS) = measureTime(showMetrics) { applyNMS(detections, 0.6f, 0.5f) }

        results.close()
        tensor.close()

        return filteredDetections to if (showMetrics) {
            if (verboseMetrics) { // show verbose metrics
                listOf(
                    MetricsValue("LetterBox", timeLetterboxing),
                    MetricsValue("Tensor", timeTensor),
                    MetricsValue("Detection", timeDetection),
                    MetricsValue("Extract detections", timeExtractDetections),
                    MetricsValue("NMS", timeNMS),
                    MetricsValue(
                        "Total",
                        timeLetterboxing + timeTensor + timeDetection + timeExtractDetections + timeNMS
                    )
                )
            } else { // show metrics but only basic (total)
                listOf(
                    MetricsValue(
                        "Total",
                        timeLetterboxing + timeTensor + timeDetection + timeExtractDetections + timeNMS
                    )
                )
            }
        } else { // no metrics
            emptyList()
        }
    }

    override fun detect(image: Bitmap): DetectorResult {
        val (detections, metricsList) = analysisFunction(image)

        return DetectorResult(
            detections,
            imageDetails,
            metricsList
        )
    }


    /**
     * Converts OpenCV Mat containing input image into an OnnxTensor that can be put into OnnxSession for object
     * detection. OpenCV uses HWC format, where the ONNX expects and CHW format, for that and image has to converted.
     *
     * @return [OnnxTensor] that can be put as an input to OnnxRuntime model
     */
    protected fun matToTensor(mat: Mat): OnnxTensor {
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