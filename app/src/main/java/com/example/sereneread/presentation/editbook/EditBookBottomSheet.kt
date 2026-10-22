package com.example.sereneread.presentation.editbook

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import com.example.sereneread.databinding.BottomSheetEditBookBinding
import com.example.sereneread.presentation.addbook.fill
import com.example.sereneread.presentation.addbook.readValidated
import com.example.sereneread.presentation.addbook.setEnabled
import com.example.sereneread.presentation.addbook.showFormError
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

// Ubah judul, penulis, dan tahun terbit (PATCH /books/{id}). Argumen: "bookId".
@AndroidEntryPoint
class EditBookBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetEditBookBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditBookViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = BottomSheetEditBookBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Buka penuh supaya tombol Save tidak tertutup keyboard
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        binding.btnClose.setOnClickListener { dismiss() }
        binding.btnSave.setOnClickListener {
            binding.form.readValidated()?.let { viewModel.save(it) }
        }

        viewModel.book.observe(viewLifecycleOwner) { book ->
            if (book != null && !viewModel.formFilled) {
                viewModel.formFilled = true
                binding.form.fill(book.title, book.author, book.publishedYear)
            }
        }

        viewModel.state.observe(viewLifecycleOwner) { state ->
            val saving = state == EditBookState.Saving
            binding.progressSave.isVisible = saving
            binding.btnSave.isEnabled = !saving
            binding.form.setEnabled(!saving)
            isCancelable = !saving

            when (state) {
                is EditBookState.Failed -> binding.form.showFormError(state.message)
                EditBookState.Saved -> dismiss()
                else -> Unit
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
