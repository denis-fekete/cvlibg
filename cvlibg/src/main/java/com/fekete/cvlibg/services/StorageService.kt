package com.fekete.cvlibg.services

import android.app.Application
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileNotFoundException
import java.nio.charset.Charset

/**
 * Template class used for loading an JSON file from external storage located under:
 * `/storage/emulated/0/Android/data/<package>/files/[filename]`.
 *
 * Loaded JSON file must be `data class` and provide [serializer] from `@Serializable` flag.
 *
 * File must contain a list of [DataType] objects, it cannot a single object!
 *
 * @param app Application reference for loading objects with application context
 * @param filename Name of file to open and save into
 * @param serializer Serializer for data class with `@Serializable` property
 * @param keySelector Key selector of the internal map.
 * @param charset Character set used by the file
 * @param throwOnFail If set to `false` [filename] file does not have to exist, otherwise if the file does not exist,
 * an [FileNotFoundException] will be thrown.
 *
 *@author Denis Fekete <xfeket01@vutbr.cz>, <denis.fekete02@gmail.com>
 */
class StorageService<DataType : Any, KeyType : Any>(
    private val app: Application,
    private val filename: String,
    private val serializer: KSerializer<DataType>,
    private val keySelector: (DataType) -> KeyType,
    private val charset: Charset,
    private val throwOnFail: Boolean = false,
) {
    /**
     * Items is a [Map] of with [KeyType] keys and [DataType] data class objects. [DataType] must be `@Serializable`.
     */
    val data: MutableMap<KeyType, DataType> by lazy {
        val file = File(getRootDirectory(), filename)

        if (!file.exists()) {
            if (throwOnFail) {
                throw FileNotFoundException("The JSONStorageService failed to load '$filename' file in '${file.absolutePath}'")
            } else {
                return@lazy mutableMapOf()
            }
        }

        val jsonText = file.bufferedReader(charset)
            .use { it.readText() }
            .removePrefix("\uFEFF") // removes ByteOrderMark from start of the file

        val list = Json.decodeFromString(ListSerializer(serializer), jsonText)

        return@lazy list.associateBy(keySelector).toMutableMap()
    }

    /**
     * Get the first item of the [data] map.
     */
    val item: DataType? get() = data.values.firstOrNull()

    /**
     * Saves the [data] of the [com.fekete.cvlibg.services.StorageService] into the devices storage under the [filename].
     */
    fun save() {
        if (data.values.isEmpty()) {
            return
        }
        val json = Json { encodeDefaults = true } // encode default values from the data class
        val jsonString = json.encodeToString(ListSerializer(serializer), data.values.toList())

        File(getRootDirectory(), filename)
            .bufferedWriter(charset)
            .use { it.write(jsonString) }
    }

    /**
     * Getter for root directory under the: `/storage/emulated/0/Android/data/CV.ILIBG/files/`
     */
    private fun getRootDirectory(): File {
        return app.applicationContext.getExternalFilesDir(null)
            ?: throw IllegalStateException("External storage directory not available")
    }
}