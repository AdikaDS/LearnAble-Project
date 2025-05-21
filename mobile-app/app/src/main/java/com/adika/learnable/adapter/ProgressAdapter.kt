package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.databinding.ItemProgressBinding
import com.adika.learnable.model.LearningProgress
import java.text.SimpleDateFormat
import java.util.Locale

class ProgressAdapter : ListAdapter<LearningProgress, ProgressAdapter.ProgressViewHolder>(ProgressDiffCallback()) {

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

        fun bind(learningProgress: LearningProgress) {
            binding.apply {
                tvSubjectName.text = learningProgress.subjectName
                tvProgressPercentage.text = "${learningProgress.progressPercentage}%"
                tvCompletedLessons.text = "${learningProgress.completedLessons}/${learningProgress.totalLessons} lessons"
                tvQuizAverage.text = String.format("%.1f", learningProgress.quizAverage)
                tvStreak.text = "${learningProgress.streak} days"
                tvTotalTimeSpent.text = "${learningProgress.totalTimeSpent} minutes"
                tvLastActivity.text = "Last activity: ${dateFormat.format(learningProgress.lastActivityDate.toDate())}"

                // Update progress bar
                progressBar.progress = learningProgress.progressPercentage
            }
        }
    }

    private class ProgressDiffCallback : DiffUtil.ItemCallback<LearningProgress>() {
        override fun areItemsTheSame(oldItem: LearningProgress, newItem: LearningProgress): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: LearningProgress, newItem: LearningProgress): Boolean {
            return oldItem == newItem
        }
    }
} 