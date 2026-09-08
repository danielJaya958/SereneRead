package com.example.sereneread.domain.usecase

data class BookUseCases(
    val getBooks: GetBooksUseCase,
    val getBook: GetBookDetailsUseCase,
    val addBook: AddBookUseCase,
    val toggleFavorite: ToggleFavoriteUseCase
    // Nanti bisa tambah: deleteBook, updateProgress, dll.
)