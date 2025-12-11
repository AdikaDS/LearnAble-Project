package com.adika.learnable.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemProgressBinding
import com.adika.learnable.model.SubjectProgressItem
import com.adika.learnable.util.NumberFormatUtils
import com.adika.learnable.util.SubjectIconUtils.setSubjectIcon
import com.adika.learnable.util.TimeUtils
import com.adika.learnable.util.TimeUtils.formatTime

class SubjectProgressAdapter :
    ListAdapter<SubjectProgressItem, SubjectProgressAdapter.SubjectProgressViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SubjectProgressViewHolder {
        val binding = ItemProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SubjectProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SubjectProgressViewHolder, position: Int) {
        Log.d("SubjectProgressAdapter", "Binding subject at position $position")
        holder.bind(getItem(position))
    }

    inner class SubjectProgressViewHolder(
        private val binding: ItemProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: SubjectProgressItem) {
            val ctx = itemView.context

            binding.apply {
                tvTitle.text = item.subject.name
                progressText.text = ctx.getString(
                    R.string.progress_text,
                    item.progress.completedLessons,
                    item.subject.totalLessons
                )
                progressPercentage.text =
                    ctx.getString(R.string.progress_percentage, item.progress.progressPercentage)

                progressBar.progress = item.progress.progressPercentage

                statusChip.text = when {
                    item.progress.progressPercentage == 100 -> ctx.getString(R.string.done)
                    item.progress.progressPercentage > 0 -> ctx.getString(R.string.learning)
                    else -> ctx.getString(R.string.not_started)
                }

                statusChip.setChipBackgroundColorResource(
                    when {
                        item.progress.progressPercentage == 100 -> R.color.green
                        item.progress.progressPercentage > 0 -> R.color.blue_primary
                        else -> R.color.grey_bg
                    }
                )

                imageView.setSubjectIcon(item.subject.name)

                timeSpent.text = formatTime(ctx, item.progress.totalTimeSpent)

                val formattedAverage =
                    NumberFormatUtils.formatPercentFlexible(item.progress.quizAverage.toDouble())
                quizScore.text = formattedAverage
                lastActivity.text =
                    TimeUtils.getRelativeTimeString(ctx, item.progress.lastActivityDate)
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<SubjectProgressItem>() {
        override fun areItemsTheSame(
            oldItem: SubjectProgressItem,
            newItem: SubjectProgressItem
        ): Boolean {
            return oldItem.progress.id == newItem.progress.id
        }

        override fun areContentsTheSame(
            oldItem: SubjectProgressItem,
            newItem: SubjectProgressItem
        ): Boolean {
            return oldItem == newItem
        }
    }
}