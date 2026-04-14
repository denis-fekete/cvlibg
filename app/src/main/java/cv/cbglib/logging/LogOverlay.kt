package cv.cbglib.logging

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.View

/**
 * Abstract class for drawing/printing log values onto the screen. Class uses templates for the data type stored data.
 */
abstract class LogOverlay<T>(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    protected val data = mutableListOf<T>()

    /**
     * Updates the data of LogOverlay.
     */
    fun updateLogData(data: List<T>) {
        this.data.clear()
        this.data.addAll(data)
        invalidate()
    }

    /**
     * Abstract function that will draw/print logs
     */
    protected abstract fun drawLogs(canvas: Canvas);

    final override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        drawLogs(canvas)
    }
}