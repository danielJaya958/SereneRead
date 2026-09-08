package com.example.sereneread.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface SereneReadApi {

    // 1. Login Endpoint
    @POST("login")
    suspend fun login(
        @Body request: LoginRequest
    ): BaseResponse<AuthDataResponse>

    // 2. Get List Books
    @GET("books")
    suspend fun getAllBooks(): BaseResponse<List<BookDto>>

    // 3. Get Book Detail
    @GET("books/{booksId}")
    suspend fun getBookDetail(
        @Path("booksId") bookId: Long
    ): BaseResponse<BookDto>

    // 4. Update Read Progress
    @PATCH("books/{booksId}/progress")
    suspend fun updateProgress(
        @Path("booksId") bookId: Long,
        @Body request: UpdateProgressRequest
    ): BaseResponse<Any> // Pakai Any karena kita hanya butuh status & message

    // Ambil URL PDF untuk membaca buku
    @GET("books/{booksId}/content")
    suspend fun getBookContent(
        @Path("booksId") bookId: Long
    ): BaseResponse<BookDto>
    // Kita bisa memakai BookDto karena di dalamnya sudah ada wadah untuk "pdf_url"


}