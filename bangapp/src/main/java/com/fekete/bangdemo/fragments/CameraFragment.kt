package com.fekete.bangdemo.fragments

import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AlertDialog
import androidx.camera.view.PreviewView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.fekete.bangdemo.MyApp
import com.fekete.cvlibg.CameraController
import kotlinx.coroutines.launch
import com.fekete.bangdemo.databinding.FragmentCameraBinding
import com.fekete.bangdemo.utils.navigateAction
import com.fekete.cvlibg.ui.BaseCameraViewModel


/**
 * Class setting up [CameraController] with [CameraFragment], [context], [LifecycleOwner].
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class CameraFragment : BaseFragment<FragmentCameraBinding>(
    FragmentCameraBinding::inflate
) {
    private val class2linkService by lazy {
        (requireContext().applicationContext as MyApp).class2linkService
    }

    private val settingsService by lazy {
        (requireContext().applicationContext as MyApp).settingsService
    }

    private val viewModel: BaseCameraViewModel by viewModels()

    @Volatile
    private var cameraStopRequested: Boolean = false


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel.initialize(
            settingsService.data.realtimeModel,
            settingsService.data.precisionModel,
        )

        // scale view to use as much screen space, keep ration and crop excess
        binding.previewView.scaleType = PreviewView.ScaleType.FILL_CENTER

        viewModel.cameraController.onError = { message ->
            AlertDialog.Builder(requireContext())
                .setTitle("Error building models:")
                .setMessage(message)
                .setPositiveButton("OK", null)
                .show()
        }


        viewModel.cameraController.start(viewLifecycleOwner, binding.previewView.surfaceProvider)

        collectAnalyzerOutputs()
        setupButtonBehavior()
    }

    private fun collectAnalyzerOutputs() {
        // this code will rerun on viewLifecycleOwner STARTED == rerun when UI changes (rotation)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // collect is active, blocking code, launch it in separated coroutine
                launch {
                    viewModel.imageAnalyzer.detectionResult.collect { detectionResult ->
                        binding.detectionOverlay.update(detectionResult)

                        // camera stop was request, await first detailed result (contains input image)
                        if (detectionResult.inputImage != null) {
                            confirmDetailedMode()
                        }
                    }
                }

                if (settingsService.data.showMetrics) { // show metrics only if enabled
                    launch {
                        viewModel.imageAnalyzer.metrics.collect { metrics ->
                            binding.metricsOverlay.update(metrics)
                        }
                    }
                }
            }
        } // viewLifecycleOwner.lifecycleScope.launch
    }


    private fun setupButtonBehavior() {
        // attaching onDetectionClicked event
        binding.detectionOverlay.onDetectionClicked = { detection ->
            val action = CameraFragmentDirections
                .actionCameraFragmentToCardDetailsFragment(id = class2linkService.data[detection.classIndex]!!.linkId)
            findNavController().navigateAction(action)
        }

        if (viewModel.imageAnalyzer.usesRealtimeDetector) {
            binding.switchToFastDetectionButton.visibility = GONE
            binding.switchToDetailedDetectionButton.visibility = VISIBLE
        } else {
            binding.switchToFastDetectionButton.visibility = VISIBLE
            binding.switchToDetailedDetectionButton.visibility = GONE
        }


        // switch to fast/realtime detection, unpause camera if it was paused
        binding.switchToFastDetectionButton.setOnClickListener {
            realtimeDetectionMode()
        }

        // switch to detailed analysis and set camera stop flag, to save camera from running in background
        // camera will be stopped on next result with `inputImage` property (only detailed provide image)
        binding.switchToDetailedDetectionButton.setOnClickListener {
            requestDetailedMode()
        }
    }

    private fun realtimeDetectionMode() {
        // if camera was stopped restart it and disable flag
        if (cameraStopRequested) {
            viewModel.cameraController.start(viewLifecycleOwner, binding.previewView.surfaceProvider)
            cameraStopRequested = false
        }
        viewModel.imageAnalyzer.switchToFasterAnalysis()
        sharedViewModel.overlaysVisible(inventory = false, other = true)

        binding.switchToFastDetectionButton.visibility = View.GONE
        binding.switchToDetailedDetectionButton.visibility = View.VISIBLE
    }

    private fun requestDetailedMode() {
        cameraStopRequested = true // request camera stop on next received

        viewModel.imageAnalyzer.switchToDetailedAnalysis()

        binding.switchToDetailedDetectionButton.visibility = View.GONE
        binding.switchToFastDetectionButton.visibility = View.VISIBLE
    }

    private fun confirmDetailedMode() {
        // camera stop was request, await first detailed result (contains input image)
        if (cameraStopRequested) {
            viewModel.cameraController.stop()
            sharedViewModel.overlaysVisible(inventory = true, other = true)
        }
    }
}