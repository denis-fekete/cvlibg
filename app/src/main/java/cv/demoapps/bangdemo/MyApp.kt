package cv.demoapps.bangdemo


import cv.cbglib.CVILIBGApplication
import cv.cbglib.detection.detectors.DetectorRegistry
import cv.cbglib.detection.detectors.onnx.Yolo26OnnxDetector
import cv.cbglib.detection.detectors.onnx.YoloOnnxDetector
import cv.cbglib.services.JSONAssetService
import cv.cbglib.services.JSONStorageService
import cv.cbglib.services.SettingsService
import cv.demoapps.bangdemo.data.CardDetail
import cv.demoapps.bangdemo.data.Class2Link
import cv.demoapps.bangdemo.data.UserPreferences

class MyApp : CVILIBGApplication() {
    lateinit var cardDetailsService: JSONAssetService<CardDetail, String>
    lateinit var class2linkService: JSONAssetService<Class2Link, Int>
    var errorMessageCardDetail: String? = null
    var errorMessageClass2Link: String? = null

    val settingsService: SettingsService<UserPreferences> by lazy {
        SettingsService(
            this,
            "settings.json",
            UserPreferences.serializer(),
            Charsets.UTF_16BE
        ) { UserPreferences() }
    }

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
            // forcing load
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
            // forcing load
            class2linkService.items
        } catch (exc: Exception) {
            errorMessageClass2Link = "JsonAssetService failed for Class2Link.json: Error message: ${exc.message}"
        }
    }

    override fun registerModels() {
//        DetectorRegistry.register("Yolo 26 ALPHA40", "Y26_ALPHA40.onnx")
//        { path -> Yolo26OnnxDetector(path) }
//
//        DetectorRegistry.register("Yolo 26 ALPHA240", "Y26_ALPHA240.onnx")
//        { path -> Yolo26OnnxDetector(path) }
//
//
//        DetectorRegistry.register("Yolo 26 BETA40", "Y26_BETA40.onnx")
//        { path -> Yolo26OnnxDetector(path) }
//
//        DetectorRegistry.register("Yolo 26 GAMMA40", "Y26_GAMMA40.onnx")
//        { path -> Yolo26OnnxDetector(path) }

        DetectorRegistry.register("Y8_N_ep50_b8_w3_bg_added_CPU", "Y8_N_ep50_b8_w3_bg_added_CPU.onnx")
        { path -> YoloOnnxDetector(path) }

        DetectorRegistry.register(
            "Y8_N_ep50_b8_w3_bg_added_CPU_OPTIMIZED",
            "Y8_N_ep50_b8_w3_bg_added_CPU_OPTIMIZED.onnx"
        )
        { path -> YoloOnnxDetector(path) }

        DetectorRegistry.register(
            "Y8_N_ep50_b8_w3_bg_added_GPU",
            "Y8_N_ep50_b8_w3_bg_added_GPU.onnx"
        )
        { path -> YoloOnnxDetector(path) }

        DetectorRegistry.register(
            "Y8_N_ep50_b8_w3_bg_added_NONSIMPLIFIED",
            "Y8_N_ep50_b8_w3_bg_added_NONSIMPLIFIED.onnx"
        )
        { path -> YoloOnnxDetector(path) }


        DetectorRegistry.register(
            "Y26_sy_ep40_gamma_CPU",
            "Y26_sy_ep40_gamma_CPU.onnx"
        )
        { path -> Yolo26OnnxDetector(path) }


        DetectorRegistry.register(
            "Y26_sy_ep40_gamma_CPU_OPTIMIZED",
            "Y26_sy_ep40_gamma_CPU_OPTIMIZED.onnx"
        )
        { path -> Yolo26OnnxDetector(path) }

        DetectorRegistry.register(
            "Y26_sy_ep40_gamma_GPU",
            "Y26_sy_ep40_gamma_GPU.onnx"
        )
        { path -> Yolo26OnnxDetector(path) }

        DetectorRegistry.register(
            "Y26_sy_ep40_gamma_NONSIMPLIFIED",
            "Y26_sy_ep40_gamma_NONSIMPLIFIED.onnx"
        )
        { path -> Yolo26OnnxDetector(path) }
    }
}