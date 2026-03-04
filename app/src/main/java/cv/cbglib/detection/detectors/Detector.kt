package cv.cbglib.detection.detectors

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Size
import cv.cbglib.services.AssetService
import cv.demoapps.bangdemo.MyApp

/**
 * Abstract class for all classes that will detect objects. Should work as a common interface for all derived Detectors.
 */
abstract class Detector(
    protected val modelPath: String,
    protected val confThreshold: Float = 0.6f,
    protected val applyNMS: Boolean = true,
    protected val nmsThreshold: Float = 0.5f,
    public val inputDataSize: Size
) {
    private var isBuilt: Boolean = false
    protected var showMetrics: Boolean = false
    protected var verboseMetrics: Boolean = false

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
     * optionally metrics.
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
    fun build(context: Context, showMetrics: Boolean = false, verboseMetrics: Boolean = false) {
        if (isBuilt) return

        this.showMetrics = showMetrics
        this.verboseMetrics = verboseMetrics
        val assetService = (context.applicationContext as MyApp).assetService

        build(assetService, showMetrics, verboseMetrics)
    }


    /**
     * Loads model and builds it. Throws an exception on error.
     *
     * @param assetService Service used to load model bytes from assets.
     * @param showMetrics Boolean value whenever the metrics should be shown.
     * @param verboseMetrics Boolean value whenever verbose metrics should be shown, [showMetrics] must be true.
     */
    fun build(assetService: AssetService, showMetrics: Boolean = false, verboseMetrics: Boolean = false) {
        if (isBuilt) return

        this.showMetrics = showMetrics
        this.verboseMetrics = verboseMetrics

        runtimeSetup(assetService)

        isBuilt = true
    }


    /**
     * Generic function that performs action and measures time if [measure] is true. Returns the result of action and
     * time it took to complete action.
     *
     * @param measure If true the time will be measured, other a dummy 0 will be placed as second parameter of [Pair].
     * @return Returns a Pair<action result, time in nanoseconds it took to complete the action>.
     */
    protected inline fun <T> measureTime(measure: Boolean, action: () -> T): Pair<T, Long> {
        if (measure) {
            val start = SystemClock.elapsedRealtimeNanos()
            val result = action()
            val end = SystemClock.elapsedRealtimeNanos()

            return result to (end - start)
        } else {
            val result = action()
            return result to 0
        }
    }
}