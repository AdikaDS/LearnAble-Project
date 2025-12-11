package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemProgressBinding
import com.adika.learnable.model.LessonProgressItem
import com.adika.learnable.util.NumberFormatUtils
import com.adika.learnable.util.TimeUtils
import com.adika.learnable.util.TimeUtils.formatTime
import com.bumptech.glide.Glide

class LessonProgressAdapter :
    ListAdapter<LessonProgressItem, LessonProgressAdapter.LessonProgressViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonProgressViewHolder {
        val binding = ItemProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LessonProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LessonProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LessonProgressViewHolder(
        private val binding: ItemProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: LessonProgressItem) {
            val ctx = itemView.context

            binding.apply {
                tvTitle.text = item.lesson.title
                progressText.text = ctx.getString(
                    R.string.progress_text,
                    item.progress.completedSubBabs,
                    item.lesson.totalSubBab
                )
                progressPercentage.text =
                    ctx.getString(R.string.progress_percentage, item.progress.progressPercentage)

                progressBar.progress = item.progress.progressPercentage

                statusChip.text = when {
                    item.progress.isCompleted -> ctx.getString(R.string.done)
                    item.progress.progressPercentage > 0 -> ctx.getString(R.string.learning)
                    else -> ctx.getString(R.string.not_started)
                }

                statusChip.setChipBackgroundColorResource(
                    when {
                        item.progress.isCompleted -> R.color.green
                        item.progress.progressPercentage > 0 -> R.color.blue_primary
                        else -> R.color.grey_bg
                    }
                )

                item.lesson.coverImage.let { url ->
                    Glide.with(root.context)
                        .load(url)
                        .placeholder(R.drawable.icon_dummy_bab)
                        .error(R.drawable.icon_dummy_bab)
                        .into(imageView)
                }

                timeSpent.text = formatTime(ctx, item.progress.totalTimeSpent)

                val formattedAverage =
                    NumberFormatUtils.formatPercentFlexible(item.progress.quizAverage.toDouble())
                quizScore.text = formattedAverage
                lastActivity.text =
                    TimeUtils.getRelativeTimeString(ctx, item.progress.lastActivityDate)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<LessonProgressItem>() {
        override fun areItemsTheSame(
            oldItem: LessonProgressItem,
            newItem: LessonProgressItem
        ): Boolean {
            return oldItem.progress.id == newItem.progress.id
        }

        override fun areContentsTheSame(
            oldItem: LessonProgressItem,
            newItem: LessonProgressItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}