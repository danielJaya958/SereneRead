package com.example.sereneread.presentation.reader

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.pdf.viewer.fragment.PdfViewerFragment // Import Library Baru
import com.example.sereneread.R
import com.example.sereneread.databinding.FragmentReaderBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL

@AndroidEntryPoint
class ReaderFragment : Fragment() { // Hapus implementasi OnPageChangeListener

    private var _binding: FragmentReaderBinding? = null
    private val binding get() = _binding!!

    private val args: ReaderFragmentArgs by navArgs()
    private var isControlsVisible = false

    // Variabel untuk menampung PDF Viewer dari AndroidX
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

        // Kaitkan variabel dengan fragment yang ada di XML
        pdfViewerFragment = childFragmentManager.findFragmentById(R.id.pdfFragmentContainer) as PdfViewerFragment?

        // HACK: Download PDF dummy
        val dummyPdfUrl = "https://pdfobject.com/pdf/sample.pdf"
        downloadAndLoadPdf(dummyPdfUrl)
    }

    private fun setupUI() {
        binding.btnClose.setOnClickListener {
            findNavController().navigateUp()
        }

        // Sembunyikan footer untuk sementara karena library AndroidX saat ini
        // belum mengekspos API untuk membaca nomor halaman secara langsung.
        binding.bottomControls.isVisible = false
    }

    private fun toggleControls() {
        isControlsVisible = !isControlsVisible
        binding.topControls.isVisible = isControlsVisible
        // binding.bottomControls.isVisible = isControlsVisible
    }

    private fun downloadAndLoadPdf(urlString: String) {
        binding.progressBar.isVisible = true

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val file = File(requireContext().cacheDir, "temp_book.pdf")
                URL(urlString).openStream().use { input ->
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
                    Toast.makeText(context, "Gagal memuat buku: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun displayPdf(file: File) {
        // Konversi File lokal menjadi URI
        val uri = Uri.fromFile(file)

        // Serahkan URI ke PdfViewerFragment bawaan AndroidX
        pdfViewerFragment?.documentUri = uri

        binding.tvBookTitle.text = "Sample Book"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}