package no.utgdev.getstrong.data.local.db

import androidx.room.TypeConverter
import org.json.JSONArray

class StringListConverters {
    @TypeConverter
    fun fromStringList(values: List<String>): String = JSONArray(values).toString()

    @TypeConverter
    fun toStringList(serialized: String): List<String> {
        val jsonArray = JSONArray(serialized)
        return List(jsonArray.length()) { idx -> jsonArray.getString(idx) }
    }
}
