package com.fekete.bangdemo

import android.app.Application
import com.fekete.bangdemo.data.CardDetail
import com.fekete.bangdemo.data.Class2Link
import com.fekete.bangdemo.data.UserPreferences
import com.fekete.cvlibg.detection.detectors.DetectorRegistry
import com.fekete.cvlibg.detection.detectors.onnx.Yolo26OnnxDetector
import com.fekete.cvlibg.detection.detectors.onnx.YoloOnnxDetector
import com.fekete.cvlibg.services.JSONAssetService
import com.fekete.cvlibg.services.PermissionService.setupOpenCV
import com.fekete.cvlibg.services.SettingsService

class MyApp : Application() {
    lateinit var cardDetailsService: JSONAssetService<CardDetail, String>
    lateinit var class2linkService: JSONAssetService<Class2Link, Int>
    var errorMessageCardDetail: String? = null
    var errorMessageClass2Link: String? = null

    val settingsService: SettingsService<UserPreferences> by lazy {
        SettingsService(
            this,
            "settings.json",
            UserPreferences.serializer(),
            Charsets.UTF_16BE,
            defaultValue = { UserPreferences() }
        )
    }

    override fun onCreate() {
        super.onCreate()
        setupOpenCV()
        registerModels()

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