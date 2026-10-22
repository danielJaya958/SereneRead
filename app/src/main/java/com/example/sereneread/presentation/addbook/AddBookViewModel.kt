package com.example.sereneread.presentation.addbook

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.sereneread.domain.model.UploadStatus
import com.example.sereneread.domain.repository.BookRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import javax.inject.Inject

// State layar unggah. fileName dipakai agar pengguna tahu file mana yang sedang diproses.
sealed interface AddBookUiState {
    data object Idle : AddBookUiState
    // File sudah dipilih, pengguna mengisi judul/penulis/tahun sebelum upload
    data class Details(val fileName: String, val form: BookFormInput) : AddBookUiState
    data class Preparing(val fileName: String) : AddBookUiState
    data class Uploading(val fileName: String, val percent: Int) : AddBookUiState
    data class Finishing(val fileName: String) : AddBookUiState
    data class Done(val fileName: String) : AddBookUiState
    data class Failed(val fileName: String, val message: String) : AddBookUiState
}

@HiltViewModel
class AddBookViewModel @Inject constructor(
    private val repository: BookRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableLiveData<AddBookUiState>(AddBookUiState.Idle)
    val uiState: LiveData<AddBookUiState> = _uiState

    // Disimpan untuk tombol "Try again" dan "Edit details"
    private var lastUri: Uri? = null
    private var lastFileName = ""
    private var lastInput: BookFormInput? = null

    // State Details yang isinya sudah dimasukkan ke form (lihat AddBookBottomSheet.render)
    var formFilledFor: AddBookUiState? = null

    val isBusy: Boolean
        get() = _uiState.value.let {
            it is AddBookUiState.Preparing || it is AddBookUiState.Uploading || it is AddBookUiState.Finishing
        }

    // 1. File dipilih: tampilkan form dengan judul usulan dari nama file
    fun onFilePicked(uri: Uri) {
        if (isBusy) return
        lastUri = uri
        lastInput = null
        viewModelScope.launch {
            val fileName = withContext(Dispatchers.IO) { getFileName(uri) } ?: "Untitled.pdf"
            lastFileName = fileName
            _uiState.value = AddBookUiState.Details(fileName, BookFormInput(suggestTitle(fileName), null, null))
        }
    }

    // 2. Pengguna menekan Upload setelah mengisi form
    fun upload(input: BookFormInput) {
        val uri = lastUri ?: return
        if (isBusy) return
        lastInput = input
        val fileName = lastFileName

        viewModelScope.launch {
            _uiState.value = AddBookUiState.Preparing(fileName)

            // Salin Uri ke file di cache (di thread IO, supaya PDF besar tidak membekukan layar)
            val tempFile = try {
                withContext(Dispatchers.IO) { copyToCache(uri) }
            } catch (e: Exception) {
                _uiState.value = AddBookUiState.Failed(fileName, "Couldn't read the file: ${e.message}")
                return@launch
            }

            // Backend mewajibkan penulis dan tahun; yang dikosongkan diisi nilai bawaan
            // dan bisa diubah nanti lewat Edit di halaman detail
            val year = input.year ?: Calendar.getInstance().get(Calendar.YEAR)
            repository.uploadBook(input.title, input.author ?: "Unknown Author", year, tempFile).collect { status ->
                _uiState.value = when (status) {
                    is UploadStatus.Sending -> AddBookUiState.Uploading(fileName, status.percent)
                    UploadStatus.Finishing -> AddBookUiState.Finishing(fileName)
                    UploadStatus.Success -> AddBookUiState.Done(fileName)
                    is UploadStatus.Failed -> AddBookUiState.Failed(fileName, status.message)
                }
            }
            tempFile.delete()
        }
    }

    fun retry() {
        lastInput?.let { upload(it) }
    }

    // Kembali ke form dengan isian terakhir (misal setelah server menolak judul)
    fun editDetails() {
        val form = lastInput ?: BookFormInput(suggestTitle(lastFileName), null, null)
        _uiState.value = AddBookUiState.Details(lastFileName, form)
    }

    fun reset() {
        _uiState.value = AddBookUiState.Idle
    }

    // "_OceanofPDF.com_Saha_Mansion.pdf" -> "OceanofPDF.com Saha Mansion"
    private fun suggestTitle(fileName: String): String =
        fileName.replace(Regex("\\.pdf$", RegexOption.IGNORE_CASE), "")
            .replace('_', ' ')
            .replace(Regex("\\s+"), " ")
            .trim()

    private fun copyToCache(uri: Uri): File {
        val tempFile = File(context.cacheDir, "upload_temp.pdf")
        val input = context.contentResolver.openInputStream(uri)
            ?: throw IllegalStateException("File tidak bisa dibuka")
        input.use { inputStream ->
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        return tempFile
    }

    // Fungsi utilitas untuk mengekstrak nama file asli dari Uri
    private fun getFileName(uri: Uri): String? {
        if (uri.scheme != "content") return uri.lastPathSegment
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) return cursor.getString(index)
            }
        }
        return null
    }
}
