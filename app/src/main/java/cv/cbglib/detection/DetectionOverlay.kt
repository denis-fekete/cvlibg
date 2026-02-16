package cv.cbglib.detection

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.max

/**
 * Abstract class used as a view for drawing detections, class does not contain a [drawDetections] implementation where
 * a specific implementation of how detections are represent on screen are coded.
 */
abstract class DetectionOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    /**
     * List containing current [Detection] objects that are on screen.
     */
    protected val detections = mutableListOf<Detection>()
    private var imageDetails = ImageDetails(1f, 0, 0)
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
     * Scales [Detection] to current screen, [cv.cbglib.detection.detectors.IDetector]s might use different size as
     * inputs to models. Info about current image is stored in [imageDetails] that is updated alongside new
     * [detections] in [updateBoxes] function that is called by inside [ImageAnalyzer].
     */
    protected fun scaleDetectionToScreenRect(det: Detection): RectF {
        tmpRect = det.toRectF()

        val fixedLeft = (tmpRect.left - imageDetails.padX) / imageDetails.scale
        val fixedTop = (tmpRect.top - imageDetails.padY) / imageDetails.scale
        val fixedRight = (tmpRect.right - imageDetails.padX) / imageDetails.scale
        val fixedBottom = (tmpRect.bottom - imageDetails.padY) / imageDetails.scale

        tmpRect.set(
            fixedLeft * scale - cropX,
            fixedTop * scale - cropY,
            fixedRight * scale - cropX,
            fixedBottom * scale - cropY
        )

        return tmpRect
    }

    /**
     * Function is called each [onDraw] called (every time this view is invalidated).
     * Derived classes must implement this function!
     */
    protected abstract fun drawDetections(canvas: Canvas)

    /**
     * Cleans internal detection list and adds new boxes into it.
     *
     * @param newBoxes list of <Detection> objects that contain info about found detections in current image
     * @param imageDetails info about current Detections and image, used for scaling of [onTouchEvent] events.
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
     * Function that gets once [cv.cbglib.detection.DetectionOverlay] is invalidated and redraw is request.
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