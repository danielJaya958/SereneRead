package com.example.sereneread.domain.model

// Status unggah buku yang dilaporkan repository ke UI
sealed interface UploadStatus {
    // percent 0..100 = bagian file yang sudah terkirim
    data class Sending(val percent: Int) : UploadStatus

    // File sudah diterima server, sedang memperbarui daftar buku lokal
    data object Finishing : UploadStatus

    data object Success : UploadStatus

    data class Failed(val message: String) : UploadStatus
}
