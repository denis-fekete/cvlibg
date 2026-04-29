package com.fekete.cvlibg.utils

/**
 * Value class for storing [Timer] results, with build it conversions for milliseconds, nanoseconds, etc. The [value] at
 * construction of class must be in nanoseconds.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
@JvmInline
value class TimerResult(
    private val value: Long
) {
    val nanos: Long get() = value

    val micros: Double get() = value / 1_000.0

    val millis: Double get() = value / 1_000_000.0

    val seconds: Double get() = value / 1_000_000_000.0

    val minutes: Double get() = value / 1_000_000_000.0 / 60.0

    val hours: Double get() = value / 1_000_000_000.0 / 60.0 / 60.0
}
