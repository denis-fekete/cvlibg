package com.fekete.cvlibg.benchmark

import android.content.Context
import android.util.Log
import com.fekete.cvlibg.detection.detectors.Detector
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

abstract class DetectorBenchmark(protected val context: Context) {
    @Volatile
    protected var stopFlag = false

    @Volatile
    protected var running = false
    protected var detector: Detector? = null

    /**
     * Sets the [Detector] for the Benchmark, if detector was defined, first destroy the old detector.
     */
    open fun replaceDetector(detector: Detector) {
        if (running) {
            Log.e(
                "DetectorBenchmark",
                "Attempt to change detector while the detector is running. Detector will not be changed!"
            )
        }

        this.detector?.destroy() // destroy if not none
        this.detector = detector
        detector.build(context)
    }

    /**
     * Runs the benchmark on with the [detector] for [numberOfRuns] detection tasks.
     */
    abstract suspend fun run(numberOfRuns: ULong)

    /**
     * Saves the benchmark results into the device's storage.
     *
     * @param fileName Name of file to which results will be saved
     * @param context Context for saving image data
     */
    abstract suspend fun save(fileName: String?, context: Context)

    /**
     * Callback function that returns status update message from the benchmark.
     */
    var onStatusUpdate: ((String) -> Unit)? = null

    /**
     * Callback function called on benchmark end
     */
    var onFinished: (() -> Unit)? = null

    /**
     * Callback function called after each run from the [run] method finished.
     *
     * Returns normalized float value representing progress
     */
    var onProgressUpdate: ((Int) -> Unit)? = null

    /**
     * Callback function called once the benchmark saved all data
     *
     * @param String value containing name of the file
     */
    var onSaved: (() -> Unit)? = null

    /**
     * Clean up memory by destroying the stored detector
     */
    open fun destroy() {
        detector?.destroy()
    }

    /**
     * Stops the benchmark run
     */
    open fun stop() {
        stopFlag = true
    }

    /**
     * Check if the filename ends with `.csv` extension. If not, it will be appended. If [fileName] was not provided a
     * name will be generated from timestamp.
     *
     *
     * @param fileName string containing file name, this will be checked for `.csv` extension. If `null` name will be
     * generated from [timestamp] or current time.
     * @param prefix string added before the timestamp if [fileName] was not provided.
     * @param timestamp a timestamp used for generation of file, if not provided a current time will be used.
     * @return Generated filename from timestamp with `.csv` extension
     */
    protected fun checkNameOrGenerate(
        fileName: String? = null,
        prefix: String? = null,
        timestamp: LocalDateTime? = null
    ): String {
        if (fileName != null) {
            val finalFileName = if (!fileName.endsWith(".csv"))
                "$fileName.csv"
            else
                fileName
            return finalFileName
        } else {
            val time = timestamp ?: LocalDateTime.now()

            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")
            val timestamp = time.format(formatter)

            return if (prefix != null) "${prefix}_${detector?.modelPath}_${timestamp}.csv" else "${timestamp}.csv"
        }
    }
}