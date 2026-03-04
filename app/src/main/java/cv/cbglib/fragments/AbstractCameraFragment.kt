package cv.cbglib.fragments

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import cv.cbglib.detection.DetectionOverlay
import cv.cbglib.detection.CameraController
import cv.cbglib.logging.MetricsOverlay
import cv.demoapps.bangdemo.R
import kotlinx.coroutines.launch

/**
 * Abstract class containing code for fragments that contain camera preview with detections.
 * Initializes [CameraController] and sets up preview of camera and detection mechanism.
 * This fragment must be linked to a layout that contains [PreviewView] named with id
 * [cameraxView] and [DetectionOverlay] with id name [detectionOverlay]. These IDs must match, or [initViews] must be
 * overridden and IDs corrected (for this look implementation of [initViews] in base class).
 *
 * @param layoutRes is and android ID of layout the derived class if bound to (example: `R.layout.fragment_camera`)
 */
abstract class AbstractCameraFragment(layoutRes: Int) : BaseFragment(layoutRes) {
    private lateinit var cameraController: CameraController
    protected lateinit var cameraxView: PreviewView
    protected lateinit var detectionOverlay: DetectionOverlay
    protected var metricsOverlay: MetricsOverlay? = null
    protected lateinit var startQualityDetectionBtn: ImageButton
    protected lateinit var exitQualityDetectionBtnn: ImageButton

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // find views by ID and initialize them
        initViews(view)

        // scale view to use as much screen space, keep ration and crop excess
        cameraxView.scaleType = PreviewView.ScaleType.FILL_CENTER

        cameraController = CameraController(
            requireContext(),
            this as LifecycleOwner,
            cameraxView,
            detectionOverlay,
            metricsOverlay
        )

        lifecycleScope.launch {
            try {
                cameraController.start()
            } catch (e: Exception) {
                Log.e("CV", "Camera initialization failed: ${e.message}")
            }
        }

        startQualityDetectionBtn.setOnClickListener {
            cameraController.qualityDetection()
            startQualityDetectionBtn.visibility = View.GONE
            exitQualityDetectionBtnn.visibility = View.VISIBLE
        }

        exitQualityDetectionBtnn.visibility = View.GONE
        exitQualityDetectionBtnn.setOnClickListener {
            cameraController.realtimeDetection()
            exitQualityDetectionBtnn.visibility = View.GONE
            startQualityDetectionBtn.visibility = View.VISIBLE
        }
    }

    /**
     * Initializes [cameraxView] and [detectionOverlay] which must be initialized for camera preview and detections preview.
     *
     * @param view is a [View] for finding GUI elements ([View] derived classes) by their IDs in layout
     */
    protected open fun initViews(view: View) {
        cameraxView = view.findViewById<PreviewView>(R.id.cameraxView)
        detectionOverlay = view.findViewById<DetectionOverlay>(R.id.detectionOverlay)
        metricsOverlay = view.findViewById<MetricsOverlay>(R.id.metricsOverlay)
        startQualityDetectionBtn = view.findViewById<ImageButton>(R.id.startQualityDetectionBtn)
        exitQualityDetectionBtnn = view.findViewById<ImageButton>(R.id.endQualityDetectionBtn)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraController.stop()
    }
}