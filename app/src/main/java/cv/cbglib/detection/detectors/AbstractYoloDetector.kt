package cv.cbglib.detection.detectors

import android.util.Size
import cv.cbglib.detection.Detection
import cv.cbglib.detection.ImageDetails
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.MatOfFloat
import org.opencv.core.MatOfInt
import org.opencv.core.MatOfRect2d
import org.opencv.core.Rect2d
import org.opencv.core.Scalar
import org.opencv.dnn.Dnn
import org.opencv.imgproc.Imgproc
import kotlin.collections.iterator
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Class implementing abstract [cv.cbglib.detection.detectors.Detector]. This class contains common methods and
 * variables for more specific implementation of YOLO detectors.
 *
 * This detector scales [Detection] objects to input image resolution.
 *
 * @param modelPath path to the ONNX model in assets
 * @param confThreshold threshold used for filtering detections
 * @param applyNMS use or not use Non-Maximum Suppression
 * @param nmsThreshold Intersection over Union threshold for Non-Maximum Suppression
 * @param inputDataSize expected size for model loaded from the [modelPath]
 */
abstract class AbstractYoloDetector(
    modelPath: String,
    confThreshold: Float = 0.6f,
    applyNMS: Boolean = true,
    nmsThreshold: Float = 0.5f,
    inputDataSize: Size = Size(640, 640)

) : Detector(
    modelPath,
    confThreshold,
    applyNMS,
    nmsThreshold,
    inputDataSize
) {
    // "cache" variables to prevent initializing new object each new frame
    protected var bitmapMat = Mat()
    protected var resized = Mat()
    protected var letterBoxMat = Mat()
    protected var rgbMat = Mat()
    protected var floatMat = Mat()
    protected lateinit var imageDetails: ImageDetails

    /**
     * Resized [src] Mat into a size that model can use. If source Mat is not in 1:1 aspect ratio a letterbox is
     * applied to make it into desired size in 1:1 ratio. [newSize] is desired size and [padValue] is color value
     * that will be used for padding.
     * @param src source Mat containing image
     * @param newSize new size of pixels, since 1:1 is expected only one dimension is needed
     * @param padValue color used for padding
     *
     * @return [Mat] containing scaled and letterboxed image
     */
    protected open fun resizeAndLetterBox(
        src: Mat,
        newSize: Int,
        padValue: Scalar = Scalar(114.0, 114.0, 114.0),
    ): Mat {
        // find bigger dimension (width / height)
        val srcW = src.cols()
        val srcH = src.rows()
        val scale = newSize.toFloat() / max(srcW, srcH)

        // new image size (width and height)
        val newW = (srcW * scale).roundToInt()
        val newH = (srcH * scale).roundToInt()

        Imgproc.resize(src, resized, org.opencv.core.Size(newW.toDouble(), newH.toDouble()))

        // calculate padding, padded image is always centered
        val padX = (newSize - newW) / 2
        val padY = (newSize - newH) / 2

        // copies resized and apply border/letterboxing
        Core.copyMakeBorder(
            resized,
            letterBoxMat,
            padY,
            newSize - newH - padY,
            padX,
            newSize - newW - padX,
            Core.BORDER_CONSTANT,
            padValue
        )

        // update image details
        imageDetails = ImageDetails(scale, padX, padY)

        return letterBoxMat
    }

    /**
     * Takes input array, and creates new array that is transposed.
     *
     * @param input Input array from which a values will be takes
     * @return transposed array
     */
    protected open fun transpose(input: Array<FloatArray>): Array<FloatArray> {
        val rows = input.size
        val cols = input[0].size
        val transposed = Array(cols) { FloatArray(rows) }
        for (i in 0 until rows) {
            for (j in 0 until cols) {
                transposed[j][i] = input[i][j]
            }
        }
        return transposed
    }

    /**
     * Function that iterates through [data] that is [FloatArray], this array must be flat 1D array, however, the data
     * must be stored in ` must be in [numOfAttributes]x[numOfDetections] shape. Accessing data is more intuitive in
     * [numOfDetections]x[numOfAttributes], which would require a transposing that which might be too computationally
     * expensive for realtime image processing. Therefore, this function simplifies that.
     *
     * Note: function was separated from the `thresholdingFilter` because of code duplication.
     *
     * @param data [FloatArray] containing the data from the inference run
     * @param numOfAttributes number of attributes per detection, value is used as a column index
     * @param numOfDetections number of detections, values is used as a row
     * @param threshold thresholding used for filtering of detections
     *
     * @return [List] of [Detection] objects
     */
    protected open fun extractDetectionResults(
        data: FloatArray,
        numOfAttributes: Int,
        numOfDetections: Int,
        threshold: Float
    ): MutableList<Detection> {
        val detections = mutableListOf<Detection>()

        for (col in 0 until numOfDetections) { // iterate through all detections
            var bestClass = -1
            var bestScore = 0f

            for (row in 4 until numOfAttributes) {
                val score = data[row * numOfDetections + col]
                if (score > bestScore) {
                    bestClass = row - 4
                    bestScore = score
                }
            }

            if (bestScore < threshold)
                continue

            imageDetails.scale
            val x = data[0 * numOfDetections + col]
            val y = data[1 * numOfDetections + col]
            val w = data[2 * numOfDetections + col]
            val h = data[3 * numOfDetections + col]

            val sX = (x - imageDetails.padX) / imageDetails.scale
            val sY = (y - imageDetails.padY) / imageDetails.scale
            val sW = w / imageDetails.scale
            val sH = h / imageDetails.scale

            detections.add(Detection(sX, sY, sW, sH, bestClass, bestScore))
        }

        return detections
    }


    /**
     * Apply Non-Maximum Suppression (NMS) on list of [Detection]s. Implemented using OpenCV NSM function.
     * The thresholding value for NMS is taken from [nmsThreshold] (see [Detector]). If [applyNMS] is set to false the
     * input list will be returned.
     *
     * @param detections List of detections that will be filtered.
     * @param threshold NMS threshold value, by default [nmsThreshold] from [Detector] is used.
     * @return List of detections filtered by NMS algorithm.
     */
    protected open fun opencvNmsFilterByClass(
        detections: List<Detection>,
        threshold: Float = nmsThreshold
    ): List<Detection> {
        if (!applyNMS) return detections

        if (detections.isEmpty()) return emptyList()

        val finalDetections = mutableListOf<Detection>()

        // group detections by class
        val detectionsByClass = detections.groupBy { it.classIndex }

        for ((classId, classDetections) in detectionsByClass) {
            if (classDetections.isEmpty()) continue

            // bounding boxes
            val rect2dArr: Array<Rect2d> = Array(classDetections.size) { i ->
                classDetections[i].toRect2d()
            }
            val matRects = MatOfRect2d(*rect2dArr)

            // scores
            val scores = FloatArray(classDetections.size) { i -> classDetections[i].confidence }
            val matScores = MatOfFloat(*scores)

            // run NMS for this class
            val matIndices = MatOfInt()
            Dnn.NMSBoxes(
                matRects,
                matScores,
                confThreshold,
                nmsThreshold,
                matIndices
            )

            // collect results
            for (idx in matIndices.toArray()) {
                finalDetections.add(classDetections[idx])
            }

            // release native resources
            matRects.release()
            matScores.release()
            matIndices.release()
        }

        return finalDetections
    }

    /**
     * Removes overlapping [Detection]s by using Non-Maximum suppression on grouped [Detection] objects by detected class.
     *
     * @param detections List of [Detection] objects, must be mutable
     * @param threshold NMS/IoU threshold, if two [Detection] objects exceed this value one of them (the one with lower
     * confidence score) will be deleted.
     */
    protected open fun ilibgNmsFilterByClass(
        detections: MutableList<Detection>,
        threshold: Float = nmsThreshold
    ): List<Detection> {
        if (!applyNMS) return detections

        if (detections.isEmpty()) return emptyList()

        val keptDetections = mutableListOf<Detection>()

        // group detections by class
        val detectionsByClass = detections.groupBy { it.classIndex }

        for (cls in detectionsByClass) {
            val kept = ilibgNmsFilterOptimized(cls.value.toMutableList(), nmsThreshold)
            keptDetections.addAll(kept)
        }
        return keptDetections
    }

    /**
     * Removes overlapping [Detection]s by using Non-Maximum suppression.
     *
     * @param detections List of [Detection] objects, must be mutable
     * @param threshold NMS/IoU threshold, if two [Detection] objects exceed this value one of them (the one with lower
     * confidence score) will be deleted.
     */
    protected fun ilibgNmsFilterOptimized(detections: MutableList<Detection>, threshold: Float): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        val keptDetections = mutableListOf<Detection>()

        // important !! sort by score
        detections.sortByDescending { it.confidence }

        // mask for skipping detections, faster than removing them
        val mask = BooleanArray(detections.size) { false }

        for (idx in 0 until detections.size) {
            if (mask[idx])
                continue

            // add the best detection to keptDetections list
            val best = detections[idx]
            keptDetections.add(best)
            mask[idx] = true

            // remove that have high area of intersection with the best detection with
            for (i in idx + 1 until detections.size) {
                if (mask[i])
                    continue

                val detection = detections[i]
                val iou = Detection.computeIoU(best, detection)

                if (iou > threshold) {
                    mask[i] = true
                }
            }
        }
        return keptDetections
    }

    /**
     * Removes overlapping [Detection]s by using Non-Maximum suppression.
     *
     * @param detections List of [Detection] objects, must be mutable
     * @param threshold NMS/IoU threshold, if two [Detection] objects exceed this value one of them (the one with lower
     * confidence score) will be deleted.
     *
     * @see <a href="https://www.baeldung.com/kotlin/list-remove-elements-while-iterating"> Remove Elements From a
     * List While Iterating in Kotlin
     */
    protected fun ilibgNmsFilter(detections: MutableList<Detection>, threshold: Float): List<Detection> {
        if (detections.isEmpty()) return emptyList()

        val keptDetections = mutableListOf<Detection>()

        // important !! sort by score
        detections.sortByDescending { it.confidence }

        while (detections.isNotEmpty()) {
            // add the best detection to keptDetections list
            val best = detections.first()
            keptDetections.add(best)
            detections.remove(best)

            // remove that have high area of intersection with the best detection with
            val iterator = detections.iterator()
            while (iterator.hasNext()) {
                val detection = iterator.next()
                val iou = Detection.computeIoU(best, detection)

                if (iou > threshold) {
                    iterator.remove()
                }
            }
        }
        return keptDetections
    }


    /**
     * Clean up variables that are used as "cache" (not actual cache but frequently used where reallocation each frame
     * does not make sense).
     */
    override fun destroy() {
        bitmapMat.release()
        resized.release()
        letterBoxMat.release()
        rgbMat.release()
        floatMat.release()
    }

    companion object {
        const val METRICS_LETTERBOX_KEY = "letterbox"
        const val METRICS_CONVERSION_KEY = "conversion"
        const val METRICS_INTERFACE_KEY = "inference"
        const val METRICS_EXTRACT_KEY = "extract"
        const val METRICS_NMS_KEY = "nms"
    }
}