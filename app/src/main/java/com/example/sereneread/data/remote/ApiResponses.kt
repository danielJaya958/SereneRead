package com.example.sereneread.data.remote

import com.google.gson.annotations.SerializedName

// 1. Wrapper Generic untuk semua response (karena ada status, message, data)
data class BaseResponse<T>(
    @SerializedName("status") val status: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("data") val data: T? = null
)

// 2. DTO untuk Data Auth (Token)
data class AuthDataResponse(
    @SerializedName("token") val token: String
)

// 3. DTO untuk Data Buku
data class BookDto(
    @SerializedName("id") val id: Long,
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String,
    @SerializedName("published_year") val publishedYear: Int,
    @SerializedName("thumbnail_url") val thumbnailUrl: String?, // Bisa null
    @SerializedName("pdf_url") val pdfUrl: String?,             // Bisa null (saat get list)
    @SerializedName("last_read_page") val lastReadPage: Int?,   // Bisa null
    @SerializedName("total_pages") val totalPages: Int? = null, // Bisa null (endpoint lama)
    @SerializedName("is_favorite") val isFavorite: Boolean? = null,
    // Waktu selesai dibaca (ISO-8601, misal "2026-10-22T13:50:05.17599+08:00"), null = belum selesai
    @SerializedName("finished_at") val finishedAt: String? = null
)

// 4. DTO untuk Login Request
data class LoginRequest(
    @SerializedName("email") val email: String,
    @SerializedName("password") val password: String
)
