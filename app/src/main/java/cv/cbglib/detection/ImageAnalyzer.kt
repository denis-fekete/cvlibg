package cv.cbglib.detection

import android.R
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import cv.cbglib.detection.detectors.DetectorResult
import cv.cbglib.detection.detectors.IDetector
import cv.cbglib.logging.MetricsOverlay

class ImageAnalyzer(
    private var framesToSkip: Int = 5,
    private val detectionOverlay: DetectionOverlay,
    private val metricsOverlay: MetricsOverlay? = null,
    private val realtimeDetector: IDetector,
    private val precisionDetector: IDetector,
) : ImageAnalysis.Analyzer {
    private var skippedFramesCounter: Int = framesToSkip // activate at first frame
    private var resolutionInitialized = false

    @Volatile
    private var useRealtimeDetector = true
    private var pauseAnalysis = false

    /**
     * Function that gets called by CameraProvider to analyze the current image. The [imageProxy] is output of a camera
     * and is an in buffers, these should be freed as fast as possible.
     */
    override fun analyze(imageProxy: ImageProxy) {
        if (pauseAnalysis || skippedFramesCounter++ < framesToSkip) {
            imageProxy.close()
            return
        }

        skippedFramesCounter = 0

        if (!resolutionInitialized) {
            detectionOverlay.setCameraResolution(imageProxy.width, imageProxy.height)
            resolutionInitialized = true
        }

        val detectorResult: DetectorResult
        if (useRealtimeDetector) {
            detectorResult = realtimeDetector.detect(imageProxy)

        } else {
            detectorResult = precisionDetector.detect(imageProxy, storeImage = true)
            detectionOverlay.setBackgroundBitmap(detectorResult.image!!)
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
        skippedFramesCounter += framesToSkip // force update on analyze() class
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

        skippedFramesCounter += framesToSkip // force update on analyze() class
    }

    /**
     * Sets number of frames that will be skipped until analysis on image is done
     */
    fun setFramesToSkip(newValue: Int) {
        framesToSkip = newValue
    }

    fun destroy() {
        realtimeDetector.destroy()
    }
}