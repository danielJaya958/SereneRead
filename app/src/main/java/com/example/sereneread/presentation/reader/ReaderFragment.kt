package com.example.sereneread.presentation.reader

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.sereneread.R
import com.example.sereneread.databinding.FragmentReaderBinding
import com.example.sereneread.domain.util.Resource
import com.example.sereneread.domain.util.Constants
import com.google.android.material.slider.Slider
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class ReaderFragment : Fragment() {

    private var _binding: FragmentReaderBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ReaderViewModel by viewModels()

    // Suntikkan OkHttpClient yang sudah terpasang AuthInterceptor
    @Inject
    lateinit var okHttpClient: OkHttpClient

    private var pdfViewerFragment: SereneReaderFragment? = null

    private val bookId: Long
        get() = arguments?.getLong("bookId") ?: 0L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReaderBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupUI()

        pdfViewerFragment = childFragmentManager.findFragmentById(R.id.pdfFragmentContainer) as SereneReaderFragment?
        pdfViewerFragment?.apply {
            onPageChanged = { page, total -> viewModel.onPageChanged(page, total) }
            onVisiblePageChanged = { page, total -> showPagePosition(page, total) }
            onImmersiveModeChanged = { immersive -> setControlsVisible(!immersive) }
            onDocumentReady = { total -> onDocumentReady(total) }
        }

        // 1. Ambil bookId dari Bundle (Bypass Safe Args)
        if (bookId != 0L) {
            viewModel.loadBook(bookId)
        } else {
            Toast.makeText(requireContext(), "Buku tidak ditemukan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        setupObservers()

        viewModel.bookTitle.observe(viewLifecycleOwner) { title ->
            binding.tvBookTitle.text = title
        }
    }

    private fun setupObservers() {
        viewModel.contentState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is Resource.Success -> {
                    val cachedFile = cachedPdfFile()
                    val rawUrl = result.data?.pdfUrl ?: ""
                    when {
                        // PDF sudah pernah diunduh, tidak perlu unduh ulang
                        cachedFile.exists() -> displayPdf(cachedFile)
                        rawUrl.isNotEmpty() -> {
                            // Gabungkan dengan SERVER_URL jika url dari backend bersifat relatif
                            val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "${Constants.SERVER_URL}$rawUrl"
                            downloadAndLoadPdf(fullUrl, cachedFile)
                        }
                        else -> {
                            binding.progressBar.isVisible = false
                            Toast.makeText(requireContext(), "File PDF tidak tersedia", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    val cachedFile = cachedPdfFile()
                    if (cachedFile.exists()) {
                        // Offline: tetap bisa membaca dari cache, progres disinkronkan nanti
                        Toast.makeText(requireContext(), "Mode offline", Toast.LENGTH_SHORT).show()
                        displayPdf(cachedFile)
                    } else {
                        Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            // Progres disimpan di onPause, yang otomatis terpanggil saat keluar dari layar ini
            findNavController().navigateUp()
        }

        // Bar bawah baru muncul setelah jumlah halaman diketahui
        binding.bottomControls.isVisible = false

        binding.btnSearch.setOnClickListener {
            // Kolom pencarian bawaan PdfViewerFragment; bar kita otomatis tersembunyi
            pdfViewerFragment?.isTextSearchActive = true
        }

        binding.pageSlider.addOnChangeListener { _, value, fromUser ->
            // Saat digeser, tampilkan halaman tujuan sebelum benar-benar lompat
            if (fromUser) {
                binding.tvPageNumber.text = pageLabel(value.toInt(), binding.pageSlider.valueTo.toInt())
            }
        }
        binding.pageSlider.addOnSliderTouchListener(object : Slider.OnSliderTouchListener {
            override fun onStartTrackingTouch(slider: Slider) {
                isDraggingSlider = true
            }

            override fun onStopTrackingTouch(slider: Slider) {
                isDraggingSlider = false
                pdfViewerFragment?.goToPage(slider.value.toInt())
            }
        })
    }

    private var isDraggingSlider = false

    private fun onDocumentReady(totalPages: Int) {
        binding.btnSearch.isEnabled = true

        // Slider butuh minimal 2 halaman (valueFrom harus lebih kecil dari valueTo)
        binding.pageSlider.isVisible = totalPages >= 2
        if (totalPages >= 2) {
            binding.pageSlider.valueTo = totalPages.toFloat()
        }
        showPagePosition(viewModel.currentPage.coerceIn(1, totalPages), totalPages)
        setControlsVisible(viewModel.controlsVisible)
    }

    private fun showPagePosition(page: Int, totalPages: Int) {
        // Jangan timpa angka saat pengguna sedang menggeser slider
        if (isDraggingSlider || _binding == null) return
        binding.tvPageNumber.text = pageLabel(page, totalPages)
        if (binding.pageSlider.isVisible && page in 1..totalPages) {
            binding.pageSlider.value = page.toFloat()
        }
    }

    private fun pageLabel(page: Int, totalPages: Int) = "Page $page of $totalPages"

    // Tap halaman atau menggulir ke bawah menyembunyikan kontrol supaya fokus membaca
    private fun setControlsVisible(visible: Boolean) {
        if (_binding == null) return
        viewModel.controlsVisible = visible
        val documentReady = (pdfViewerFragment?.totalPages ?: 0) > 0
        animateBar(binding.topControls, visible, fromTop = true)
        animateBar(binding.bottomControls, visible && documentReady, fromTop = false)
    }

    private fun animateBar(bar: View, show: Boolean, fromTop: Boolean) {
        val offset = if (fromTop) -bar.height.toFloat() else bar.height.toFloat()
        bar.animate().cancel()
        if (show) {
            if (!bar.isVisible) {
                bar.translationY = offset
                bar.alpha = 0f
                bar.isVisible = true
            }
            bar.animate().translationY(0f).alpha(1f).setDuration(180).start()
        } else if (bar.isVisible) {
            bar.animate().translationY(offset).alpha(0f).setDuration(180)
                .withEndAction { bar.isVisible = false }
                .start()
        }
    }

    // Satu file cache per buku, supaya tidak saling timpa dan tidak perlu unduh ulang
    private fun cachedPdfFile(): File {
        val dir = File(requireContext().cacheDir, "books").apply { mkdirs() }
        return File(dir, "book_$bookId.pdf")
    }

    private fun downloadAndLoadPdf(urlString: String, targetFile: File) {
        binding.progressBar.isVisible = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // 2. Gunakan OkHttpClient untuk mengunduh agar Token JWT otomatis terkirim
                    val request = Request.Builder().url(urlString).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("Gagal mengunduh, kode: ${response.code}")
                        }

                        // Tulis ke file sementara dulu, supaya unduhan yang terputus tidak dianggap cache valid
                        val partFile = File(targetFile.parentFile, "${targetFile.name}.part")
                        response.body?.byteStream()?.use { input ->
                            partFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        } ?: throw Exception("Respons kosong")

                        if (!partFile.renameTo(targetFile)) {
                            partFile.delete()
                            throw Exception("Gagal menyimpan file")
                        }
                    }
                }
                binding.progressBar.isVisible = false
                displayPdf(targetFile)
            } catch (e: Exception) {
                binding.progressBar.isVisible = false
                Toast.makeText(requireContext(), "Gagal memuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun displayPdf(file: File) {
        binding.progressBar.isVisible = false
        pdfViewerFragment?.openDocument(Uri.fromFile(file), viewModel.currentPage)
    }

    override fun onPause() {
        super.onPause()
        // Simpan progres saat layar ditinggalkan (tombol kembali, pindah aplikasi, layar mati)
        viewModel.saveProgress()
    }

    override fun onDestroyView() {
        pdfViewerFragment?.apply {
            onPageChanged = null
            onVisiblePageChanged = null
            onImmersiveModeChanged = null
            onDocumentReady = null
        }
        pdfViewerFragment = null
        super.onDestroyView()
        _binding = null
    }
}
