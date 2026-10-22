package com.example.sereneread.data.repository

import com.example.sereneread.data.local.entity.BookEntity
import com.example.sereneread.data.mapper.toEntity
import com.example.sereneread.data.remote.BookDto
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.model.BookFormat
import com.example.sereneread.domain.util.Constants
import java.time.OffsetDateTime

// Aturan penggabungan data server dengan data lokal. Dipisah dari BookRepositoryImpl
// (fungsi murni, tanpa Room/Retrofit) supaya bisa dites dengan unit test biasa.

// finished_at dari Go berformat ISO-8601 dengan zona waktu, misal "2026-10-22T13:50:05.17599+08:00"
internal fun parseServerTime(value: String): Long? = try {
    OffsetDateTime.parse(value).toInstant().toEpochMilli()
} catch (e: Exception) {
    null
}

// Ubah DTO dari Golang menjadi model domain (dipakai oleh list dan detail)
internal fun BookDto.toBook(): Book = Book(
    id = id,
    title = title,
    author = author,
    // Gabungkan Server URL dengan path gambar dari Golang
    coverUrl = if (!thumbnailUrl.isNullOrEmpty()) "${Constants.SERVER_URL}$thumbnailUrl" else "",
    description = "",
    genre = "General",
    rating = 0f,
    isFavorite = isFavorite ?: false,
    filePath = if (!pdfUrl.isNullOrEmpty()) "${Constants.SERVER_URL}$pdfUrl" else "",
    format = BookFormat.PDF,
    lastReadPosition = lastReadPage ?: 0,
    totalPages = totalPages ?: 0,
    publishedYear = publishedYear,
    finishedAt = finishedAt?.let { parseServerTime(it) }
)

// Data dari server bisa lebih lama dari progres lokal yang belum terkirim.
// Jika progres lokal belum tersinkron, pertahankan halaman lokal agar tidak tertimpa.
internal fun mergeWithLocal(serverBook: Book, local: BookEntity?): BookEntity {
    val entity = serverBook.toEntity().copy(
        // Server mengirim total_pages; jika kosong (0), pakai nilai yang tercatat saat PDF dibuka
        totalPages = serverBook.totalPages.takeIf { it > 0 } ?: local?.totalPages ?: 0,
        // Response list tidak berisi pdf_url, jangan hapus path yang sudah diketahui
        filePath = serverBook.filePath.ifEmpty { local?.filePath.orEmpty() },
        // Pertahankan tanggal tambah lokal supaya urutan buku tidak berubah setiap refresh
        dateAdded = local?.dateAdded ?: serverBook.dateAdded,
        // Server tidak menyimpan kapan terakhir dibaca, jadi pertahankan nilai lokal
        lastReadAt = local?.lastReadAt ?: 0
    )
    return if (local != null && !local.progressSynced) {
        entity.copy(lastReadPosition = local.lastReadPosition, progressSynced = false)
    } else {
        entity
    }
}
