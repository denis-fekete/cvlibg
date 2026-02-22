package cv.cbglib.detection

import android.os.SystemClock
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import cv.cbglib.detection.detectors.DetectorResult
import cv.cbglib.detection.detectors.Detector
import cv.cbglib.logging.MetricsOverlay

class ImageAnalyzer(
    private val detectionOverlay: DetectionOverlay,
    private val metricsOverlay: MetricsOverlay? = null,
    private val realtimeDetector: Detector,
    private val precisionDetector: Detector,
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

        val totalTimeStart = SystemClock.elapsedRealtimeNanos()
        if (useRealtimeDetector) {
            detectorResult = realtimeDetector.run(bitmap)
            detectorRunning = false
        } else {
            detectorResult = precisionDetector.run(bitmap)
            detectorRunning = false

            detectionOverlay.setBackgroundBitmap(bitmap)
            pauseAnalysis = true
        }
        val totalTimeEnd = SystemClock.elapsedRealtimeNanos()

        metricsOverlay?.post {
            metricsOverlay.updateLogData(
                detectorResult.metrics,
                if (detectorResult.showMetrics) totalTimeEnd - totalTimeStart else null
            )
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