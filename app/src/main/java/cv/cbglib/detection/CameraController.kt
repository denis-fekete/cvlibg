package cv.cbglib.detection

import android.content.Context
import android.util.Size
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.concurrent.futures.await
import androidx.lifecycle.LifecycleOwner
import cv.cbglib.detection.detectors.Detector
import cv.cbglib.detection.detectors.DetectorRegistry
import cv.cbglib.logging.MetricsOverlay
import cv.demoapps.bangdemo.MyApp
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Class making camera control abstracted. Creates new thread on which a [ImageAnalyzer] is run.
 *
 * @param context Should be a context of a [android.app.Fragment] or [android.app.Activity], in case either of these are
 * destroyed, new camera controller along with [ExecutorService] will be created.
 * @param lifecycleOwner Owner of lifecycle, used by CameraX to correctly bind.
 * @param previewView [PreviewView] that is in layout where this [CameraController] is situated,
 * this preview shows unedited stream of images from camera (in another word video from camera).
 * @param detectionOverlay Class used for drawing, it is expected that the class will be subclassed.
 *
 * Function [stop] must be called, otherwise a detached thread might cause memory errors.
 */
open class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val detectionOverlay: DetectionOverlay,
    private val metricsOverlay: MetricsOverlay?,
    public val useRealtimeDetector: Boolean = true,
    public val useQualityDetector: Boolean = true
) {
    private var cameraExecutorInitialized: Boolean = false
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private var imageAnalyzer: ImageAnalyzer? = null
    private lateinit var resolutionSelector: ResolutionSelector
    private var realtimeDetector: Detector? = null
    private var qualityDetector: Detector? = null

    private val settingsService by lazy {
        (context.applicationContext as MyApp).settingsService
    }

    /**
     * Initializes all camera and image analysis related options.
     */
    open suspend fun start() {
        cameraProvider = ProcessCameraProvider.getInstance(context).await()

        // setup detectors, must be called before the getResolutionSelector() as it uses detectors for choosing the resolution
        setupDetectors()

        resolutionSelector = getResolutionSelector()

        imageAnalyzer = ImageAnalyzer(
            detectionOverlay,
            metricsOverlay,
            realtimeDetector,
            qualityDetector
        )

        if (imageAnalyzer == null) return

        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraExecutorInitialized = true

        // keep only latest, if image analyzer is not keeping up (calculations take too much time), then keep only the
        // most recent image instead of buffering them
        val imageAnalysis = ImageAnalysis.Builder()
            .setOutputImageRotationEnabled(true)
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor, imageAnalyzer!!)

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
        preview.surfaceProvider = previewView.surfaceProvider

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )
        } catch (exc: Exception) {
            AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage("Exception during camera initialization: ${exc.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    /**
     * Starts realtime/faster detection method
     */
    fun realtimeDetection() {
        imageAnalyzer?.resumeRealtimeAnalysis()
    }

    /**
     * Starts precises/slower detection method
     */
    fun qualityDetection() {
        imageAnalyzer?.performSingleQualityAnalysis()
    }

    /**
     * Destroys [cameraExecutor] that is running on different thread, to prevent memory leaks, this must be called!
     */
    open fun stop() {
        if (cameraExecutorInitialized)
            cameraExecutor.shutdown()

        imageAnalyzer?.destroy()
    }

    /**
     * Sets up image analysis for realtime (low latency) detection. Uses values stored in
     * [cv.cbglib.services.SettingsService].
     */
    private fun setupDetectors() {
        if (useRealtimeDetector) {
            try {
                realtimeDetector = DetectorRegistry.createDetector(settingsService.realtimeModel)
            } catch (exc: IOException) {
                AlertDialog.Builder(context)
                    .setTitle("Error loading model for real time detector")
                    .setMessage("Model'${settingsService.realtimeModel}' could not be loaded. Please choose a different model in Settings.")
                    .setPositiveButton("OK", null)
                    .show()

                return
            }

            realtimeDetector?.build(context, settingsService.showMetrics, settingsService.verboseMetrics)
        }

        if (useQualityDetector) {
            try {
                qualityDetector = DetectorRegistry.createDetector(settingsService.precisionModel)
            } catch (exc: IOException) {
                AlertDialog.Builder(context)
                    .setTitle("Error loading model for real precision detector")
                    .setMessage("Model'${settingsService.precisionModel}' could not be loaded. Please choose a different model in Settings.")
                    .setPositiveButton("OK", null)
                    .show()

                return
            }

            qualityDetector?.build(context, settingsService.showMetrics, settingsService.verboseMetrics)
        }
    }

    protected open fun getResolutionSelector(): ResolutionSelector {
        val qualityDetectorSize: Size = qualityDetector?.inputDataSize ?: Size(0, 0)
        val realtimeDetectorSize: Size = realtimeDetector?.inputDataSize ?: Size(0, 0)

        val rtDetectorPixelCount = realtimeDetectorSize.width * realtimeDetectorSize.height
        val qDetectorPixelCount = qualityDetectorSize.width * qualityDetectorSize.height

        val resultSize =
            if (qDetectorPixelCount > rtDetectorPixelCount && qualityDetector != null) {
                qualityDetector!!.inputDataSize
            } else if (realtimeDetector != null) {
                realtimeDetector!!.inputDataSize
            } else {
                Size(640, 480)
            }

        // minimal size with ration 16:9, fewer pixels, less accurate but, more performance
        return ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    resultSize,
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                )
            )
            .setAspectRatioStrategy(
                AspectRatioStrategy(
                    AspectRatio.RATIO_16_9,
                    AspectRatioStrategy.FALLBACK_RULE_AUTO
                )
            )
            .build()
    }
}
