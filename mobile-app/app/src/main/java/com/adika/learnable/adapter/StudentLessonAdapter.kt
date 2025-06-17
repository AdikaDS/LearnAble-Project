package com.adika.learnable.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemLessonBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.SubBab
import com.adika.learnable.model.StudentLessonProgress
import com.adika.learnable.viewmodel.LessonViewModel

class StudentLessonAdapter(
    private val onLessonClick: (Lesson) -> Unit,
    private val onSubBabClick: (SubBab) -> Unit,
    private val viewModel: LessonViewModel
) : ListAdapter<Lesson, StudentLessonAdapter.StudentLessonViewHolder>(LessonDiffCallback()) {

    private val expandedItems = mutableSetOf<String>()
    private val subBabMap = mutableMapOf<String, List<SubBab>>()
    private val progressMap = mutableMapOf<String, StudentLessonProgress>()

    fun updateSubBabsForLesson(lessonId: String, subBabs: List<SubBab>) {
        try {
            subBabMap[lessonId] = subBabs
            // Update expanded state
            if (subBabs.isNotEmpty()) {
                expandedItems.add(lessonId)
            }
            notifyItemChanged(currentList.indexOfFirst { it.id == lessonId })
        } catch (e: Exception) {
            Log.e("StudentLessonAdapter", "Error updating sub-babs: ${e.message}")
        }
    }

    fun updateProgress(lessonId: String, progress: StudentLessonProgress) {
        try {
            progressMap[lessonId] = progress
            notifyItemChanged(currentList.indexOfFirst { it.id == lessonId })
        } catch (e: Exception) {
            Log.e("StudentLessonAdapter", "Error updating progress: ${e.message}")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentLessonViewHolder {
        val binding = ItemLessonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return StudentLessonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StudentLessonViewHolder, position: Int) {
        try {
            val lesson = getItem(position)
            val subBabs = subBabMap[lesson.id] ?: emptyList()
            val progress = progressMap[lesson.id]
            holder.bind(lesson, subBabs, progress)
        } catch (e: Exception) {
            Log.e("StudentLessonAdapter", "Error binding view holder: ${e.message}")
        }
    }

    inner class StudentLessonViewHolder(
        private val binding: ItemLessonBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        private val studentSubBabAdapter = StudentSubBabAdapter { subBab ->
            onSubBabClick(subBab)
        }

        init {
            binding.subBabRecyclerView.apply {
                adapter = studentSubBabAdapter
                layoutManager = LinearLayoutManager(context)
            }

            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val lesson = getItem(position)
                    val isExpanded = expandedItems.contains(lesson.id)
                    
                    if (isExpanded) {
                        // Jika sudah expand, collapse
                        expandedItems.remove(lesson.id)
                        notifyItemChanged(position)
                    } else {
                        // Jika belum expand, expand dan load sub-babs
                        expandedItems.add(lesson.id)
                        notifyItemChanged(position)
                        onLessonClick(lesson)
                    }
                }
            }
        }

        fun bind(lesson: Lesson, subBabs: List<SubBab>, progress: StudentLessonProgress?) {
            try {
                binding.apply {
                    titleText.text = lesson.title
                    contentText.text = lesson.content
                    durationText.text = itemView.context.getString(R.string.duration_learning, lesson.duration)
                    difficultyText.text = when (lesson.difficulty) {
                        "easy" -> "Mudah"
                        "medium" -> "Sedang"
                        "hard" -> "Sulit"
                        else -> lesson.difficulty
                    }

                    // Update progress
                    progress?.let {
                        val completedSubabs = it.completedSubBabs
                        val totalSubBabs = it.totalSubBabs
                        val percentage = if (it.totalSubBabs > 0) {
                            (completedSubabs * 100) / totalSubBabs
                        } else 0
                        progressBar.progress = percentage
                        progressText.text = itemView.context.getString(R.string.progress_text, completedSubabs, totalSubBabs)
                    }

                    // Update sub-babs visibility
                    val isExpanded = expandedItems.contains(lesson.id)
                    subBabRecyclerView.visibility = if (isExpanded) ViewGroup.VISIBLE else ViewGroup.GONE
                    expandButton.setImageResource(
                        if (isExpanded) R.drawable.ic_expand_less
                        else R.drawable.ic_expand_more
                    )

                    // Update sub-babs list
                    studentSubBabAdapter.submitList(subBabs)
                }
            } catch (e: Exception) {
                Log.e("StudentLessonAdapter", "Error in bind: ${e.message}")
            }
        }
    }

    fun cleanup() {
        expandedItems.clear()
        subBabMap.clear()
        progressMap.clear()
    }

    private class LessonDiffCallback : DiffUtil.ItemCallback<Lesson>() {
        override fun areItemsTheSame(oldItem: Lesson, newItem: Lesson): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Lesson, newItem: Lesson): Boolean {
            return oldItem == newItem
        }
    }
} 