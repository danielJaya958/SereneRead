package com.example.sereneread.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.example.sereneread.databinding.FragmentHomeBinding
import com.example.sereneread.domain.model.Book
import dagger.hilt.android.AndroidEntryPoint
// Pastikan import ini ada
import androidx.navigation.fragment.findNavController

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var bookAdapter: BookAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupObservers()
        setupUIInteractions()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { selectedBook ->
            navigateToDetail(selectedBook)
        }

        binding.rvBooks.apply {
            adapter = bookAdapter
            layoutManager = GridLayoutManager(requireContext(), 2)
        }
    }

    private fun setupObservers() {
        viewModel.books.observe(viewLifecycleOwner) { books ->
            bookAdapter.submitList(books)
            updateEmptyState(books.isEmpty())
        }
    }

    private fun setupUIInteractions() {
        binding.chipAll.setOnClickListener {
            viewModel.setFilter(BookFilter.ALL)
        }

        binding.chipFavorites.setOnClickListener {
            viewModel.setFilter(BookFilter.FAVORITES)
        }

        binding.fabAddBook.setOnClickListener {
            // Lupakan Safe Args, kita tembak langsung ke ID Bottom Sheet-nya!
            findNavController().navigate(com.example.sereneread.R.id.addBookBottomSheet)
        }

        binding.tvGreeting.text = "Good Morning, User"
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.layoutEmptyState.isVisible = isEmpty
        binding.rvBooks.isVisible = !isEmpty
    }

    private fun navigateToDetail(book: Book) {
        val bundle = android.os.Bundle().apply {
            putLong("bookId", book.id) // Pastikan ID ini terbaca
        }

        // KUNCI PERBAIKAN: Gunakan R.id.detailFragment (ID layarnya), BUKAN R.id.action_...
        findNavController().navigate(com.example.sereneread.R.id.detailFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}