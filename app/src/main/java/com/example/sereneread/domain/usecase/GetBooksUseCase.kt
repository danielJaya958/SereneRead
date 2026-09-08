package com.example.sereneread.domain.usecase

import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.repository.BookRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GetBooksUseCase(
    private val repository: BookRepository
) {
    // Operator invoke membuat class ini bisa dipanggil seperti fungsi: getBooks()
    operator fun invoke(): Flow<List<Book>> {
        // Di sini kita bisa tambah logic filter/sort default
        return repository.getBooks().map { books ->
            // Contoh: Pastikan yang ditampilkan urut berdasarkan tanggal (sudah di DAO, tapi bisa dipertegas di sini)
            books.sortedByDescending { it.dateAdded }
        }
    }
}