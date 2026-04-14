package cv.cbglib.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.camera.view.PreviewView
import androidx.fragment.app.Fragment
import androidx.lifecycle.LifecycleOwner
import cv.cbglib.detection.DetectionOverlay
import cv.cbglib.detection.CameraController
import cv.cbglib.logging.MetricsOverlay
import cv.demoapps.bangdemo.R
import cv.cbglib.detection.detectors.Detector

/**
 * Abstract class containing code for fragments that contain camera preview with detections.
 * Initializes [CameraController] and sets up preview of camera and detection mechanism.
 * This fragment must be linked to a layout that contains [PreviewView] named with id
 * [cameraxView] and [DetectionOverlay] with id name [detectionOverlay]. These IDs must match, or [initViews] must be
 * overridden and IDs corrected (for this look implementation of [initViews] in base class).
 *
 * @param layoutRes is and android ID of layout the derived class if bound to (example: `R.layout.fragment_camera`)
 */
abstract class AbstractCameraFragment(private val layoutRes: Int) : Fragment() {
    protected lateinit var cameraController: CameraController
    protected lateinit var cameraxView: PreviewView
    protected lateinit var detectionOverlay: DetectionOverlay
    protected var metricsOverlay: MetricsOverlay? = null
    protected lateinit var switchToDetailedDetectionButton: View
    protected lateinit var switchToFastDetectionButton: View
    protected var realtimeDetector: Detector? = null
    protected var qualityDetector: Detector? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(layoutRes, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // find views by ID and initialize them
        initViews(view)

        setupDetectors()

        cameraController = AbstractCameraBase.cameraControllerSetup(
            requireContext(),
            this as LifecycleOwner,
            cameraxView,
            detectionOverlay,
            metricsOverlay,
            switchToDetailedDetectionButton,
            switchToFastDetectionButton,
            realtimeDetector,
            qualityDetector
        )
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
        switchToDetailedDetectionButton = view.findViewById<View>(R.id.startQualityDetectionBtn)
        switchToFastDetectionButton = view.findViewById<View>(R.id.endQualityDetectionBtn)
    }

    /**
     * Abstract function for setting up [Detector] classes. Derived class must implement this function and initialize
     * the [realtimeDetector], [qualityDetector], otherwise detections will not be performed [CameraController].
     */
    protected abstract fun setupDetectors()

    override fun onDestroyView() {
        super.onDestroyView()
        cameraController.stop()
    }
}