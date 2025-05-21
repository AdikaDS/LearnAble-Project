package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.databinding.ItemLessonBinding
import com.adika.learnable.model.Lesson

class LessonAdapter(
    private val onLessonClick: (Lesson) -> Unit
) : ListAdapter<Lesson, LessonAdapter.LessonViewHolder>(LessonDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LessonViewHolder {
        val binding = ItemLessonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return LessonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: LessonViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class LessonViewHolder(
        private val binding: ItemLessonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onLessonClick(getItem(position))
                }
            }
        }

        fun bind(lesson: Lesson) {
            binding.apply {
                titleText.text = lesson.title
                contentText.text = lesson.content
                durationText.text = "${lesson.duration} menit"
                difficultyText.text = when (lesson.difficulty) {
                    "easy" -> "Mudah"
                    "medium" -> "Sedang"
                    "hard" -> "Sulit"
                    else -> lesson.difficulty
                }

                // Set disability type chips
                tunarunguChip.visibility = if (lesson.disabilityTypes.contains("tunarungu")) {
                    ViewGroup.VISIBLE
                } else {
                    ViewGroup.GONE
                }

                tunanetraChip.visibility = if (lesson.disabilityTypes.contains("tunanetra")) {
                    ViewGroup.VISIBLE
                } else {
                    ViewGroup.GONE
                }

            }
        }
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