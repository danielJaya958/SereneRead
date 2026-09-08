package com.example.sereneread.presentation.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.load
import coil.transform.RoundedCornersTransformation
import com.example.sereneread.R
import com.example.sereneread.databinding.ItemBookBinding
import com.example.sereneread.domain.model.Book
import java.io.File

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
                tvBookTitle.text = book.title
                tvBookAuthor.text = book.author

                // Progress bar
                progressBar.progress = book.progressPercentage

                // REVISI: Load Image dari URL (Online)
                // Coil otomatis mendeteksi string ini sebagai URL HTTP/HTTPS
                if (!book.coverUrl.isNullOrEmpty()) {
                    ivBookCover.load(book.coverUrl) {
                        crossfade(true)
                        placeholder(R.drawable.ic_book_placeholder)
                        error(R.drawable.ic_book_placeholder)
                        transformations(RoundedCornersTransformation(0f))
                    }
                } else {
                    ivBookCover.setImageResource(R.drawable.ic_book_placeholder)
                }

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