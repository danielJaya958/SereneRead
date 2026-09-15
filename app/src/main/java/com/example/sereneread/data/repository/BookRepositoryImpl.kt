package com.example.sereneread.data.repository

import com.example.sereneread.data.local.dao.BookDao
import com.example.sereneread.data.mapper.toDomain
import com.example.sereneread.data.mapper.toEntity
import com.example.sereneread.data.remote.SereneReadApi
import com.example.sereneread.data.remote.dto.ProgressRequest
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.model.BookFormat
import com.example.sereneread.domain.repository.BookRepository
import com.example.sereneread.domain.util.Constants
import com.example.sereneread.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import javax.inject.Inject

// Suntikkan BookDao DAN SereneReadApi sekaligus
class BookRepositoryImpl @Inject constructor(
    private val dao: BookDao,
    private val api: SereneReadApi
) : BookRepository {

    override fun getBooks(): Flow<List<Book>> = flow {
        // 1. Coba ambil data terbaru dari server Golang
        try {
            val response = api.getAllBooks()

            // Jika sukses, ubah formatnya dan simpan ke Room DB
            if (response.status && response.data != null) {
                val booksFromApi = response.data.map { dto ->
                    Book(
                        id = dto.id,
                        title = dto.title,
                        author = dto.author,
                        // Gabungkan Server URL dengan path gambar dari Golang
                        coverUrl = if (!dto.thumbnailUrl.isNullOrEmpty()) "${Constants.SERVER_URL}${dto.thumbnailUrl}" else "",
                        description = "Tahun Terbit: ${dto.publishedYear}", // Data default
                        genre = "General",
                        rating = 0.0f,
                        isFavorite = false,
                        filePath = if (!dto.pdfUrl.isNullOrEmpty()) "${Constants.SERVER_URL}${dto.pdfUrl}" else "",
                        format = BookFormat.PDF // UBAH INI: Dari "PDF" menjadi BookFormat.PDF
                    )
                }

                // Simpan semua buku dari API ke dalam database lokal
                // (Menggunakan DAO yang sudah Anda buat)
                booksFromApi.forEach { book ->
                    dao.insertBook(book.toEntity())
                }
            }
        } catch (e: Exception) {
            // Jika gagal (misal tidak ada internet), abaikan errornya.
            // Aplikasi akan tetap berjalan menggunakan data yang sudah tersimpan di Room.
        }

        // 2. SELALU pantau dan kirim data dari Room Database ke UI (Single Source of Truth)
        emitAll(
            dao.getAllBooks().map { entities ->
                entities.map { it.toDomain() }
            }
        )
    }

    override fun getBookDetailApi(id: Long): Flow<Resource<Book>> = flow {
        emit(Resource.Loading())
        try {
            // Panggil API GET /books/{id}
            val response = api.getBookDetail(id)

            if (response.status && response.data != null) {
                val dto = response.data
                val book = Book(
                    id = dto.id,
                    title = dto.title,
                    author = dto.author,
                    coverUrl = if (!dto.thumbnailUrl.isNullOrEmpty()) "${Constants.SERVER_URL}${dto.thumbnailUrl}" else "",
                    description = "Tahun Terbit: ${dto.publishedYear}",
                    genre = "General",
                    rating = 0f,
                    isFavorite = false,
                    filePath = if (!dto.pdfUrl.isNullOrEmpty()) "${Constants.SERVER_URL}${dto.pdfUrl}" else "",
                    format = BookFormat.PDF,
                    // Opsional: Jika model Book Anda punya properti lastReadPage, bisa ditambahkan:
                    lastReadPosition = dto.lastReadPage ?: 0
                )
                // Simpan/Update ke database lokal
                dao.updateBook(book.toEntity())
                emit(Resource.Success(book))
            } else {
                emit(Resource.Error(response.message ?: "Gagal mengambil detail buku"))
            }
        } catch (e: Exception) {
            // Jika offline/gagal, coba ambil dari database lokal Room
            val localBook = dao.getBookById(id)?.toDomain()
            if (localBook != null) {
                emit(Resource.Success(localBook))
            } else {
                emit(Resource.Error("Terjadi kesalahan jaringan: ${e.message}"))
            }
        }
    }
    
    override fun uploadBook(
        title: String,
        author: String,
        publishedYear: Int,
        pdfFile: java.io.File
    ): Flow<Resource<Unit>> = flow {
        emit(Resource.Loading())
        try {
            // 1. Bungkus teks biasa menjadi RequestBody
            val titleBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, title)
            val authorBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, author)
            val yearBody = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, publishedYear.toString())

            // 2. Bungkus File fisik menjadi RequestBody berformat PDF
            val requestFile = okhttp3.RequestBody.create("application/pdf".toMediaTypeOrNull(), pdfFile)
            
            // 3. Masukkan RequestBody File ke dalam Multipart (nama part harus "pdf_file" sesuai Golang Anda)
            val pdfPart = okhttp3.MultipartBody.Part.createFormData("pdf_file", pdfFile.name, requestFile)

            // 4. Tembak ke API
            val response = api.uploadBook(titleBody, authorBody, yearBody, pdfPart)

            if (response.status) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "Gagal mengunggah buku"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Terjadi kesalahan jaringan: ${e.message}"))
        }
    }
    
    override fun saveReadingProgress(bookId: Long, lastReadPage: Int): Flow<Resource<Unit>> = flow {
        try {
            val response = api.updateReadingProgress(bookId, ProgressRequest(lastReadPage))
            if (response.status) {
                emit(Resource.Success(Unit))
            } else {
                emit(Resource.Error(response.message ?: "Gagal menyimpan progres"))
            }
        } catch (e: Exception) {
            emit(Resource.Error("Kesalahan jaringan: ${e.message}"))
        }
    }

    override suspend fun getBookById(id: Long): Book? {
        return dao.getBookById(id)?.toDomain()
    }

    override suspend fun insertBook(book: Book) {
        dao.insertBook(book.toEntity())
    }

    override suspend fun deleteBook(book: Book) {
        dao.deleteBook(book.toEntity())
    }

    override suspend fun updateBookProgress(book: Book) {
        dao.updateBook(book.toEntity())
    }
}