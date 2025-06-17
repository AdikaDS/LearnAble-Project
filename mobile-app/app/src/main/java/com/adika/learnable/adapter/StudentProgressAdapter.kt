package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemSubjectProgressBinding
import com.adika.learnable.model.StudentSubjectProgress
import java.text.SimpleDateFormat
import java.util.Locale

class StudentProgressAdapter : ListAdapter<StudentSubjectProgress, StudentProgressAdapter.ViewHolder>(SubjectProgressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemSubjectProgressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemSubjectProgressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

        fun bind(progress: StudentSubjectProgress) {
            binding.apply {
                val formattedDate = dateFormat.format(progress.lastActivityDate.toDate())
                tvSubjectName.text = progress.subjectName
                tvProgressPercentage.text = itemView.context.getString(R.string.progress_percentage, progress.progressPercentage)
                tvCompletedLessons.text = itemView.context.getString(R.string.completed_lessons, progress.completedLessons, progress.totalLessons)
                tvQuizAverage.text = itemView.context.getString(R.string.quiz_average, progress.quizAverage)
                tvStreak.text = itemView.context.getString(R.string.streak, progress.streak)
                tvTotalTimeSpent.text = itemView.context.getString(R.string.total_time_spent, progress.totalTimeSpent)
                tvLastActivity.text = itemView.context.getString(R.string.last_Activity, formattedDate)

                // Update progress bar
                progressBar.progress = progress.progressPercentage
            }
        }
    }

    private class SubjectProgressDiffCallback : DiffUtil.ItemCallback<StudentSubjectProgress>() {
        override fun areItemsTheSame(oldItem: StudentSubjectProgress, newItem: StudentSubjectProgress): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: StudentSubjectProgress, newItem: StudentSubjectProgress): Boolean {
            return oldItem == newItem
        }
    }
} 