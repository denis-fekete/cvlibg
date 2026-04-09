package cv.cbglib.ui

import android.content.Context
import android.util.Log
import android.view.View
import android.widget.ImageButton
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
            startQualityDetectionBtn: ImageButton,
            exitQualityDetectionBtn: ImageButton,
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

            startQualityDetectionBtn.setOnClickListener {
                cameraController.switchToDetailedAnalysis()
                startQualityDetectionBtn.visibility = View.GONE
                exitQualityDetectionBtn.visibility = View.VISIBLE
            }

            exitQualityDetectionBtn.visibility = View.GONE
            exitQualityDetectionBtn.setOnClickListener {
                cameraController.switchToFasterAnalysis()
                exitQualityDetectionBtn.visibility = View.GONE
                startQualityDetectionBtn.visibility = View.VISIBLE
            }

            return cameraController
        }
    }
}