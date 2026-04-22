package cv.cbglib.services

import android.app.Application
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.Charset

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
     * Saves the [items] of the [cv.cbglib.services.JSONAssetService] into the devices storage under the [fileName].
     *
     * @param fileName name the result '.json' file. If string doesn't end with '.json', it is appended.
     */
    fun save() {
        val file = File(app.applicationContext.filesDir, filename)
        val jsonText = Json.encodeToString(serializer, data)
        file.bufferedWriter(charset).use { it.write(jsonText) }
    }
}