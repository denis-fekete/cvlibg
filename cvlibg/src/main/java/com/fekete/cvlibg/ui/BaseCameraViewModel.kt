package com.fekete.cvlibg.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.fekete.cvlibg.CameraController
import com.fekete.cvlibg.ImageAnalyzer
import com.fekete.cvlibg.detection.Detector
import com.fekete.cvlibg.utils.DetectorRegistry

/**
 * Custom [ViewModel] for fragments/activities used for persisting [imageAnalyzer] and its [Detector] classes loaded.
 * This way detectors won't have to be recreated on views being destroyed, for example rotation of screen.
 *
 * Usage:
 * 1. Define view model `private val viewModel: BaseCameraViewModel by viewModels()`.
 * 2. Initialize detectors using [initialize] method.
 * 3. (optional) Build detectors using [ImageAnalyzer.buildDetectors], ideally on another thread using IO dispatchers.
 * 4. Call [CameraController.start] method. If 3rd step was skipped, [cameraController] will build detectors internally.
 * This build method can be called multiple times, as it is guarded with internal flags.
 * 5. (optional) Collect outputs from the [imageAnalyzer]. It is recommended to use viewLifecycleOwner from example
 * below to control its lifecycle, otherwise errors might occur.
 *
 * Example code for starting and collecting
 * ```
 * viewLifecycleOwner.lifecycleScope.launch {
 * 	viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
 * 		try {
 * 			cameraController.start()
 * 		} catch (e: Exception) {
 * 			// handle camera controller failing to initialize
 * 		}
 *
 * 		launch {
 * 			viewModel.imageAnalyzer.detectionResult.collect { detectionResult ->
 * 				binding.detectionOverlay.update(detectionResult)
 * 			}
 * 		}
 * 	}
 * }
 * ```
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
open class BaseCameraViewModel(private val app: Application) : AndroidViewModel(app) {
    private var detailedDetector: Detector? = null
    private var realtimeDetector: Detector? = null
    private var initializeCalled = false

    private var _imageAnalyzer: ImageAnalyzer? = null
    private var _cameraController: CameraController? = null

    val cameraController: CameraController
        get() {
            if (_cameraController == null) {
                _cameraController = CameraController(app.applicationContext, imageAnalyzer, manageAnalyzer = true)
            }
            return _cameraController!!
        }

    val imageAnalyzer: ImageAnalyzer
        get() {
            if (_imageAnalyzer == null) {
                _imageAnalyzer = ImageAnalyzer(
                    realtimeDetector,
                    detailedDetector
                )
            }
            return _imageAnalyzer!!
        }

    /**
     * Initializes [Detector] classes for the [imageAnalyzer]. This method must be called before accessing
     * [imageAnalyzer].
     *
     * @param realtimeDetector used by the [imageAnalyzer]
     * @param detailedDetector used by the [imageAnalyzer]
     */
    fun initialize(
        realtimeDetector: Detector,
        detailedDetector: Detector,
    ) {
        if (initializeCalled) {
            return
        }

        initializeCalled = true
        this.realtimeDetector = realtimeDetector
        this.detailedDetector = detailedDetector
    }

    /**
     * Initializes [Detector] classes for the [imageAnalyzer]. This method must be called before accessing
     * [imageAnalyzer].
     *
     * @param realtimeRegistryKey string key used for loading [realtimeDetector] from the [DetectorRegistry].
     * @param detailedRegistryKey string key used for loading [detailedDetector] from the [DetectorRegistry].
     *
     * @throws IllegalArgumentException if string keys are not registered
     */
    fun initialize(
        realtimeRegistryKey: String,
        detailedRegistryKey: String,
    ) {
        if (initializeCalled) {
            return
        }

        initializeCalled = true
        realtimeDetector = DetectorRegistry.createDetector(realtimeRegistryKey)
        detailedDetector = DetectorRegistry.createDetector(detailedRegistryKey)
    }

    /**
     * Called on view model being destroyed
     */
    override fun onCleared() {
        _cameraController?.destroy()
        //_imageAnalyzer?.destroy() // not needed because controllers `manageAnalyzer` is set to true

        super.onCleared()
    }
}