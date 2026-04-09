package cv.cbglib.services

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log

/**
 * Service to load images and ONNX models from assets folder. Must be initialized in class that subclasses
 * [Application]. In this case automatically done by subclassing [cv.cbglib.CVILIBGApplication], for use in
 * activities, fragments, etc... use:
 *
 * ```
 *     private val assetService =
 *         (context.applicationContext as SUBCLASSED_APP).assetService
 * ```
 *
 * Where SUBCLASSED_APP is a subclass of an [Application], or more precise [cv.cbglib.CVILIBGApplication]. For more
 * detail see also [cv.cbglib.CVILIBGApplication]
 */
class AssetService(
    private val app: Application,
    private val pathToModels: String = "models",
    private val allowedExtensions: List<String> = listOf(".onnx")
) {
    val availableModels: Array<String> by lazy {
        try {
            val files = app.assets.list(pathToModels) ?: return@lazy emptyArray<String>()

            if (allowedExtensions.isEmpty()) {
                return@lazy files
            }

            var availableModels: ArrayList<String> = arrayListOf()
            for (file in files) {
                for (extension in allowedExtensions) {
                    if (file.endsWith(extension, ignoreCase = true)) {
                        availableModels.add(file)
                    }
                }
            }
            return@lazy availableModels.toTypedArray()
        } catch (e: Exception) {
            e.message?.let { Log.e("CBGLIB", it) }
            emptyArray<String>()
        }
    }

    /**
     * Gets [ByteArray] of provided model.
     *
     * @param modelName Name of model with extension
     * @param modelPath Root path of directory in which a model is stored under [assets]. Must end with `/`!
     */
    fun getModel(modelName: String, modelPath: String = "models/"): ByteArray {
        val fullPath = "$modelPath$modelName"
        return app.assets.open(fullPath).readBytes()
    }


    /**
     * Returns a [Bitmap] of image asset, image path does not have to be full, a recursive search will be applied until
     * a file with [filename] will be found (extension must be matching).
     */
    fun getImageBitmap(filename: String, rootDir: String = ""): Bitmap? {
        val files = app.assets.list(rootDir) ?: return null

        for (file in files) {
            val fullPath = if (rootDir.isEmpty()) file else "$rootDir/$file"

            if (file == filename) {
                val stream = app.assets.open(fullPath)
                val bitmap = BitmapFactory.decodeStream(stream)
                stream.close()
                return bitmap
            }

            val subFiles = app.assets.list(fullPath)
            if (!subFiles.isNullOrEmpty()) {
                val result = getImageBitmap(filename, fullPath)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }
}