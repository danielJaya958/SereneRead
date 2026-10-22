package com.example.sereneread.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sereneread.databinding.ItemBookBinding
import com.example.sereneread.domain.model.Book

class BookAdapter(
    private val onBookClick: (Book) -> Unit
) : ListAdapter<Book, BookAdapter.BookViewHolder>(BookDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val binding = ItemBookBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = getItem(position)
        holder.bind(book)
    }

    inner class BookViewHolder(private val binding: ItemBookBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(book: Book) {
            binding.apply {
                cover.bind(book)
                tvBookTitle.text = book.displayTitle()

                // Baris kedua: status selesai, progres, atau nama penulis jika belum dibaca
                val status = book.statusLabel()
                tvBookMeta.text = status ?: book.author

                progressBook.isVisible = book.isStarted && book.totalPages > 0
                progressBook.progress = if (book.isFinished) 100 else book.progressPercentage
                ivFavorite.isVisible = book.isFavorite

                root.contentDescription = listOfNotNull(
                    book.displayTitle(),
                    book.author,
                    status,
                    "favorite".takeIf { book.isFavorite }
                ).joinToString(", ")
                root.setOnClickListener {
                    onBookClick(book)
                }
            }
        }
    }

    // DiffUtil: Mesin cerdas yang membandingkan list lama vs list baru
    class BookDiffCallback : DiffUtil.ItemCallback<Book>() {
        override fun areItemsTheSame(oldItem: Book, newItem: Book): Boolean {
            // Cek apakah ID-nya sama (ini buku yang sama?)
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Book, newItem: Book): Boolean {
            // Cek apakah isinya ada yang berubah (misal progress nambah)?
            // Data Class di Kotlin otomatis punya fungsi equals() yang membandingkan semua properti
            return oldItem == newItem
        }
    }
}

// Total halaman baru diketahui setelah PDF pernah dibuka di perangkat ini
fun Book.progressLabel(): String =
    if (totalPages > 0) "Page $lastReadPosition of $totalPages" else "Page $lastReadPosition"

// "Finished", "Page X of Y", atau null jika belum mulai dibaca
fun Book.statusLabel(): String? = when {
    isFinished -> "Finished"
    isStarted -> progressLabel()
    else -> null
}
