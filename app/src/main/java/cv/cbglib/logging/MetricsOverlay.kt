package cv.cbglib.logging

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.AttributeSet

/**
 * PerformanceLogOverlay is derived from LogOverlay, its purpose is draw performance related metrics/logs onto the screen.
 */
class MetricsOverlay(context: Context, attrs: AttributeSet?) : LogOverlay<MetricsValue>(context, attrs) {
    private var tmpAverage: Long = 0
    private var average: Long = 0
    private var cnt: Int = 0
    private val avgUpdateVal: Int = 10
    private var bgRect: RectF = RectF()

    @Volatile
    private var textList = mutableListOf<String>()

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

    override fun drawLogs(canvas: Canvas) {
        if (data.isEmpty()) return


        val baseOffset = textPaint.fontMetrics.run { bottom - top }
        val startX = width * 0.02f
        val startY = height * 0.05f
        var offsetY = startY
        var maxWidth = 0f

        // measure all text widths
        textList.clear()
        data.forEach {
            val text = "${it.key}: ${it.value / 1_000_000.0}ms\n"
            textList.add(text)

            val textWidth = textPaint.measureText(text)
            if (maxWidth < textWidth) {
                maxWidth = textWidth
            }
        }

        // draw background rectangle
        val padding = 8
        val textHeight = (data.size + 1) * baseOffset // +1 for Average

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

        if (cnt >= avgUpdateVal) {
            average = tmpAverage / avgUpdateVal
            tmpAverage = 0
            cnt = 0
        }
        tmpAverage += data.last().value
        cnt++

        canvas.drawText("Average (last $avgUpdateVal): ${average / 1_000_000}ms", startX, offsetY, textPaint)
    }
}