package com.example.sereneread.presentation.addbook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.sereneread.databinding.BottomSheetAddBookBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class AddBookBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAddBookBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAddBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Tombol Close (X)
        binding.btnClose.setOnClickListener {
            dismiss() // Menutup bottom sheet
        }

        // 2. Klik Import File
        binding.cardImportFile.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka File Manager...", Toast.LENGTH_SHORT).show()
            // TODO: Buka Intent untuk memilih file PDF/EPUB
            dismiss()
        }

        // 3. Klik Scan ISBN
        binding.cardScanIsbn.setOnClickListener {
            Toast.makeText(requireContext(), "Membuka Kamera...", Toast.LENGTH_SHORT).show()
            // TODO: Buka Scanner Barcode
            dismiss()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}