package com.example.sereneread.testutil

import com.example.sereneread.data.local.entity.BookEntity
import com.example.sereneread.data.remote.BookDto
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.model.BookFormat
import java.util.Date

// Pembuat data uji dengan nilai bawaan, supaya tiap test hanya menulis field yang relevan

fun book(
    id: Long = 1,
    title: String = "Babel",
    author: String = "R. F. Kuang",
    lastReadPosition: Int = 0,
    totalPages: Int = 0,
    isFavorite: Boolean = false,
    finishedAt: Long? = null,
    lastReadAt: Long = 0,
    coverUrl: String? = null
) = Book(
    id = id,
    title = title,
    author = author,
    coverUrl = coverUrl,
    filePath = "",
    format = BookFormat.PDF,
    isFavorite = isFavorite,
    lastReadPosition = lastReadPosition,
    totalPages = totalPages,
    lastReadAt = lastReadAt,
    finishedAt = finishedAt
)

fun entity(
    id: Long = 1,
    title: String = "Babel",
    lastReadPosition: Int = 0,
    totalPages: Int = 0,
    filePath: String = "",
    dateAdded: Date = Date(1_000L),
    progressSynced: Boolean = true,
    lastReadAt: Long = 0,
    isFavorite: Boolean = false
) = BookEntity(
    id = id,
    title = title,
    author = "R. F. Kuang",
    description = "",
    coverUrl = null,
    filePath = filePath,
    format = BookFormat.PDF,
    rating = 0f,
    genre = "General",
    isFavorite = isFavorite,
    dateAdded = dateAdded,
    lastReadPosition = lastReadPosition,
    totalPages = totalPages,
    progressSynced = progressSynced,
    lastReadAt = lastReadAt
)

fun dto(
    id: Long = 1,
    title: String = "Babel",
    thumbnailUrl: String? = "/uploads/thumbnails/11_1.jpg",
    pdfUrl: String? = null,
    lastReadPage: Int? = 0,
    totalPages: Int? = 717,
    isFavorite: Boolean? = false,
    finishedAt: String? = null
) = BookDto(
    id = id,
    title = title,
    author = "R. F. Kuang",
    publishedYear = 2022,
    thumbnailUrl = thumbnailUrl,
    pdfUrl = pdfUrl,
    lastReadPage = lastReadPage,
    totalPages = totalPages,
    isFavorite = isFavorite,
    finishedAt = finishedAt
)
