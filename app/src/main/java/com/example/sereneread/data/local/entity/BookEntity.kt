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
    val totalPages: Int,

    // false = progres lokal belum berhasil dikirim ke server, jangan ditimpa data server
    val progressSynced: Boolean = true,

    // Waktu (epoch millis) progres terakhir disimpan, untuk kartu "Continue reading" di Home
    val lastReadAt: Long = 0,

    // v4: tahun terbit (0 = belum diketahui) dan waktu selesai dibaca (null = belum)
    val publishedYear: Int = 0,
    val finishedAt: Long? = null
)