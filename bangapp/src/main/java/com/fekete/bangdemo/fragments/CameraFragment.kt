package com.fekete.bangdemo.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.fekete.bangdemo.MyApp
import com.fekete.cvlibg.detection.CameraConfig
import com.fekete.cvlibg.detection.CameraController
import com.fekete.cvlibg.detection.detectors.Detector
import com.fekete.cvlibg.detection.detectors.DetectorRegistry
import com.fekete.bangdemo.R
import kotlinx.coroutines.launch
import com.fekete.bangdemo.databinding.FragmentCameraBinding


/**
 * Class setting up [CameraController] with [CameraFragment], [context], [LifecycleOwner].
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

    private lateinit var cameraController: CameraController

    var realtimeDetector: Detector? = null
    var qualityDetector: Detector? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupDetectors()

        cameraController = CameraController(
            requireContext(),
            this as LifecycleOwner,
            CameraConfig(
                binding.detectionOverlay,
                binding.metricsOverlay,
                realtimeDetector,
                qualityDetector,
                true,
                binding.previewView,
            )
        )

        lifecycleScope.launch {
            try {
                cameraController.start()
            } catch (e: Exception) {
                Log.e("CV", "Camera initialization failed: ${e.message}")
            }
        }

        // attaching onDetectionClicked event
        binding.detectionOverlay.onDetectionClicked = { detection ->
            val bundle = bundleOf(
                "id" to class2linkService.items[detection.classIndex]!!.linkId
            )

            findNavController().navigate(R.id.cardDetailsFragment, bundle)
        }

        binding.switchToFastDetectionButton.visibility = View.GONE

        binding.switchToFastDetectionButton.setOnClickListener {
            cameraController.switchToFasterAnalysis()
            binding.switchToFastDetectionButton.visibility = View.GONE
            binding.switchToDetailedDetectionButton.visibility = View.VISIBLE
        }

        binding.switchToDetailedDetectionButton.setOnClickListener {
            cameraController.switchToDetailedAnalysis()
            binding.switchToDetailedDetectionButton.visibility = View.GONE
            binding.switchToFastDetectionButton.visibility = View.VISIBLE
        }
    }

    fun setupDetectors() {
        try {
            realtimeDetector = DetectorRegistry.createDetector(settingsService.data.realtimeModel)
            realtimeDetector?.setMetricsEnabled(settingsService.data.showMetrics)
            realtimeDetector?.setVerboseMetricsEnabled(settingsService.data.verboseMetrics)
        } catch (exc: Exception) {
            AlertDialog.Builder(requireContext())
                .setTitle("Error loading model for real time detector")
                .setMessage("Model'${settingsService.data.realtimeModel}' could not be loaded. Please choose a different model in Settings.")
                .setPositiveButton("OK", null)
                .show()

            realtimeDetector = null
        }

        try {
            qualityDetector = DetectorRegistry.createDetector(settingsService.data.precisionModel)
            qualityDetector?.setMetricsEnabled(settingsService.data.showMetrics)
            qualityDetector?.setVerboseMetricsEnabled(settingsService.data.verboseMetrics)
        } catch (exc: Exception) {
            AlertDialog.Builder(requireContext())
                .setTitle("Error loading model for real precision detector")
                .setMessage("Model'${settingsService.data.precisionModel}' could not be loaded. Please choose a different model in Settings.")
                .setPositiveButton("OK", null)
                .show()

            qualityDetector = null
        }
    }
}