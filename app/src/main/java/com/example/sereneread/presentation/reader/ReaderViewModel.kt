package com.example.sereneread.presentation.reader

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sereneread.data.remote.SereneReadApi
import com.example.sereneread.domain.repository.BookRepository
import com.example.sereneread.domain.util.Resource
import com.example.sereneread.presentation.home.displayTitle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import javax.inject.Inject

// pdfUrl null berarti server tidak bisa dihubungi, coba buka dari cache lokal
data class ReaderContent(val pdfUrl: String?, val startPage: Int)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val api: SereneReadApi, private val repository: BookRepository
) : ViewModel() {

    private val _contentState = MutableLiveData<Resource<ReaderContent>>()
    val contentState: LiveData<Resource<ReaderContent>> = _contentState

    // Judul buku untuk bar atas reader (dari Room)
    private val _bookTitle = MutableLiveData<String>()
    val bookTitle: LiveData<String> = _bookTitle

    // Status tampil/sembunyi kontrol reader, disimpan di sini agar tetap sama saat layar diputar
    var controlsVisible = true

    private var bookId = 0L

    // Halaman dimulai dari 1. 0 berarti belum diketahui.
    var currentPage = 0
        private set
    private var totalPages = 0
    private var lastSavedPage = 0

    fun loadBook(bookId: Long) {
        // Jangan muat ulang saat layar diputar, ViewModel sudah menyimpan state-nya
        if (this.bookId == bookId) return
        this.bookId = bookId

        _contentState.value = Resource.Loading()
        viewModelScope.launch {
            val localBook = repository.getBookById(bookId)
            localBook?.let { _bookTitle.value = it.displayTitle() }
            totalPages = localBook?.totalPages ?: 0
            try {
                // Panggil API Golang untuk ambil link PDF
                val response = api.getBookContent(bookId)
                if (response.status && response.data != null) {
                    // Room sudah digabung dengan data server oleh repository, dan menyimpan
                    // progres yang belum terkirim, jadi utamakan nilai lokal
                    setInitialPage(
                        localBook?.lastReadPosition?.takeIf { it > 0 }
                            ?: response.data.lastReadPage
                            ?: 0
                    )
                    _contentState.value = Resource.Success(
                        ReaderContent(response.data.pdfUrl ?: "", currentPage)
                    )
                } else {
                    setInitialPage(localBook?.lastReadPosition ?: 0)
                    _contentState.value = Resource.Error(
                        response.message ?: "Gagal mendapatkan URL",
                        ReaderContent(null, currentPage)
                    )
                }
            } catch (e: Exception) {
                setInitialPage(localBook?.lastReadPosition ?: 0)
                _contentState.value = Resource.Error(
                    "Kesalahan jaringan: ${e.message}",
                    ReaderContent(null, currentPage)
                )
            }
        }
    }

    private fun setInitialPage(page: Int) {
        Log.d("ReadingProgress", "Mulai dari halaman $page (buku=$bookId)")
        currentPage = page
        lastSavedPage = page
    }

    fun onPageChanged(page: Int, total: Int) {
        currentPage = page
        totalPages = total
    }

    fun saveProgress() {
        // Hanya simpan kalau halaman benar-benar berubah, supaya tidak ada request ganda
        if (bookId == 0L || currentPage < 1 || currentPage == lastSavedPage) return
        lastSavedPage = currentPage

        val page = currentPage
        val total = totalPages
        // NonCancellable: onPause bisa terjadi tepat sebelum ViewModel dibersihkan saat keluar,
        // penyimpanan ke Room tidak boleh ikut dibatalkan
        viewModelScope.launch(NonCancellable) {
            // Room + WorkManager: progres tetap terkirim walau layar ditutup atau sedang offline
            repository.saveReadingProgress(bookId, page, total)
            Log.d("ReadingProgress", "Disimpan lokal: buku=$bookId halaman=$page/$total")
        }
    }
}
