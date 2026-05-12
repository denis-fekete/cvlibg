package com.fekete.cvlibg

import android.content.Context
import android.util.Size
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Class initializing camera, connecting its use cases, and manages resource that can be reused after the view was
 * destroyed. Each view recreation, such as screen rotation, requires binding new binding using [start] method.
 * Consecutive call will use already initialized values and only preview and new lifecycle owner will be rebound. This
 * class use [ExecutorService] which must be destroyed using [destroy] method.
 *
 * Camera controller will silently fail on errors, as it catches exceptions, to inform use it is required to define
 * [onError] callback method.
 *
 * @param context Should be a context of a [android.app.Fragment] or [android.app.Activity], in case either of these are
 * destroyed, new camera controller along with [ExecutorService] will be created.
 * @param imageAnalyzer analyses image data from camera, if `null` only camera preview will be displayed.
 * @param manageAnalyzer if set to `true` (default), the camera controller will build and destroy provided [imageAnalyzer].
 *
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
open class CameraController(
    private val context: Context,
    private val imageAnalyzer: ImageAnalyzer?,
    private val manageAnalyzer: Boolean = true,
) {
    private var cameraExecutorInitialized: Boolean = false
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var resolutionSelector: ResolutionSelector
    private lateinit var preview: Preview
    private lateinit var imageAnalysis: ImageAnalysis
    var onError: ((String) -> Unit)? = null

    @Volatile
    private var isInitialized: Boolean = false

    @Volatile
    private var isBeingInitialized: Boolean = false
    private val controllerScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var _status: CameraStatus = CameraStatus.NOT_INITIALIZED

    val status: CameraStatus get() = _status


    /**
     * Binds [CameraController] to provided lifecycle owner and sets a new surface provider.
     *
     * @param lifecycleOwner to which [CameraController] is bound.
     * @param surfaceProvider surface, onto which a camera preview will be bound to.
     */
    open fun start(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
    ) {
        if (manageAnalyzer) {
            controllerScope.launch(Dispatchers.IO) {
                try {
                    imageAnalyzer?.buildDetectors(context)
                } catch (e: Exception) {
                    withContext(Dispatchers.Main.immediate) {
                        onError?.invoke(e.message ?: "Error loading models.")
                    }
                }
            }
        }

        if (!isInitialized) {
            initialize(lifecycleOwner, surfaceProvider)
        } else {
            bind(lifecycleOwner, surfaceProvider)
        }
    }

    /**
     * Bind camera controller to the current [lifecycleOwner]
     *
     * @param lifecycleOwner to which [CameraController] is bound.
     * @param surfaceProvider surface, onto which a camera preview will be bound to.
     */
    protected fun bind(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
    ) {
        preview.surfaceProvider = surfaceProvider

        try {
            if (::imageAnalysis.isInitialized) {
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

            _status = CameraStatus.RUNNING
        } catch (e: Exception) {
            onError?.invoke(e.message ?: "CameraController.start(): cameraProvider.bindToLifecycle error.")
            stop() // on error stop camera
        }
    }

    /**
     * Initializes [CameraController] and calls [bind] after it finishes.
     *
     * @param lifecycleOwner to which [CameraController] is bound.
     * @param surfaceProvider surface, onto which a camera preview will be bound to.
     */
    protected open fun initialize(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider
    ) {
        if (isBeingInitialized || isInitialized) return
        isBeingInitialized = true

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context.applicationContext)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()
            resolutionSelector = getResolutionSelector()

            cameraExecutor = Executors.newSingleThreadExecutor()
            cameraExecutorInitialized = true

            // keep only latest, if image analyzer is not keeping up (calculations take too much time), then keep only the
            // most recent image instead of buffering them
            // provide ImageAnalyzer with RGBA and autorotate
            if (imageAnalyzer != null) {
                imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .setOutputImageRotationEnabled(true)
                    .setResolutionSelector(resolutionSelector)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                imageAnalysis.setAnalyzer(cameraExecutor, imageAnalyzer)
            }

            preview = Preview.Builder()
                .setResolutionSelector(resolutionSelector)
                .build()

            isBeingInitialized = false
            isInitialized = true

            _status = CameraStatus.STOPPED

            bind(lifecycleOwner, surfaceProvider)
        }, ContextCompat.getMainExecutor(context))
    }

    /**
     * Returns the [ResolutionSelector] for camera. Detectors are used for getting minimal size, so that the detector
     * will not have to upscale the image.
     */
    protected open fun getResolutionSelector(): ResolutionSelector {
        val qualityDetectorSize: Size? = imageAnalyzer?.getRealtimeDetectorInputSize()
        val realtimeDetectorSize: Size? = imageAnalyzer?.getDetailedDetectorInputSize()

        val rtDetectorPixelCount =
            if (realtimeDetectorSize != null) (realtimeDetectorSize.width * realtimeDetectorSize.height)
            else 0

        val qDetectorPixelCount =
            if (qualityDetectorSize != null) (qualityDetectorSize.width * qualityDetectorSize.height)
            else 0

        // choose bigger resolution based on pixel count of detections
        val resultSize =
            if (qDetectorPixelCount > rtDetectorPixelCount) {
                qualityDetectorSize
            } else {
                realtimeDetectorSize
            }

        // try to get the resolution of based on the detector with higher pixel count
        // minimal size with ration 16:9, fewer pixels, less accurate but, more performance
        return ResolutionSelector.Builder()
            .setResolutionStrategy(
                ResolutionStrategy(
                    resultSize ?: Size(640, 480),
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
     * Stop the [CameraController] by unbinding camera provider and its use-cases.
     */
    open fun stop() {
        if (::cameraProvider.isInitialized) {
            cameraProvider.unbindAll()
        }

        _status = CameraStatus.STOPPED
    }

    /**
     * Clean up launched executor, coroutine scopes and resets internal state of [CameraController]. This must be called.
     */
    open fun destroy() {
        stop()

        if (cameraExecutorInitialized) {
            cameraExecutor.shutdown()
            cameraExecutorInitialized = false
        }
        controllerScope.cancel()
        isInitialized = false
        isBeingInitialized = false
        _status = CameraStatus.NOT_INITIALIZED

        if (manageAnalyzer)
            imageAnalyzer?.destroy()
    }
}

enum class CameraStatus {
    STOPPED,
    RUNNING,
    NOT_INITIALIZED,
}