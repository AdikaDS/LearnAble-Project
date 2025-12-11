package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemProgressBinding
import com.adika.learnable.model.SubBabProgressItem
import com.adika.learnable.util.NumberFormatUtils
import com.adika.learnable.util.TimeUtils
import com.adika.learnable.util.TimeUtils.formatTime
import com.bumptech.glide.Glide

class SubBabProgressAdapter :
    ListAdapter<SubBabProgressItem, SubBabProgressAdapter.SubBabProgressViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubBabProgressViewHolder {
        val binding = ItemProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubBabProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubBabProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SubBabProgressViewHolder(
        private val binding: ItemProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SubBabProgressItem) {
            val ctx = itemView.context

            binding.apply {
                tvTitle.text = item.subBab.title

                item.subBab.coverImage.takeIf { it.isNotBlank() }?.let { url ->
                    Glide.with(root.context)
                        .load(url)
                        .placeholder(R.drawable.icon_dummy_bab)
                        .error(R.drawable.icon_dummy_bab)
                        .into(imageView)
                } ?: run {
                    imageView.setImageResource(R.drawable.icon_dummy_bab)
                }
                val completedMaterials = item.progress.completedMaterials.values.count { it }
                val totalMaterials = item.progress.completedMaterials.size
                val progressPercentages = if (totalMaterials > 0) {
                    (completedMaterials * 100) / totalMaterials
                } else 0

                progressText.text =
                    ctx.getString(R.string.progress_text, completedMaterials, totalMaterials)
                progressPercentage.text =
                    ctx.getString(R.string.progress_percentage, progressPercentages)
                progressBar.progress = progressPercentages

                statusChip.text = when {
                    item.progress.isCompleted -> ctx.getString(R.string.done)
                    completedMaterials > 0 -> ctx.getString(R.string.learning)
                    else -> ctx.getString(R.string.not_started)
                }

                statusChip.setChipBackgroundColorResource(
                    when {
                        item.progress.isCompleted -> R.color.green
                        completedMaterials > 0 -> R.color.blue_primary
                        else -> R.color.grey_bg
                    }
                )

                timeSpent.text = formatTime(ctx, item.progress.timeSpent)

                val formattedAverage =
                    NumberFormatUtils.formatPercentFlexible(item.progress.quizScore.toDouble())
                quizScore.text = formattedAverage
                lastActivity.text =
                    TimeUtils.getRelativeTimeString(ctx, item.progress.lastActivityDate)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SubBabProgressItem>() {
        override fun areItemsTheSame(
            oldItem: SubBabProgressItem,
            newItem: SubBabProgressItem
        ): Boolean {
            return oldItem.progress.subBabId == newItem.progress.subBabId
        }

        override fun areContentsTheSame(
            oldItem: SubBabProgressItem,
            newItem: SubBabProgressItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}


