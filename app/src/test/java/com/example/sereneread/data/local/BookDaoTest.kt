package com.example.sereneread.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.sereneread.data.local.dao.BookDao
import com.example.sereneread.testutil.entity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Query DAO yang dipakai sinkron progres dan penghapusan buku
@RunWith(RobolectricTestRunner::class)
class BookDaoTest {

    private lateinit var database: SereneDatabase
    private lateinit var dao: BookDao

    @Before
    fun setUp() {
        val context: Context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SereneDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = database.bookDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `saving progress marks the book as unsynced and records the time`() = runTest {
        dao.insertBook(entity(id = 1, lastReadPosition = 0, progressSynced = true))

        dao.updateProgress(1, page = 13, totalPages = 300, readAt = 5_000L)

        val book = dao.getBookById(1)!!
        assertEquals(13, book.lastReadPosition)
        assertEquals(300, book.totalPages)
        assertEquals(5_000L, book.lastReadAt)
        assertFalse(book.progressSynced)
        assertEquals(listOf(1L), dao.getUnsyncedProgress().map { it.id })
    }

    @Test
    fun `sync confirmation only applies to the page that was sent`() = runTest {
        dao.insertBook(entity(id = 1))
        dao.updateProgress(1, page = 13, totalPages = 300)
        // Pengguna sudah lanjut ke halaman 20 sebelum PATCH halaman 13 selesai
        dao.updateProgress(1, page = 20, totalPages = 300)

        dao.markProgressSynced(1, page = 13)
        assertFalse("page 20 still needs sending", dao.getBookById(1)!!.progressSynced)

        dao.markProgressSynced(1, page = 20)
        assertTrue(dao.getBookById(1)!!.progressSynced)
        assertTrue(dao.getUnsyncedProgress().isEmpty())
    }

    @Test
    fun `books missing from the server list are removed`() = runTest {
        dao.insertBooks(listOf(entity(id = 1), entity(id = 2), entity(id = 3)))

        dao.deleteAllExcept(listOf(1L, 3L))

        assertEquals(listOf(1L, 3L), dao.getAllBooksOnce().map { it.id }.sorted())
    }

    @Test
    fun `favorite update changes only that book`() = runTest {
        dao.insertBooks(listOf(entity(id = 1), entity(id = 2)))

        dao.updateFavorite(2, true)

        assertFalse(dao.getBookById(1)!!.isFavorite)
        assertTrue(dao.getBookById(2)!!.isFavorite)
    }
}
