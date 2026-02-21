package cv.cbglib;

import android.app.Application
import cv.cbglib.detection.detectors.Detector
import cv.cbglib.services.AssetService
import cv.cbglib.services.SettingsService

/**
 * Application that initializes [AssetService], needed for the [cv.cbglib.fragments.AbstractCameraFragment]. For use
 * subclass this class and set it in `AndroidManifest.xml`:
 *
 *  <application
 *             android:name = ".MyApp"
 *
 *  Where the name of derived class from this class is `MyApp` (can be whatever else, just must match the
 *  manifest). Continue adding code in `MyApp` class.
 *
 */
abstract class CustomApplication : Application() {
    val assetService: AssetService by lazy {
        AssetService(this)
    }

    val settingsService: SettingsService by lazy {
        SettingsService(this)
    }


    override fun onCreate() {
        super.onCreate()
        setupModels()
    }

    /**
     * Abstract function for setting up models that will be available for users to choose from. Override this function
     * and add entries to [cv.cbglib.detection.CameraController.modelNameToPathMap] like this:
     * ```
     * CameraController.addModel("Name shown in app", "path to the model in assets/models/model_name.onnx") { path ->
     *     DetectorClass(
     *         path
     *     )
     * }
     * ```
     *
     * for DetectorClass could be used [cv.cbglib.detection.detectors.realtime.YoloOnnx8to11Detector] or
     * [cv.cbglib.detection.detectors.realtime.YoloOnnx26Detector]
     */
    abstract fun setupModels()
}