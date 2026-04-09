package cv.cbglib.detection

/**
 * Data class containing information about letterboxing and rescaling of the image during object detection process.
 *
 * Image data might need to be scaled to correct resolution for inference engine and object detection model to work.
 * Scaling images to correct resolution wil work, however in order to preserve the aspect ratio of the image and not
 * distort it, a letterboxing is introduced. This data is later used for reconstructing detected bounding boxes inside a
 * [DetectionOverlay].
 *
 * Padding is centered and padded value is added to both start and end of the axis.
 *
 * Padding on the X axis:
 * __________________________
 * | PadX   | Image  | PadX |
 * __________________________
 *
 * Padding on the Y axis:
 * _________
 * | PadY  |
 * | Image |
 * | PadY  |
 * _________
 */
data class ImageDetails(
    // used for scaling camera image into a model image size, used for reverse scaling to properly display detections
    val scale: Float,
    // padding applied to in X axis
    val padX: Int,
    // padding applied to in Y axis, meaning camera image height<camera and Y axis was filled with default value
    val padY: Int
)
