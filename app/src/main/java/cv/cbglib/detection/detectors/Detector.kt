package cv.cbglib.detection.detectors

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import cv.cbglib.services.AssetService
import cv.demoapps.bangdemo.MyApp

/**
 * Abstract class for all classes that will detect objects. Should work as a common interface for all derived Detectors.
 */
abstract class Detector(protected val modelPath: String) {
    private var isBuilt: Boolean = false
    protected var modelBytes: ByteArray? = null
    protected var showMetrics: Boolean = false
    protected var verboseMetrics: Boolean = false

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

    protected abstract fun detect(image: Bitmap): DetectorResult

    /**
     * Destroys allocated memory of the [Detector]. Must be called to prevent memory leaks!
     */
    abstract fun destroy()


    /**
     * Loads model and builds it. Throws an exception on error.
     *
     * @param context Context required to get load model bytes from assets.
     * @param showMetrics Boolean value whenever the metrics should be shown.
     * @param verboseMetrics Boolean value whenever verbose metrics should be shown, [showMetrics] must be true.
     */
    fun build(context: Context, showMetrics: Boolean = false, verboseMetrics: Boolean = false) {
        if (isBuilt) return

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

        val modelBytes = assetService.getModel(modelPath)
        build(modelBytes, showMetrics, verboseMetrics)
    }

    /**
     * Loads model and builds it. Throws an exception on error.
     *
     * @param modelBytes [ByteArray] containing model loaded as bytes.
     * @param showMetrics Boolean value whenever the metrics should be shown.
     * @param verboseMetrics Boolean value whenever verbose metrics should be shown, [showMetrics] must be true.
     */
    private fun build(modelBytes: ByteArray, showMetrics: Boolean = false, verboseMetrics: Boolean = false) {
        if (isBuilt) return

        this.showMetrics = showMetrics
        this.verboseMetrics = verboseMetrics
        this.modelBytes = modelBytes

        afterModelLoaded()

        isBuilt = true
    }

    /**
     * Function called after model bytes were loaded. Make any preparations that would be called inside of init{} for
     * this model.
     */
    abstract fun afterModelLoaded()

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