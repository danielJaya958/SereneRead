package com.example.sereneread.domain.usecase

import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.repository.BookRepository

class GetBookDetailsUseCase(
    private val repository: BookRepository
) {
    suspend operator fun invoke(id: Long): Book? {
        return repository.getBookById(id)
    }
}