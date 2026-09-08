package com.example.sereneread.data.local

import androidx.room.TypeConverter
import com.example.sereneread.domain.model.BookFormat
import java.util.Date

class Converters {
    // Mengubah Timestamp (Long) <-> Date
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    // Mengubah String <-> Enum BookFormat
    @TypeConverter
    fun fromBookFormat(value: String): BookFormat {
        return try {
            BookFormat.valueOf(value)
        } catch (e: Exception) {
            BookFormat.PDF // Default jika error
        }
    }

    @TypeConverter
    fun bookFormatToString(format: BookFormat): String {
        return format.name
    }
}