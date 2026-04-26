package com.fekete.cvlibg.benchmark

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import com.fekete.cvlibg.detection.Detection
import com.fekete.cvlibg.services.AssetService
import com.fekete.cvlibg.utils.Timer
import java.io.File
import java.time.LocalDateTime

/**
 * Benchmark for measuring accuracy of object detection models and [com.fekete.cvlibg.detection.detectors.Detector]s.
 */
class AccuracyBenchmark(context: Context, private val validationDataPath: String, private val iouThreshold: Float) :
    DetectorBenchmark(context) {
    private val assetService = AssetService(context.applicationContext as Application)
    private var results = mutableMapOf<String, MutableList<AccuracyResult>>()
    private var benchmarkStartTime: LocalDateTime? = null
    private var bitmap: Bitmap? = null

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

        detector?.setMetricsEnabled(false)
        detector?.setVerboseMetricsEnabled(false)

        onStatusUpdate?.invoke("Initializing Accuracy Benchmark for ${detector?.modelPath}")

        val validationPaths = findValidationFiles()
        results.clear()

        // check all files from the [validationDataPath]
        for (i in 0 until validationPaths.size) {
            if (stopFlag) break

            val (imagePath, labelPath) = validationPaths[i]
            val valDetections = openValidationData(imagePath, labelPath) ?: continue

            val resultsPerValData = mutableListOf<AccuracyResult>()
            // run each validation detection for [numberOfRuns] times
            for (i in 0uL until numberOfRuns) {
                if (stopFlag) break

                val (detections, runTime) = Timer.measure { detector!!.run(bitmap!!) }

                val aRes = accuracyEvaluator(valDetections, detections.detections)

                val percent = (i * validationPaths.size.toULong() + numberOfRuns).toFloat() /
                        (validationPaths.size.toULong() * numberOfRuns).toFloat()

                onProgressUpdate?.invoke((percent * 100).toInt())

                aRes.time = runTime.millis
                resultsPerValData.add(aRes)
            }

            onStatusUpdate?.invoke("${i + 1}/${validationPaths.size}: '$imagePath' finished")
            results[validationPaths[i].first] = resultsPerValData
        }

        onStatusUpdate?.invoke("Benchmark finished")
        onProgressUpdate?.invoke(100)
        onFinished?.invoke()
        running = false
    }

    /**
     * Evaluator of detection results accuracy.
     *
     * @param valDetections list of validation detections/ground truths
     * @param detections list of all detections
     *
     * @return [AccuracyResult] object containing results
     */
    private fun accuracyEvaluator(
        valDetections: List<ValidationDetection>,
        detections: List<Detection>
    ): AccuracyResult {
        val aRes = AccuracyResult()
        var matched = 0
        var matchedIoU = 0f
        var correctlyMatched = 0
        var correctlyMatchedIoU = 0f
        var duplicates = 0
        var matches = 0

        // check all validation detections
        valDetections.forEach { valDet ->
            valDet.detection = null
            valDet.iou = 0f
            matches = 0


            // check all detections for IoU
            detections.forEach { detection ->
                val iou = valDet.truth.iou(detection)

                // if iou is beyond threshold
                if (iou >= iouThreshold) {
                    matches++ // how many detections were matched with ground truth
                    if (iou > valDet.iou) { // remember the best iou
                        valDet.iou = iou
                        valDet.detection = detection
                    }
                }
            } // detections

            if (matches > 1)
                duplicates += matches - 1

            if (valDet.detection != null) {
                matched++
                matchedIoU += valDet.iou

                if (valDet.truth.classIndex == valDet.detection!!.classIndex) {
                    correctlyMatched++
                    correctlyMatchedIoU += valDet.iou
                }
            }
        } // valDetections

        val unmatchedDetections = detections.count { det ->
            valDetections.none { it.truth.iou(det) >= iouThreshold }
        }

        val missedGroundTruths = valDetections.count {
            it.detection == null
        }


        aRes.recall = matched / valDetections.size.toFloat()
        aRes.falsePositiveRate = unmatchedDetections.toFloat() / valDetections.size.toFloat()
        aRes.foundBoxRate = detections.size.toFloat() / valDetections.size.toFloat()
        aRes.matchClassAccuracy = if (matched > 0) correctlyMatched / matched.toFloat() else 0f
        aRes.meanIoU = if (matched > 0) matchedIoU / matched.toFloat() else 0f
        aRes.correctMeanIoU = if (correctlyMatched > 0) correctlyMatchedIoU / correctlyMatched.toFloat() else 0f
        aRes.duplicateRate = duplicates.toFloat() / valDetections.size.toFloat()
        aRes.missedRate = missedGroundTruths.toFloat() / valDetections.size.toFloat()

        return aRes
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

        val finalFileName = checkNameOrGenerate(fileName, "AB", benchmarkStartTime)
        val file = File(context.getExternalFilesDir(null), finalFileName)
        val writer = file.bufferedWriter(Charsets.UTF_8)


        writer.write("${detector?.toCsvString()},,,,,,,\n")
        writer.write("file,recall,foundBox,falsePositives,missedRate,duplicates,matchClsAcc,mIoU,cmIoU,time(ms)\n")

        val averagedPerValidationData = mutableListOf<AccuracyResult>()
        results.forEach { (key, value) ->
            val avg = calculateAverage(value)
            avg.filename = key
            averagedPerValidationData.add(avg)
        }

        averagedPerValidationData.forEach { aRes ->
            writer.write("${aRes.csvString()}\n")
        }

        val totalAvg = calculateAverage(averagedPerValidationData)
        totalAvg.filename = "average"
        writer.write("${totalAvg.csvString()}\n")
        writer.close()

        onStatusUpdate?.invoke("Results saved to: ${file.absolutePath}")
        onSaved?.invoke()
        running = false
    }

    private fun calculateAverage(data: MutableList<AccuracyResult>): AccuracyResult {
        val avg = AccuracyResult()

        data.forEach {
            avg.recall += it.recall
            avg.foundBoxRate += it.foundBoxRate
            avg.falsePositiveRate += it.falsePositiveRate
            avg.missedRate += it.missedRate
            avg.duplicateRate += it.duplicateRate
            avg.matchClassAccuracy += it.matchClassAccuracy
            avg.meanIoU += it.meanIoU
            avg.correctMeanIoU += it.correctMeanIoU
            avg.time += it.time
        }
        val samples = data.size

        avg.recall /= samples
        avg.foundBoxRate /= samples
        avg.falsePositiveRate /= samples
        avg.missedRate /= samples
        avg.duplicateRate /= samples
        avg.matchClassAccuracy /= samples
        avg.meanIoU /= samples
        avg.correctMeanIoU /= samples
        avg.time /= samples

        return avg
    }

    /**
     * Loads validation images and label text files.
     *
     * @return List of absolute paths in format: Pair<ImagePath, LabelPath>.
     */
    fun findValidationFiles(): List<Pair<String, String>> {
        val pairs = mutableListOf<Pair<String, String>>()

        val filePaths = assetService.recursiveFilesSearch(validationDataPath)
        val imagePaths = filePaths.filter { it.endsWith("png") || it.endsWith("jpg") }

        // find label file for each image file
        imagePaths.forEach { imagePath ->
            val imageName = imagePath.substringBeforeLast(".").substringAfterLast("/")
            filePaths.forEach { filePath ->
                val otherExt = filePath.substringAfterLast(".")

                if (otherExt == "txt") { // look only into .txt files
                    val otherName = filePath.substringBeforeLast(".").substringAfterLast("/")
                    // if names match, store pair
                    if (imageName.compareTo(otherName) == 0) {
                        pairs.add(imagePath.substringAfterLast("/") to filePath.substringAfterLast("/"))
                    }
                }
            }
        }

        return pairs
    }

    /**
     * Opens the validation data from [imagePath] and [labelPath] into list of [ValidationDetection] objects.
     * Also sets the private variable [bitmap] with loaded image or `null` if image could not be loaded.
     *
     * @param imagePath absolute path to the image data
     * @param labelPath absolute path to the label data
     * @return list of loaded [ValidationDetection] objects.
     */
    private fun openValidationData(imagePath: String, labelPath: String): List<ValidationDetection>? {
        bitmap = assetService.getImageBitmap(imagePath, validationDataPath) ?: return null
        val labelText = assetService.getTextFile(labelPath, validationDataPath) ?: return null
        val labelLines = labelText.split("\n")

        // create list of ValidationDetection objects
        val valDetections = mutableListOf<ValidationDetection>()
        labelLines.forEach {
            if (it.isNotEmpty()) {
                val normalizedLabel = Detection.fromString(it, " ")
                val groundTruth = normalizedLabel.absolute(bitmap!!.width.toFloat(), bitmap!!.height.toFloat())
                valDetections.add(ValidationDetection(groundTruth))
            }
        }

        return valDetections
    }
}