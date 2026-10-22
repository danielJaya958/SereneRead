package com.example.sereneread.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

// Pastikan pengguna yang memasang versi lama (DB v1) bisa update tanpa crash dan tanpa kehilangan data.
// Room memvalidasi skema setelah migrasi; jika migrasi salah, build() / query pertama melempar exception.
@RunWith(RobolectricTestRunner::class)
class SereneDatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val dbName = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(dbName)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(dbName)
    }

    // Skema v1 persis seperti yang dibuat Room untuk BookEntity versi pertama
    private fun createVersion1Database() {
        val db = SQLiteDatabase.openOrCreateDatabase(context.getDatabasePath(dbName).apply { parentFile?.mkdirs() }, null)
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `books` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `title` TEXT NOT NULL,
                `author` TEXT NOT NULL,
                `description` TEXT NOT NULL,
                `coverUrl` TEXT,
                `filePath` TEXT NOT NULL,
                `format` TEXT NOT NULL,
                `rating` REAL NOT NULL,
                `genre` TEXT NOT NULL,
                `isFavorite` INTEGER NOT NULL,
                `dateAdded` INTEGER NOT NULL,
                `lastReadPosition` INTEGER NOT NULL,
                `totalPages` INTEGER NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            INSERT INTO books (id, title, author, description, coverUrl, filePath, format, rating,
                genre, isFavorite, dateAdded, lastReadPosition, totalPages)
            VALUES (6, 'Saha Mansion', 'Cho Nam-Joo', '', NULL, '', 'PDF', 0, 'General', 1, 1700000000000, 17, 300)
            """.trimIndent()
        )
        db.version = 1
        db.close()
    }

    @Test
    fun `migrates v1 to v4 keeping data and filling new columns with defaults`() = runTest {
        createVersion1Database()

        val database = Room.databaseBuilder(context, SereneDatabase::class.java, dbName)
            .addMigrations(
                SereneDatabase.MIGRATION_1_2,
                SereneDatabase.MIGRATION_2_3,
                SereneDatabase.MIGRATION_3_4
            )
            .allowMainThreadQueries()
            .build()

        try {
            val book = database.bookDao().getBookById(6)!!

            // Data lama utuh
            assertEquals("Saha Mansion", book.title)
            assertEquals(17, book.lastReadPosition)
            assertEquals(300, book.totalPages)
            assertTrue(book.isFavorite)
            assertEquals(1700000000000L, book.dateAdded.time)

            // Kolom baru: v2 progressSynced, v3 lastReadAt, v4 publishedYear & finishedAt
            assertTrue(book.progressSynced)
            assertEquals(0L, book.lastReadAt)
            assertEquals(0, book.publishedYear)
            assertNull(book.finishedAt)
        } finally {
            database.close()
        }
    }
}
