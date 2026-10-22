package com.example.sereneread.presentation.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.PopupMenu
import android.widget.Toast
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.NavOptions
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.sereneread.R
import com.example.sereneread.databinding.FragmentHomeBinding
import com.example.sereneread.domain.model.Book
import dagger.hilt.android.AndroidEntryPoint
// Pastikan import ini ada
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.Calendar

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
        setupLogoutButton()
    }

    private fun setupRecyclerView() {
        bookAdapter = BookAdapter { selectedBook ->
            navigateToDetail(selectedBook)
        }

        // 3 kolom di HP, bertambah di layar lebar supaya sampul tidak terlalu besar
        val widthDp = resources.configuration.screenWidthDp
        val spanCount = (widthDp / 120).coerceAtLeast(3)

        binding.rvBooks.apply {
            adapter = bookAdapter
            layoutManager = GridLayoutManager(requireContext(), spanCount)
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    // Kecilkan tombol saat menggulir ke bawah agar tidak menutupi sampul
                    if (dy > 0) binding.fabAddBook.shrink() else if (dy < 0) binding.fabAddBook.extend()
                }
            })
        }
    }

    // Setelah urutan diganti, mulai lagi dari atas daftar
    private var scrollToTopAfterUpdate = false

    private fun setupObservers() {
        viewModel.books.observe(viewLifecycleOwner) { books ->
            bookAdapter.submitList(books) {
                if (scrollToTopAfterUpdate) {
                    scrollToTopAfterUpdate = false
                    _binding?.rvBooks?.scrollToPosition(0)
                }
            }
            updateEmptyState(books.isEmpty())
        }

        viewModel.isRefreshing.observe(viewLifecycleOwner) { refreshing ->
            binding.swipeRefresh.isRefreshing = refreshing
        }

        viewModel.refreshError.observe(viewLifecycleOwner) { message ->
            if (message != null) {
                Toast.makeText(requireContext(), "Couldn't refresh. $message", Toast.LENGTH_LONG).show()
                viewModel.refreshMessageShown()
            }
        }

        viewModel.summary.observe(viewLifecycleOwner) { summary ->
            binding.tvSubtitle.text = when {
                summary.total == 0 -> "No books yet"
                summary.inProgress == 0 -> booksLabel(summary.total)
                else -> "${booksLabel(summary.total)} · ${summary.inProgress} in progress"
            }
        }

        viewModel.continueBook.observe(viewLifecycleOwner) { book ->
            bindContinueCard(book)
        }
    }

    private fun bindContinueCard(book: Book?) {
        binding.cardContinue.isVisible = book != null
        if (book == null) return

        binding.apply {
            continueCover.bind(book, compact = true)
            tvContinueTitle.text = book.displayTitle()
            tvContinueAuthor.text = book.author
            tvContinueProgress.text = book.progressLabel()
            progressContinue.isVisible = book.totalPages > 0
            progressContinue.progress = book.progressPercentage

            cardContinue.contentDescription = "Continue reading ${book.displayTitle()}, ${book.progressLabel()}"
            cardContinue.setOnClickListener { navigateToReader(book) }
        }
    }

    private fun setupUIInteractions() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val filter = when (checkedIds.firstOrNull()) {
                R.id.chipReading -> BookFilter.IN_PROGRESS
                R.id.chipUnread -> BookFilter.NOT_STARTED
                R.id.chipFinished -> BookFilter.FINISHED
                R.id.chipFavorites -> BookFilter.FAVORITES
                else -> BookFilter.ALL
            }
            viewModel.setFilter(filter)
        }

        binding.fabAddBook.setOnClickListener {
            // Lupakan Safe Args, kita tembak langsung ke ID Bottom Sheet-nya!
            findNavController().navigate(R.id.addBookBottomSheet)
        }

        binding.tvGreeting.text = greetingForNow()

        // Cari: hasil langsung berubah saat mengetik
        binding.etSearch.doAfterTextChanged { text ->
            viewModel.setSearchQuery(text?.toString().orEmpty())
        }
        binding.etSearch.setOnEditorActionListener { view, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                // Tutup keyboard agar hasil terlihat penuh
                ViewCompat.getWindowInsetsController(view)?.hide(WindowInsetsCompat.Type.ime())
                view.clearFocus()
                true
            } else {
                false
            }
        }

        binding.btnSort.setOnClickListener { showSortMenu(it) }

        binding.swipeRefresh.setColorSchemeResources(R.color.deep_teal)
        binding.swipeRefresh.setOnRefreshListener { viewModel.refresh() }
    }

    private fun showSortMenu(anchor: View) {
        val current = viewModel.sortOrder.value ?: SortOrder.RECENTLY_ADDED
        PopupMenu(requireContext(), anchor).apply {
            SortOrder.entries.forEachIndexed { index, order ->
                menu.add(SORT_GROUP, index, index, order.label).isChecked = order == current
            }
            menu.setGroupCheckable(SORT_GROUP, true, true)
            setOnMenuItemClickListener { item ->
                val selected = SortOrder.entries[item.itemId]
                if (selected != current) {
                    scrollToTopAfterUpdate = true
                    viewModel.setSortOrder(selected)
                }
                true
            }
            show()
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        // RecyclerView tetap tampil (kosong) supaya tarik-untuk-refresh tetap bisa dipakai
        binding.layoutEmptyState.isVisible = isEmpty
        if (!isEmpty) return

        // Pencarian tanpa hasil punya pesan sendiri
        val query = viewModel.searchQuery
        if (query.isNotEmpty()) {
            binding.tvEmptyTitle.text = "No books match “$query”"
            binding.tvEmptyBody.text = "Try another title or author, or clear the search."
            return
        }

        // Pesan kosong dibedakan: perpustakaan memang kosong, atau hanya filter yang tidak cocok
        val (title, body) = when (viewModel.currentFilter.value) {
            BookFilter.IN_PROGRESS -> "Nothing in progress" to
                "Books you open will show up here with your last page."
            BookFilter.NOT_STARTED -> "You've started every book" to
                "Add a new PDF to grow your reading list."
            BookFilter.FINISHED -> "No finished books yet" to
                "Books you read to the last page will show up here."
            BookFilter.FAVORITES -> "No favorites yet" to
                "Open a book and tap the heart to keep it here."
            else -> "Your library is empty" to
                "Tap Add book to upload your first PDF."
        }
        binding.tvEmptyTitle.text = title
        binding.tvEmptyBody.text = body
    }

    private fun booksLabel(count: Int) = if (count == 1) "1 book" else "$count books"

    private fun greetingForNow(): String =
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 4..10 -> "Good morning"
            in 11..14 -> "Good afternoon"
            in 15..18 -> "Good evening"
            else -> "Reading late tonight"
        }

    private fun navigateToDetail(book: Book) {
        val bundle = android.os.Bundle().apply {
            putLong("bookId", book.id) // Pastikan ID ini terbaca
        }

        // KUNCI PERBAIKAN: Gunakan R.id.detailFragment (ID layarnya), BUKAN R.id.action_...
        findNavController().navigate(R.id.detailFragment, bundle)
    }

    // Kartu "Continue reading" langsung membuka reader, tanpa lewat halaman detail
    private fun navigateToReader(book: Book) {
        val bundle = Bundle().apply { putLong("bookId", book.id) }
        findNavController().navigate(R.id.readerFragment, bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupLogoutButton() {
        binding.btnLogout.setOnClickListener {
            // 1. Minta konfirmasi, karena data di perangkat ini akan dihapus
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Log out?")
                .setMessage(
                    "Downloaded books and reading data on this phone will be removed. " +
                        "Your library stays on the server."
                )
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Log out") { _, _ -> viewModel.logout() }
                .show()
        }

        viewModel.logoutState.observe(viewLifecycleOwner) { state ->
            val inProgress = state == LogoutState.InProgress
            binding.btnLogout.isVisible = !inProgress
            binding.progressLogout.isVisible = inProgress

            when (state) {
                is LogoutState.UnsyncedProgress -> showUnsyncedProgressDialog(state.count)
                LogoutState.Done -> navigateToLogin()
                else -> Unit
            }
        }
    }

    // Progres baca gagal dikirim (misal offline). Beri pilihan: tunggu online, atau tetap logout.
    private fun showUnsyncedProgressDialog(count: Int) {
        val books = if (count == 1) "1 book" else "$count books"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reading progress not saved")
            .setMessage(
                "Progress for $books couldn't be sent to the server. " +
                    "Check your connection and try again, or log out and lose that progress."
            )
            .setCancelable(false)
            .setNegativeButton("Cancel") { _, _ -> viewModel.cancelLogout() }
            .setPositiveButton("Log out anyway") { _, _ -> viewModel.logout(force = true) }
            .show()
    }

    private fun navigateToLogin() {
        // Pindah ke Login dan hancurkan riwayat halaman Home
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.homeFragment, true)
            .build()

        findNavController().navigate(
            R.id.loginFragment,
            null,
            navOptions
        )
    }

    private companion object {
        const val SORT_GROUP = 1
    }
}
