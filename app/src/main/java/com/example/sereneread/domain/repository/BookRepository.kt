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
    
    fun uploadBook(title: String, author: String, publishedYear: Int, pdfFile: java.io.File): kotlinx.coroutines.flow.Flow<Resource<Unit>>
    
    fun saveReadingProgress(bookId: Long, lastReadPage: Int): kotlinx.coroutines.flow.Flow<Resource<Unit>>
}