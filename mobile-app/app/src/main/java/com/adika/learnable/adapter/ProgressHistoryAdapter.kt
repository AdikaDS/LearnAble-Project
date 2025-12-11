package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemProgressHistoryBinding
import com.adika.learnable.model.StudyHistoryItem
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.SubjectIconUtils.setSubjectIcon
import com.adika.learnable.util.TextScaleUtils
import com.bumptech.glide.Glide

class ProgressHistoryAdapter :
    ListAdapter<StudyHistoryItem, ProgressHistoryAdapter.ViewHolder>(DIFF) {
    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<StudyHistoryItem>() {
            override fun areItemsTheSame(
                oldItem: StudyHistoryItem,
                newItem: StudyHistoryItem
            ): Boolean =
                oldItem.timeText == newItem.timeText && oldItem.title == newItem.title

            override fun areContentsTheSame(
                oldItem: StudyHistoryItem,
                newItem: StudyHistoryItem
            ): Boolean =
                oldItem == newItem
        }
    }

    inner class ViewHolder(private val binding: ItemProgressHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: StudyHistoryItem) {
            binding.apply {
                tvTitle.text = item.title
                tvSubtitle.text = item.subtitle
                tvTime.text = item.timeText

                item.coverImage.takeIf { it.isNotBlank() }?.let { url ->
                    Glide.with(root.context)
                        .load(url)
                        .placeholder(R.drawable.icon_dummy_bab)
                        .error(R.drawable.icon_dummy_bab)
                        .into(ivIcon)
                } ?: run {
                    ivIcon.setSubjectIcon(item.subtitle)
                }

                ivIcon.post {
                    val imageHeight = ivIcon.height
                    val overlayHeight = imageHeight / 2

                    val params = gradientOverlay.layoutParams
                    params.height = overlayHeight
                    gradientOverlay.layoutParams = params
                }

                val textScaleRepository = TextScaleRepository(binding.root.context)
                val scale = textScaleRepository.getScale()
                TextScaleUtils.applyScaleToHierarchy(binding.root, scale)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemProgressHistoryBinding.inflate(inflater, parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}