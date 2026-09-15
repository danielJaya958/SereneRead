package com.example.sereneread.data.remote.dto

import com.google.gson.annotations.SerializedName

data class ProgressRequest(
    @SerializedName("last_read_page")
    val lastReadPage: Int
)
