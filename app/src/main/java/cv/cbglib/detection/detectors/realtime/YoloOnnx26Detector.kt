package cv.cbglib.detection.detectors.realtime

import android.graphics.Bitmap
import cv.cbglib.detection.Detection
import cv.cbglib.logging.MetricsValue
import org.opencv.android.Utils

/**
 * Open class for Yolo model version 26
 */
open class YoloOnnx26Detector(
    modelPath: String
) : AbstractYoloOnnxDetector(
    modelPath
) {
    override fun extractDetections(results: Array<Array<FloatArray>>, confThreshold: Float): List<Detection> {
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

        results.close()
        tensor.close()

        return detections to if (showMetrics) {
            if (verboseMetrics) { // show verbose metrics
                listOf(
                    MetricsValue("LetterBox", timeLetterboxing),
                    MetricsValue("Tensor", timeTensor),
                    MetricsValue("Detection", timeDetection),
                    MetricsValue("Extract detections", timeExtractDetections),
                    MetricsValue(
                        "Total",
                        timeLetterboxing + timeTensor + timeDetection + timeExtractDetections
                    )
                )
            } else { // show metrics but only basic (total)
                listOf(
                    MetricsValue(
                        "Total",
                        timeLetterboxing + timeTensor + timeDetection + timeExtractDetections
                    )
                )
            }
        } else { // no metrics
            emptyList()
        }
    }
}