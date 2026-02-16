package cv.cbglib.detection.detectors

import androidx.camera.core.ImageProxy

interface IDetector {
    /**
     * Runs image detection analysis and returns [DetectorResult] containing detections, image information and
     * optionally metrics.
     */
    fun detect(imageProxy: ImageProxy, storeImage: Boolean = false): DetectorResult
    fun destroy()
}