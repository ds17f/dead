package com.deadly.core.database

import androidx.room.TypeConverter
import com.deadly.core.model.Track
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date

class Converters {
    
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }
    
    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
    
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
    
    @TypeConverter
    fun fromListString(list: List<String>?): String? {
        return list?.joinToString(",")
    }
    
    @TypeConverter
    fun fromTrackListJson(value: String?): List<Track> {
        return try {
            value?.let { Json.decodeFromString<List<Track>>(it) } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    @TypeConverter
    fun trackListToJson(tracks: List<Track>?): String? {
        return try {
            tracks?.let { Json.encodeToString(it) }
        } catch (e: Exception) {
            null
        }
    }
}