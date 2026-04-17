package cv.cbglib.utils

import android.os.SystemClock

object Timer {
    /**
     * Returns elapsed nanoseconds from boot
     */
    @JvmStatic
    fun getTime() = SystemClock.elapsedRealtimeNanos()

    /**
     * Generic function that performs action and measures time if [measure] is true. Returns the result of action and
     * time it took to complete action.
     *
     * @param measure If true the time will be measured, other a dummy 0 will be placed as second parameter of [Pair].
     * @return Returns a Pair<action result, time in nanoseconds it took to complete the action>.
     */
    @JvmStatic
    inline fun <T> measure(measure: Boolean = true, action: () -> T): Pair<T, TimerResult> {
        if (measure) {
            val start = SystemClock.elapsedRealtimeNanos()
            val result = action()
            val end = SystemClock.elapsedRealtimeNanos()

            return result to TimerResult((end - start))
        } else {
            val result = action()
            return result to TimerResult(0)
        }
    }
}