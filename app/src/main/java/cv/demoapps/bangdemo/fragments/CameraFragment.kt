package cv.demoapps.bangdemo.fragments

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.navigation.fragment.findNavController
import cv.cbglib.detection.detectors.DetectorRegistry
import cv.cbglib.ui.AbstractCameraFragment
import cv.demoapps.bangdemo.MyApp
import cv.demoapps.bangdemo.R

/**
 * [CameraFragment] is class derived from [AbstractCameraFragment]. Basic functionality can be achieved by simply
 * inheriting from class, giving current layout "ID". On detection click must be activated here!
 */
class CameraFragment : AbstractCameraFragment(R.layout.fragment_camera) {
    private val class2linkService by lazy {
        (requireContext().applicationContext as MyApp).class2linkService
    }

    private val settingsService by lazy {
        (requireContext().applicationContext as MyApp).settingsService
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // attaching onDetectionClicked event
        detectionOverlay.onDetectionClicked = { detection ->
            val bundle = bundleOf(
                "id" to class2linkService.items[detection.classIndex]!!.linkId
            )

            findNavController().navigate(R.id.cardDetailsFragment, bundle)
        }
    }

    override fun setupDetectors() {
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