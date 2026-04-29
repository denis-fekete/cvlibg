package com.fekete.cvlibg.benchmark

/**
 * Result of [AccuracyBenchmark] run.
 *
 * @param filename Name of the file this results belong to
 * @param recall Matched detections to ground truths based on IoU
 * @param foundBoxRate Rate of: found detections / ground truth
 * @param falsePositiveRate False positives not matched to any ground truth
 * @param duplicateRate Extra detections matching the same ground truth (IoU >= threshold)
 * @param missedRate Rate of missed ground truths, not matched to detections
 * @param matchClassAccuracy Matched ground truths to detections, based on IoU and Class
 * @param meanIoU IoU of matched detections to ground truths based on IoU
 * @param correctMeanIoU IoU of correctly matched ground truths to detections
 * @param time Total time it took for detector to analyze image in milliseconds
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
data class AccuracyResult(
    var filename: String = "",
    var recall: Float = 0f,
    var foundBoxRate: Float = 0f,
    var falsePositiveRate: Float = 0f,
    var duplicateRate: Float = 0f,
    var missedRate: Float = 0f,
    var matchClassAccuracy: Float = 0f,
    var meanIoU: Float = 0f,
    var correctMeanIoU: Float = 0f,
    var time: Double = 0.0
) {
    /**
     * Converts the [AccuracyResult] into CSV file format, separated by the commas
     */
    fun csvString(): String {
        return "$filename," +
                "$recall," +
                "$foundBoxRate," +
                "$falsePositiveRate," +
                "$missedRate," +
                "$duplicateRate," +
                "$matchClassAccuracy," +
                "$meanIoU," +
                "$correctMeanIoU," +
                "$time"
    }
}