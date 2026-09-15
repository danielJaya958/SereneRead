package com.example.sereneread.presentation.reader

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sereneread.data.remote.SereneReadApi
import com.example.sereneread.domain.repository.BookRepository
import com.example.sereneread.domain.util.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val api: SereneReadApi, private val repository: BookRepository
) : ViewModel() {

    private val _pdfUrlState = MutableLiveData<Resource<String>>()
    val pdfUrlState: LiveData<Resource<String>> = _pdfUrlState

    fun getBookContentUrl(bookId: Long) {
        _pdfUrlState.value = Resource.Loading()
        viewModelScope.launch {
            try {
                // Panggil API Golang untuk ambil link PDF
                val response = api.getBookContent(bookId)
                if (response.status && response.data != null) {
                    val pdfUrl = response.data.pdfUrl ?: ""
                    _pdfUrlState.value = Resource.Success(pdfUrl)
                } else {
                    _pdfUrlState.value = Resource.Error(response.message ?: "Gagal mendapatkan URL")
                }
            } catch (e: Exception) {
                _pdfUrlState.value = Resource.Error("Kesalahan jaringan: ${e.message}")
            }
        }
    }
    
    fun saveProgress(bookId: Long, page: Int) {
        // Menggunakan scope independen agar proses tetap berjalan meski layar ditutup
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.saveReadingProgress(bookId, page).collect {}
            } catch (e: Exception) {
                // Biarkan senyap
            }
        }
    }
}