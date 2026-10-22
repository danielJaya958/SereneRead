package com.example.sereneread.data.session

import android.content.Context
import androidx.work.WorkManager
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import com.example.sereneread.data.local.SereneDatabase
import com.example.sereneread.data.local.dao.BookDao
import com.example.sereneread.data.remote.SereneReadApi
import com.example.sereneread.data.remote.dto.ProgressRequest
import com.example.sereneread.data.remote.network.TokenManager
import com.example.sereneread.data.sync.ProgressSyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

// Membersihkan semua data milik akun di perangkat ini, supaya akun lain yang login
// di HP yang sama tidak melihat buku, progres, atau PDF milik akun sebelumnya.
@Singleton
class SessionCleaner @Inject constructor(
    @ApplicationContext private val context: Context,
    private val db: SereneDatabase,
    private val dao: BookDao,
    private val api: SereneReadApi,
    private val tokenManager: TokenManager
) {

    // Kirim progres yang belum tersinkron selagi token masih berlaku.
    // Mengembalikan jumlah buku yang progresnya tetap gagal terkirim.
    suspend fun syncPendingProgress(): Int {
        val pending = dao.getUnsyncedProgress()
        if (pending.isEmpty()) return 0

        // Batasi waktu agar logout tidak menggantung saat jaringan lambat
        withTimeoutOrNull(SYNC_TIMEOUT_MS) {
            pending.forEach { book ->
                try {
                    val response = api.updateReadingProgress(book.id, ProgressRequest(book.lastReadPosition))
                    if (response.status) dao.markProgressSynced(book.id, book.lastReadPosition)
                } catch (e: Exception) {
                    // Dihitung sebagai belum terkirim di bawah
                }
            }
        }
        return dao.getUnsyncedProgress().size
    }

    // Logout manual: hapus data lokal lalu token
    suspend fun logout() {
        clearLocalData()
        tokenManager.clearToken()
        tokenManager.clearAccount()
    }

    // Dipanggil setelah login berhasil. Data lokal hanya dihapus jika akunnya berbeda,
    // supaya progres yang belum terkirim tetap aman saat sesi habis lalu login ulang.
    suspend fun onLoginSuccess(email: String) {
        val normalized = email.trim().lowercase()
        val previous = tokenManager.getAccount()
        if (previous != null && previous != normalized) clearLocalData()
        tokenManager.saveAccount(normalized)
    }

    @OptIn(ExperimentalCoilApi::class)
    private suspend fun clearLocalData() = withContext(Dispatchers.IO) {
        // 1. Batalkan antrean sinkron progres milik akun ini
        WorkManager.getInstance(context).cancelAllWorkByTag(ProgressSyncWorker.TAG)

        // 2. Kosongkan database Room (buku, progres, status sinkron)
        db.clearAllTables()

        // 3. Hapus PDF yang sudah diunduh dan sisa file unggahan
        File(context.cacheDir, "books").deleteRecursively()
        File(context.cacheDir, "upload_temp.pdf").delete()

        // 4. Hapus cache sampul buku (memori dan disk)
        val imageLoader = context.imageLoader
        imageLoader.diskCache?.clear()
        withContext(Dispatchers.Main) { imageLoader.memoryCache?.clear() }
    }

    private companion object {
        const val SYNC_TIMEOUT_MS = 8_000L
    }
}
