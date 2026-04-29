package com.fekete.cvlibg.detection

import android.graphics.RectF
import org.opencv.core.Rect2d
import kotlin.math.max
import kotlin.math.min

/**
 * Data class representing bounding boxes of detection, its class and confidence score.
 *
 * CVLiBG works and expects Detection objects to be center based, meaning the X,Y represent center of the detection.
 *
 * @param x center x coordinate of the bounding box
 * @param y center y coordinate of the bounding box
 * @param width width of bounding box
 * @param height height of bounding box
 * @param classIndex class with the highest confidence score for given bounding box (detection)
 * @param confidence score of highest detection
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
data class Detection(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val classIndex: Int,
    val confidence: Float
) {
    /**
     * Returns Detection converted into a Kotlin's [RectF] object.
     *
     * @return [RectF] detection as a [RectF] object
     */
    fun toRectF(): RectF {
        val wHalf = width / 2.0f
        val hHalf = height / 2.0f
        return RectF(
            x - wHalf,
            y - hHalf,
            x + wHalf,
            y + hHalf
        )
    }

    /**
     * Returns Detection converted into a OpenCV's [Rect2d] object.
     *
     * @return [Rect2d] detection as a [Rect2d] object
     */
    fun toRect2d(): Rect2d {
        return Rect2d(
            (x - width / 2f).toDouble(),
            (y - height / 2f).toDouble(),
            width.toDouble(),
            height.toDouble()
        )
    }

    /**
     * Returns normalized version of [Detection] object.
     *
     * @param width Width dimension used for normalization
     * @param height Height dimension used for normalization
     *
     * @return Normalized detection based in input parameters.
     */
    fun normalized(width: Float, height: Float): Detection {
        return Detection(
            x / width,
            y / height,
            this.width / width,
            this.height / height,
            classIndex,
            confidence
        )
    }

    /**
     * Returns [Detection] converted from normalized format into absolute.
     *
     * @param width Width dimension used for calculation
     * @param height Height dimension used for calculation
     *
     * @return [Detection] with absolute coordinates.
     */
    fun absolute(width: Float, height: Float): Detection {
        return Detection(
            x * width,
            y * height,
            this.width * width,
            this.height * height,
            classIndex,
            confidence
        )
    }

    /**
     * Computes the Intersection over Union for given [Detection] objects.
     *
     * @param other [Detection] object used for calculation
     *
     * @return Intersection Over Union of `this` and [other] [Detection] objects.
     */
    fun iou(other: Detection): Float = computeIoU(this, other)


    /**
     * X1 value of (x1,y1) (x2,y2) == (left top) (right bottom) coordinate system.
     *
     * x1,x2 ... do not use getters but are fixed fields, as they are always used at least once and do not need
     * recomputing
     *
     */
    val x1 = x - width / 2.0f

    /**
     * X2 value of (x1,y1) (x2,y2) == (left top) (right bottom) coordinate system
     */
    val x2 = x + width / 2.0f

    /**
     * Y1 value of (x1,y1) (x2,y2) == (left top) (right bottom) coordinate system
     */
    val y1 = y - height / 2.0f

    /**
     * Y2 value of (x1,y1) (x2,y2) == (left top) (right bottom) coordinate system
     */
    val y2 = y + height / 2.0f

    /**
     * Area of the detection
     */
    val area: Float get() = width * height

    companion object {
        fun empty() = Detection(0f, 0f, 0f, 0f, 0, 0f)

        fun fromString(input: String, separator: String): Detection {
            val values = input.split(separator)
                .map { it.toFloat() }

            return if (values.size == 5) Detection(
                values[1],
                values[2],
                values[3],
                values[4],
                values[0].toInt(),
                0f
            ) else
                empty()
        }

        /**
         * Computes the Intersection over Union for given [Detection] objects.
         *
         * @see <a href="https://learnopencv.com/non-maximum-suppression-theory-and-implementation-in-pytorch/"> Non Maximum
         * Suppression: Theory and Implementation in PyTorch
         *
         * @return Intersection over Union value
         */
        fun computeIoU(first: Detection, second: Detection): Float {
            // intersection box coordinates (top left) (bottom right) = (xx yy) (aa bb)
            val xx = max(first.x1, second.x1)
            val yy = max(first.y1, second.y1)
            val aa = min(first.x2, second.x2)
            val bb = min(first.y2, second.y2)

            // intersection box dimensions
            val intersectionW = max(0f, aa - xx)
            val intersectionH = max(0f, bb - yy)
            val intersectionArea = intersectionW * intersectionH

            val unionArea = first.area + second.area - intersectionArea
            return intersectionArea / unionArea
        }
    }
}
