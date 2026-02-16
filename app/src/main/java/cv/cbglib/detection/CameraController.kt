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
import cv.cbglib.detection.detectors.realtime.YoloONNXDetector
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
class CameraController(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val previewView: PreviewView,
    private val detectionOverlay: DetectionOverlay,
    private val metricsOverlay: MetricsOverlay
) {
    private var cameraControllerInitialized: Boolean = false
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private var imageAnalyzer: ImageAnalyzer? = null
    private lateinit var resolutionSelector: ResolutionSelector

    private val assetService by lazy {
        (context.applicationContext as MyApp).assetService
    }

    private val settingsService by lazy {
        (context.applicationContext as MyApp).settingsService
    }

    /**
     * Initializes all camera and image analysis related options.
     * Source [source](https://developer.android.com/media/camera/camerax/analyze#operating_modes)
     */
    suspend fun start() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraControllerInitialized = true

        cameraProvider = ProcessCameraProvider.getInstance(context).await()
        resolutionSelector = getResolutionSelector()

        val imageAnalysis = setupImageAnalysis()

        val preview = Preview.Builder()
            .setResolutionSelector(resolutionSelector)
            .build()
        preview.surfaceProvider = previewView.surfaceProvider

        try {
            if (imageAnalysis != null) {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )
            } else {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                )
            }
        } catch (exc: Exception) {
            AlertDialog.Builder(context)
                .setTitle("Error")
                .setMessage("Exception during camera initialization: ${exc.message}")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    /**
     * Sets up image analysis for realtime (low latency) detection. Uses values stored in
     * [cv.cbglib.services.SettingsService].
     */
    private fun setupImageAnalysis(): ImageAnalysis? {
        val realtimeModel = getModelBytes(
            settingsService.realtimeModel,
            "Real-time detection"
        ) ?: return null

        val realtimeDetector = YoloONNXDetector(
            realtimeModel,
            settingsService.showPerformance,
            settingsService.verbosePerformance
        )

        val preciseModel = getModelBytes(
            settingsService.precisionModel,
            "Precise detection"
        ) ?: return null

        val preciseDetector = YoloONNXDetector(
            preciseModel,
            settingsService.showPerformance,
            settingsService.verbosePerformance
        )

        imageAnalyzer = ImageAnalyzer(
            settingsService.framesToSkip,
            detectionOverlay,
            metricsOverlay,
            realtimeDetector,
            preciseDetector
        )

        if (imageAnalyzer == null)
            return null

        // keep only latest, if image analyzer is not keeping up (calculations take too much time), then keep only the
        // most recent image instead of buffering them
        val imageAnalysis = ImageAnalysis.Builder()
            .setOutputImageRotationEnabled(true)
            .setResolutionSelector(resolutionSelector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor, imageAnalyzer!!)

        return imageAnalysis
    }

    /**
     * Starts realtime/faster detection method
     */
    fun realtimeDetection() {
        imageAnalyzer?.resumeAnalysis()
    }

    /**
     * Starts precises/slower detection method
     */
    fun preciseDetection() {
        imageAnalyzer?.preciseDetectAndPause()
    }

    /**
     * Returns [ByteArray] of model from [cv.cbglib.services.SettingsService]. Models must be stored inside
     * `assets/models/`.
     */
    private fun getModelBytes(modelName: String, modelUseCase: String): ByteArray? {
        try {
            return assetService.getModel(modelName, "models/")
        } catch (exc: IOException) {
            AlertDialog.Builder(context)
                .setTitle("Error loading model for $modelUseCase")
                .setMessage("Model'$modelName' could not be loaded. Please choose a different model in Settings.")
                .setPositiveButton("OK", null)
                .show()

            return null
        }
    }

    private fun getResolutionSelector(): ResolutionSelector {
        // minimal size with ration 16:9, fewer pixels, less accurate but, more performance
        return ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(640, 480),
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


    /**
     * Destroys [cameraExecutor] that is running on different thread, to prevent memory leaks, this must be called!
     */
    fun stop() {
        if (cameraControllerInitialized)
            cameraExecutor.shutdown()

        imageAnalyzer?.destroy()
    }
}
