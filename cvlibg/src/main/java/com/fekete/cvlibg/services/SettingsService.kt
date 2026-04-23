package com.fekete.cvlibg.services

import android.app.Application
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.Charset

/**
 * Template class for storing settings, preferences, and configurations that need to be private and not accessible.
 *
 * @param app Reference to application for load and saving files
 * @param filename Name file that will be loaded and saved, if the file does not exist a [defaultValue] lambda function
 * will be used to create an instance of [DataType].
 * @param serializer Serializer for the [DataType] `data class`. This request the class to use `@Serializable` property
 * in `data class`.
 * @param charset Character set used for reading and storing of file.
 * @param defaultValue Lambda function to create an instance of [DataType]. A simple constructor `{ DataTypeClass() }`
 * can be used.
 */
class SettingsService<DataType : Any>(
    private val app: Application,
    private val filename: String,
    private val serializer: KSerializer<DataType>,
    private val charset: Charset = Charsets.UTF_8,
    private val defaultValue: () -> DataType
) {
    /**
     * Data class of [DataType]. [DataType] must be `@Serializable`.
     */
    val data: DataType by lazy {
        val file = File(app.applicationContext.filesDir, filename)

        if (!file.exists()) {
            return@lazy defaultValue()
        }

        val jsonText = file.bufferedReader(charset)
            .use { it.readText() }
            .removePrefix("\uFEFF") // removes ByteOrderMark from start of the file

        val jsonObject = Json.decodeFromString(serializer, jsonText)
        return@lazy jsonObject
    }

    /**
     * Saves the [data] of into private storage for application under the [filename] filename.
     *
     * @param filename name the result '.json' file. If string doesn't end with '.json', it is appended.
     */
    fun save() {
        val file = File(app.applicationContext.filesDir, filename)
        val jsonText = Json.encodeToString(serializer, data)
        file.bufferedWriter(charset).use { it.write(jsonText) }
    }
}