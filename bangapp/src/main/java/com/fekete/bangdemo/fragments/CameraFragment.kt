package com.fekete.bangdemo.fragments

import android.os.Bundle
import android.view.View
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
        bindOnClickBehavior()
    }

    private fun collectAnalyzerOutputs() {
        // this code will rerun on viewLifecycleOwner STARTED, this code will rerun when UI changes (rotation),
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (settingsService.data.showMetrics) { // show metrics only if enabled
                    launch {
                        viewModel.imageAnalyzer.metrics.collect { metrics ->
                            binding.metricsOverlay.update(metrics)
                        }
                    }
                }

                launch {
                    viewModel.imageAnalyzer.detectionResult.collect { detectionResult ->
                        binding.detectionOverlay.update(detectionResult)
                    }
                }
            }
        } // viewLifecycleOwner.lifecycleScope.launch
    }

    private fun bindOnClickBehavior() {
        // attaching onDetectionClicked event
        binding.detectionOverlay.onDetectionClicked = { detection ->
            val action = CameraFragmentDirections
                .actionCameraFragmentToCardDetailsFragment(id = class2linkService.data[detection.classIndex]!!.linkId)
            findNavController().navigate(action)
        }

        binding.switchToFastDetectionButton.visibility = View.GONE

        binding.switchToFastDetectionButton.setOnClickListener {
            viewModel.imageAnalyzer.switchToFasterAnalysis()
            binding.switchToFastDetectionButton.visibility = View.GONE
            binding.switchToDetailedDetectionButton.visibility = View.VISIBLE
        }

        binding.switchToDetailedDetectionButton.setOnClickListener {
            viewModel.imageAnalyzer.switchToDetailedAnalysis()
            binding.switchToDetailedDetectionButton.visibility = View.GONE
            binding.switchToFastDetectionButton.visibility = View.VISIBLE
        }
    }
}