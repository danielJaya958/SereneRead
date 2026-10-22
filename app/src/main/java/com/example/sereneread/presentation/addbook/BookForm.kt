package com.example.sereneread.presentation.addbook

import androidx.core.view.isVisible
import com.example.sereneread.databinding.ViewBookFormBinding
import java.util.Calendar

// Isi form yang sudah divalidasi. author/year null = dikosongkan pengguna.
data class BookFormInput(val title: String, val author: String?, val year: Int?)

const val MIN_PUBLISHED_YEAR = 1000

fun ViewBookFormBinding.fill(title: String, author: String?, year: Int?) {
    etTitle.setText(title)
    etAuthor.setText(author.orEmpty())
    etYear.setText(year?.takeIf { it > 0 }?.toString().orEmpty())
}

// Validasi sama dengan backend (judul wajib, tahun 1000..tahun ini) supaya pengguna
// langsung tahu kesalahannya tanpa menunggu respons server. null = ada kolom yang salah.
fun ViewBookFormBinding.readValidated(): BookFormInput? {
    tilTitle.error = null
    tilYear.error = null
    showFormError(null)

    val title = etTitle.text?.toString()?.trim().orEmpty()
    val author = etAuthor.text?.toString()?.trim()?.ifEmpty { null }
    val yearText = etYear.text?.toString()?.trim().orEmpty()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val year = yearText.toIntOrNull()

    var valid = true
    if (title.isEmpty()) {
        tilTitle.error = "Enter a title"
        valid = false
    }
    if (yearText.isNotEmpty() && (year == null || year !in MIN_PUBLISHED_YEAR..currentYear)) {
        tilYear.error = "Use a year between $MIN_PUBLISHED_YEAR and $currentYear"
        valid = false
    }
    return if (valid) BookFormInput(title, author, year) else null
}

fun ViewBookFormBinding.showFormError(message: String?) {
    tvFormError.text = message
    tvFormError.isVisible = message != null
}

fun ViewBookFormBinding.setEnabled(enabled: Boolean) {
    tilTitle.isEnabled = enabled
    tilAuthor.isEnabled = enabled
    tilYear.isEnabled = enabled
}
