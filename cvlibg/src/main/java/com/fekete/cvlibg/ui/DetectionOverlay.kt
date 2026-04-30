package com.fekete.cvlibg.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.fekete.cvlibg.detection.Detection
import com.fekete.cvlibg.detection.ImageDetails
import kotlin.math.max

/**
 * Open class used as a view for drawing detections. This view is transparent and draws the detected bounding boxes
 * on top of whatever view is underneath in hierarchy of views. Recommended approach is to place this view on top of
 * [androidx.camera.view.PreviewView].
 *
 * This class is ready to use, however, its [drawDetections] function is as simple as possible, it is strongly
 * recommended to subclass [com.fekete.cvlibg.ui.DetectionOverlay] and rewrite this method to suit specific application
 * style, information levels, etc.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
open class DetectionOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    /**
     * List containing current [com.fekete.cvlibg.detection.Detection] objects that are on screen.
     */
    protected val detections = mutableListOf<Detection>()
    protected var imageDetails = ImageDetails(1f, 0, 0)
    private var cameraWidth: Int = 0
    private var cameraHeight: Int = 0
    private var scale: Float = 1f
    private var cropX: Float = 0f
    private var cropY: Float = 0f
    private var tmpRect: RectF = RectF()
    private var backgroundRect: RectF = RectF()
    protected var backgroundImage: Bitmap? = null


    /**
     * Callback function invoked when [Detection] is clicked on screen. Contains a [Detection] that was clicked.
     */
    var onDetectionClicked: ((detection: Detection) -> Unit)? = null

    /**
     * Sets camera resolution values and calculates scale and crop values for the [DetectionOverlay].
     */
    fun setCameraResolution(cameraW: Int, cameraH: Int) {
        cameraWidth = cameraW
        cameraHeight = cameraH

        scale = max(
            this.width.toFloat() / cameraWidth.toFloat(),
            this.height.toFloat() / cameraHeight.toFloat()
        )

        cropX = (cameraWidth * scale - this.width) / 2f
        cropY = (cameraHeight * scale - this.height) / 2f
    }

    /**
     * Simple check whenever camera dimensions have been set. Mostly to prevent division by zero errors.
     */
    protected fun cameraDimensionsCorrect(): Boolean {
        return (cameraWidth > 0 && cameraHeight > 0)
    }

    /**
     * Scales [Detection] objects from [com.fekete.cvlibg.detection.Detector] resolution (same as resolution provided
     * as its input [Bitmap]) into a resolution used by the smartphones screen.
     *
     * This method uses [tmpRect] variable!s
     *
     * @param det [Detection] object to be scaled
     * @param detectionScaledToCamera whenever the detection was scaled to camera resolution, if not it will be scaled
     * using [imageDetails].
     *
     * @return [RectF] object with correct resolution.
     */
    protected open fun scaleDetectionToScreenRect(det: Detection, detectionScaledToCamera: Boolean = true): RectF {
        tmpRect = det.toRectF()

        if (!detectionScaledToCamera) {
            tmpRect.left = (tmpRect.left - imageDetails.padX) / imageDetails.scale
            tmpRect.top = (tmpRect.top - imageDetails.padY) / imageDetails.scale
            tmpRect.right = (tmpRect.right - imageDetails.padX) / imageDetails.scale
            tmpRect.bottom = (tmpRect.bottom - imageDetails.padY) / imageDetails.scale
        }

        tmpRect.set(
            tmpRect.left * scale - cropX,
            tmpRect.top * scale - cropY,
            tmpRect.right * scale - cropX,
            tmpRect.bottom * scale - cropY
        )
        return tmpRect
    }

    private val defaultPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
        alpha = 200
    }

    /**
     * Function is called each [onDraw] called (every time this view is invalidated).
     * Derived classes must implement this function!
     */
    protected open fun drawDetections(canvas: Canvas) {
        detections.forEach { det ->
            // scale detection into correct size, because detection from model might not have same width and height
            // as camera images, or screen that this overlay is drawing onto
            tmpRect = scaleDetectionToScreenRect(det) // result stored in tmpRect
            canvas.drawRect(tmpRect, defaultPaint)
        }
    }


    /**
     * Cleans internal detection list and adds new [Detection] objects into it.
     *
     * @param newBoxes list of <Detection> objects that contain info about found detections in current image
     * @param imageDetails info about current detections and image, used for scaling detection to screen.
     */
    fun updateBoxes(newBoxes: List<Detection>, imageDetails: ImageDetails) {
        detections.clear()
        detections.addAll(newBoxes)
        this.imageDetails = imageDetails
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            detections.firstOrNull { scaleDetectionToScreenRect(it).contains(event.x, event.y) }
                ?.let { onDetectionClicked?.invoke(it) }
        }

        return true
    }

    /**
     * Convert Scalable Pixels to pixel size for text drawn on detections
     */
    protected fun unitSpToPix(sp: Float): Float {
        return sp * context.resources.displayMetrics.density
    }

    /**
     * Sets background image of [DetectionOverlay]
     */
    fun setBackgroundBitmap(image: Bitmap?) {
        backgroundImage = image
    }

    /**
     * Function that gets once [com.fekete.cvlibg.detection.DetectionOverlay] is invalidated and redraw is request.
     */
    final override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // draw background if not null
        backgroundImage?.let { bitmap ->
            val viewWidth = width.toFloat()
            val viewHeight = height.toFloat()

            val bitmapWidth = bitmap.width.toFloat()
            val bitmapHeight = bitmap.height.toFloat()

            val scale = maxOf(
                viewWidth / bitmapWidth,
                viewHeight / bitmapHeight
            )

            val scaledWidth = bitmapWidth * scale
            val scaledHeight = bitmapHeight * scale

            val left = (viewWidth - scaledWidth) / 2f
            val top = (viewHeight - scaledHeight) / 2f

            backgroundRect.left = left
            backgroundRect.top = top
            backgroundRect.right = left + scaledWidth
            backgroundRect.bottom = top + scaledHeight

            canvas.drawBitmap(bitmap, null, backgroundRect, null)
        }

        drawDetections(canvas)
    }
}