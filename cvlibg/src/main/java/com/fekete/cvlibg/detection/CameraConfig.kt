package com.fekete.cvlibg.detection

import androidx.camera.view.PreviewView
import com.fekete.cvlibg.detection.detectors.Detector
import com.fekete.cvlibg.logging.MetricsOverlay
import com.fekete.cvlibg.ui.DetectionOverlay

/**
 * @param previewView [androidx.camera.view.PreviewView] that is in layout where this [CameraController] is situated,
 * this preview shows unedited stream of images from camera (in another word video from camera).
 * @param detectionOverlay Class used for drawing, it is expected that the class will be subclassed.
 * @param metricsOverlay lass used for displaying the metrics.
 * @param realtimeDetector Faster detector uses in realtime, if not provided a realtime analysis will not be used
 * @param qualityDetector Detailed detector used for running slower models with frozen background, if not provided it
 * will not be used
 * @param bindToLifecycle Whenever, should the [CameraController] be bound to the [lifecycleOwner], if set to true,
 * the clean memory cleanup will be automatically done. Otherwise, it is required call [stop] method.
 */
data class CameraConfig(
    val detectionOverlay: DetectionOverlay,
    val metricsOverlay: MetricsOverlay?,
    val realtimeDetector: Detector?,
    val qualityDetector: Detector?,
    val bindToLifecycle: Boolean = true,
    val previewView: PreviewView,
    val previewViewScale: PreviewView.ScaleType = PreviewView.ScaleType.FILL_CENTER,
)
