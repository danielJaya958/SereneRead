package com.example.sereneread.data.repository

import com.example.sereneread.domain.util.Constants
import com.example.sereneread.testutil.dto
import com.example.sereneread.testutil.entity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class BookSyncTest {

    // --- parseServerTime ---

    @Test
    fun `parses finished_at with fraction and timezone`() {
        // 13:50:05.175 di +08:00 = 05:50:05.175 UTC
        val millis = parseServerTime("2026-10-22T13:50:05.17599+08:00")
        assertEquals(1792648205175L, millis)
    }

    @Test
    fun `invalid finished_at returns null instead of crashing`() {
        assertNull(parseServerTime("not a date"))
        assertNull(parseServerTime(""))
    }

    // --- BookDto.toBook ---

    @Test
    fun `maps server fields to domain book`() {
        val book = dto(
            id = 7,
            pdfUrl = "/api/v1/books/7/file",
            lastReadPage = 12,
            totalPages = 364,
            isFavorite = true,
            finishedAt = "2026-10-22T13:50:05.17599+08:00"
        ).toBook()

        assertEquals(7L, book.id)
        assertEquals("${Constants.SERVER_URL}/uploads/thumbnails/11_1.jpg", book.coverUrl)
        assertEquals("${Constants.SERVER_URL}/api/v1/books/7/file", book.filePath)
        assertEquals(12, book.lastReadPosition)
        assertEquals(364, book.totalPages)
        assertEquals(2022, book.publishedYear)
        assertTrue(book.isFavorite)
        assertEquals(1792648205175L, book.finishedAt)
    }

    @Test
    fun `missing optional fields get safe defaults`() {
        val book = dto(
            thumbnailUrl = "",
            pdfUrl = null,
            lastReadPage = null,
            totalPages = null,
            isFavorite = null,
            finishedAt = null
        ).toBook()

        assertEquals("", book.coverUrl)
        assertEquals("", book.filePath)
        assertEquals(0, book.lastReadPosition)
        assertEquals(0, book.totalPages)
        assertFalse(book.isFavorite)
        assertNull(book.finishedAt)
    }

    // --- mergeWithLocal ---

    @Test
    fun `new book from server is stored as synced`() {
        val merged = mergeWithLocal(dto(lastReadPage = 5).toBook(), local = null)
        assertEquals(5, merged.lastReadPosition)
        assertTrue(merged.progressSynced)
        assertEquals(0L, merged.lastReadAt)
    }

    @Test
    fun `synced local progress is replaced by server progress`() {
        // Misal dibaca di perangkat lain: server lebih baru
        val local = entity(lastReadPosition = 10, progressSynced = true)
        val merged = mergeWithLocal(dto(lastReadPage = 40).toBook(), local)
        assertEquals(40, merged.lastReadPosition)
        assertTrue(merged.progressSynced)
    }

    @Test
    fun `unsynced local progress is never overwritten by server`() {
        // Kasus bug lama: progres lokal 17 belum terkirim, server masih 1
        val local = entity(lastReadPosition = 17, progressSynced = false)
        val merged = mergeWithLocal(dto(lastReadPage = 1).toBook(), local)
        assertEquals(17, merged.lastReadPosition)
        assertFalse(merged.progressSynced)
    }

    @Test
    fun `server total pages wins, local used only when server sends 0`() {
        val local = entity(totalPages = 250)
        assertEquals(717, mergeWithLocal(dto(totalPages = 717).toBook(), local).totalPages)
        assertEquals(250, mergeWithLocal(dto(totalPages = 0).toBook(), local).totalPages)
        assertEquals(250, mergeWithLocal(dto(totalPages = null).toBook(), local).totalPages)
    }

    @Test
    fun `list response without pdf_url keeps the known file path`() {
        val local = entity(filePath = "${Constants.SERVER_URL}/api/v1/books/1/file")
        val merged = mergeWithLocal(dto(pdfUrl = null).toBook(), local)
        assertEquals("${Constants.SERVER_URL}/api/v1/books/1/file", merged.filePath)
    }

    @Test
    fun `local date added and last read time survive a refresh`() {
        val local = entity(dateAdded = Date(42_000L), lastReadAt = 99_000L)
        val merged = mergeWithLocal(dto().toBook(), local)
        assertEquals(Date(42_000L), merged.dateAdded)
        assertEquals(99_000L, merged.lastReadAt)
    }

    @Test
    fun `favorite status comes from server`() {
        val local = entity(isFavorite = false)
        assertTrue(mergeWithLocal(dto(isFavorite = true).toBook(), local).isFavorite)
    }
}
