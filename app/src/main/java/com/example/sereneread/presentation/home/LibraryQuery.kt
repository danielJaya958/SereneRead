package com.example.sereneread.presentation.home

import com.example.sereneread.domain.model.Book

// Filter, cari, dan urutan daftar buku di Home. Fungsi murni (tanpa ViewModel/LiveData)
// supaya mudah dites. query harus sudah di-trim; string kosong = tanpa pencarian.
fun filterAndSortBooks(
    books: List<Book>,
    filter: BookFilter,
    query: String,
    sort: SortOrder
): List<Book> {
    val filtered = when (filter) {
        BookFilter.ALL -> books
        BookFilter.IN_PROGRESS -> books.filter { it.isInProgress }
        BookFilter.FINISHED -> books.filter { it.isFinished }
        BookFilter.FAVORITES -> books.filter { it.isFavorite }
        BookFilter.NOT_STARTED -> books.filter { !it.isStarted }
    }

    // Cocokkan judul (yang sudah dirapikan) atau penulis, tanpa membedakan huruf besar/kecil
    val searched = if (query.isEmpty()) filtered else filtered.filter {
        it.displayTitle().contains(query, ignoreCase = true) ||
            it.author.contains(query, ignoreCase = true)
    }

    return when (sort) {
        // id dari server bertambah setiap upload, jadi id terbesar = paling baru (setara created_at)
        SortOrder.RECENTLY_ADDED -> searched.sortedByDescending { it.id }
        SortOrder.TITLE -> searched.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.displayTitle() })
        // Buku yang belum pernah dibuka di perangkat ini (lastReadAt = 0) di akhir, urut terbaru
        SortOrder.RECENTLY_READ -> searched.sortedWith(
            compareByDescending<Book> { it.lastReadAt }.thenByDescending { it.id }
        )
    }
}
