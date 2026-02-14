package cv.demoapps.bangdemo.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import cv.cbglib.detection.DetectionOverlay
import cv.demoapps.bangdemo.MyApp
import cv.demoapps.bangdemo.R
import kotlin.math.max

class BangDetectionOverlay(context: Context, attrs: AttributeSet?) : DetectionOverlay(context, attrs) {
    // helper variable to prevent creating a new object on each redraw
    private var scaledRect: RectF = RectF()

    // helper variable to prevent creating a new object on each redraw
    private var bgRect: RectF = RectF()

    /**
     * Service used for looking up details about [cv.cbglib.detection.Detection] objects.
     *
     * @see [cv.demoapps.bangdemo.data.CardDetail] for other values that can be read
     */
    private val cardDetailsService by lazy {
        (context.applicationContext as MyApp).cardDetailsService
    }


    /**
     * Service used for looking translating `classId` into `linkId` from [cv.demoapps.bangdemo.data.Class2Link] data class.
     * Since [cardDetailsService] uses `String` as ID it is needed translation.
     */
    private val class2linkService by lazy {
        (context.applicationContext as MyApp).class2linkService
    }

    /**
     * Service used for retrieving a global application settings, mostly used for font size.
     */
    private val settingsService by lazy {
        (context.applicationContext as MyApp).settingsService
    }
    private val boxPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.detection_box)
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
        alpha = 200
    }

    private val textBackgroundPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.detection_text_background)
        style = Paint.Style.FILL
        alpha = 200
    }

    private val textPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.detection_text)
        textSize = unitSpToPix(settingsService.fontSize.toFloat())
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }
    private val errorTextPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.detection_text)
        textSize = unitSpToPix(settingsService.fontSize.toFloat())
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }

    fun writeErrorOnScreen(canvas: Canvas, text: String) {
        canvas.drawText(text, 0f, height / 2f, errorTextPaint)
    }

    override fun drawDetections(canvas: Canvas) {
        if (!cameraDimensionsCorrect()) {
            writeErrorOnScreen(canvas, "Camera dimensions were not initialized yet!")
        } else {
            detections.forEach { det ->
                // scale detection into correct size, because detection from model might not have same width and height
                // as camera images, or screen that this overlay is drawing onto
                scaledRect = scaleDetectionToScreenRect(det)

                canvas.drawRect(scaledRect, boxPaint)

                // get label from services
                val linkId = class2linkService.items[det.classIndex]?.linkId
                val className = cardDetailsService.items[linkId]?.title
                val label = "${className}: ${(det.score * 100).toInt()}%"

                // determine text width and height
                val textWidth = textPaint.measureText(label)
                val textHeight = textPaint.fontMetrics.run { bottom - top }

                val padding = 8
                // filled rectangle for text
                bgRect.left = max(scaledRect.left, 0f)
                bgRect.top = max(scaledRect.top - textHeight - padding, 0f)
                bgRect.right = bgRect.left + textWidth + 2 * padding
                bgRect.bottom = bgRect.top + textHeight + padding

                canvas.drawRect(bgRect, textBackgroundPaint)

                canvas.drawText(label, bgRect.left + padding, bgRect.bottom - padding, textPaint)
            }
        }
    }
}