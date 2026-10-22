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
                val totalPages: Int = 0,        // Total halaman
                val lastReadAt: Long = 0,       // Kapan terakhir dibaca (epoch millis), 0 = belum pernah
                val publishedYear: Int = 0,     // 0 = belum diketahui
                val finishedAt: Long? = null    // Kapan selesai dibaca (epoch millis), null = belum selesai
) {
    // Properti bantuan (Logic sederhana boleh ada di Domain Model)
    // Backend mengirim last_read_page = 0 untuk buku yang belum pernah dibuka
    val isStarted: Boolean
        get() = lastReadPosition > 0

    // Server mengisi finished_at saat halaman terakhir tercapai; cek halaman juga untuk progres lokal
    val isFinished: Boolean
        get() = finishedAt != null || (totalPages > 0 && lastReadPosition >= totalPages)

    val isInProgress: Boolean
        get() = isStarted && !isFinished

    val progressPercentage: Int
        get() = if (totalPages > 0) ((lastReadPosition.toFloat() / totalPages) * 100).toInt() else 0
}

// Enum untuk membedakan tipe file
enum class BookFormat {
    PDF,
    EPUB
}