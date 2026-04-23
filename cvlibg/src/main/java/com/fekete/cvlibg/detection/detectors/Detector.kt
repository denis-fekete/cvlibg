package com.fekete.cvlibg.detection.detectors

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.util.Size
import com.fekete.cvlibg.detection.Detection
import com.fekete.cvlibg.services.AssetService

/**
 * Abstract class for all classes that will detect objects. Should work as a common interface for all derived Detectors.
 */
abstract class Detector(
    public val modelPath: String,
    protected val confThreshold: Float = 0.6f,
    protected val applyNMS: Boolean = true,
    protected val nmsThreshold: Float = 0.5f,
    val inputDataSize: Size = Size(640, 640)
) {
    private var isBuilt: Boolean = false
    protected var showMetrics: Boolean = false
    protected var verboseMetrics: Boolean = false
    protected open val detectorName = "Detector"

    /**
     * Abstract function that takes [Bitmap] containing camera image as input and returns [DetectorResult].
     *
     * @param image [Bitmap] containing input image that inference engine will run object detection model on.
     *
     * @return [DetectorResult] object containing list of detection, image info and performance metrics (optional)
     */
    protected abstract fun detect(image: Bitmap): DetectorResult

    /**
     * Destroys allocated memory of the [Detector]. Must be called to prevent memory leaks!
     */
    open fun destroy() {}

    /**
     * Internal build function that is called on Detector build. In this function the Detector MUST load model and be
     * finalized and ready to perform detections. Throws an exception on error.
     *
     * @param verboseMetrics Boolean value whenever verbose metrics should be shown, [showMetrics] must be true.
     */
    protected abstract fun runtimeSetup(assetService: AssetService)

    /**
     * Runs image detection analysis and returns [DetectorResult] containing detections, image information and
     * optionally metrics. Output [Detection]s may and may not be scaled to input image, this depends on implementation.
     *
     * @param image input [Bitmap] containing the image to be analyzed
     *
     * @return [DetectorResult] object containing list [Detection] objects and metrics if enabled.
     */
    fun run(image: Bitmap): DetectorResult {
        if (!isBuilt) {
            throw IllegalStateException("Detector is not yet built")
        }

        return detect(image)
    }

    /**
     * Loads model and builds it. Throws an exception on error.
     *
     * @param context Context used for loading [AssetService], from which a model will be loaded.
     * @param showMetrics Boolean value whenever the metrics should be shown.
     * @param verboseMetrics Boolean value whenever verbose metrics should be shown, [showMetrics] must be true.
     */
    fun build(context: Context) {
        if (isBuilt) return
        val assetService = AssetService(context.applicationContext as Application)
        build(assetService)
    }


    /**
     * Loads model and builds it. Throws an exception on error.
     *
     * @param assetService Service used to load model bytes from assets.
     * @param showMetrics Boolean value whenever the metrics should be shown.
     * @param verboseMetrics Boolean value whenever verbose metrics should be shown, [showMetrics] must be true.
     */
    fun build(assetService: AssetService) {
        if (isBuilt) return
        runtimeSetup(assetService)
        isBuilt = true
    }

    /**
     * Sets the logging of metrics to [value].
     */
    fun setMetricsEnabled(value: Boolean) {
        showMetrics = value
    }

    /**
     * Sets the logging of verbose metrics to [value].
     */
    fun setVerboseMetricsEnabled(value: Boolean) {
        verboseMetrics = value
    }

    override fun toString(): String {
        return "$detectorName(" +
                "modelPath=$modelPath, " +
                "confThreshold=$confThreshold, " +
                "nmsThreshold=$nmsThreshold, " +
                "applyNMS=$applyNMS, )" +
                "inputDataSize=(${inputDataSize.width},${inputDataSize.height}))"
    }

    /**
     * To string function for CSV export
     */
    open fun toCsvString(): String {
        return "$detectorName," +
                "$modelPath, " +
                "(confThreshold=$confThreshold;" +
                "nmsThreshold=$nmsThreshold;" +
                "applyNMS=$applyNMS;" +
                "inputDataSize=(${inputDataSize.width},${inputDataSize.height}))"
    }
}