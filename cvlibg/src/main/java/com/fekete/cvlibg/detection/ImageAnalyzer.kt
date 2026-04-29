package com.fekete.cvlibg.detection

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.fekete.cvlibg.detection.detectors.DetectorResult
import com.fekete.cvlibg.detection.detectors.Detector
import com.fekete.cvlibg.logging.MetricsOverlay
import com.fekete.cvlibg.ui.DetectionOverlay
import com.fekete.cvlibg.utils.Timer
import com.fekete.cvlibg.utils.TimerResult

/**
 * Class implementing the [ImageAnalysis.Analyzer], responsible for providing [realtimeDetector] and [qualityDetector]
 * with image data from the device's camera.
 *
 * Image analyzer has two modes, switching between the [realtimeDetector] and [qualityDetector]. When switched to
 * quality mode an input bitmap will be printed to the [DetectionOverlay] and detections will be printed on top of it,
 * effectively freezing camera.
 *
 * @param detectionOverlay reference to [DetectionOverlay] or derived class that will be updated with the results of the
 * image analysis.
 * @param metricsOverlay optional overlay for displaying metrics from the [Detector] classes.
 * @param realtimeDetector reference to [Detector] object for realtime, continuous image analysis
 * @param qualityDetector reference to [Detector] object, used for slower analysis with frozen background image
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
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

    @Volatile
    private var pauseAnalysis = false

    /**
     * Function that gets called by CameraProvider to analyze the current image. The [imageProxy] is output of a camera
     * and is an in buffers, these should be freed as fast as possible.
     */
    override fun analyze(imageProxy: ImageProxy) {
        // used for scaling camera resolution to screen resolution
        // this can be before checking whenever analysis should run
        if (!resolutionInitialized) {
            detectionOverlay.setCameraResolution(imageProxy.width, imageProxy.height)
            resolutionInitialized = true
        }

        if (detectorRunning || pauseAnalysis) {
            imageProxy.close()
            return
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
                    if (detectorResult.performanceMetrics.isNotEmpty()) TimerResult(totalTimeEnd - totalTimeStart) else null
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

    fun pauseAnalysis() {
        pauseAnalysis = true
    }

    fun unpauseAnalysis() {
        pauseAnalysis = false
    }

    fun destroy() {
        realtimeDetector?.destroy()
        qualityDetector?.destroy()
    }
}