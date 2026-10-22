package com.example.sereneread.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.sereneread.data.local.dao.BookDao
import com.example.sereneread.data.remote.SereneReadApi
import com.example.sereneread.data.remote.dto.ProgressRequest
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import retrofit2.HttpException
import java.io.IOException
import java.util.concurrent.TimeUnit

// Mengirim progres baca ke backend Golang di background.
// WorkManager menjamin request tetap terkirim walau aplikasi ditutup atau sedang offline.
class ProgressSyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    // Hilt tidak bisa langsung menyuntik ke Worker tanpa hilt-work, jadi ambil lewat EntryPoint
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun api(): SereneReadApi
        fun bookDao(): BookDao
    }

    override suspend fun doWork(): Result {
        val bookId = inputData.getLong(KEY_BOOK_ID, 0L)
        val page = inputData.getInt(KEY_PAGE, -1)
        if (bookId == 0L || page < 0) return Result.failure()

        val entryPoint = EntryPointAccessors
            .fromApplication(applicationContext, WorkerEntryPoint::class.java)
        val api = entryPoint.api()

        return try {
            val response = api.updateReadingProgress(bookId, ProgressRequest(page))
            Log.d("ReadingProgress", "PATCH buku=$bookId halaman=$page status=${response.status} pesan=${response.message}")
            if (response.status) {
                entryPoint.bookDao().markProgressSynced(bookId, page)
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: HttpException) {
            // 4xx (misal token kadaluarsa / buku tidak ada) tidak akan sembuh dengan retry
            Log.w("ReadingProgress", "PATCH buku=$bookId gagal HTTP ${e.code()}")
            if (e.code() in 400..499) Result.failure() else Result.retry()
        } catch (e: IOException) {
            Result.retry()
        }
    }

    companion object {
        // Dipakai SessionCleaner untuk membatalkan semua antrean saat logout
        const val TAG = "progress_sync"

        private const val KEY_BOOK_ID = "book_id"
        private const val KEY_PAGE = "page"

        // Batalkan sinkron progres satu buku (misal bukunya dihapus)
        fun cancel(context: Context, bookId: Long) {
            WorkManager.getInstance(context).cancelUniqueWork(uniqueName(bookId))
        }

        private fun uniqueName(bookId: Long) = "progress_sync_$bookId"

        fun enqueue(context: Context, bookId: Long, page: Int) {
            val request = OneTimeWorkRequestBuilder<ProgressSyncWorker>()
                .setInputData(workDataOf(KEY_BOOK_ID to bookId, KEY_PAGE to page))
                .addTag(TAG)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()

            // REPLACE: cukup kirim halaman terbaru per buku, request lama yang belum terkirim dibuang
            WorkManager.getInstance(context).enqueueUniqueWork(
                uniqueName(bookId),
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
