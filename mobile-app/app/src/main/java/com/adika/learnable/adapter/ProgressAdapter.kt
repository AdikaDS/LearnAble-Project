package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.databinding.ItemProgressBinding
import com.adika.learnable.model.Progress
import java.text.SimpleDateFormat
import java.util.Locale

class ProgressAdapter : ListAdapter<Progress, ProgressAdapter.ProgressViewHolder>(ProgressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProgressViewHolder {
        val binding = ItemProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProgressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProgressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ProgressViewHolder(
        private val binding: ItemProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(progress: Progress) {
            binding.apply {
                tvSubjectName.text = progress.subjectName
                tvProgressPercentage.text = "${progress.progressPercentage}%"
                tvCompletedLessons.text = "${progress.completedLessons}/${progress.totalLessons} lessons"
                tvQuizAverage.text = String.format("%.1f", progress.quizAverage)
                tvStreak.text = "${progress.streak} days"
                tvTotalTimeSpent.text = "${progress.totalTimeSpent} minutes"
                tvLastActivity.text = "Last activity: ${dateFormat.format(progress.lastActivityDate.toDate())}"

                // Update progress bar
                progressBar.progress = progress.progressPercentage
            }
        }
    }

    private class ProgressDiffCallback : DiffUtil.ItemCallback<Progress>() {
        override fun areItemsTheSame(oldItem: Progress, newItem: Progress): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Progress, newItem: Progress): Boolean {
            return oldItem == newItem
        }
    }
} 