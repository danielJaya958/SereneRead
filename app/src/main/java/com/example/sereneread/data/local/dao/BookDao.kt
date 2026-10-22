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

    // Ambil semua buku sekali (bukan Flow), dipakai saat menggabungkan data dari server
    @Query("SELECT * FROM books")
    suspend fun getAllBooksOnce(): List<BookEntity>

    // Simpan banyak buku sekaligus dalam satu transaksi
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBooks(books: List<BookEntity>)

    // Insert (Jika ID sama, timpa datanya)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookEntity)

    // Update data buku (misal update progress baca)
    @Update
    suspend fun updateBook(book: BookEntity)

    // Update khusus progres baca tanpa menimpa kolom lain
    @Query("UPDATE books SET lastReadPosition = :page, totalPages = :totalPages, progressSynced = 0, lastReadAt = :readAt WHERE id = :id")
    suspend fun updateProgress(id: Long, page: Int, totalPages: Int, readAt: Long = System.currentTimeMillis())

    // Tandai tersinkron hanya jika halaman belum berubah lagi sejak request dikirim
    @Query("UPDATE books SET progressSynced = 1 WHERE id = :id AND lastReadPosition = :page")
    suspend fun markProgressSynced(id: Long, page: Int)

    // Pantau satu buku (null jika belum/tidak ada), dipakai halaman detail
    @Query("SELECT * FROM books WHERE id = :id")
    fun observeBook(id: Long): Flow<BookEntity?>

    @Query("UPDATE books SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFavorite: Boolean)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteById(id: Long)

    // Buku yang sudah tidak ada di server (dihapus) ikut dibuang dari perangkat
    @Query("DELETE FROM books WHERE id NOT IN (:keepIds)")
    suspend fun deleteAllExcept(keepIds: List<Long>)

    @Query("DELETE FROM books")
    suspend fun deleteAll()

    // Buku yang progres lokalnya belum berhasil dikirim ke server
    @Query("SELECT * FROM books WHERE progressSynced = 0")
    suspend fun getUnsyncedProgress(): List<BookEntity>

    // Hapus buku
    @Delete
    suspend fun deleteBook(book: BookEntity)
}