package com.example.sereneread.data.remote

import com.google.gson.Gson
import retrofit2.HttpException
import java.io.IOException

// Ambil pesan dari body error backend ({"status":false,"message":"..."}),
// supaya pengguna melihat alasan sebenarnya, misal "Judul buku tidak boleh kosong"
fun Throwable.toUserMessage(): String = when (this) {
    is HttpException -> {
        val serverMessage = try {
            response()?.errorBody()?.string()?.let {
                Gson().fromJson(it, BaseResponse::class.java)?.message
            }
        } catch (e: Exception) {
            null
        }
        serverMessage?.takeIf { it.isNotBlank() } ?: "Server error (${code()})"
    }
    is IOException -> "No internet connection. Check your connection and try again."
    else -> message ?: "Something went wrong"
}
