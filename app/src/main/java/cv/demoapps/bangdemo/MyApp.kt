package cv.demoapps.bangdemo


import cv.cbglib.CustomApplication
import cv.cbglib.detection.detectors.DetectorRegistry
import cv.cbglib.detection.detectors.onnx.Yolo26OnnxDetector
import cv.cbglib.detection.detectors.onnx.YoloOnnxDetector
import cv.cbglib.detection.detectors.opencv.YoloOpenCVDetector
import cv.cbglib.services.JsonAssetService
import cv.demoapps.bangdemo.data.CardDetail
import cv.demoapps.bangdemo.data.Class2Link

class MyApp : CustomApplication() {
    lateinit var cardDetailsService: JsonAssetService<CardDetail, String>
    lateinit var class2linkService: JsonAssetService<Class2Link, Int>
    var errorMessageCardDetail: String? = null
    var errorMessageClass2Link: String? = null

    override fun onCreate() {
        super.onCreate()
        try {
            cardDetailsService = JsonAssetService(
                this,
                path = "details",
                serializer = CardDetail.serializer(),
                keySelector = { it.id },
                Charsets.UTF_16BE
            )
            // forcing load, these will always be used, better to load them now
            cardDetailsService.items
        } catch (exc: Exception) {
            errorMessageCardDetail = "JsonAssetService failed for assets/details/: Error message: ${exc.message}"
        }

        try {
            class2linkService = JsonAssetService(
                this,
                path = "Class2Link.json",
                serializer = Class2Link.serializer(),
                keySelector = { it.classId },
                Charsets.UTF_8
            )
            // forcing load, these will always be used, better to load them now
            class2linkService.items
        } catch (exc: Exception) {
            errorMessageClass2Link = "JsonAssetService failed for Class2Link.json: Error message: ${exc.message}"
        }
    }

    override fun setupModels() {
        DetectorRegistry.link("ONNX Yolo8 RT", "YV8_N_ep40.onnx")
        { path -> YoloOnnxDetector(path) }
        DetectorRegistry.link("ONNX Yolo8 P", "YV8_M_ep50.onnx")
        { path -> YoloOnnxDetector(path) }
        DetectorRegistry.link("ONNX Yolo11 RT(exp)", "Y11_N_ep10.onnx")
        { path -> YoloOnnxDetector(path) }
        DetectorRegistry.link("ONNX Yolo 26 RT", "Y26_N_ep40.onnx")
        { path -> Yolo26OnnxDetector(path) }

        DetectorRegistry.link("OpenCV Yolo8 RT", "YV8_N_ep40.onnx")
        { path -> YoloOpenCVDetector(path) }
        DetectorRegistry.link("OpenCV Yolo8 P", "YV8_M_ep50.onnx")
        { path -> YoloOpenCVDetector(path) }
    }
}