package com.fekete.cvlibg.benchmark

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import com.fekete.cvlibg.utils.Timer
import com.fekete.cvlibg.utils.TimerResult
import com.fekete.cvlibg.detection.detectors.AbstractYoloDetector.Companion.METRICS_NMS_KEY
import com.fekete.cvlibg.detection.detectors.AbstractYoloDetector.Companion.METRICS_EXTRACT_KEY
import com.fekete.cvlibg.detection.detectors.AbstractYoloDetector.Companion.METRICS_LETTERBOX_KEY
import com.fekete.cvlibg.detection.detectors.AbstractYoloDetector.Companion.METRICS_CONVERSION_KEY
import com.fekete.cvlibg.detection.detectors.AbstractYoloDetector.Companion.METRICS_INTERFACE_KEY
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

/**
 * Class for benchmarking latency and inference speed of [com.fekete.cvlibg.detection.detectors.Detector] and its models.
 *
 * @param context context used for loading models from the assets
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class PerformanceBenchmark(context: Context) : DetectorBenchmark(context) {
    val totalList = mutableListOf<TimerResult>()
    val letterboxList = mutableListOf<TimerResult>()
    val conversionList = mutableListOf<TimerResult>()
    val inferenceList = mutableListOf<TimerResult>()
    val extractList = mutableListOf<TimerResult>()
    val nmsList = mutableListOf<TimerResult>()
    var benchmarkStartTime: LocalDateTime? = null

    /**
     * Runs the Benchmark for the [numberOfRuns]. Results of each run/pass are stored. Results are stored using [save]
     * method. This method updates the [onStatusUpdate] and [onProgressUpdate], however, these data might be lossy and
     * are for UI purposes to inform user of progress. On end the method invokes [onFinished] callback.
     */
    override suspend fun run(numberOfRuns: ULong) {
        benchmarkStartTime = LocalDateTime.now()
        stopFlag = false
        running = true

        if (detector == null) {
            onStatusUpdate?.invoke("Detector was not set, use replaceDetector() method")
            onFinished?.invoke()
            return
        }

        detector?.setMetricsEnabled(true)
        detector?.setVerboseMetricsEnabled(true)

        val bitmap =
            createBitmap(detector?.inputDataSize!!.width, detector?.inputDataSize!!.height, Bitmap.Config.ARGB_8888)

        totalList.clear()
        letterboxList.clear()
        conversionList.clear()
        inferenceList.clear()
        extractList.clear()
        nmsList.clear()

        onStatusUpdate?.invoke("Initializing Performance Benchmark for ${detector?.modelPath}")

        var total = 0L
        for (i in 0uL until numberOfRuns) {
            if (stopFlag)
                break

            val (results, runTime) = Timer.measure { detector?.run(bitmap) }

            results?.showMetrics?.let {
                if (!it) {
                    onStatusUpdate?.invoke("Detector did not provide metrics. Ending the benchmark.")
                    onFinished?.invoke()
                    return
                }
            }

            letterboxList.add(results?.performanceMetrics[METRICS_LETTERBOX_KEY] ?: TimerResult(0))
            conversionList.add(results?.performanceMetrics[METRICS_CONVERSION_KEY] ?: TimerResult(0))
            inferenceList.add(results?.performanceMetrics[METRICS_INTERFACE_KEY] ?: TimerResult(0))
            extractList.add(results?.performanceMetrics[METRICS_EXTRACT_KEY] ?: TimerResult(0))
            nmsList.add(results?.performanceMetrics[METRICS_NMS_KEY] ?: TimerResult(0))
            totalList.add(runTime)

            total += runTime.nanos

            val percent = (i + 1uL).toFloat() / numberOfRuns.toFloat()
            onProgressUpdate?.invoke((percent * 100).toInt())

            if (i % 10uL == 0uL) {
                onStatusUpdate?.invoke("${i + 1uL}/${numberOfRuns}: average=${TimerResult(total / (i + 1uL).toLong()).millis}ms")
            }
        }

        onStatusUpdate?.invoke("Benchmark finished")
        onProgressUpdate?.invoke(100)
        onFinished?.invoke()
        running = false
    }

    /**
     * Saves last run of the benchmark. File is saved into `Android/data/this_project_/files/fileName` where [fileName]
     * is the name of the file in `.csv`. Function invokes the [onSaved] callback function and updates [onStatusUpdate]
     * with result file.
     *
     * @param fileName Name of saved file, if file does not end with `.csv`, it will be appended. If the [fileName] is
     * `null` a name will be generated automatically using time.
     *
     * @param context [Context] for writing files using [File]. Recommended to use application context.
     */
    override suspend fun save(fileName: String?, context: Context) {
        running = true
        onStatusUpdate?.invoke("Saving results...")

        val finalFileName = checkNameOrGenerate(fileName, "PB", benchmarkStartTime)
        val file = File(context.getExternalFilesDir(null), finalFileName)
        val writer = file.bufferedWriter(Charsets.UTF_8)

        val startTimestamp = benchmarkStartTime?.format(ISO_LOCAL_DATE_TIME)
        val endTimestamp = LocalDateTime.now().format(ISO_LOCAL_DATE_TIME)
        writer.write(
            "${
                detector.toString().replace(",", ";")
            },start=${startTimestamp},end=${endTimestamp},runs=${totalList.size},,,\n"
        )


        writer.write("run,total,letterbox,conversion,inference,extract,nms\n")

        for (i in 0 until totalList.size) {
            val text = "$i," +
                    "${totalList[i].nanos}," +
                    "${letterboxList[i].nanos}," +
                    "${conversionList[i].nanos}," +
                    "${inferenceList[i].nanos}," +
                    "${extractList[i].nanos}," +
                    "${nmsList[i].nanos}"

            writer.write(text)
            writer.write("\n")
        }

        val (totalA, totalM) = calculateAverageAndMedian(totalList)
        val (letterboxA, letterboxM) = calculateAverageAndMedian(letterboxList)
        val (conversionA, conversionM) = calculateAverageAndMedian(conversionList)
        val (inferenceA, inferenceM) = calculateAverageAndMedian(inferenceList)
        val (extractA, extractM) = calculateAverageAndMedian(extractList)
        val (nmsA, nmsM) = calculateAverageAndMedian(nmsList)

        val textA = "average," +
                "${totalA.nanos}," +
                "${letterboxA.nanos}," +
                "${conversionA.nanos}," +
                "${inferenceA.nanos}," +
                "${extractA.nanos}," +
                "${nmsA.nanos}\n"

        val textM = "median," +
                "${totalM.nanos}," +
                "${letterboxM.nanos}," +
                "${conversionM.nanos}," +
                "${inferenceM.nanos}," +
                "${extractM.nanos}," +
                "${nmsM.nanos}\n"

        writer.write(textA)
        writer.write(textM)

        writer.close()

        onStatusUpdate?.invoke("Results saved to: ${file.absolutePath}")
        onSaved?.invoke()
        running = false
    }

    /**
     * Calculates average and median from the [data] [MutableList].
     *
     * @return [Pair] of two [TimerResult]. First is Average, second is Median.
     */
    private fun calculateAverageAndMedian(data: MutableList<TimerResult>): Pair<TimerResult, TimerResult> {
        data.sortBy { it.nanos }

        val medianTime: TimerResult
        if (data.size % 2 == 0) {
            val first = data[data.size / 2 - 1]
            val second = data[data.size / 2]
            medianTime = TimerResult((first.nanos + second.nanos) / 2)
        } else {
            medianTime = data[data.size / 2]
        }

        var total = 0L
        data.forEach {
            total += it.nanos
        }
        val averageTime = TimerResult(total / data.size)

        return Pair(averageTime, medianTime)
    }
}