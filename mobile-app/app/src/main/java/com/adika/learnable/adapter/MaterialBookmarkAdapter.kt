package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemMaterialBookmarkBinding
import com.adika.learnable.model.Bookmark
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.NormalizeSchoolLevel.formatSchoolLevel
import com.adika.learnable.util.SubjectIconUtils.setSubjectIcon
import com.adika.learnable.util.TextScaleUtils
import com.bumptech.glide.Glide
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class MaterialBookmarkAdapter(
    private val onBookmarkClick: (Bookmark) -> Unit,
    private val onRemoveBookmark: (Bookmark) -> Unit
) : ListAdapter<Bookmark, MaterialBookmarkAdapter.BookmarkViewHolder>(BookmarkDiffCallback()) {

    private val bookmarkStateMap = mutableMapOf<String, Boolean>()

    fun updateBookmarkStatus(bookmarkId: String, isBookmarked: Boolean) {
        bookmarkStateMap[bookmarkId] = isBookmarked
        val index = currentList.indexOfFirst { it.id == bookmarkId }
        if (index != -1) notifyItemChanged(index)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookmarkViewHolder {
        val binding = ItemMaterialBookmarkBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return BookmarkViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BookmarkViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class BookmarkViewHolder(
        private val binding: ItemMaterialBookmarkBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onBookmarkClick(getItem(position))
                }
            }

            binding.btnBookmark.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onRemoveBookmark(getItem(position))
                }
            }
        }

        fun bind(bookmark: Bookmark) {
            val ctx = itemView.context
            binding.apply {

                tvLessonTitle.text = bookmark.lessonTitle
                tvSubBabTitle.text = bookmark.subBabTitle
                val completedCount = bookmark.completedMaterials.count { it.value }
                val totalCount = bookmark.completedMaterials.size
                val percentage: Int = if (totalCount > 0) {
                    val raw = ((completedCount.toFloat() * 100f) / totalCount.toFloat()).roundToInt()
                    min(100, max(0, raw))
                } else 0
                progressBar.progress = percentage

                tvProgressText.text =
                    ctx.getString(R.string.progress_text, completedCount, totalCount)

                val colorRes = if (completedCount == totalCount && totalCount > 0) {
                    R.color.green
                } else {
                    R.color.blue_primary
                }
                progressBar.setIndicatorColor(ctx.getColor(colorRes))

                val schoolLevel  = formatSchoolLevel(ctx, bookmark.schoolLevel)
                tvGradeBadge.text = schoolLevel
                
                bookmark.coverImage.takeIf { it.isNotBlank() }?.let { url ->
                    Glide.with(root.context)
                        .load(url)
                        .placeholder(R.drawable.icon_dummy_bab)
                        .error(R.drawable.icon_dummy_bab)
                        .into(ivSubjectIcon)
                } ?: run {
                    ivSubjectIcon.setSubjectIcon(bookmark.subBabTitle)
                }

                ivSubjectIcon.post {
                    val imageHeight = ivSubjectIcon.height
                    val overlayHeight = imageHeight / 2

                    val params = gradientOverlay.layoutParams
                    params.height = overlayHeight
                    gradientOverlay.layoutParams = params
                }

                val isBookmarked = bookmarkStateMap[bookmark.id] ?: true
                btnBookmark.setImageResource(
                    if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark
                )

                val textScaleRepository = TextScaleRepository(binding.root.context)
                val scale = textScaleRepository.getScale()
                TextScaleUtils.applyScaleToHierarchy(binding.root, scale)
            }
        }
    }

    private class BookmarkDiffCallback : DiffUtil.ItemCallback<Bookmark>() {
        override fun areItemsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Bookmark, newItem: Bookmark): Boolean {
            return oldItem == newItem
        }
    }
}