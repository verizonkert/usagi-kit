package org.draken.usagikit.tester

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.draken.usagikit.tester.databinding.ItemChapterBinding
import tsuki.model.MangaChapter

class ChapterAdapter(
    private val onChapterClick: (MangaChapter) -> Unit,
) : RecyclerView.Adapter<ChapterAdapter.ViewHolder>() {

    private val items = mutableListOf<MangaChapter>()

    fun submitList(list: List<MangaChapter>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemChapterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemChapterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chapter: MangaChapter) {
            binding.tvChapterNumber.text = "#${chapter.number.toInt()}"
            binding.tvChapterTitle.text = chapter.title.takeUnless { it.isNullOrBlank() } ?: "Chapter ${chapter.number}"
            val scanlator = chapter.scanlator?.takeIf { it.isNotBlank() } ?: chapter.branch
            binding.tvChapterScanlator.text = scanlator ?: "Default group"

            binding.root.setOnClickListener {
                onChapterClick(chapter)
            }
        }
    }
}
