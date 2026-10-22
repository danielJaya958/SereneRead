package com.example.sereneread.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProgressRequest(
    @SerializedName("last_read_page")
    val lastReadPage: Int
)

// PATCH /books/{id}: field null tidak ikut terkirim (Gson melewati null), jadi tidak diubah server
data class UpdateBookRequest(
    @SerializedName("title") val title: String? = null,
    @SerializedName("author") val author: String? = null,
    @SerializedName("published_year") val publishedYear: Int? = null
)

data class FavoriteRequest(
    @SerializedName("is_favorite") val isFavorite: Boolean
)
