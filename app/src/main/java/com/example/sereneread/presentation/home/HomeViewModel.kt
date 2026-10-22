package com.example.sereneread.presentation.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.model.BookFormat
import com.example.sereneread.domain.usecase.BookUseCases
import dagger.hilt.android.lifecycle.HiltViewModel
import com.example.sereneread.domain.repository.BookRepository
import com.example.sereneread.data.remote.toUserMessage
import com.example.sereneread.data.session.SessionCleaner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val bookUseCases: BookUseCases,
    private val repository: BookRepository,
    private val sessionCleaner: SessionCleaner
) : ViewModel() {

    // State proses logout
    private val _logoutState = MutableLiveData<LogoutState>(LogoutState.Idle)
    val logoutState: LiveData<LogoutState> = _logoutState

    // force = true: tetap logout walau ada progres yang gagal terkirim
    fun logout(force: Boolean = false) {
        if (_logoutState.value == LogoutState.InProgress) return
        // Hentikan refresh sampul agar tidak mengisi ulang database yang sedang dikosongkan
        coverRefreshJob?.cancel()
        _logoutState.value = LogoutState.InProgress

        viewModelScope.launch {
            if (!force) {
                // Kirim dulu progres yang tertunda selagi token masih berlaku
                val remaining = sessionCleaner.syncPendingProgress()
                if (remaining > 0) {
                    _logoutState.value = LogoutState.UnsyncedProgress(remaining)
                    return@launch
                }
            }
            sessionCleaner.logout()
            _logoutState.value = LogoutState.Done
        }
    }

    fun cancelLogout() {
        _logoutState.value = LogoutState.Idle
    }

    // State untuk List Buku
    private val _books = MutableLiveData<List<Book>>()
    val books: LiveData<List<Book>> = _books

    // State untuk Filter (All, In progress, Not started)
    private val _currentFilter = MutableLiveData<BookFilter>(BookFilter.ALL)
    val currentFilter: LiveData<BookFilter> = _currentFilter

    // Buku yang terakhir dibaca, untuk kartu "Continue reading". null = belum ada yang dibaca
    private val _continueBook = MutableLiveData<Book?>()
    val continueBook: LiveData<Book?> = _continueBook

    // Ringkasan isi perpustakaan (selalu dari seluruh buku, tidak terpengaruh filter)
    private val _summary = MutableLiveData<LibrarySummary>()
    val summary: LiveData<LibrarySummary> = _summary

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
                val inProgress = list.filter { it.isInProgress }
                _summary.value = LibrarySummary(total = list.size, inProgress = inProgress.size)
                // Utamakan yang paling baru dibaca di perangkat ini; progres dari server
                // (lastReadAt = 0) tetap bisa muncul jika belum ada yang dibaca di sini
                _continueBook.value = inProgress.maxByOrNull { it.lastReadAt }
                // Terapkan filter saat ini ke data yang baru didapat
                applyFilter(_currentFilter.value ?: BookFilter.ALL)
                scheduleCoverRefreshIfNeeded(list)
            }
            .launchIn(viewModelScope)
    }

    // --- Auto-refresh sampul ---
    // Backend membuat sampul (halaman pertama PDF) SETELAH upload selesai, jadi buku baru
    // awalnya datang tanpa sampul. Refresh daftar beberapa kali sampai sampulnya tersedia.
    // Dibatasi supaya ringan: hanya untuk buku yang baru ditambahkan, satu job sekaligus,
    // maksimal 3 request dengan jeda bertambah (3s, 6s, 12s).
    private var coverRefreshJob: Job? = null
    private var coverRefreshAttempts = 0
    private var coverRefreshIds: Set<Long> = emptySet()

    private fun scheduleCoverRefreshIfNeeded(list: List<Book>) {
        val now = System.currentTimeMillis()
        val waitingIds = list
            .filter { !it.hasCoverImage() && now - it.dateAdded.time < NEW_BOOK_WINDOW_MS }
            .map { it.id }
            .toSet()

        if (waitingIds.isEmpty()) return

        // Ada buku baru lain yang menunggu sampul: tambahkan dan mulai hitungan percobaan dari awal
        if (!coverRefreshIds.containsAll(waitingIds)) {
            coverRefreshIds = coverRefreshIds + waitingIds
            coverRefreshAttempts = 0
        }

        if (coverRefreshJob?.isActive == true || coverRefreshAttempts >= MAX_COVER_REFRESH) return

        coverRefreshJob = viewModelScope.launch {
            while (coverRefreshAttempts < MAX_COVER_REFRESH) {
                delay(COVER_REFRESH_BASE_DELAY_MS shl coverRefreshAttempts)
                coverRefreshAttempts++
                try {
                    // Hasil refresh masuk ke Room, dan Home otomatis menampilkan sampul baru
                    repository.refreshBooks()
                } catch (e: Exception) {
                    // Offline atau server sibuk: coba lagi di putaran berikutnya
                }
                // Cek langsung ke Room (hanya buku yang ditunggu), berhenti jika semua sudah punya sampul
                coverRefreshIds = coverRefreshIds.filter {
                    repository.getBookById(it)?.hasCoverImage() == false
                }.toSet()
                if (coverRefreshIds.isEmpty()) break
            }
        }
    }

    private companion object {
        const val NEW_BOOK_WINDOW_MS = 2 * 60 * 1000L
        const val MAX_COVER_REFRESH = 3
        const val COVER_REFRESH_BASE_DELAY_MS = 3_000L
    }

    fun setFilter(filter: BookFilter) {
        _currentFilter.value = filter
        applyFilter(filter)
    }

    // --- Pencarian & urutan ---
    // Dikerjakan di perangkat: semua buku sudah ada di Room (GET /books tanpa pagination),
    // jadi hasil langsung muncul saat mengetik dan tetap jalan saat offline.
    var searchQuery: String = ""
        private set

    private val _sortOrder = MutableLiveData(SortOrder.RECENTLY_ADDED)
    val sortOrder: LiveData<SortOrder> = _sortOrder

    fun setSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed == searchQuery) return
        searchQuery = trimmed
        applyFilter(_currentFilter.value ?: BookFilter.ALL)
    }

    fun setSortOrder(order: SortOrder) {
        _sortOrder.value = order
        applyFilter(_currentFilter.value ?: BookFilter.ALL)
    }

    // --- Pull-to-refresh ---
    private val _isRefreshing = MutableLiveData(false)
    val isRefreshing: LiveData<Boolean> = _isRefreshing

    // Pesan sekali tampil; fragment memanggil refreshMessageShown() setelah menampilkannya
    private val _refreshError = MutableLiveData<String?>()
    val refreshError: LiveData<String?> = _refreshError

    fun refresh() {
        if (_isRefreshing.value == true) return
        _isRefreshing.value = true
        viewModelScope.launch {
            try {
                // Hasil masuk ke Room, lalu daftar di atas otomatis ter-update
                repository.refreshBooks()
            } catch (e: Exception) {
                _refreshError.value = e.toUserMessage()
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    fun refreshMessageShown() {
        _refreshError.value = null
    }

    private fun applyFilter(filter: BookFilter) {
        val filteredList = when (filter) {
            BookFilter.ALL -> allBooksCache
            BookFilter.IN_PROGRESS -> allBooksCache.filter { it.isInProgress }
            BookFilter.FINISHED -> allBooksCache.filter { it.isFinished }
            BookFilter.FAVORITES -> allBooksCache.filter { it.isFavorite }
            BookFilter.NOT_STARTED -> allBooksCache.filter { !it.isStarted }
        }

        // Cocokkan judul (yang sudah dirapikan) atau penulis, tanpa membedakan huruf besar/kecil
        val searched = if (searchQuery.isEmpty()) filteredList else filteredList.filter {
            it.displayTitle().contains(searchQuery, ignoreCase = true) ||
                it.author.contains(searchQuery, ignoreCase = true)
        }

        _books.value = when (_sortOrder.value ?: SortOrder.RECENTLY_ADDED) {
            // id dari server bertambah setiap upload, jadi id terbesar = paling baru (setara created_at)
            SortOrder.RECENTLY_ADDED -> searched.sortedByDescending { it.id }
            SortOrder.TITLE -> searched.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle() })
            // Buku yang belum pernah dibuka di perangkat ini (lastReadAt = 0) di akhir, urut terbaru
            SortOrder.RECENTLY_READ -> searched.sortedWith(
                compareByDescending<Book> { it.lastReadAt }.thenByDescending { it.id }
            )
        }
    }
}

enum class SortOrder(val label: String) {
    RECENTLY_ADDED("Recently added"),
    TITLE("Title A–Z"),
    RECENTLY_READ("Recently read")
}

// Enum sederhana untuk UI Filter
enum class BookFilter {
    ALL,
    IN_PROGRESS,
    NOT_STARTED,
    FINISHED,
    FAVORITES
}

data class LibrarySummary(val total: Int, val inProgress: Int)
sealed interface LogoutState {
    data object Idle : LogoutState
    data object InProgress : LogoutState
    // Ada progres baca yang gagal dikirim; minta konfirmasi sebelum dihapus
    data class UnsyncedProgress(val count: Int) : LogoutState
    data object Done : LogoutState
}
