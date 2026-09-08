package com.example.sereneread.domain.usecase

import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.repository.BookRepository

class AddBookUseCase(
    private val repository: BookRepository
) {
    suspend operator fun invoke(book: Book) {
        if (book.title.isBlank()) {
            // Contoh validasi bisnis logic sederhana
            throw Exception("Book title cannot be empty")
        }
        repository.insertBook(book)
    }
}