package com.fekete.bangdemo

import android.app.Application
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.data.Class2Link
import com.fekete.bangdemo.data.UserPreferences
import com.fekete.cvlibg.detection.detectors.DetectorRegistry
import com.fekete.cvlibg.detection.detectors.onnx.Yolo26OnnxDetector
import com.fekete.cvlibg.detection.detectors.onnx.YoloOnnxDetector
import com.fekete.cvlibg.services.AssetService
import com.fekete.cvlibg.utils.CommonUtils
import com.fekete.cvlibg.services.ConfigService

/**
 * Class derived from the Android's [Application] class. Initializes the [cardDetailsService], [class2linkService], and
 * [settingsService]. Furthermore, registers models and detectors into [DetectorRegistry].
 *
 * The public [errorMessageCardDetail] and [errorMessageClass2Link] are used by the [MainActivity] to display errors
 * that occurred during initialization of services.
 *
 * @author Denis Fekete, (xfeket01@vutbr.cz), (denis.fekete02@gmail.com)
 */
class MyApp : Application() {
    lateinit var cardDetailsService: AssetService<CardDetail, String>
    lateinit var class2linkService: AssetService<Class2Link, Int>
    var errorMessageCardDetail: String? = null
    var errorMessageClass2Link: String? = null

    val settingsService: ConfigService<UserPreferences> by lazy {
        ConfigService(
            this,
            "settings.json",
            UserPreferences.serializer(),
            Charsets.UTF_16BE,
            defaultValue = { UserPreferences() }
        )
    }

    override fun onCreate() {
        super.onCreate()
        CommonUtils.setupOpenCV()
        registerModels()

        try {
            cardDetailsService = AssetService(
                app = this,
                path = "details",
                serializer = CardDetail.serializer(),
                keySelector = { it.id },
                Charsets.UTF_16BE
            )
            // forcing load
            cardDetailsService.data
        } catch (exc: Exception) {
            errorMessageCardDetail = "JsonAssetService failed for assets/details/: Error message: ${exc.message}"
        }

        try {
            class2linkService = AssetService(
                this,
                path = "Class2Link.json",
                serializer = Class2Link.serializer(),
                keySelector = { it.classId },
                Charsets.UTF_8
            )
            // forcing load
            class2linkService.data
        } catch (exc: Exception) {
            errorMessageClass2Link = "JsonAssetService failed for Class2Link.json: Error message: ${exc.message}"
        }


    }

    fun registerModels() {
        DetectorRegistry.register("Yolo 8 Precision", "Y8_M_GAMMA_CPU.onnx")
        { path -> YoloOnnxDetector(path) }

        DetectorRegistry.register("Yolo 8 Realtime", "Y8_GAMMA_CPU.onnx")
        { path -> YoloOnnxDetector(path) }

        DetectorRegistry.register("Yolo 11 Realtime", "Y11_GAMMA_CPU.onnx")
        { path -> YoloOnnxDetector(path) }

        DetectorRegistry.register("Yolo 26 Realtime", "Y26_GAMMA_CPU.onnx")
        { path -> Yolo26OnnxDetector(path) }

        DetectorRegistry.register("Yolo 26 Realtime NPU", "Y26_GAMMA_NPU.onnx")
        { path -> Yolo26OnnxDetector(path, useNNAPI = true) }
    }
}