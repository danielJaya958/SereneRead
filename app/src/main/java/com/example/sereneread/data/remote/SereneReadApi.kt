package com.example.sereneread.data.remote

import com.example.sereneread.data.remote.dto.FavoriteRequest
import com.example.sereneread.data.remote.dto.ProgressRequest
import com.example.sereneread.data.remote.dto.UpdateBookRequest
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
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

    // Ambil URL PDF untuk membaca buku
    @GET("books/{booksId}/content")
    suspend fun getBookContent(
        @Path("booksId") bookId: Long
    ): BaseResponse<BookDto>
    // Kita bisa memakai BookDto karena di dalamnya sudah ada wadah untuk "pdf_url"
    
    // Body berupa multipart utuh (title, author, published_year, pdf_file) yang dibungkus
    // ProgressRequestBody, supaya progres unggah bisa ditampilkan
    @POST("books")
    suspend fun uploadBook(
        @Body body: okhttp3.RequestBody
    ): BaseResponse<BookDto> // 201: data buku baru (thumbnail_url masih "" sampai sampul selesai dibuat)

    // Ubah judul/penulis/tahun. 400 jika tidak valid, 403 jika bukan pemilik
    @PATCH("books/{id}")
    suspend fun updateBook(
        @Path("id") bookId: Long,
        @Body request: UpdateBookRequest
    ): BaseResponse<BookDto>

    // 403 jika bukan pemilik, 404 jika sudah terhapus
    @DELETE("books/{id}")
    suspend fun deleteBook(
        @Path("id") bookId: Long
    ): BaseResponse<Any>

    @PUT("books/{id}/favorite")
    suspend fun setFavorite(
        @Path("id") bookId: Long,
        @Body request: FavoriteRequest
    ): BaseResponse<BookDto>
    
    
    // Update progres baca (Pakai Any karena kita hanya butuh status & message)
    @PATCH("books/{id}/progress")
    suspend fun updateReadingProgress(
        @Path("id") bookId: Long,
        @Body request: ProgressRequest
    ): BaseResponse<Any>

}