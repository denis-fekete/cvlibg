package cv.cbglib.ui

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import cv.cbglib.detection.CameraController
import cv.cbglib.detection.DetectionOverlay
import cv.cbglib.detection.detectors.Detector
import cv.cbglib.logging.MetricsOverlay
import cv.demoapps.bangdemo.R

/**
 * Abstract class containing code for activities that contain camera preview with detections.
 * Initializes [CameraController] and sets up preview of camera and detection mechanism.
 * This activity must be linked to a layout that contains [PreviewView] named with id
 * [cameraxView] and [DetectionOverlay] with id name [detectionOverlay]. These IDs must match, or [initViews] must be
 * overridden and IDs corrected (for this look implementation of [initViews] in base class).
 *
 * @param layoutRes is and android ID of layout the derived class if bound to (example: `R.layout.fragment_camera`)
 */
abstract class AbstractCameraActivity(private val layoutRes: Int) : AppCompatActivity() {
    private lateinit var cameraController: CameraController
    protected lateinit var cameraxView: PreviewView
    protected lateinit var detectionOverlay: DetectionOverlay
    protected var metricsOverlay: MetricsOverlay? = null
    protected lateinit var startQualityDetectionBtn: ImageButton
    protected lateinit var exitQualityDetectionBtn: ImageButton
    protected var realtimeDetector: Detector? = null
    protected var qualityDetector: Detector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(layoutRes)

        // find views by ID and initialize them
        initViews()

        setupDetectors()

        cameraController = AbstractCameraBase.cameraControllerSetup(
            this as Context,
            this as LifecycleOwner,
            cameraxView,
            detectionOverlay,
            metricsOverlay,
            startQualityDetectionBtn,
            exitQualityDetectionBtn,
            realtimeDetector,
            qualityDetector
        )
    }

    /**
     * Initializes [cameraxView] and [detectionOverlay] which must be initialized for camera preview and detections preview.
     */
    protected open fun initViews() {
        cameraxView = findViewById<PreviewView>(R.id.cameraxView)
        detectionOverlay = findViewById<DetectionOverlay>(R.id.detectionOverlay)
        metricsOverlay = findViewById<MetricsOverlay>(R.id.metricsOverlay)
        startQualityDetectionBtn = findViewById<ImageButton>(R.id.startQualityDetectionBtn)
        exitQualityDetectionBtn = findViewById<ImageButton>(R.id.endQualityDetectionBtn)
    }

    /**
     * Abstract function for setting up [Detector] classes. Derived class must implement this function and initialize
     * the [realtimeDetector], [qualityDetector], otherwise detections will not be performed [CameraController].
     */
    protected abstract fun setupDetectors()

    override fun onDestroy() {
        super.onDestroy()
        cameraController.stop()
    }
}