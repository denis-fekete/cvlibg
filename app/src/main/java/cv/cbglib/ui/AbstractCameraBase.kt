package cv.cbglib.ui

import android.content.Context
import android.util.Log
import android.view.View
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import cv.cbglib.detection.CameraController
import cv.cbglib.detection.DetectionOverlay
import cv.cbglib.detection.detectors.Detector
import cv.cbglib.logging.MetricsOverlay
import kotlinx.coroutines.launch

/**
 * Abstract class containing shared code for [AbstractCameraActivity] and [AbstractCameraFragment].
 */
class AbstractCameraBase {
    companion object {
        fun cameraControllerSetup(
            context: Context,
            lifecycleOwner: LifecycleOwner,
            cameraxView: PreviewView,
            detectionOverlay: DetectionOverlay,
            metricsOverlay: MetricsOverlay?,
            switchToDetailedDetectionButton: View,
            switchToFastDetectionButton: View,
            realtimeDetector: Detector?,
            qualityDetector: Detector?
        ): CameraController {
            // scale view to use as much screen space, keep ration and crop excess
            cameraxView.scaleType = PreviewView.ScaleType.FILL_CENTER

            val cameraController = CameraController(
                context,
                lifecycleOwner,
                cameraxView,
                detectionOverlay,
                metricsOverlay,
                realtimeDetector,
                qualityDetector
            )

            lifecycleOwner.lifecycleScope.launch {
                try {
                    cameraController.start()
                } catch (e: Exception) {
                    Log.e("CV", "Camera initialization failed: ${e.message}")
                }
            }

            switchToDetailedDetectionButton.setOnClickListener {
                cameraController.switchToDetailedAnalysis()
                switchToDetailedDetectionButton.visibility = View.GONE
                switchToFastDetectionButton.visibility = View.VISIBLE
            }

            switchToFastDetectionButton.visibility = View.GONE
            switchToFastDetectionButton.setOnClickListener {
                cameraController.switchToFasterAnalysis()
                switchToFastDetectionButton.visibility = View.GONE
                switchToDetailedDetectionButton.visibility = View.VISIBLE
            }

            return cameraController
        }
    }
}