package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemOverallProgressBinding
import com.adika.learnable.model.StudentOverallProgress
import com.adika.learnable.util.NumberFormatUtils
import com.adika.learnable.util.TimeUtils.formatTime

class OverallProgressAdapter :
    ListAdapter<StudentOverallProgress, OverallProgressAdapter.OverallProgressViewHolder>(
        DiffCallback()
    ) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OverallProgressViewHolder {
        val binding = ItemOverallProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OverallProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OverallProgressViewHolder, position: Int) {
        android.util.Log.d("OverallProgressAdapter", "Binding item at position $position")
        holder.bind(getItem(position))
    }

    inner class OverallProgressViewHolder(
        private val binding: ItemOverallProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(overallProgress: StudentOverallProgress) {
            val ctx = itemView.context

            binding.apply {
                tvOverallProgress.text = ctx.getString(
                    R.string.progress_percentage,
                    overallProgress.overallProgressPercentage
                )
                tvCompletedSubjects.text = ctx.getString(
                    R.string.done_material,
                    overallProgress.completedSubjects,
                    overallProgress.totalSubjects
                )
                tvTotalTimeSpent.text = formatTime(ctx, overallProgress.totalTimeSpent)

                val formattedAverage =
                    NumberFormatUtils.formatPercentFlexible(overallProgress.quizAverage.toDouble())
                tvQuizAverage.text = formattedAverage
                tvStreak.text = ctx.getString(R.string.streak, overallProgress.streak)

                progressBar.progress = overallProgress.overallProgressPercentage
            }
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<StudentOverallProgress>() {
        override fun areItemsTheSame(
            oldItem: StudentOverallProgress,
            newItem: StudentOverallProgress
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: StudentOverallProgress,
            newItem: StudentOverallProgress
        ): Boolean {
            return oldItem == newItem
        }
    }
}

