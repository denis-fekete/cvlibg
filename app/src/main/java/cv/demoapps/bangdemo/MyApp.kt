package cv.demoapps.bangdemo


import cv.cbglib.CVILIBGApplication
import cv.cbglib.detection.detectors.DetectorRegistry
import cv.cbglib.detection.detectors.onnx.Yolo26OnnxDetector
import cv.cbglib.detection.detectors.onnx.YoloOnnxDetector
import cv.cbglib.detection.detectors.opencv.YoloOpenCVDetector
import cv.cbglib.services.JSONAssetService
import cv.demoapps.bangdemo.data.CardDetail
import cv.demoapps.bangdemo.data.Class2Link

class MyApp : CVILIBGApplication() {
    lateinit var cardDetailsService: JSONAssetService<CardDetail, String>
    lateinit var class2linkService: JSONAssetService<Class2Link, Int>
    var errorMessageCardDetail: String? = null
    var errorMessageClass2Link: String? = null

    override fun onCreate() {
        super.onCreate()
        try {
            cardDetailsService = JSONAssetService(
                app = this,
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
            class2linkService = JSONAssetService(
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

    override fun registerModels() {
        DetectorRegistry.register("ONNX Yolo8 RT", "YV8_N_ep40.onnx")
        { path -> YoloOnnxDetector(path) }

        DetectorRegistry.register("ONNX Yolo8 RT no NMS", "YV8_N_ep40.onnx")
        { path -> YoloOnnxDetector(path, applyNMS = false) }

        DetectorRegistry.register("ONNX Yolo8 P", "YV8_M_ep50.onnx")
        { path -> YoloOnnxDetector(path) }
        DetectorRegistry.register("ONNX Yolo11 RT(exp)", "Y11_N_ep10.onnx")
        { path -> YoloOnnxDetector(path) }
        DetectorRegistry.register("ONNX Yolo 26 RT", "Y26_N_ep40.onnx")
        { path -> Yolo26OnnxDetector(path) }

        DetectorRegistry.register("OpenCV Yolo8 RT", "YV8_N_ep40.onnx")
        { path -> YoloOpenCVDetector(path) }
        DetectorRegistry.register("OpenCV Yolo8 P", "YV8_M_ep50.onnx")
        { path -> YoloOpenCVDetector(path) }

        DetectorRegistry.register("ONNX Yolo 26 RT NNAPI", "YV26_N_ep40_OPTIMIZED.onnx")
        { path -> Yolo26OnnxDetector(path, useNNAPI = true) }

        DetectorRegistry.register("ONNX Yolo 8 RT Newest", "YV8_N_ep50_OPTIMIZED.onnx")
        { path -> YoloOnnxDetector(path) }
    }
}