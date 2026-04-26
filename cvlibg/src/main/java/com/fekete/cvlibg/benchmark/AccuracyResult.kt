package com.fekete.cvlibg.benchmark

data class AccuracyResult(
    /**
     * Name of the file this results belong to
     */
    var filename: String = "",
    /**
     * Matched detections to ground truths based on IoU
     */
    var recall: Float = 0f,

    /**
     * Rate found detections / ground truth
     */
    var foundBoxRate: Float = 0f,
    /**
     * False positives not matched to any ground truth
     */
    var falsePositiveRate: Float = 0f,

    /**
     * Extra detections matching the same ground truth (IoU >= threshold)
     */
    var duplicateRate: Float = 0f,

    /**
     * Rate of missed ground truths, not matched to detections
     */
    var missedRate: Float = 0f,
    /**
     * Matched ground truths to detections, based on IoU and Class
     */
    var matchClassAccuracy: Float = 0f,

    /**
     * IoU of matched detections to ground truths based on IoU
     */
    var meanIoU: Float = 0f,

    /**
     * IoU of correctly matched ground truths to detections
     */
    var correctMeanIoU: Float = 0f,

    /**
     * Total time it took for detector to analyze image in milliseconds
     */
    var time: Double = 0.0
) {
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
