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

// Hasil validasi: input terisi jika valid, selain itu pesan error per kolom
data class BookFormValidation(
    val input: BookFormInput?,
    val titleError: String? = null,
    val yearError: String? = null
)

// Validasi sama dengan backend (judul wajib, tahun 1000..tahun ini) supaya pengguna
// langsung tahu kesalahannya tanpa menunggu respons server. Fungsi murni, mudah dites.
fun validateBookForm(
    titleText: String,
    authorText: String,
    yearText: String,
    currentYear: Int = Calendar.getInstance().get(Calendar.YEAR)
): BookFormValidation {
    val title = titleText.trim()
    val author = authorText.trim().ifEmpty { null }
    val yearTrimmed = yearText.trim()
    val year = yearTrimmed.toIntOrNull()

    val titleError = if (title.isEmpty()) "Enter a title" else null
    val yearError = if (yearTrimmed.isNotEmpty() && (year == null || year !in MIN_PUBLISHED_YEAR..currentYear)) {
        "Use a year between $MIN_PUBLISHED_YEAR and $currentYear"
    } else {
        null
    }

    val input = if (titleError == null && yearError == null) BookFormInput(title, author, year) else null
    return BookFormValidation(input, titleError, yearError)
}

// Baca form dan tampilkan error di kolomnya. null = ada kolom yang salah.
fun ViewBookFormBinding.readValidated(): BookFormInput? {
    showFormError(null)
    val result = validateBookForm(
        titleText = etTitle.text?.toString().orEmpty(),
        authorText = etAuthor.text?.toString().orEmpty(),
        yearText = etYear.text?.toString().orEmpty()
    )
    tilTitle.error = result.titleError
    tilYear.error = result.yearError
    return result.input
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

// Judul usulan dari nama file: "_OceanofPDF.com_Saha_Mansion.pdf" -> "OceanofPDF.com Saha Mansion"
fun suggestTitleFromFileName(fileName: String): String =
    fileName.replace(Regex("\\.pdf$", RegexOption.IGNORE_CASE), "")
        .replace('_', ' ')
        .replace(Regex("\\s+"), " ")
        .trim()
