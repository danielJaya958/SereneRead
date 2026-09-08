package com.example.sereneread.data.mapper

import com.example.sereneread.data.local.entity.BookEntity
import com.example.sereneread.domain.model.Book

// Mengubah Entity (Database) -> Domain (UI)
fun BookEntity.toDomain(): Book {
    return Book(
        id = id,
        title = title,
        author = author,
        description = description,
        coverUrl = coverUrl,
        filePath = filePath,
        format = format,
        rating = rating,
        genre = genre,
        isFavorite = isFavorite,
        dateAdded = dateAdded,
        lastReadPosition = lastReadPosition,
        totalPages = totalPages
    )
}

// Mengubah Domain (UI) -> Entity (Database)
fun Book.toEntity(): BookEntity {
    return BookEntity(
        id = id, // Jika id 0, Room akan auto-generate
        title = title,
        author = author,
        description = description,
        coverUrl = coverUrl,
        filePath = filePath,
        format = format,
        rating = rating,
        genre = genre,
        isFavorite = isFavorite,
        dateAdded = dateAdded,
        lastReadPosition = lastReadPosition,
        totalPages = totalPages
    )
}