package com.example.sereneread.presentation.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.repository.BookRepository
import com.example.sereneread.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DetailState {
    data object Loading : DetailState
    data class Content(val book: Book) : DetailState
    // Buku tidak ada lagi (dihapus di sini atau di perangkat lain): tutup halaman
    data class Gone(val message: String) : DetailState
}

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookRepository
) : ViewModel() {

    val bookId: Long = savedStateHandle["bookId"] ?: 0L

    private val _state = MutableLiveData<DetailState>(DetailState.Loading)
    val state: LiveData<DetailState> = _state

    // Pesan sekali tampil (gagal favorit/hapus). Fragment memanggil messageShown() setelahnya.
    private val _message = MutableLiveData<String?>()
    val message: LiveData<String?> = _message

    private val _deleting = MutableLiveData(false)
    val deleting: LiveData<Boolean> = _deleting

    // true setelah server menjawab; sebelum itu, buku yang belum ada di Room berarti masih memuat
    private var serverChecked = false

    val currentBook: Book?
        get() = (_state.value as? DetailState.Content)?.book

    init {
        // Detail selalu membaca dari Room, jadi edit, favorit, dan progres langsung terlihat
        viewModelScope.launch {
            repository.observeBook(bookId).collect { book ->
                when {
                    book != null -> _state.value = DetailState.Content(book)
                    _deleting.value == true -> Unit // ditangani oleh deleteBook()
                    serverChecked -> _state.value = DetailState.Gone("This book is no longer in your library")
                }
            }
        }
        refresh()
    }

    // Ambil data terbaru dari server; hasilnya masuk ke Room dan tampil lewat observeBook
    private fun refresh() {
        viewModelScope.launch {
            repository.getBookDetailApi(bookId).collect { result ->
                if (result is Resource.Loading) return@collect
                serverChecked = true
                if (result is Resource.Error && repository.getBookById(bookId) == null) {
                    _state.value = DetailState.Gone(result.message ?: "Couldn't load this book")
                }
            }
        }
    }

    fun toggleFavorite() {
        val book = currentBook ?: return
        viewModelScope.launch {
            val result = repository.setFavorite(bookId, !book.isFavorite)
            if (result is Resource.Error) _message.value = result.message
        }
    }

    fun deleteBook() {
        val book = currentBook ?: return
        if (_deleting.value == true) return
        _deleting.value = true
        viewModelScope.launch {
            when (val result = repository.removeBook(bookId)) {
                is Resource.Success -> _state.value = DetailState.Gone("“${book.title}” deleted")
                else -> {
                    _deleting.value = false
                    _message.value = result.message
                }
            }
        }
    }

    fun messageShown() {
        _message.value = null
    }
}
