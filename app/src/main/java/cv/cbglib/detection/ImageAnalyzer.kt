package cv.cbglib.detection

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import cv.cbglib.detection.detectors.DetectorResult
import cv.cbglib.detection.detectors.IDetector
import cv.cbglib.logging.MetricsOverlay

class ImageAnalyzer(
    private val detectionOverlay: DetectionOverlay,
    private val metricsOverlay: MetricsOverlay? = null,
    private val realtimeDetector: IDetector,
    private val precisionDetector: IDetector,
) : ImageAnalysis.Analyzer {
    private var resolutionInitialized = false

    @Volatile
    private var detectorRunning = false

    @Volatile
    private var useRealtimeDetector = true
    private var pauseAnalysis = false

    /**
     * Function that gets called by CameraProvider to analyze the current image. The [imageProxy] is output of a camera
     * and is an in buffers, these should be freed as fast as possible.
     */
    override fun analyze(imageProxy: ImageProxy) {
        if (detectorRunning || pauseAnalysis) {
            imageProxy.close()
            return
        }

        val bitmap = imageProxy.toBitmap()
        imageProxy.close()

        detectorRunning = true

        if (!resolutionInitialized) {
            detectionOverlay.setCameraResolution(imageProxy.width, imageProxy.height)
            resolutionInitialized = true
        }

        val detectorResult: DetectorResult

        if (useRealtimeDetector) {
            detectorResult = realtimeDetector.detect(bitmap)
            detectorRunning = false
        } else {
            detectorResult = precisionDetector.detect(bitmap)
            detectorRunning = false

            detectionOverlay.setBackgroundBitmap(bitmap)
            pauseAnalysis = true
        }


        if (detectorResult.metrics != null) {
            metricsOverlay?.post {
                metricsOverlay.updateLogData(detectorResult.metrics)
            }
        }

        // add new [Detection] boxes to draw and invalidate View that is drawing them
        detectionOverlay.post {
            detectionOverlay.updateBoxes(detectorResult.detections, detectorResult.details)
        }
    }

    /**
     * Sets Analyzer internal state to use precise detector and freeze next image analysis. Finalized image analysis
     * will be shown on [detectionOverlay]. To unfreeze and continue with realtime detection call [resumeAnalysis].
     */
    fun preciseDetectAndPause() {
        useRealtimeDetector = false
    }

    /**
     * Resumes analyzer to use realtime detector instead on precise detector.
     */
    fun resumeAnalysis() {
        useRealtimeDetector = true
        pauseAnalysis = false

        // delete background image
        detectionOverlay.post {
            detectionOverlay.setBackgroundBitmap(null)
        }
    }

    fun destroy() {
        realtimeDetector.destroy()
    }
}