package com.fekete.cvlibg.utils

import android.os.SystemClock

/**
 * Utility object/static class for measuring time of function calls. [Timer] uses [SystemClock.elapsedRealtimeNanos]
 * for measuring time.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
object Timer {
    /**
     * Returns elapsed nanoseconds from boot
     */
    @JvmStatic
    fun getTime() = SystemClock.elapsedRealtimeNanos()

    /**
     * Generic function that performs the [action] and if [measure] is enabled a time will be returned.
     *
     * Usage:
     * `val (result, time) = Timer.measure(isLoggingEnabled) { foo() }`
     *
     * Where `foo()` is a function to be measured and `isLoggingEnable` is boolean expression or variable enabling
     * measurement.
     *
     * @param measure If true the time will be measured, other a dummy 0 will be placed as second parameter of [Pair].
     * This parameter is available to remove copied code, one with time measurement and other without. It should be
     * noted that, this function does add a small overhead, even if [measure] is set to `false`.
     * @param action Mostly lamda function defining was action should be performed
     *
     * @return Returns a [Pair] or action result, [TimerResult] with time it took to compute [action]
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