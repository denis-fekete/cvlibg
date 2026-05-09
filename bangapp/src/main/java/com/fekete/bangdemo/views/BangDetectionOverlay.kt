package com.fekete.bangdemo.views

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.fekete.bangdemo.MyApp
import com.fekete.cvlibg.ui.DetectionOverlay
import com.fekete.bangdemo.R
import com.fekete.cvlibg.utils.CommonUtils
import kotlin.math.max

/**
 * Implementation of [DetectionOverlay] for a Bang! card game. Uses [cardDetailsService] and [class2linkService]
 * services to convert [com.fekete.cvlibg.detection.Detection] class index into a readable text.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class BangDetectionOverlay(context: Context, attrs: AttributeSet?) : DetectionOverlay(context, attrs) {
    // helper variable to prevent creating a new object on each redraw
    private var scaledRect: RectF = RectF()

    // helper variable to prevent creating a new object on each redraw
    private var bgRect: RectF = RectF()

    /**
     * Service used for looking up details about [com.fekete.cvlibg.detection.Detection] objects.
     *
     * @see [com.fekete.bangdemo.data.CardDetail] for other values that can be read
     */
    private val cardDetailsService by lazy {
        (context.applicationContext as MyApp).cardDetailsService
    }


    /**
     * Service used for looking translating `classId` into `linkId` from [bang.data.Class2Link] data class.
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
        strokeWidth = CommonUtils.spToPix(context, 2f)
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
        textSize = CommonUtils.spToPix(context, settingsService.data.fontSize.toFloat())
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
    }

    override fun drawDetections(canvas: Canvas) {
        detections.forEach { det ->
            // scale detection into correct size, because detection from model might not have same width and height
            // as camera images, or screen that this overlay is drawing onto
            scaledRect = scaleDetectionToScreenRect(det)

            canvas.drawRect(scaledRect, boxPaint)

            // get label from services
            val linkId = class2linkService.data[det.classIndex]?.linkId
            val className = cardDetailsService.data[linkId]?.title
            val label = "$className"

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