package com.example.sereneread.presentation.detail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.sereneread.R
import com.example.sereneread.databinding.FragmentDetailBinding
import com.example.sereneread.domain.model.Book
import com.example.sereneread.domain.util.Resource
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DetailFragment : Fragment() {

    private var _binding: FragmentDetailBinding? = null
    private val binding get() = _binding!!

    private val viewModel: DetailViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Ambil bookId dari Bundle yang dikirim oleh HomeFragment
        val bookId = arguments?.getLong("bookId") ?: 0L

        if (bookId != 0L) {
            viewModel.loadBook(bookId)
        } else {
            Toast.makeText(requireContext(), "Buku tidak ditemukan", Toast.LENGTH_SHORT).show()
            findNavController().navigateUp()
        }

        setupObservers()
        setupListeners()
    }

    private fun setupObservers() {
        viewModel.bookState.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Resource.Loading -> {
                    // Bisa tambahkan animasi loading atau sembunyikan UI sementara
                    binding.btnRead.text = "Loading..."
                    binding.btnRead.isEnabled = false
                }
                is Resource.Success -> {
                    binding.btnRead.text = "Start Reading"
                    binding.btnRead.isEnabled = true
                    result.data?.let { bindBookData(it) }
                }
                is Resource.Error -> {
                    binding.btnRead.text = "Error"
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bindBookData(book: Book) {
        binding.apply {
            tvBookTitle.text = book.title
            tvBookAuthor.text = book.author
            tvDescription.text = book.description
            tvRating.text = book.rating.toString()
            tvGenre.text = book.genre

            if (!book.coverUrl.isNullOrEmpty()) {
                ivBookCover.load(book.coverUrl) {
                    crossfade(true)
                    placeholder(R.drawable.ic_book_placeholder)
                    transformations(RoundedCornersTransformation(0f)) // Sesuaikan radius jika perlu
                }
            }
            updateFavoriteIcon(book.isFavorite)
        }
    }

    private fun updateFavoriteIcon(isFavorite: Boolean) {
        if (isFavorite) {
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite)
        } else {
            binding.btnFavorite.setIconResource(R.drawable.ic_favorite_border)
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnFavorite.setOnClickListener {
            viewModel.toggleFavorite()
        }

        binding.btnRead.setOnClickListener {
            val currentBook = viewModel.currentBook
            if (currentBook != null) {
                // 1. Kita hapus syarat pengecekan filePath!
                // Cukup pastikan bukunya ada, lalu kirim ID-nya ke ReaderFragment.
                val bundle = android.os.Bundle().apply {
                    putLong("bookId", currentBook.id)
                }

                // 2. BYPASS SAFE ARGS: Tembak langsung ke ID layarnya (readerFragment)
                // agar tidak terjadi crash "Destination id == 0" seperti sebelumnya
                findNavController().navigate(com.example.sereneread.R.id.readerFragment, bundle)
            } else {
                Toast.makeText(requireContext(), "Data buku belum siap", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}