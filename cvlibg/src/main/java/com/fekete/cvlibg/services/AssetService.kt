package com.fekete.cvlibg.services

import android.app.Application
import android.util.Log
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.nio.charset.Charset

/**
 * Template class used for loading an JSON files from `assets` as a [Map] of [KeyType] keys and data class objects.
 * Usage:
 * Create a `@Serializable` data class that will contain data in JSON file, it is expected that the JSON file is
 * a list of these objects. [keySelector] is a key of [DataType] for the map. [charset] a coding of file, make sure all
 * files are saved with correct charset. [path] may be a single JSON file or a directory. For path to directory all
 * files under the directory will be loaded and must be of same [charset] and [DataType].
 *
 * Declaration in class that subclassed [Application]:
 * ```
 * val DATA_CLASS_SERVICE: JsonAssetService<DATA_CLASS_NAME> by lazy {
 *         JsonAssetService(
 *             app = this,
 *             path = "JSON_FILE_PATH_IN_ASSETS.json",
 *             serializer = DATA_CLASS_NAME.serializer(),
 *             keySelector = { it.DATA_CLASS_NAME.PROPERTY_OF_TYPE_INT },
 *             charset = Charsets.UTF_16BE
 *         )
 *     }
 * ```
 * In declaration use lazy if assets are not used all the time, otherwise loading them at start of application might be
 * better design.
 *
 * Use in any other activity, fragments, class, etc... (SUBCLASSED_APP is a class that subclassed
 * [Application]:
 * ```
 *     private val DATA_CLASS_NAME by lazy {
 *         (context.applicationContext as SUBCLASSED_APP).DATA_CLASS_SERVICE
 *     }
 * ```
 *
 * @author Denis Fekete <xfeket01@vutbr.cz>, <denis.fekete02@gmail.com>
 */
class AssetService<DataType : Any, KeyType : Any>(
    private val app: Application,
    private val path: String,
    private val serializer: KSerializer<DataType>,
    private val keySelector: (DataType) -> KeyType,
    private val charset: Charset,
    private val onRepetitiveKeyError: (() -> Unit)? = null,
) {
    /**
     * Items is a [Map] of with [KeyType] keys and [DataType] data class objects. [DataType] must be `@Serializable`.
     */
    val data: Map<KeyType, DataType> by lazy {
        val result = mutableMapOf<KeyType, DataType>()

        // returns empty list if file is not a directory
        val listOfFiles = readFilesRecursive(path)

        if (listOfFiles.isNotEmpty()) {
            // path is directory, read all its files
            for (file in listOfFiles) {
                val elements = loadSingleFile(file)
                if (result.keys.containsAll(elements.keys)) {
                    Log.e("AssetService", "Duplicate keys found in $path. Application will continue.")
                    onRepetitiveKeyError?.invoke()
                }
                result.putAll(elements)
            }
        } else {
            // path is file, read it
            result.putAll(loadSingleFile(path))
        }

        return@lazy result
    }

    /**
     * Loads a single JSON file and all its objects of [DataType]. File must contain a list of these objects, cannot be
     * just single JSON object
     *
     * @param filename path to the file in assets directory
     * @return [Map] of [DataType] with [KeyType]
     */
    private fun loadSingleFile(filename: String): Map<KeyType, DataType> {
        val jsonText = app.assets.open(filename)
            .bufferedReader(charset)
            .use { it.readText() }
            .removePrefix("\uFEFF") // removes ByteOrderMark from start of the file

        val list = Json.decodeFromString(ListSerializer(serializer), jsonText)
        return list.associateBy(keySelector)
    }

    /**
     * Reads path with recursion and returns list of all files with its absolute paths (under assets).
     *
     * @return [List] of all absolute paths of files, meaning parent directory will be included in name of file.
     * If path is not directory but a file an empty list is returned.
     */
    private fun readFilesRecursive(path: String): List<String> {
        val children = app.assets.list(path) ?: return emptyList()
        val result = mutableListOf<String>()

        for (file in children) {
            val fullPath = if (path.isEmpty()) file else "$path/$file"

            try {
                // try to open file, failing means it is a directory
                app.assets.open(fullPath).close()
                result.add(fullPath)
            } catch (e: Throwable) {
                // path == directory, open it recursively
                result.addAll(readFilesRecursive(fullPath))
            }
        }

        return result
    }
}