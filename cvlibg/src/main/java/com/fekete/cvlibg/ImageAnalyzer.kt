package com.fekete.cvlibg

import android.content.Context
import android.util.Log
import android.util.Size
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.fekete.cvlibg.detection.DetectorResult
import com.fekete.cvlibg.detection.Detector
import com.fekete.cvlibg.metrics.AnalyzerMetrics
import com.fekete.cvlibg.ui.DetectionOverlay
import com.fekete.cvlibg.utils.Timer
import com.fekete.cvlibg.utils.TimerResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.FileNotFoundException

/**
 * Class implementing the [ImageAnalysis.Analyzer], responsible for providing [realtimeDetector] and [detailedDetector]
 * with image data from the device's camera.
 *
 * Image analyzer has two modes, switching between the [realtimeDetector] and [detailedDetector]. When switched to
 * quality mode an input bitmap will be printed to the [DetectionOverlay] and detections will be printed on top of it,
 * effectively freezing camera.
 *
 * This class provides "collectable" data which user can "subscribe" to. Provided data are:
 *  - [detectionResult]: latest [DetectorResult] from current detector.
 *  - [inputImage]: input image of the last image analysis, when [detailedDetector] was used.
 *  - [metrics]: last [AnalyzerMetrics] values recorded in analyzer
 *
 * @param realtimeDetector reference to [Detector] object for realtime, continuous image analysis
 * @param detailedDetector reference to [Detector] object, used for slower analysis with frozen background image
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class ImageAnalyzer(
    private val realtimeDetector: Detector?,
    private val detailedDetector: Detector?,
) : ImageAnalysis.Analyzer {
    @Volatile
    private var analysisRunning = false

    @Volatile
    private var useRealtimeDetector = true

    @Volatile
    private var pauseAnalysis = false

    @Volatile
    private var detectorsAreBuilt = false

    private val _detectionResult = MutableStateFlow(DetectorResult())
    val detectionResult = _detectionResult.asStateFlow()

    private val _metrics = MutableStateFlow<AnalyzerMetrics?>(null)
    val metrics = _metrics.asStateFlow()

    val usesRealtimeDetector: Boolean get() = useRealtimeDetector
    val usesDetailedDetector: Boolean get() = !useRealtimeDetector

    /**
     * Function that gets called by CameraProvider to analyze the current image. The [imageProxy] is output of a camera
     * and is an in buffers, these should be freed as fast as possible.
     */
    override fun analyze(imageProxy: ImageProxy) {
        // if new image was produced by the camera while detector are running, or analysis is paused, skip the frame
        if (analysisRunning || pauseAnalysis) {
            imageProxy.close()
            return
        }
        analysisRunning = true

        val totalTimeStart = Timer.getTime()

        val bitmap = imageProxy.toBitmap() // don't force detectors to use and manage ImageProxy, convert it here
        imageProxy.close()

        val detectorResult: DetectorResult?
        val tempUseRealtimeDetector = useRealtimeDetector
        if (realtimeDetector != null && tempUseRealtimeDetector) {
            detectorResult = realtimeDetector.run(bitmap)

        } else if (detailedDetector != null) {
            val tmp = detailedDetector.run(bitmap)
            detectorResult = tmp.copyFrom(inputImage = bitmap)
            pauseAnalysis = true
        } else {
            detectorResult = null
        }

        val totalTimeEnd = Timer.getTime()
        _metrics.tryEmit(
            AnalyzerMetrics(
                TimerResult(totalTimeEnd - totalTimeStart),
                detectorResult?.timeMetrics ?: emptyMap(),
                detectorResult?.metrics ?: emptyList(),
            )
        )

        if (detectorResult != null) {
            _detectionResult.tryEmit(detectorResult)
        }
        analysisRunning = false
    }

    /**
     * Builds detectors using provided [context].
     *
     * @throws FileNotFoundException if model paths provided to [realtimeDetector] or [detailedDetector] do not exist.
     */
    fun buildDetectors(context: Context) {
        if (!detectorsAreBuilt) {
            detectorsAreBuilt = true
            pauseAnalysis()
            realtimeDetector?.build(context)
            detailedDetector?.build(context)
            unpauseAnalysis()
        }
    }

    /**
     * Sets Analyzer internal state to use precise detector and freeze next image analysis. Finalized image analysis
     * will be emitted using [detectionResult]. To unfreeze and continue with realtime detection call [switchToFasterAnalysis].
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
    }

    /**
     * Sets the logging of metrics to [value].
     */
    fun setMetricsEnabled(value: Boolean) {
        realtimeDetector?.setMetricsEnabled(value)
        detailedDetector?.setMetricsEnabled(value)
    }

    /**
     * Sets the logging of verbose metrics to [value].
     */
    fun setVerboseMetricsEnabled(value: Boolean) {
        realtimeDetector?.setVerboseMetricsEnabled(value)
        detailedDetector?.setVerboseMetricsEnabled(value)
    }

    fun getRealtimeDetectorInputSize(): Size? {
        return realtimeDetector?.inputDataSize
    }

    fun getDetailedDetectorInputSize(): Size? {
        return detailedDetector?.inputDataSize
    }

    fun pauseAnalysis() {
        pauseAnalysis = true
    }

    fun unpauseAnalysis() {
        pauseAnalysis = false
    }

    fun destroy() {
        detectorsAreBuilt = false
        realtimeDetector?.destroy()
        detailedDetector?.destroy()
    }
}