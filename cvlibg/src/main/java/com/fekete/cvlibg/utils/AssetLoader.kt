package com.fekete.cvlibg.utils

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.nio.charset.Charset

/**
 *
 * Loads object detection models as [ByteArray], images as [Bitmap], and text data from the application's assets.
 *
 * Most functionality can be achieved with static methods. However, it can be constructed to hold reference to
 * [Application], which avoids requiring application reference for each call.
 *
 * @author Denis Fekete <xfeket01@vutbr.cz>, <denis.fekete02@gmail.com>
 */
class AssetLoader(private val app: Application) {
    /**
     * Gets [ByteArray] of provided model.
     *
     * @param modelName Name of model with extension
     * @param modelPath Root path of directory in which a model is stored under [assets]. Must end with `/`!
     */
    fun loadModel(modelName: String, modelPath: String = "models/"): ByteArray {
        return loadModel(modelName, modelPath, app)
    }

    /**
     * Returns a [android.graphics.Bitmap] of image asset, image path does not have to be full, a recursive search will be applied until
     * a file with [filename] will be found (extension must be matching).
     */
    fun loadImage(filename: String, rootDir: String = ""): Bitmap? {
        return loadImage(filename, rootDir, app)
    }

    /**
     * Returns a [Bitmap] of image asset, image path does not have to be full, a recursive search will be applied until
     * a file with [filename] will be found (extension must be matching).
     */
    fun loadText(
        filename: String,
        rootDir: String = "",
        charset: Charset = Charsets.UTF_8,
    ): String? {
        return loadText(filename, rootDir, charset, app)
    }

    /**
     * Recursive search for [filename] under the [rootDir] in application's assets.
     *
     * @param filename string that will be searched for
     * @param rootDir directory under which a recursive search will start
     * @return full path to the file or `null` on not finding the file
     */
    fun recursiveFileSearch(filename: String, rootDir: String = ""): String? {
        return recursiveFileSearch(filename, rootDir, app)
    }

    /**
     * Recursive search for all files under the [path] in application's assets.
     *
     * @param path that will be searched under the assets directory
     * @return [List] of all absolute paths of files, meaning parent directory will be included in name of file.
     * If path is not directory but a file an empty list is returned.
     */
    fun recursiveFilesSearch(path: String): List<String> {
        return recursiveFilesSearch(path, app = app)
    }

    companion object {
        /**
         * Gets [ByteArray] of provided model.
         *
         * @param modelName Name of model with extension
         * @param modelPath Root path of directory in which a model is stored under [assets]. Must end with `/`!
         */
        fun loadModel(modelName: String, modelPath: String = "models/", app: Application): ByteArray {
            val fullPath = "$modelPath$modelName"
            return app.assets.open(fullPath).readBytes()
        }


        /**
         * Returns a [Bitmap] of image asset, image path does not have to be full, a recursive search will be applied until
         * a file with [filename] will be found (extension must be matching).
         */
        fun loadImage(filename: String, rootDir: String = "", app: Application): Bitmap? {
            val foundFilePath = recursiveFileSearch(filename, rootDir, app) ?: return null

            val stream = app.assets.open(foundFilePath)

            val bitmap = BitmapFactory.decodeStream(stream)
            stream.close()
            return bitmap
        }

        /**
         * Returns a [Bitmap] of image asset, image path does not have to be full, a recursive search will be applied until
         * a file with [filename] will be found (extension must be matching).
         */
        fun loadText(
            filename: String,
            rootDir: String = "",
            charset: Charset = Charsets.UTF_8,
            app: Application,
        ): String? {
            val foundFilePath = recursiveFileSearch(filename, rootDir, app) ?: return null
            val stream = app.assets.open(foundFilePath)
            val text = stream.bufferedReader(charset).use { it.readText() }
            stream.close()
            return text
        }

        /**
         * Recursive search for [filename] under the [rootDir] in application's assets.
         *
         * @param filename string that will be searched for
         * @param app Application reference for reading from the assets
         * @param rootDir directory under which a recursive search will start
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