package cv.cbglib.detection

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import cv.cbglib.detection.detectors.DetectorResult
import cv.cbglib.detection.detectors.Detector
import cv.cbglib.logging.MetricsOverlay
import cv.cbglib.utils.Timer
import cv.cbglib.utils.TimerResult

class ImageAnalyzer(
    private val detectionOverlay: DetectionOverlay,
    private val metricsOverlay: MetricsOverlay? = null,
    private val realtimeDetector: Detector?,
    private val qualityDetector: Detector?,
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

        if (!resolutionInitialized) {
            detectionOverlay.setCameraResolution(imageProxy.width, imageProxy.height)
            resolutionInitialized = true
        }

        val bitmap = imageProxy.toBitmap()
        imageProxy.close()

        detectorRunning = true

        val detectorResult: DetectorResult?

        val totalTimeStart = Timer.getTime()
        if (realtimeDetector != null && useRealtimeDetector) {
            detectorResult = realtimeDetector.run(bitmap)
            detectorRunning = false
        } else if (qualityDetector != null) {
            detectorResult = qualityDetector.run(bitmap)
            detectorRunning = false

            detectionOverlay.setBackgroundBitmap(bitmap)
            pauseAnalysis = true
        } else {
            detectorResult = null
        }
        val totalTimeEnd = Timer.getTime()

        if (detectorResult != null) {
            metricsOverlay?.post {
                metricsOverlay.updateLogData(
                    detectorResult.performanceMetrics,
                    detectorResult.otherMetrics,
                    if (detectorResult.showMetrics) TimerResult(totalTimeEnd - totalTimeStart) else null
                )
            }

            // add new [Detection] boxes to draw and invalidate View that is drawing them
            detectionOverlay.post {
                detectionOverlay.updateBoxes(detectorResult.detections, detectorResult.details)
            }
        }
    }

    /**
     * Sets Analyzer internal state to use precise detector and freeze next image analysis. Finalized image analysis
     * will be shown on [detectionOverlay]. To unfreeze and continue with realtime detection call [switchToFasterAnalysis].
     */
    fun switchToDetailedAnalysis() {
        useRealtimeDetector = false
    }

    /**
     * Resumes analyzer to use realtime detector instead on precise detector.
     */
    fun switchToFasterAnalysis() {
        useRealtimeDetector = true
        pauseAnalysis = false

        // delete background image
        detectionOverlay.post {
            detectionOverlay.setBackgroundBitmap(null)
        }
    }

    /**
     * Sets the logging of metrics to [value].
     */
    fun setMetricsEnabled(value: Boolean) {
        realtimeDetector?.setMetricsEnabled(value)
        qualityDetector?.setMetricsEnabled(value)
    }

    /**
     * Sets the logging of verbose metrics to [value].
     */
    fun setVerboseMetricsEnabled(value: Boolean) {
        realtimeDetector?.setVerboseMetricsEnabled(value)
        qualityDetector?.setVerboseMetricsEnabled(value)
    }

    fun destroy() {
        realtimeDetector?.destroy()
        qualityDetector?.destroy()
    }
}