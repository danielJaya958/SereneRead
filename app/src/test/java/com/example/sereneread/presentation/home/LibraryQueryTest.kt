package com.example.sereneread.presentation.home

import com.example.sereneread.testutil.book
import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryQueryTest {

    private val notStarted = book(id = 1, title = "Babel", author = "R. F. Kuang")
    private val reading = book(id = 2, title = "_Good_Habits_Bad_Habits", author = "Wendy Wood",
        lastReadPosition = 5, totalPages = 364, lastReadAt = 2_000)
    private val finished = book(id = 3, title = "atomic habits", author = "James Clear",
        lastReadPosition = 320, totalPages = 320, lastReadAt = 1_000, isFavorite = true)
    private val favoriteUnread = book(id = 4, title = "Clean Architecture", author = "Robert Martin",
        isFavorite = true)

    private val all = listOf(notStarted, reading, finished, favoriteUnread)

    private fun ids(filter: BookFilter = BookFilter.ALL, query: String = "", sort: SortOrder = SortOrder.RECENTLY_ADDED) =
        filterAndSortBooks(all, filter, query, sort).map { it.id }

    @Test
    fun `filters by reading status`() {
        assertEquals(listOf(2L), ids(BookFilter.IN_PROGRESS))
        assertEquals(listOf(3L), ids(BookFilter.FINISHED))
        assertEquals(listOf(4L, 1L), ids(BookFilter.NOT_STARTED))
        assertEquals(listOf(4L, 3L), ids(BookFilter.FAVORITES))
    }

    @Test
    fun `search matches title or author ignoring case`() {
        assertEquals(listOf(3L, 2L), ids(query = "HABITS"))
        assertEquals(listOf(1L), ids(query = "kuang"))
    }

    @Test
    fun `search uses the cleaned title`() {
        // Judul asli memakai garis bawah; pengguna mengetik dengan spasi
        assertEquals(listOf(2L), ids(query = "good habits"))
    }

    @Test
    fun `search combines with filter`() {
        assertEquals(listOf(3L), ids(BookFilter.FAVORITES, query = "habits"))
        assertEquals(emptyList<Long>(), ids(BookFilter.IN_PROGRESS, query = "kuang"))
    }

    @Test
    fun `recently added puts the highest id first`() {
        assertEquals(listOf(4L, 3L, 2L, 1L), ids(sort = SortOrder.RECENTLY_ADDED))
    }

    @Test
    fun `title sort is case insensitive and uses cleaned title`() {
        // atomic habits, Babel, Clean Architecture, Good Habits Bad Habits
        assertEquals(listOf(3L, 1L, 4L, 2L), ids(sort = SortOrder.TITLE))
    }

    @Test
    fun `recently read puts unopened books last, newest first`() {
        assertEquals(listOf(2L, 3L, 4L, 1L), ids(sort = SortOrder.RECENTLY_READ))
    }
}
