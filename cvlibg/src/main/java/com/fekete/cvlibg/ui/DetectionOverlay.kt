package com.fekete.cvlibg.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import com.fekete.cvlibg.detection.Detection
import com.fekete.cvlibg.detection.DetectorResult
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
    protected var imageDetails = ImageDetails(1f, 0, 0, 0, 0)
    private var scale: Float = 1f
    private var cropX: Float = 0f
    private var cropY: Float = 0f

    /**
     * Internal [RectF] object used for calculation and "cache", to prevent creating new object each iteration
     */
    private var tmpRect: RectF = RectF()

    /**
     * Internal [RectF] object used for calculation and "cache", to prevent creating new object each iteration
     */
    private var backgroundRect: RectF = RectF()
    protected var backgroundImage: Bitmap? = null

    private val defaultPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
        alpha = 200
    }

    /**
     * Callback function invoked when [Detection] is clicked on screen. Contains a [Detection] that was clicked.
     */
    var onDetectionClicked: ((detection: Detection) -> Unit)? = null

    /**
     * Sets camera resolution values and calculates scale and crop values for the [DetectionOverlay].
     */
    protected open fun updateCameraResolution() {
        scale = max(
            this.width.toFloat() / imageDetails.inputWidth.toFloat(),
            this.height.toFloat() / imageDetails.inputHeight.toFloat()
        )

        cropX = (imageDetails.inputWidth * scale - this.width) / 2f
        cropY = (imageDetails.inputHeight * scale - this.height) / 2f
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
     * @param detections list of <Detection> objects that contain info about found detections in current image
     * @param imageDetails info about current detections and image, used for scaling detection to screen.
     * @param backgroundImage if not `null`, this bitmap will be drawn by the [DetectionOverlay] as a background
     * for [detections]
     */
    fun update(result: DetectorResult) {
        detections.clear()
        detections.addAll(result.detections)

        val cameraResolutionChanged = (imageDetails.inputWidth != result.imageDetails.inputWidth ||
                imageDetails.inputHeight != result.imageDetails.inputHeight)

        // update only if input image resolution changed
        if (cameraResolutionChanged) {
            imageDetails = result.imageDetails.copy()
            updateCameraResolution()
        }

        backgroundImage = result.inputImage

        invalidate()
    }

    /**
     * Update [scale] used for scaling on size changing.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        if (w > 0 && h > 0) {
            updateCameraResolution()
            invalidate()
        }
    }


    /**
     * Draws background image underneath the detections. Mostly used "freezing" image for detailed detections, whose
     * latency is too high for realtime, non-frozen, image analysis
     */
    open fun drawBackgroundImage(canvas: Canvas) {
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
    }

    /**
     * Function that gets once [DetectionOverlay] is invalidated and redraw is request.
     */
    final override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawBackgroundImage(canvas)
        drawDetections(canvas)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event?.action == MotionEvent.ACTION_DOWN) {
            detections.firstOrNull { scaleDetectionToScreenRect(it).contains(event.x, event.y) }
                ?.let { onDetectionClicked?.invoke(it) }
        }

        return true
    }
}