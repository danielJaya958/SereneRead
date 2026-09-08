package com.example.sereneread.domain.model

import java.util.Date

data class Book(val id: Long = 0,               // ID 0 menandakan data baru (belum masuk DB)
                val title: String,
                val author: String,
                val description: String = "",
                val coverUrl: String? = null,   // Bisa null jika buku tidak punya cover
                val filePath: String,           // Lokasi penyimpanan file buku (PDF/EPUB) di HP
                val format: BookFormat,         // Tipe file (Enum)

    // Metadata & User Interaction
                val rating: Float = 0.0f,       // 0.0 sampai 5.0
                val genre: String = "General",
                val isFavorite: Boolean = false,
                val dateAdded: Date = Date(),   // Tanggal ditambahkan

    // Progress Membaca
                val lastReadPosition: Int = 0,  // Halaman/lokasi terakhir dibaca
                val totalPages: Int = 0         // Total halaman
) {
    // Properti bantuan (Logic sederhana boleh ada di Domain Model)
    val progressPercentage: Int
        get() = if (totalPages > 0) ((lastReadPosition.toFloat() / totalPages) * 100).toInt() else 0
}

// Enum untuk membedakan tipe file
enum class BookFormat {
    PDF,
    EPUB
}