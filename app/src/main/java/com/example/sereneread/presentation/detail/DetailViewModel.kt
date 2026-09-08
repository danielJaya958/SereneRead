package com.example.sereneread.presentation.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.repository.BookRepository
import com.example.sereneread.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val repository: BookRepository
) : ViewModel() {

    private val _bookState = MutableLiveData<Resource<Book>>()
    val bookState: LiveData<Resource<Book>> = _bookState

    // Menyimpan data buku yang sedang aktif agar gampang diakses
    var currentBook: Book? = null

    fun loadBook(id: Long) {
        repository.getBookDetailApi(id).onEach { result ->
            _bookState.value = result
            if (result is Resource.Success) {
                currentBook = result.data
            }
        }.launchIn(viewModelScope)
    }

    fun toggleFavorite() {
        // Nanti kita bisa isi logika update favorite ke Room Database di sini
    }
}