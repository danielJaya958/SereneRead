package com.example.sereneread.domain.usecase

import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.repository.BookRepository

class ToggleFavoriteUseCase(
    private val repository: BookRepository
) {
    suspend operator fun invoke(book: Book) {
        // Logic: Copy buku yang ada, lalu balik nilai boolean isFavorite
        val updatedBook = book.copy(isFavorite = !book.isFavorite)
        repository.insertBook(updatedBook) // Insert dengan ID sama = Update (di Room Strategy kita)
    }
}