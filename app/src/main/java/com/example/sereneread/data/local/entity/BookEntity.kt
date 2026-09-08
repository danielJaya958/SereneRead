package com.example.sereneread.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.sereneread.domain.model.BookFormat
import java.util.Date

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val title: String,
    val author: String,
    val description: String,
    val coverUrl: String?,
    val filePath: String,
    val format: BookFormat,

    // Metadata
    val rating: Float,
    val genre: String,
    val isFavorite: Boolean,
    val dateAdded: Date,

    // Progress
    val lastReadPosition: Int,
    val totalPages: Int
)