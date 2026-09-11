package com.example.sereneread.presentation.addbook

import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.viewModels
import com.example.sereneread.databinding.BottomSheetAddBookBinding
import com.example.sereneread.domain.util.Resource
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint // Penting agar Hilt bisa menyuntikkan ViewModel
class AddBookBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddBookBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddBookViewModel by viewModels()

    // 1. "Pancingan" untuk membuka File Manager (Hanya PDF)
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            processSelectedFile(uri)
        } else {
            Toast.makeText(requireContext(), "Batal memilih file", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupObservers()

        binding.btnClose.setOnClickListener {
            dismiss()
        }

        binding.cardImportFile.setOnClickListener {
            // Meluncurkan File Manager!
            filePickerLauncher.launch("application/pdf")
        }

        binding.cardScanIsbn.setOnClickListener {
            Toast.makeText(requireContext(), "Fitur kamera belum tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        viewModel.uploadState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    Toast.makeText(requireContext(), "Mengunggah buku...", Toast.LENGTH_SHORT).show()
                    binding.cardImportFile.isEnabled = false // Kunci tombol agar tidak di-spam
                }
                is Resource.Success -> {
                    Toast.makeText(requireContext(), "Buku berhasil diunggah!", Toast.LENGTH_LONG).show()
                    dismiss() // Langsung tutup sheet jika sukses
                }
                is Resource.Error -> {
                    binding.cardImportFile.isEnabled = true
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun processSelectedFile(uri: Uri) {
        try {
            // 1. Dapatkan nama file asli untuk dijadikan Judul sementara
            val fileName = getFileName(uri) ?: "Unknown Book"
            val title = fileName.replace(".pdf", "", ignoreCase = true)

            // 2. Ubah Uri Android (Content Provider) menjadi File fisik di Cache agar bisa dikirim Retrofit
            val tempFile = File(requireContext().cacheDir, "upload_temp.pdf")
            requireContext().contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }

            // 3. Eksekusi pengunggahan! (Author & Tahun kita hardcode sementara)
            viewModel.uploadBook(
                title = title,
                author = "Unknown Author",
                year = 2026,
                file = tempFile
            )

        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal membaca file: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi utilitas untuk mengekstrak nama file asli dari Uri
    private fun getFileName(uri: Uri): String? {
        var result: String? = null
        if (uri.scheme == "content") {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            }
        }
        return result
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}