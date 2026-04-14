package cv.cbglib.detection

import android.graphics.RectF
import org.opencv.core.Rect2d

/**
 * Data class representing bounding boxes or detections, its highest rated class and confidence score for given class.
 * Values stored are in a model dimension representation, meaning that if [Detection] should be used in Kotlin drawing
 * algorithms and functions [toRectF] should be called.
 *
 * CBGLIB works and expects Detection objects to be center based, meaning the X,Y represent center of the detection.
 */
data class Detection(
    val x: Float, // center x coordinate of bounding box
    val y: Float, // center y coordinate of bounding box
    val width: Float, // width of detection
    val height: Float, // height of detection
    val classIndex: Int, // class with the highest confidence score for given bounding box (detection)
    val score: Float // score of highest detection
) {
    /**
     * Transforms detection into a Kotlin [RectF] object
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
     * Transforms detection into a OpenCV [Rect2d] object
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
     * X1 value of (x1,y1) (x2,y2) == (left top) (right bottom) coordinate system
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
    val area = width * height
}
