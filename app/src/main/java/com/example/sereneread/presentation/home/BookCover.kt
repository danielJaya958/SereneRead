package com.example.sereneread.presentation.home

import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import coil.load
import com.example.sereneread.R
import com.example.sereneread.databinding.ViewBookCoverBinding
import com.example.sereneread.domain.model.Book

// Warna sampul tipografi, dipilih tetap per buku supaya tidak berganti setiap kali list di-refresh
private val coverTones = intArrayOf(R.color.cover_tone_1, R.color.cover_tone_2, R.color.cover_tone_3)

// Nama file hasil upload sering memakai garis bawah ("Saha_Mansion_Edition"), tampilkan sebagai spasi
fun Book.displayTitle(): String =
    title.replace('_', ' ').replace(Regex("\\s+"), " ").trim().ifEmpty { title }

// Backend mengirim path folder kosong ("/uploads/thumbnails/") jika sampul belum/tidak dibuat
fun Book.hasCoverImage(): Boolean = !coverUrl.isNullOrEmpty() && !coverUrl.endsWith("/")

fun ViewBookCoverBinding.bind(book: Book, compact: Boolean = false) {
    val context = root.context
    tvCoverTitle.text = book.displayTitle()
    // Sampul kecil (kartu "Continue reading") butuh teks lebih kecil agar kata tidak terpotong per huruf
    tvCoverTitle.textSize = if (compact) 9f else 13f
    val padding = ((if (compact) 6 else 10) * context.resources.displayMetrics.density).toInt()
    tvCoverTitle.setPadding(padding, padding, padding, padding)
    tvCoverTitle.setBackgroundColor(
        ContextCompat.getColor(context, coverTones[(book.id % coverTones.size).toInt()])
    )

    val coverUrl = book.coverUrl
    if (!book.hasCoverImage() || coverUrl == null) {
        ivCover.isVisible = false
        ivCover.setImageDrawable(null)
        return
    }

    ivCover.isVisible = true
    ivCover.load(coverUrl) {
        crossfade(true)
        listener(onError = { _, _ -> ivCover.isVisible = false })
    }
}
