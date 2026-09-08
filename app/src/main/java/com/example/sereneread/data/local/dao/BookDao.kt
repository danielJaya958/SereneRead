package com.example.sereneread.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.sereneread.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {

    // Mengambil semua buku, diurutkan dari yang terbaru ditambahkan
    @Query("SELECT * FROM books ORDER BY dateAdded DESC")
    fun getAllBooks(): Flow<List<BookEntity>>

    // Mengambil satu buku berdasarkan ID
    @Query("SELECT * FROM books WHERE id = :id")
    suspend fun getBookById(id: Long): BookEntity?

    // Insert (Jika ID sama, timpa datanya)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    // Update data buku (misal update progress baca)
    @Update
    suspend fun updateBook(book: BookEntity)

    // Hapus buku
    @Delete
    suspend fun deleteBook(book: BookEntity)
}