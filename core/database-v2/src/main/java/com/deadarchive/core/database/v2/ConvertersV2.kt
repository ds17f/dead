package com.deadarchive.core.database.v2

import androidx.room.TypeConverter

class ConvertersV2 {
    
    @TypeConverter
    fun fromStringList(value: String?): List<String> {
        return value?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
    }
    
    @TypeConverter
    fun fromListString(list: List<String>?): String? {
        return list?.joinToString(",")
    }
}