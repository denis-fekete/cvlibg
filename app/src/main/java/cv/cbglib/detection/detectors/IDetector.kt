package cv.cbglib.detection.detectors

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy

interface IDetector {
    /**
     * Runs image detection analysis and returns [DetectorResult] containing detections, image information and
     * optionally metrics.
     */
    fun detect(image: Bitmap): DetectorResult
    fun destroy()
}