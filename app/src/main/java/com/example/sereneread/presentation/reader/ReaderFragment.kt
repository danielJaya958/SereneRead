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
import androidx.pdf.viewer.fragment.PdfViewerFragment
import com.example.sereneread.R
import com.example.sereneread.databinding.FragmentReaderBinding
import com.example.sereneread.domain.util.Resource
import com.example.sereneread.domain.util.Constants
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

    private var pdfViewerFragment: PdfViewerFragment? = null

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

        pdfViewerFragment = childFragmentManager.findFragmentById(R.id.pdfFragmentContainer) as PdfViewerFragment?

        // 1. Ambil bookId dari Bundle (Bypass Safe Args)
        val bookId = arguments?.getLong("bookId") ?: 0L
        if (bookId != 0L) {
            viewModel.getBookContentUrl(bookId)
        } else {
            Toast.makeText(requireContext(), "Buku tidak ditemukan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        setupObservers()
    }

    private fun setupObservers() {
        viewModel.pdfUrlState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    binding.progressBar.isVisible = true
                }
                is Resource.Success -> {
                    val rawUrl = result.data ?: ""
                    if (rawUrl.isNotEmpty()) {
                        // Gabungkan dengan SERVER_URL jika url dari backend bersifat relatif
                        val fullUrl = if (rawUrl.startsWith("http")) rawUrl else "${Constants.SERVER_URL}$rawUrl"
                        downloadAndLoadPdf(fullUrl)
                    }
                }
                is Resource.Error -> {
                    binding.progressBar.isVisible = false
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            // 1. Ambil ID buku saat ini
            val currentBookId = arguments?.getLong("bookId") ?: 0L
            
            if (currentBookId != 0L) {
                // 2. TODO: Ganti angka 10 ini dengan variabel halaman asli dari PDF Viewer nanti
                val currentPage = 10 
                
                // 3. Simpan ke Golang!
                viewModel.saveProgress(currentBookId, currentPage)
            }
            
            // 4. Keluar dari halaman baca
            findNavController().navigateUp()
        }
        
        binding.bottomControls.isVisible = false
    }

    private fun downloadAndLoadPdf(urlString: String) {
        binding.progressBar.isVisible = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // 2. Gunakan OkHttpClient untuk mengunduh agar Token JWT otomatis terkirim
                val request = Request.Builder().url(urlString).build()
                val response = okHttpClient.newCall(request).execute()

                if (!response.isSuccessful) {
                    throw Exception("Gagal mengunduh, kode: ${response.code}")
                }

                val file = File(requireContext().cacheDir, "temp_book.pdf")
                
                response.body?.byteStream()?.use { input ->
                    file.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                withContext(Dispatchers.Main) {
                    binding.progressBar.isVisible = false
                    displayPdf(file)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    binding.progressBar.isVisible = false
                    Toast.makeText(context, "Gagal memuat PDF: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayPdf(file: File) {
        val uri = Uri.fromFile(file)
        pdfViewerFragment?.documentUri = uri
        binding.tvBookTitle.text = "Membaca Buku"
    }

    override fun onPause() {
        super.onPause()
        
        // Simpan progres saat layar mulai ditinggalkan atau disembunyikan
        val currentBookId = arguments?.getLong("bookId") ?: 0L
        if (currentBookId != 0L) {
            val currentPage = 10 // TODO: Ganti dengan halaman asli nanti
            viewModel.saveProgress(currentBookId, currentPage)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}