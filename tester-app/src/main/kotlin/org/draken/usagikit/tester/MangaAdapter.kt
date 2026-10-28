package org.draken.usagikit.tester

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import coil.load
import org.draken.usagikit.tester.databinding.ItemMangaBinding
import tsuki.model.Manga

class MangaAdapter(
    private val onItemClick: (Manga) -> Unit,
) : RecyclerView.Adapter<MangaAdapter.ViewHolder>() {

    private val items = mutableListOf<Manga>()

    fun submitList(newList: List<Manga>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemMangaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemMangaBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(manga: Manga) {
            binding.tvTitle.text = manga.title
            binding.tvAuthor.text = manga.authors.firstOrNull() ?: manga.description ?: "Unknown Author"
            val chapterCount = manga.chapters?.size ?: 0
            binding.tvChapters.text = if (chapterCount > 0) "$chapterCount chapters" else "Tap to fetch details & chapters"

            binding.ivCover.load(manga.coverUrl) {
                crossfade(true)
            }

            binding.root.setOnClickListener {
                onItemClick(manga)
            }
        }
    }
}
