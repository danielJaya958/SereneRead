package com.example.sereneread.data.repository

import androidx.room.withTransaction
import com.example.sereneread.data.local.SereneDatabase
import com.example.sereneread.data.local.dao.BookDao
import com.example.sereneread.data.remote.BookDto
import com.example.sereneread.data.local.entity.BookEntity
import com.example.sereneread.data.mapper.toDomain
import com.example.sereneread.data.mapper.toEntity
import com.example.sereneread.data.remote.SereneReadApi
import android.content.Context
import com.example.sereneread.data.sync.ProgressSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.model.BookFormat
import com.example.sereneread.domain.repository.BookRepository
import com.example.sereneread.domain.util.Constants
import com.example.sereneread.domain.util.Resource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import com.example.sereneread.data.remote.network.ProgressRequestBody
import com.example.sereneread.data.remote.dto.FavoriteRequest
import com.example.sereneread.data.remote.dto.UpdateBookRequest
import com.example.sereneread.data.remote.toUserMessage
import retrofit2.HttpException
import java.io.File
import java.time.OffsetDateTime
import com.example.sereneread.domain.model.UploadStatus
import kotlinx.coroutines.flow.channelFlow
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import javax.inject.Inject

// Suntikkan BookDao DAN SereneReadApi sekaligus
class BookRepositoryImpl @Inject constructor(
    private val db: SereneDatabase, // Untuk withTransaction saat menggabungkan data server
    private val dao: BookDao,
    private val api: SereneReadApi,
    @ApplicationContext private val context: Context
) : BookRepository {

    override fun getBooks(): Flow<List<Book>> = flow {
        // 1. Coba ambil data terbaru dari server Golang
        try {
            refreshBooks()
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

    // Ambil daftar buku dari server dan simpan ke Room. Semua layar yang mengamati
    // dao.getAllBooks() (misal Home) otomatis ikut ter-update.
    override suspend fun refreshBooks() {
        val response = api.getAllBooks()

        // Jika sukses, ubah formatnya dan simpan ke Room DB
        if (response.status && response.data != null) {
            val booksFromApi = response.data.map { it.toBook() }

            // Baca data lokal sekali, gabungkan di memori, lalu simpan sekaligus.
            // Dalam satu transaksi: progres yang disimpan reader tidak bisa terselip di tengah.
            val removedIds = db.withTransaction {
                val localById = dao.getAllBooksOnce().associateBy { it.id }
                dao.insertBooks(booksFromApi.map { mergeWithLocal(it, localById[it.id]) })

                // Daftar dari server lengkap (tanpa pagination), jadi buku lokal yang tidak ada
                // di dalamnya sudah dihapus di server: buang juga dari perangkat
                val serverIds = booksFromApi.map { it.id }
                if (serverIds.isEmpty()) dao.deleteAll() else dao.deleteAllExcept(serverIds)
                localById.keys - serverIds.toSet()
            }
            removedIds.forEach { removeDownloadedFiles(it) }
        }
    }

    override fun getBookDetailApi(id: Long): Flow<Resource<Book>> = flow {
        emit(Resource.Loading())
        try {
            // Panggil API GET /books/{id}
            val response = api.getBookDetail(id)

            if (response.status && response.data != null) {
                val book = response.data.toBook()
                // Simpan/Update ke database lokal
                val merged = db.withTransaction {
                    mergeWithLocal(book, dao.getBookById(id)).also { dao.insertBook(it) }
                }
                emit(Resource.Success(merged.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Gagal mengambil detail buku"))
            }
        } catch (e: HttpException) {
            // 404: buku sudah dihapus (mungkin dari perangkat lain), buang juga dari perangkat ini
            if (e.code() == 404) {
                removeLocalBook(id)
                emit(Resource.Error("This book is no longer in your library"))
            } else {
                val localBook = dao.getBookById(id)?.toDomain()
                emit(if (localBook != null) Resource.Success(localBook) else Resource.Error(e.toUserMessage()))
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
    ): Flow<UploadStatus> = channelFlow {
        send(UploadStatus.Sending(0))
        try {
            // 1. Bungkus File fisik menjadi RequestBody berformat PDF
            val requestFile = pdfFile.asRequestBody("application/pdf".toMediaTypeOrNull())

            // 2. Susun multipart (nama part harus "pdf_file" sesuai Golang Anda)
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("title", title)
                .addFormDataPart("author", author)
                .addFormDataPart("published_year", publishedYear.toString())
                .addFormDataPart("pdf_file", pdfFile.name, requestFile)
                .build()

            // 3. Bungkus agar progres terkirim bisa dilaporkan (dipanggil dari thread OkHttp)
            val body = ProgressRequestBody(multipart) { percent ->
                trySend(UploadStatus.Sending(percent))
            }

            // 4. Tembak ke API
            val response = api.uploadBook(body)

            if (response.status) {
                // 5. Simpan buku dari response ke Room supaya langsung muncul di Home
                //    (tanpa GET /books tambahan). Sampul menyusul lewat auto-refresh di Home.
                send(UploadStatus.Finishing)
                val created = response.data
                if (created != null) {
                    dao.insertBook(mergeWithLocal(created.toBook(), null))
                } else {
                    try {
                        refreshBooks()
                    } catch (e: Exception) {
                        // Unggah sudah berhasil; daftar akan ter-update saat refresh berikutnya
                    }
                }
                send(UploadStatus.Success)
            } else {
                send(UploadStatus.Failed(response.message ?: "Gagal mengunggah buku"))
            }
        } catch (e: Exception) {
            send(UploadStatus.Failed(e.toUserMessage()))
        }
    }
    
    override suspend fun saveReadingProgress(bookId: Long, lastReadPage: Int, totalPages: Int) {
        // 1. Simpan ke Room dulu supaya progres aman walau offline
        dao.updateProgress(bookId, lastReadPage, totalPages)
        // 2. Serahkan pengiriman ke Golang pada WorkManager (otomatis retry saat online)
        ProgressSyncWorker.enqueue(context, bookId, lastReadPage)
    }

    override suspend fun getBookById(id: Long): Book? {
        return dao.getBookById(id)?.toDomain()
    }

    override suspend fun insertBook(book: Book) {
        dao.insertBook(keepSyncState(book))
    }

    override suspend fun deleteBook(book: Book) {
        dao.deleteBook(book.toEntity())
    }

    override suspend fun updateBookProgress(book: Book) {
        dao.updateBook(keepSyncState(book))
    }

    override fun observeBook(id: Long): Flow<Book?> =
        dao.observeBook(id).map { it?.toDomain() }

    override suspend fun updateBookInfo(
        id: Long,
        title: String?,
        author: String?,
        publishedYear: Int?
    ): Resource<Book> = try {
        val response = api.updateBook(id, UpdateBookRequest(title, author, publishedYear))
        val updated = response.data
        if (response.status && updated != null) {
            // Simpan hasil dari server ke Room; Home dan Detail otomatis ikut berubah
            val merged = db.withTransaction {
                mergeWithLocal(updated.toBook(), dao.getBookById(id)).also { dao.insertBook(it) }
            }
            Resource.Success(merged.toDomain())
        } else {
            Resource.Error(response.message.orEmpty().ifBlank { "Couldn't save changes" })
        }
    } catch (e: Exception) {
        Resource.Error(e.toUserMessage())
    }

    override suspend fun removeBook(id: Long): Resource<Unit> = try {
        api.deleteBook(id)
        removeLocalBook(id)
        Resource.Success(Unit)
    } catch (e: HttpException) {
        if (e.code() == 404) {
            // Sudah terhapus di server: cukup bersihkan dari perangkat
            removeLocalBook(id)
            Resource.Success(Unit)
        } else {
            Resource.Error(e.toUserMessage())
        }
    } catch (e: Exception) {
        Resource.Error(e.toUserMessage())
    }

    override suspend fun setFavorite(id: Long, isFavorite: Boolean): Resource<Unit> {
        // Ubah dulu di Room supaya ikon langsung berganti, kembalikan jika server menolak
        dao.updateFavorite(id, isFavorite)
        return try {
            val response = api.setFavorite(id, FavoriteRequest(isFavorite))
            if (response.status) {
                Resource.Success(Unit)
            } else {
                dao.updateFavorite(id, !isFavorite)
                Resource.Error(response.message.orEmpty().ifBlank { "Couldn't update favorite" })
            }
        } catch (e: Exception) {
            dao.updateFavorite(id, !isFavorite)
            Resource.Error(e.toUserMessage())
        }
    }

    // Hapus buku dari perangkat: data Room, PDF yang diunduh, dan antrean sinkron progresnya
    private suspend fun removeLocalBook(id: Long) {
        ProgressSyncWorker.cancel(context, id)
        dao.deleteById(id)
        removeDownloadedFiles(id)
    }

    // Nama file harus sama dengan cache di ReaderFragment.cachedPdfFile()
    private fun removeDownloadedFiles(id: Long) {
        File(File(context.cacheDir, "books"), "book_$id.pdf").delete()
    }

    // finished_at dari Go berformat ISO-8601 dengan zona waktu, misal "2026-10-22T13:50:05.17599+08:00"
    private fun parseServerTime(value: String): Long? = try {
        OffsetDateTime.parse(value).toInstant().toEpochMilli()
    } catch (e: Exception) {
        null
    }

    // Ubah DTO dari Golang menjadi model domain (dipakai oleh list dan detail)
    private fun BookDto.toBook(): Book = Book(
        id = id,
        title = title,
        author = author,
        // Gabungkan Server URL dengan path gambar dari Golang
        coverUrl = if (!thumbnailUrl.isNullOrEmpty()) "${Constants.SERVER_URL}$thumbnailUrl" else "",
        description = "",
        genre = "General",
        rating = 0f,
        isFavorite = isFavorite ?: false,
        filePath = if (!pdfUrl.isNullOrEmpty()) "${Constants.SERVER_URL}$pdfUrl" else "",
        format = BookFormat.PDF,
        lastReadPosition = lastReadPage ?: 0,
        totalPages = totalPages ?: 0,
        publishedYear = publishedYear,
        finishedAt = finishedAt?.let { parseServerTime(it) }
    )

    // Data dari server bisa lebih lama dari progres lokal yang belum terkirim.
    // Jika progres lokal belum tersinkron, pertahankan halaman lokal agar tidak tertimpa.
    private fun mergeWithLocal(serverBook: Book, local: BookEntity?): BookEntity {
        val entity = serverBook.toEntity().copy(
            // Server mengirim total_pages; jika kosong (0), pakai nilai yang tercatat saat PDF dibuka
            totalPages = serverBook.totalPages.takeIf { it > 0 } ?: local?.totalPages ?: 0,
            // Response list tidak berisi pdf_url, jangan hapus path yang sudah diketahui
            filePath = serverBook.filePath.ifEmpty { local?.filePath.orEmpty() },
            // Pertahankan tanggal tambah lokal supaya urutan buku tidak berubah setiap refresh
            dateAdded = local?.dateAdded ?: serverBook.dateAdded,
            // Server tidak menyimpan kapan terakhir dibaca, jadi pertahankan nilai lokal
            lastReadAt = local?.lastReadAt ?: 0
        )
        return if (local != null && !local.progressSynced) {
            entity.copy(lastReadPosition = local.lastReadPosition, progressSynced = false)
        } else {
            entity
        }
    }

    // Update lokal (misal toggle favorit) tidak boleh menghapus tanda "belum tersinkron"
    private suspend fun keepSyncState(book: Book): BookEntity {
        val synced = dao.getBookById(book.id)?.progressSynced ?: true
        return book.toEntity().copy(progressSynced = synced)
    }
}
