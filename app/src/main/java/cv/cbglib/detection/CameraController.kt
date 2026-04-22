package cv.cbglib.detection

import android.content.Context
import android.util.Log
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
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import cv.cbglib.detection.detectors.Detector
import cv.cbglib.logging.MetricsOverlay
import cv.cbglib.utils.Timer
import cv.cbglib.utils.TimerResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Class making camera control abstracted. Creates new thread on which a [ImageAnalyzer] is run.
 *
 * @param context Should be a context of a [android.app.Fragment] or [android.app.Activity], in case either of these are
 * destroyed, new camera controller along with [ExecutorService] will be created.
 * @param previewView [PreviewView] that is in layout where this [CameraController] is situated,
 * this preview shows unedited stream of images from camera (in another word video from camera).
 * @param detectionOverlay Class used for drawing, it is expected that the class will be subclassed.
 * @param metricsOverlay lass used for displaying the metrics.
 * @param realtimeDetector Faster detector uses in realtime, if not provided a realtime analysis will not be used
 * @param qualityDetector Detailed detector used for running slower models with frozen background, if not provided it
 * will not be used
 * @param lifecycleOwner Owner of lifecycle, used by CameraX to correctly bind and exit on lifecycle change
 * @param bindToLifecycle Whenever, should the [CameraController] be bound to the [lifecycleOwner], if set to true,
 * the clean memory cleanup will be automatically done. Otherwise, it is required call [stop] method.
 *
 */
open class CameraController(
    private val context: Context,
    private val previewView: PreviewView,
    private val detectionOverlay: DetectionOverlay,
    private val metricsOverlay: MetricsOverlay?,
    private val realtimeDetector: Detector?,
    private val qualityDetector: Detector?,
    private val lifecycleOwner: LifecycleOwner,
    private val bindToLifecycle: Boolean = true,
) : DefaultLifecycleObserver {
    private var cameraExecutorInitialized: Boolean = false
    private var isStarted: Boolean = false
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private var imageAnalyzer: ImageAnalyzer? = null
    private lateinit var resolutionSelector: ResolutionSelector

    init {
        // if allowed, bind to lifecycle for automatic clean up
        if (bindToLifecycle) {
            lifecycleOwner.lifecycle.addObserver(this)
        }
    }


    /**
     * Initializes all camera and image analysis related options.
     */
    open suspend fun start() {
        if (isStarted) return

        // build detectors on another thread
        lifecycleOwner.lifecycleScope.launch {
            val realtime = async(Dispatchers.IO) {
                realtimeDetector?.build(context)
            }

            val quality = async(Dispatchers.IO) {
                qualityDetector?.build(context)
            }

            realtime.await()
            quality.await()
            imageAnalyzer?.unpauseAnalysis()

        }

        cameraProvider = ProcessCameraProvider.getInstance(context).await()


        // setup detectors, must be called before the getResolutionSelector() as it uses detectors for choosing the resolution
        resolutionSelector = getResolutionSelector()
        imageAnalyzer = ImageAnalyzer(
            detectionOverlay,
            metricsOverlay,
            realtimeDetector,
            qualityDetector
        )
        imageAnalyzer?.pauseAnalysis()

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
        } finally {
            isStarted = true
        }
    }

    /**
     * Starts realtime/faster detection method
     */
    fun switchToFasterAnalysis() {
        imageAnalyzer?.switchToFasterAnalysis()
    }

    /**
     * Starts precises/slower detection method
     */
    fun switchToDetailedAnalysis() {
        imageAnalyzer?.switchToDetailedAnalysis()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        stop()
    }

    /**
     * Destroys [cameraExecutor] that is running on different thread, to prevent memory leaks, this must be called!
     */
    open fun stop() {
        if (cameraExecutorInitialized)
            cameraExecutor.shutdown()

        cameraProvider.unbindAll()
        imageAnalyzer?.destroy()
        imageAnalyzer = null
    }

    /**
     * Returns the [ResolutionSelector] for camera. Detectors are used for getting minimal size, so that the detector
     * will not have to upscale the image.
     */
    protected open fun getResolutionSelector(): ResolutionSelector {
        val qualityDetectorSize: Size = qualityDetector?.inputDataSize ?: Size(0, 0)
        val realtimeDetectorSize: Size = realtimeDetector?.inputDataSize ?: Size(0, 0)

        val rtDetectorPixelCount = realtimeDetectorSize.width * realtimeDetectorSize.height
        val qDetectorPixelCount = qualityDetectorSize.width * qualityDetectorSize.height

        // choose bigger resolution based on pixel count of detections
        val resultSize = if (qDetectorPixelCount > rtDetectorPixelCount && qualityDetector != null) {
            qualityDetector.inputDataSize
        } else {
            realtimeDetector?.inputDataSize ?: Size(640, 480)
        }

        // try to get the resolution of based on the detector with higher pixel count
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

    /**
     * Sets the logging of metrics to [value].
     */
    fun setMetricsEnabled(value: Boolean) {
        imageAnalyzer?.setMetricsEnabled(value)
    }

    /**
     * Sets the logging of verbose metrics to [value].
     */
    fun setVerboseMetricsEnabled(value: Boolean) {
        imageAnalyzer?.setVerboseMetricsEnabled(value)
    }
}
