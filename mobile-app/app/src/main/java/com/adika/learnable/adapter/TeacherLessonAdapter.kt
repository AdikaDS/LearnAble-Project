package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemTeacherLessonBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.SubBab

class TeacherLessonAdapter(
    private val onLessonClick: (Lesson) -> Unit,
    private val onEditClick: (Lesson) -> Unit,
    private val onDeleteClick: (Lesson) -> Unit,
    private val onSubBabEditClick: (SubBab) -> Unit,
    private val onSubBabDeleteClick: (SubBab) -> Unit
) : ListAdapter<Lesson, TeacherLessonAdapter.TeacherLessonViewHolder>(LessonDiffCallback()) {

    private val expandedItems = mutableSetOf<String>()
    private val subBabMap = mutableMapOf<String, List<SubBab>>()

    fun updateSubBabsForLesson(lessonId: String, subBabs: List<SubBab>) {
        subBabMap[lessonId] = subBabs
        val position = currentList.indexOfFirst { it.id == lessonId }
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun isLessonExpanded(lessonId: String): Boolean {
        return expandedItems.contains(lessonId)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TeacherLessonViewHolder {
        val binding = ItemTeacherLessonBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return TeacherLessonViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TeacherLessonViewHolder, position: Int) {
        val lesson = getItem(position)
        holder.bind(lesson, subBabMap[lesson.id] ?: emptyList())
    }

    inner class TeacherLessonViewHolder(
        private val binding: ItemTeacherLessonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val teacherSubBabAdapter = TeacherSubBabAdapter(
            onEditClick = onSubBabEditClick,
            onDeleteClick = onSubBabDeleteClick
        )

        init {
            binding.root.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    val lesson = getItem(position)
                    if (expandedItems.contains(lesson.id)) {
                        expandedItems.remove(lesson.id)
                        binding.subBabRecyclerView.visibility = View.GONE
                        onLessonClick(lesson)
                    } else {
                        expandedItems.add(lesson.id)
                        binding.subBabRecyclerView.visibility = View.VISIBLE
                        onLessonClick(lesson)
                    }
                }
            }

            binding.editButton.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onEditClick(getItem(position))
                }
            }

            binding.deleteButton.setOnClickListener {
                val position = layoutPosition
                if (position != RecyclerView.NO_POSITION) {
                    onDeleteClick(getItem(position))
                }
            }

            binding.subBabRecyclerView.apply {
                adapter = teacherSubBabAdapter
                layoutManager = LinearLayoutManager(context)
            }
        }

        fun bind(lesson: Lesson, subBabs: List<SubBab>) {
            binding.apply {
                titleText.text = lesson.title
                contentText.text = lesson.content
                
                schoolLevelText.text = when(lesson.schoolLevel) {
                    "sd" -> "SD"
                    "smp" -> "SMP"
                    "sma" -> "SMA"
                    else -> lesson.schoolLevel
                }

                subjectText.text = when (lesson.schoolLevel) {
                    "sd" -> when (lesson.idSubject) {
                        "mathES" -> "Matematika"
                        "bahasaIndonesiaES" -> "Bahasa Indonesia"
                        "naturalAndSocialScienceES" -> "IPAS"
                        else -> lesson.idSubject
                    }
                    "smp", "sma" -> when (lesson.idSubject) {
                        "mathJHS" -> "Matematika"
                        "bahasaIndonesiaJHS" -> "Bahasa Indonesia"
                        "naturalScienceJHS" -> "IPA"
                        else -> lesson.idSubject
                    }
                    else -> lesson.idSubject
                }
                
                durationText.text = itemView.context.getString(R.string.duration_learning, lesson.duration)
                
                difficultyText.text = when(lesson.difficulty) {
                    "easy" -> "Mudah"
                    "medium" -> "Sedang"
                    "hard" -> "Sulit"
                    else -> lesson.difficulty
                }

                if (expandedItems.contains(lesson.id)) {
                    subBabRecyclerView.visibility = View.VISIBLE
                    teacherSubBabAdapter.submitList(subBabs)
                } else {
                    subBabRecyclerView.visibility = View.GONE
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