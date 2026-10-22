package com.example.sereneread.presentation.editbook

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.repository.BookRepository
import com.example.sereneread.domain.util.Resource
import com.example.sereneread.presentation.addbook.BookFormInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface EditBookState {
    data object Editing : EditBookState
    data object Saving : EditBookState
    data object Saved : EditBookState
    data class Failed(val message: String) : EditBookState
}

@HiltViewModel
class EditBookViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: BookRepository
) : ViewModel() {

    private val bookId: Long = savedStateHandle["bookId"] ?: 0L

    // Data awal untuk mengisi form (dari Room)
    private val _book = MutableLiveData<Book?>()
    val book: LiveData<Book?> = _book

    private val _state = MutableLiveData<EditBookState>(EditBookState.Editing)
    val state: LiveData<EditBookState> = _state

    // Form sudah diisi data awal; jangan isi ulang saat layar diputar
    var formFilled = false

    init {
        viewModelScope.launch { _book.value = repository.getBookById(bookId) }
    }

    fun save(input: BookFormInput) {
        val original = _book.value ?: return
        if (_state.value == EditBookState.Saving) return

        // Kirim hanya kolom yang berubah. Penulis yang dikosongkan tidak dikirim
        // (tetap memakai nilai lama), karena backend mewajibkan penulis.
        val title = input.title.takeIf { it != original.title }
        val author = input.author?.takeIf { it != original.author }
        val year = input.year?.takeIf { it != original.publishedYear }
        if (title == null && author == null && year == null) {
            _state.value = EditBookState.Saved
            return
        }

        _state.value = EditBookState.Saving
        viewModelScope.launch {
            _state.value = when (val result = repository.updateBookInfo(bookId, title, author, year)) {
                is Resource.Success -> EditBookState.Saved
                else -> EditBookState.Failed(result.message ?: "Couldn't save changes")
            }
        }
    }
}
