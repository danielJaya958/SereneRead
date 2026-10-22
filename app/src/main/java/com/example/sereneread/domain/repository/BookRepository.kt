package com.example.sereneread.domain.repository

import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.util.Resource
import kotlinx.coroutines.flow.Flow

interface BookRepository {
    // Perhatikan: Return type-nya adalah Flow<List<Book>>, BUKAN Entity
    fun getBooks(): Flow<List<Book>>

    suspend fun getBookById(id: Long): Book?

    fun getBookDetailApi(id: Long): Flow<Resource<Book>>

    suspend fun insertBook(book: Book)

    suspend fun deleteBook(book: Book)

    suspend fun updateBookProgress(book: Book)
    
    // Ambil daftar terbaru dari server ke Room (melempar exception jika gagal)
    suspend fun refreshBooks()

    // Pantau satu buku dari Room (null jika tidak ada)
    fun observeBook(id: Long): Flow<Book?>

    // Ubah info buku di server; field null tidak diubah
    suspend fun updateBookInfo(id: Long, title: String?, author: String?, publishedYear: Int?): Resource<Book>

    // Hapus buku di server dan di perangkat
    suspend fun removeBook(id: Long): Resource<Unit>

    suspend fun setFavorite(id: Long, isFavorite: Boolean): Resource<Unit>

    fun uploadBook(title: String, author: String, publishedYear: Int, pdfFile: java.io.File): kotlinx.coroutines.flow.Flow<com.example.sereneread.domain.model.UploadStatus>
    
    // Simpan lokal lalu antrekan sinkronisasi ke server. lastReadPage dimulai dari 1.
    suspend fun saveReadingProgress(bookId: Long, lastReadPage: Int, totalPages: Int)
}