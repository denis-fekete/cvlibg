package com.fekete.cvlibg.logging

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.View
import com.fekete.cvlibg.utils.TimerResult
import java.util.Locale.getDefault
import kotlin.math.max

/**
 * View for drawing [com.fekete.cvlibg.detection.detectors.Detector] related metrics, such as performance, or detected objects.
 * Its main purpose is to provide developers with quick debug tool showing performance.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class MetricsOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private var tmpAverage: Long = 0
    private var average: Long = 0
    private var cnt: Int = 0
    private val avgUpdateVal: Int = 10
    private var bgRect: RectF = RectF()
    private var textList = mutableListOf<String>()

    private var performanceMetrics: MutableMap<String, TimerResult> = mutableMapOf()
    private var otherMetrics: MutableList<MetricsValue> = mutableListOf()
    private var totalTime: TimerResult? = null

    private val textBackgroundPaint = Paint().apply {
        color = Color.rgb(20, 20, 20)
        style = Paint.Style.FILL
        alpha = 100
    }

    private val textPaint = Paint().apply {
        color = Color.rgb(40, 255, 40)
        textSize = 32f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        isAntiAlias = true
        alpha = 200
    }

    fun updateLogData(performance: Map<String, TimerResult>, other: List<MetricsValue>, total: TimerResult?) {
        performanceMetrics.clear()
        otherMetrics.clear()

        performanceMetrics.putAll(performance)
        otherMetrics.addAll(other)

        totalTime = total

        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (performanceMetrics.isEmpty() && otherMetrics.isEmpty() && totalTime == null) return

        val baseOffset = textPaint.fontMetrics.run { bottom - top }
        val startX = width * 0.02f
        val startY = height * 0.05f
        var offsetY = startY
        var maxWidth = 0f

        if (totalTime != null) {
            // count average
            if (cnt >= avgUpdateVal) {
                average = tmpAverage / avgUpdateVal
                tmpAverage = 0
                cnt = 0
            }
            tmpAverage += totalTime!!.nanos
            cnt++
        }

        // measure all text widths and add it to the textList variable
        textList.clear()

        performanceMetrics.forEach {
            val text = "${it.key.uppercase(getDefault())}: ${it.value.millis}ms\n"
            textList.add(text)

            val textWidth = textPaint.measureText(text)
            if (maxWidth < textWidth) {
                maxWidth = textWidth
            }
        }

        textList.add("") // spacer

        otherMetrics.forEach {
            val text = "${it.prefix}: ${it.value}${it.value}\n"
            textList.add(text)

            val textWidth = textPaint.measureText(text)
            if (maxWidth < textWidth) {
                maxWidth = textWidth
            }
        }


        if (totalTime != null) {
            textList.add("Total : ${totalTime!!.millis}ms")
            maxWidth = max(maxWidth, textPaint.measureText(textList.last()))

            textList.add("Average (last $avgUpdateVal): ${average / 1_000_000}ms")
            maxWidth = max(maxWidth, textPaint.measureText(textList.last()))
        }

        // draw background rectangle
        val padding = 8
        // +1 for Average, +1 for total, +1 for space
        val textHeight = (textList.size) * baseOffset

        bgRect.left = startX - padding
        bgRect.top = startY - baseOffset - padding
        bgRect.right = bgRect.left + maxWidth + padding
        bgRect.bottom = bgRect.top + textHeight + 2 * padding

        canvas.drawRect(bgRect, textBackgroundPaint)

        // draw texts onto the screen
        textList.forEach {
            canvas.drawText(it, startX, offsetY, textPaint)
            offsetY += baseOffset
        }
    }
}