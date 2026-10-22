package com.example.sereneread.domain.model

import com.example.sereneread.presentation.home.displayTitle
import com.example.sereneread.presentation.home.hasCoverImage
import com.example.sereneread.presentation.home.progressLabel
import com.example.sereneread.presentation.home.statusLabel
import com.example.sereneread.testutil.book
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BookStatusTest {

    @Test
    fun `book with page 0 is not started`() {
        val b = book(lastReadPosition = 0, totalPages = 300)
        assertFalse(b.isStarted)
        assertFalse(b.isInProgress)
        assertFalse(b.isFinished)
        assertNull(b.statusLabel())
    }

    @Test
    fun `book read to page 1 counts as started`() {
        // Backend sekarang mengirim 0 untuk buku yang belum dibuka, jadi halaman 1 = sudah mulai
        val b = book(lastReadPosition = 1, totalPages = 300)
        assertTrue(b.isStarted)
        assertTrue(b.isInProgress)
    }

    @Test
    fun `reaching the last page finishes the book even without finished_at`() {
        val b = book(lastReadPosition = 300, totalPages = 300, finishedAt = null)
        assertTrue(b.isFinished)
        assertFalse(b.isInProgress)
        assertEquals("Finished", b.statusLabel())
    }

    @Test
    fun `finished_at from server finishes the book`() {
        val b = book(lastReadPosition = 5, totalPages = 300, finishedAt = 1_700_000_000_000)
        assertTrue(b.isFinished)
        assertFalse(b.isInProgress)
    }

    @Test
    fun `unknown total pages never counts as finished`() {
        val b = book(lastReadPosition = 40, totalPages = 0)
        assertFalse(b.isFinished)
        assertTrue(b.isInProgress)
    }

    @Test
    fun `progress percentage is 0 when total pages unknown`() {
        assertEquals(0, book(lastReadPosition = 40, totalPages = 0).progressPercentage)
        assertEquals(25, book(lastReadPosition = 50, totalPages = 200).progressPercentage)
    }

    @Test
    fun `progress label includes total only when known`() {
        assertEquals("Page 13 of 300", book(lastReadPosition = 13, totalPages = 300).progressLabel())
        assertEquals("Page 13", book(lastReadPosition = 13, totalPages = 0).progressLabel())
        assertEquals("Page 13 of 300", book(lastReadPosition = 13, totalPages = 300).statusLabel())
    }

    @Test
    fun `display title turns underscores into spaces`() {
        assertEquals(
            "OceanofPDF.com Saha Mansion Indonesian Edition - Cho Nam-Joo",
            book(title = "_OceanofPDF.com_Saha_Mansion_Indonesian_Edition_-_Cho_Nam-Joo").displayTitle()
        )
        assertEquals("ticket-JS-DHP26- 14087", book(title = "ticket-JS-DHP26-  14087").displayTitle())
    }

    @Test
    fun `display title falls back to raw title when only underscores`() {
        assertEquals("___", book(title = "___").displayTitle())
    }

    @Test
    fun `empty thumbnail folder path means no cover`() {
        assertFalse(book(coverUrl = null).hasCoverImage())
        assertFalse(book(coverUrl = "").hasCoverImage())
        assertFalse(book(coverUrl = "https://sereneread.my.id/uploads/thumbnails/").hasCoverImage())
        assertTrue(book(coverUrl = "https://sereneread.my.id/uploads/thumbnails/11_1.jpg").hasCoverImage())
    }
}
