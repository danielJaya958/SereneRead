package com.example.sereneread.presentation.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.sereneread.R
import com.example.sereneread.databinding.FragmentDetailBinding
import com.example.sereneread.domain.model.Book
import com.example.sereneread.presentation.home.bind
import com.example.sereneread.presentation.home.displayTitle
import com.example.sereneread.presentation.home.progressLabel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    // bookId dibaca ViewModel dari argumen navigasi (SavedStateHandle)
    private val viewModel: DetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (viewModel.bookId == 0L) {
            Toast.makeText(requireContext(), "Buku tidak ditemukan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
            return
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                DetailState.Loading -> {
                    binding.progressLoading.isVisible = true
                    binding.scrollContent.visibility = View.INVISIBLE
                }
                is DetailState.Content -> {
                    binding.progressLoading.isVisible = false
                    binding.scrollContent.visibility = View.VISIBLE
                    bindBookData(state.book)
                }
                is DetailState.Gone -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    // Cegah navigasi ganda jika state terkirim ulang
                    if (findNavController().currentDestination?.id == R.id.detailFragment) {
                        findNavController().navigateUp()
                    }
                }
            }
        }

        viewModel.message.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                viewModel.messageShown()
            }
        }

        viewModel.deleting.observe(viewLifecycleOwner) { deleting ->
            binding.btnDelete.isEnabled = !deleting && viewModel.currentBook != null
            binding.btnEdit.isEnabled = !deleting && viewModel.currentBook != null
            binding.btnRead.isEnabled = !deleting
        }
    }

    private fun bindBookData(book: Book) {
        binding.apply {
            cover.bind(book)
            tvBookTitle.text = book.displayTitle()
            tvBookAuthor.text = book.author

            // "2024 · 252 pages", hanya bagian yang diketahui
            tvBookMeta.text = listOfNotNull(
                book.publishedYear.takeIf { it > 0 }?.toString(),
                book.totalPages.takeIf { it > 0 }?.let { if (it == 1) "1 page" else "$it pages" }
            ).joinToString(" · ")
            tvBookMeta.isVisible = tvBookMeta.text.isNotEmpty()

            when {
                book.isFinished -> {
                    tvReadingStatus.text = "Finished"
                    progressReading.isVisible = true
                    progressReading.progress = 100
                    btnRead.text = "Read again"
                }
                book.isStarted -> {
                    tvReadingStatus.text = book.progressLabel()
                    progressReading.isVisible = book.totalPages > 0
                    progressReading.progress = book.progressPercentage
                    btnRead.text = "Continue reading"
                }
                else -> {
                    tvReadingStatus.text = "Not started yet"
                    progressReading.isVisible = false
                    btnRead.text = "Start reading"
                }
            }

            btnFavorite.setIconResource(
                if (book.isFavorite) R.drawable.ic_favorite else R.drawable.ic_favorite_border
            )
            btnFavorite.contentDescription =
                if (book.isFavorite) "Remove from favorites" else "Add to favorites"

            val deleting = viewModel.deleting.value == true
            btnEdit.isEnabled = !deleting
            btnDelete.isEnabled = !deleting
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.btnEdit.setOnClickListener {
            val bundle = Bundle().apply { putLong("bookId", viewModel.bookId) }
            findNavController().navigate(R.id.editBookBottomSheet, bundle)
        }

        binding.btnDelete.setOnClickListener {
            val book = viewModel.currentBook ?: return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete “${book.displayTitle()}”?")
                .setMessage("The PDF, your reading progress, and favorite status will be removed. This can't be undone.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete") { _, _ -> viewModel.deleteBook() }
                .show()
        }

        binding.btnRead.setOnClickListener {
            // BYPASS SAFE ARGS: Tembak langsung ke ID layarnya (readerFragment)
            val bundle = Bundle().apply { putLong("bookId", viewModel.bookId) }
            findNavController().navigate(R.id.readerFragment, bundle)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
