package com.example.sereneread.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.model.BookFormat
import com.example.sereneread.domain.usecase.BookUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookUseCases: BookUseCases
) : ViewModel() {

    // State untuk List Buku
    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    // State untuk Filter (All, Favorites)
    private val _currentFilter = MutableLiveData<BookFilter>(BookFilter.ALL)
    val currentFilter: LiveData<BookFilter> = _currentFilter

    // Menyimpan data asli sebelum difilter
    private var allBooksCache: List<Book> = emptyList()

    init {
        // seedDummyData()
        getBooks()
    }

    // --- FUNGSI BARU: MEMBUAT DATA PALSU ---
    private fun seedDummyData() {
        viewModelScope.launch {
            // Cek dulu apakah database kosong dengan mengambil data sekali saja (.first())
            val currentList = bookUseCases.getBooks().first()

            if (currentList.isEmpty()) {
                val dummyBooks = listOf(
                    Book(
                        title = "The Great Gatsby",
                        author = "F. Scott Fitzgerald",
                        description = "The story of the mysteriously wealthy Jay Gatsby and his love for the beautiful Daisy Buchanan.",
                        // URL gambar dummy (Saya pakai placehold.co dengan warna tema kita)
                        coverUrl = "https://placehold.co/400x600/005F73/FFFFFF/png?text=Great+Gatsby",
                        filePath = "dummy_path_1",
                        format = BookFormat.EPUB,
                        rating = 4.5f,
                        genre = "Classic",
                        isFavorite = true, // Ini untuk tes tab Favorit
                        dateAdded = Date(),
                        lastReadPosition = 50,
                        totalPages = 200
                    ),
                    Book(
                        title = "Clean Architecture",
                        author = "Robert C. Martin",
                        description = "A craftsman's guide to software structure and design.",
                        coverUrl = "https://placehold.co/400x600/E9C46A/2D3748/png?text=Clean+Arch",
                        filePath = "dummy_path_2",
                        format = BookFormat.PDF,
                        rating = 5.0f,
                        genre = "Technology",
                        isFavorite = false,
                        dateAdded = Date(),
                        lastReadPosition = 0,
                        totalPages = 350
                    ),
                    Book(
                        title = "Serene Mindset",
                        author = "Daniel Setiawan",
                        description = "How to stay calm while coding Android apps.",
                        coverUrl = "https://placehold.co/400x600/F8F9FA/2D3748/png?text=Serene+Read",
                        filePath = "dummy_path_3",
                        format = BookFormat.EPUB,
                        rating = 4.8f,
                        genre = "Self Help",
                        isFavorite = true,
                        dateAdded = Date(),
                        lastReadPosition = 120,
                        totalPages = 150
                    )
                )

                // Masukkan ke database satu per satu
                dummyBooks.forEach { book ->
                    bookUseCases.addBook(book)
                }
            }
        }
    }

    private fun getBooks() {
        // Mengambil data dari UseCase (Flow)
        bookUseCases.getBooks()
            .onEach { list ->
                allBooksCache = list
                // Terapkan filter saat ini ke data yang baru didapat
                applyFilter(_currentFilter.value ?: BookFilter.ALL)
            }
            .launchIn(viewModelScope)
    }

    fun setFilter(filter: BookFilter) {
        _currentFilter.value = filter
        applyFilter(filter)
    }

    private fun applyFilter(filter: BookFilter) {
        val filteredList = when (filter) {
            BookFilter.ALL -> allBooksCache
            BookFilter.FAVORITES -> allBooksCache.filter { it.isFavorite }
        }
        _books.value = filteredList
    }
}

// Enum sederhana untuk UI Filter
enum class BookFilter {
    ALL,
    FAVORITES
}