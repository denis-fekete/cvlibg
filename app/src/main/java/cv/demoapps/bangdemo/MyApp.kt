package cv.demoapps.bangdemo


import cv.cbglib.CustomApplication
import cv.cbglib.detection.CameraController
import cv.cbglib.detection.detectors.realtime.YoloOnnx26Detector
import cv.cbglib.detection.detectors.realtime.YoloOnnx8to11Detector
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
        CameraController.addModel("Yolo N V8 early", "YV8_N_ep40_b8_w1_nosynth.onnx") { path ->
            YoloOnnx8to11Detector(
                path
            )
        }
        CameraController.addModel("Yolo N V8 EP40", "YV8_N_ep40_b12_w2.onnx") { path -> YoloOnnx8to11Detector(path) }
        CameraController.addModel("Yolo M V8 EP50", "YV8_M_ep50_b8_w4.onnx") { path -> YoloOnnx8to11Detector(path) }
        CameraController.addModel("Yolo N V11 EP10", "Y11_N_ep10_b8_w4.onnx") { path -> YoloOnnx8to11Detector(path) }
        CameraController.addModel("Yolo N V26 EP10", "Y26_N_ep10_b8_w0.onnx") { path -> YoloOnnx26Detector(path) }
    }
}