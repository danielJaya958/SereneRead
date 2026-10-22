package com.example.sereneread.presentation.addbook

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.sereneread.R
import com.example.sereneread.databinding.BottomSheetAddBookBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint // Penting agar Hilt bisa menyuntikkan ViewModel
class AddBookBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddBookBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddBookViewModel by viewModels()

    // 1. "Pancingan" untuk membuka File Manager (Hanya PDF)
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        // Batal memilih file: cukup kembali ke pilihan, tidak perlu pesan
        if (uri != null) viewModel.onFilePicked(uri)
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

        // Buka penuh supaya form dan tombol Upload tidak tertutup keyboard
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

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

        binding.btnRetry.setOnClickListener { viewModel.retry() }

        binding.btnEditDetails.setOnClickListener { viewModel.editDetails() }

        binding.btnUpload.setOnClickListener {
            binding.form.readValidated()?.let { viewModel.upload(it) }
        }

        binding.btnPickOther.setOnClickListener {
            viewModel.reset()
            filePickerLauncher.launch("application/pdf")
        }
    }

    private fun setupObservers() {
        viewModel.uiState.observe(viewLifecycleOwner) { state ->
            render(state)
        }
    }

    private fun render(state: AddBookUiState) = with(binding) {
        val showDetails = state is AddBookUiState.Details
        val showUpload = state !is AddBookUiState.Idle && !showDetails
        layoutOptions.isVisible = state is AddBookUiState.Idle
        layoutDetails.isVisible = showDetails
        layoutUpload.isVisible = showUpload

        // Selama unggah berjalan, sheet tidak bisa ditutup (geser atau tap di luar)
        // supaya pengguna tidak mengira prosesnya batal
        val busy = viewModel.isBusy
        isCancelable = !busy
        btnClose.isVisible = !busy

        tvUploadStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.ink_muted))
        layoutUploadActions.isVisible = false
        tvUploadHint.isVisible = busy

        when (state) {
            AddBookUiState.Idle -> {
                tvTitle.text = "Add to Library"
            }
            is AddBookUiState.Details -> {
                tvTitle.text = "Book details"
                tvDetailsFileName.text = state.fileName
                // Isi form sekali per state, supaya ketikan pengguna tidak tertimpa saat layar diputar
                if (viewModel.formFilledFor !== state) {
                    viewModel.formFilledFor = state
                    form.fill(state.form.title, state.form.author, state.form.year)
                }
            }
            is AddBookUiState.Preparing -> {
                tvTitle.text = "Uploading book"
                tvUploadFileName.text = state.fileName
                tvUploadStatus.text = "Preparing file…"
                progressUpload.isIndeterminate = true
            }
            is AddBookUiState.Uploading -> {
                tvTitle.text = "Uploading book"
                tvUploadFileName.text = state.fileName
                tvUploadStatus.text = "Uploading · ${state.percent}%"
                progressUpload.isIndeterminate = false
                progressUpload.setProgressCompat(state.percent, true)
            }
            is AddBookUiState.Finishing -> {
                // File sudah 100% terkirim, server sedang memproses dan daftar sedang diperbarui
                tvTitle.text = "Uploading book"
                tvUploadFileName.text = state.fileName
                tvUploadStatus.text = "Adding to your library…"
                progressUpload.isIndeterminate = true
            }
            is AddBookUiState.Done -> {
                tvTitle.text = "Added to your library"
                tvUploadFileName.text = state.fileName
                tvUploadStatus.text = "Upload complete"
                progressUpload.isIndeterminate = false
                progressUpload.setProgressCompat(100, true)
                // Beri waktu sebentar agar pengguna melihat konfirmasinya, lalu tutup.
                // Buku baru sudah ada di Room, jadi Home langsung menampilkannya.
                root.postDelayed({ if (isAdded) dismiss() }, 900)
            }
            is AddBookUiState.Failed -> {
                tvTitle.text = "Upload failed"
                tvUploadFileName.text = state.fileName
                tvUploadStatus.text = state.message
                tvUploadStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.error_red))
                progressUpload.isIndeterminate = false
                progressUpload.setProgressCompat(0, false)
                layoutUploadActions.isVisible = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
