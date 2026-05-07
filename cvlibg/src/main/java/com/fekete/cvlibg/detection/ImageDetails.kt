package com.fekete.cvlibg.detection

/**
 * Data class containing information about letterboxing and rescaling of the image during object detection process.
 *
 * Image data might need to be scaled to correct resolution for inference engine and object detection model to work.
 * Scaling images to correct resolution wil work, however in order to preserve the aspect ratio of the image and not
 * distort it, a letterboxing is introduced. This data is later used for reconstructing detected bounding boxes inside a
 * [com.fekete.cvlibg.ui.DetectionOverlay].
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
 *
 * @param scale value used to convert input image, into dimensions required by the object detection model
 * @param padX padding applied in X axis to keep image in correct aspect ratio
 * @param padY padding applied in Y axis to keep image in correct aspect ratio
 * @param inputWidth original width of input image
 * @param inputHeight original height of input image
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
data class ImageDetails(
    val scale: Float = 0f,
    val padX: Int = 0,
    val padY: Int = 0,
    val inputWidth: Int = 0,
    val inputHeight: Int = 0,
)
