package com.example.sereneread.presentation.reader

import android.net.Uri
import android.os.Build
import android.util.SparseArray
import android.graphics.RectF
import androidx.annotation.RequiresExtension
import androidx.pdf.ExperimentalPdfApi
import androidx.pdf.view.PdfView
import androidx.pdf.viewer.fragment.PdfViewerFragment

// Turunan PdfViewerFragment agar kita bisa membaca halaman yang sedang tampil
// dan melompat ke halaman terakhir yang dibaca. Semua nomor halaman di sini dimulai dari 1.
@RequiresExtension(extension = Build.VERSION_CODES.S, version = 13)
class SereneReaderFragment : PdfViewerFragment() {

    // Dipanggil setiap halaman berubah karena pengguna menggulir (untuk menyimpan progres)
    var onPageChanged: ((page: Int, totalPages: Int) -> Unit)? = null

    // Dipanggil setiap halaman yang tampil berubah, termasuk saat dokumen dimuat (untuk UI)
    var onVisiblePageChanged: ((page: Int, totalPages: Int) -> Unit)? = null

    // true = sembunyikan kontrol (tap halaman atau menggulir ke bawah), false = tampilkan
    var onImmersiveModeChanged: ((immersive: Boolean) -> Unit)? = null

    // Dokumen selesai dimuat; pencarian teks baru bisa dipakai setelah ini
    var onDocumentReady: ((totalPages: Int) -> Unit)? = null

    private var readerPdfView: PdfView? = null
    private var pageToRestore = 0

    // Viewport juga berubah saat dokumen baru dimuat (selalu halaman 1) dan saat kita
    // melompat ke halaman terakhir. Abaikan sampai pengguna benar-benar menyentuh layar,
    // supaya progres di server tidak tertimpa halaman 1.
    private var userHasScrolled = false

    val totalPages: Int
        get() = readerPdfView?.pdfDocument?.pageCount ?: 0

    fun openDocument(uri: Uri, startPage: Int) {
        pageToRestore = startPage
        if (documentUri != uri) {
            userHasScrolled = false
            documentUri = uri
        }
    }

    // Lompat ke halaman karena aksi pengguna (slider), dihitung sebagai progres baca
    fun goToPage(page: Int) {
        val view = readerPdfView ?: return
        val total = totalPages
        if (total == 0) return
        userHasScrolled = true
        view.scrollToPage(page.coerceIn(1, total) - 1) // PdfView memakai index mulai 0
    }

    // Kontrol kita sendiri yang mengatur tampil/sembunyi. Tidak memanggil super karena
    // super menampilkan tombol edit/anotasi bawaan, sedangkan file ini hanya salinan cache.
    override fun onRequestImmersiveMode(enterImmersive: Boolean) {
        onImmersiveModeChanged?.invoke(enterImmersive)
    }

    @OptIn(ExperimentalPdfApi::class)
    override fun onPdfViewCreated(pdfView: PdfView) {
        super.onPdfViewCreated(pdfView)
        readerPdfView = pdfView

        pdfView.addOnGestureStateChangedListener(object : PdfView.OnGestureStateChangedListener {
            override fun onGestureStateChanged(newState: Int) {
                if (newState == PdfView.GESTURE_STATE_INTERACTING) userHasScrolled = true
            }
        })

        pdfView.addOnViewportChangedListener(object : PdfView.OnViewportChangedListener {
            override fun onViewportChanged(
                firstVisiblePage: Int,
                visiblePagesCount: Int,
                pageLocations: SparseArray<RectF>,
                zoomLevel: Float
            ) {
                val total = pdfView.pdfDocument?.pageCount ?: return
                val page = firstVisiblePage + 1
                onVisiblePageChanged?.invoke(page, total)
                if (userHasScrolled) onPageChanged?.invoke(page, total)
            }
        })
    }

    override fun onLoadDocumentSuccess() {
        super.onLoadDocumentSuccess()
        val view = readerPdfView ?: return
        val target = pageToRestore
        pageToRestore = 0

        // PdfViewerFragment memanggil callback ini SEBELUM pdfView.pdfDocument di-set,
        // jadi tunda sampai dokumen benar-benar terpasang di PdfView
        view.post {
            val total = view.pdfDocument?.pageCount ?: return@post
            onDocumentReady?.invoke(total)
            if (target >= 2) {
                view.scrollToPage(target.coerceAtMost(total) - 1) // PdfView memakai index mulai 0
            }
        }
    }

    override fun onDestroyView() {
        readerPdfView = null
        super.onDestroyView()
    }
}
