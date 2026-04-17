package cv.cbglib.services

import android.app.Application
import android.content.Context
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.nio.charset.Charset

/**
 * Template class used for loading an JSON files from `assets` as a [Map] of [KeyType] keys and data class objects.
 * Use:
 * Create a `@Serializable` data class that will contain data in JSON file, it is expected that the JSON file is
 * a list of these objects. [keySelector] is a key of [DataType] for the map. [charset] a coding of file, make sure all
 * files are saved with correct charset. [path] may be a single JSON file or a directory. For path to directory all
 * files under the directory will be loaded and must be of same [charset] and [DataType].
 *
 * Declaration in class that subclassed [cv.cbglib.CVILIBGApplication]:
 * ```
 * val DATA_CLASS_SERVICE: JsonAssetService<DATA_CLASS_NAME> by lazy {
 *         JsonAssetService(
 *             app = this,
 *             fileName = "JSON_FILE_PATH_IN_ASSETS.json",
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
 * [cv.cbglib.CVILIBGApplication] and was added to `AndroidManifest.xml`, for more
 * detail see [cv.cbglib.CVILIBGApplication]):
 * ```
 *     private val DATA_CLASS_NAME by lazy {
 *         (context.applicationContext as SUBCLASSED_APP).DATA_CLASS_SERVICE
 *     }
 * ```
 */
class JSONAssetService<DataType : Any, KeyType : Any>(
    private val app: Application,
    private val path: String,
    private val serializer: KSerializer<DataType>,
    private val keySelector: (DataType) -> KeyType,
    private val charset: Charset,
) {
    /**
     * Items is a [Map] of with [KeyType] keys and [DataType] data class objects. [DataType] must be `@Serializable`.
     */
    val items: Map<KeyType, DataType> by lazy {
        var result = mutableListOf<DataType>()

        // returns empty list if file is not a directory
        val listOfFiles = readFilesRecursive(path)

        if (listOfFiles.isNotEmpty()) {
            // path is directory, read all its files
            for (file in listOfFiles) {
                result.addAll(loadSingleFile(file))
            }
        } else {
            // path is file, read it
            result.addAll(loadSingleFile(path))
        }

        result.associateBy(keySelector)
    }

    /**
     * Loads a single JSON file and all its objects of [DataType]. File must contain a list of these objects, cannot be
     * just single JSON object
     *
     * @param filename path to the file in assets directory
     * @return [List] of [DataType]
     */
    private fun loadSingleFile(filename: String): List<DataType> {
        val jsonText = app.assets.open(filename)
            .bufferedReader(charset)
            .use { it.readText() }
            .removePrefix("\uFEFF") // removes ByteOrderMark from start of the file

        return Json.decodeFromString(ListSerializer(serializer), jsonText)
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

    /**
     * @return [Boolean] True if [fileName] ends with '.json'.
     */
    private fun isJsonFile(fileName: String): Boolean {
        return fileName.endsWith(".json")
    }

    /**
     * Saves the [items] of the [cv.cbglib.services.JSONAssetService] into the devices storage under the [fileName].
     *
     * @param fileName name the result '.json' file. If string doesn't end with '.json', it is appended.
     */
    fun save(fileName: String) {
        val jsonString = Json.encodeToString(items)

        val jsonFileName = if (isJsonFile(fileName))
            fileName
        else
            "$fileName.json"

        app.applicationContext.openFileOutput(jsonFileName, Context.MODE_PRIVATE)
            .bufferedWriter(charset)
            .use { it.write(jsonString) }
    }
}