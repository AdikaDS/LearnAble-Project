package com.adika.learnable.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemSubBabBinding
import com.adika.learnable.model.StudentSubBabProgress
import com.adika.learnable.model.SubBab
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.TextScaleUtils
import com.bumptech.glide.Glide
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class StudentSubBabAdapter(
    private val onSubBabClick: (SubBab) -> Unit,
    private val onBookmarkClick: (SubBab) -> Unit
) : ListAdapter<SubBab, StudentSubBabAdapter.StudentSubBabViewHolder>(SubBabDiffCallback()) {

    private val progressMap = mutableMapOf<String, StudentSubBabProgress>()
    private val bookmarkMap = mutableMapOf<String, Boolean>()

    fun updateProgress(subBabId: String, progress: StudentSubBabProgress) {
        try {
            progressMap[subBabId] = progress
            val index = currentList.indexOfFirst { it.id == subBabId }
            if (index != -1) {
                notifyItemChanged(index)
            }
        } catch (e: Exception) {
            Log.e("StudentSubBabAdapter", "Error updating progress: ${e.message}")
        }
    }

    fun updateBookmarkStatus(subBabId: String, isBookmarked: Boolean) {
        try {
            bookmarkMap[subBabId] = isBookmarked
            val index = currentList.indexOfFirst { it.id == subBabId }
            if (index != -1) {
                notifyItemChanged(index)
            }
        } catch (e: Exception) {
            Log.e("StudentSubBabAdapter", "Error updating bookmark status: ${e.message}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentSubBabViewHolder {
        val binding = ItemSubBabBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentSubBabViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentSubBabViewHolder, position: Int) {
        val subBab = getItem(position)
        val progress = progressMap[subBab.id]
        holder.bind(subBab, progress)
    }

    inner class StudentSubBabViewHolder(
        private val binding: ItemSubBabBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val subBab = getItem(position)
                    onSubBabClick(subBab)
                }
            }

            binding.btnBookmark.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val subBab = getItem(position)
                    onBookmarkClick(subBab)
                }
            }
        }

        fun bind(subBab: SubBab, progress: StudentSubBabProgress?) {
            binding.apply {
                val ctx = itemView.context

                titleText.text = subBab.title
                val completedCount = progress?.completedMaterials?.count { it.value } ?: 0
                val totalCount = progress?.completedMaterials?.size ?: 3
                val percentage: Int = if (totalCount > 0) {
                    val raw =
                        ((completedCount.toFloat() * 100f) / totalCount.toFloat()).roundToInt()
                    min(100, max(0, raw))
                } else 0
                progressBar.progress = percentage

                progressText.text =
                    ctx.getString(R.string.progress_text, completedCount, totalCount)

                val colorRes = if (completedCount == totalCount && totalCount > 0) {
                    R.color.green
                } else {
                    R.color.blue_primary
                }
                progressBar.setIndicatorColor(ctx.getColor(colorRes))
                val isBookmarked = bookmarkMap[subBab.id] ?: false
                btnBookmark.setImageResource(
                    if (isBookmarked) {
                        R.drawable.ic_bookmark_filled
                    } else {
                        R.drawable.ic_bookmark
                    }
                )

                ivIcon.post {
                    val imageHeight = ivIcon.height
                    val overlayHeight = imageHeight / 2

                    val params = gradientOverlay.layoutParams
                    params.height = overlayHeight
                    gradientOverlay.layoutParams = params
                }

                subBab.coverImage.let { url ->
                    Glide.with(root.context)
                        .load(url)
                        .placeholder(R.drawable.icon_dummy_bab)
                        .error(R.drawable.icon_dummy_bab)
                        .into(ivIcon)
                }
                val textScaleRepository = TextScaleRepository(binding.root.context)
                val scale = textScaleRepository.getScale()
                TextScaleUtils.applyScaleToHierarchy(binding.root, scale)
            }
        }
    }

    fun cleanup() {
        progressMap.clear()
        bookmarkMap.clear()
    }

    private class SubBabDiffCallback : DiffUtil.ItemCallback<SubBab>() {
        override fun areItemsTheSame(oldItem: SubBab, newItem: SubBab): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: SubBab, newItem: SubBab): Boolean {
            return oldItem == newItem
        }
    }
} 