package cv.cbglib.services

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import java.io.File
import java.io.InputStream
import java.nio.charset.Charset

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
    val availableModels: List<String> by lazy {
        try {
            val files = app.assets.list(pathToModels) ?: return@lazy emptyList<String>()

            // return all files if extensions are not specified
            if (allowedExtensions.isEmpty()) {
                return@lazy files.toList()
            }

            var availableModels = mutableListOf<String>()
            for (file in files) {
                for (extension in allowedExtensions) {
                    if (file.endsWith(extension, ignoreCase = true)) {
                        availableModels.add(file)
                    }
                }
            }
            return@lazy availableModels
        } catch (e: Exception) {
            e.message?.let { Log.e("AssetService", it) }
            return@lazy emptyList()
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
        val foundFilePath = recursiveFileSearch(filename, rootDir) ?: return null

        val stream = app.assets.open(foundFilePath)

        val bitmap = BitmapFactory.decodeStream(stream)
        stream.close()
        return bitmap
    }

    /**
     * Returns a [Bitmap] of image asset, image path does not have to be full, a recursive search will be applied until
     * a file with [filename] will be found (extension must be matching).
     */
    fun getTextFile(filename: String, rootDir: String = "", charset: Charset = Charsets.UTF_8): String? {
        val foundFilePath = recursiveFileSearch(filename, rootDir) ?: return null
        val stream = app.assets.open(foundFilePath)
        val text = stream.bufferedReader(charset).use { it.readText() }
        stream.close()
        return text
    }

    /**
     * Recursive search for [filename] under the [rootDir] in assets folder.
     *
     * @param filename string that will be searched for
     * @param rootDir directory under which a recursive search will start
     * @return full path to the file or `null` on not finding the file
     */
    fun recursiveFileSearch(filename: String, rootDir: String = ""): String? {
        return AssetService.recursiveFileSearch(filename, rootDir, app)
    }

    /**
     * Recursive search for all files under the [path] in application's assets.
     *
     * @param path that will be searched under the assets directory
     * @return [List] of all absolute paths of files, meaning parent directory will be included in name of file.
     * If path is not directory but a file an empty list is returned.
     */
    fun recursiveFilesSearch(path: String): List<String> {
        return AssetService.recursiveFilesSearch(path, app)
    }

    companion object {
        /**
         * Recursive search for [filename] under the [rootDir] in application's assets.
         *
         * @param filename string that will be searched for
         * @param rootDir directory under which a recursive search will start
         * @param app Application reference for reading from the assets
         * @return full path to the file or `null` on not finding the file
         */
        fun recursiveFileSearch(filename: String, rootDir: String = "", app: Application): String? {
            val files = app.assets.list(rootDir) ?: return null

            for (file in files) {
                val fullPath = if (rootDir.isEmpty()) file else "$rootDir/$file"

                if (file == filename) {
                    return fullPath
                }

                val subFiles = app.assets.list(fullPath)
                if (!subFiles.isNullOrEmpty()) {
                    val result = recursiveFileSearch(filename, fullPath, app)
                    if (result != null) {
                        return result
                    }
                }
            }

            return null
        }

        /**
         * Recursive search for all files under the [path] in application's assets.
         *
         * @param path that will be searched under the assets directory
         * @param app Application reference for reading from the assets
         * @return [List] of all absolute paths of files, meaning parent directory will be included in name of file.
         * If path is not directory but a file an empty list is returned.
         */
        fun recursiveFilesSearch(path: String, app: Application): List<String> {
            val result = mutableListOf<String>()

            app.assets.list(path)?.forEach { file ->
                val fullPath = if (path.isEmpty()) file else "$path/$file"

                try {
                    // try to open file, failing means it is a directory
                    app.assets.open(fullPath).close()
                    result.add(fullPath)
                } catch (e: Throwable) {
                    // path == directory, open it recursively
                    result.addAll(recursiveFilesSearch(fullPath, app))
                }
            }

            return result
        }
    }

}