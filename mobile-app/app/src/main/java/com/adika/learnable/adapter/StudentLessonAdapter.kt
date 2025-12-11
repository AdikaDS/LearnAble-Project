package com.adika.learnable.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemLessonBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.StudentLessonProgress
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.TextScaleUtils
import com.bumptech.glide.Glide
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

class StudentLessonAdapter(
    private val onLessonClick: (Lesson) -> Unit
) : ListAdapter<Lesson, StudentLessonAdapter.StudentLessonViewHolder>(LessonDiffCallback()) {

    private val progressMap = mutableMapOf<String, StudentLessonProgress>()

    fun updateProgress(lessonId: String, progress: StudentLessonProgress) {
        try {
            progressMap[lessonId] = progress
            val index = currentList.indexOfFirst { it.id == lessonId }
            if (index != -1) {
                notifyItemChanged(index)
            }
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
            val progress = progressMap[lesson.id]
            holder.bind(lesson, progress)
        } catch (e: Exception) {
            Log.e("StudentLessonAdapter", "Error binding view holder: ${e.message}")
        }
    }

    inner class StudentLessonViewHolder(
        private val binding: ItemLessonBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val lesson = getItem(position)
                    onLessonClick(lesson)
                }
            }
        }

        fun bind(lesson: Lesson, progress: StudentLessonProgress?) {
            try {
                binding.apply {
                    val ctx = itemView.context

                    titleText.text = lesson.title

                    val completed = progress?.completedSubBabs ?: 0
                    val total = lesson.totalSubBab
                    val percentage: Int = if (total > 0) {
                        val raw = ((completed.toFloat() * 100f) / total.toFloat()).roundToInt()
                        min(100, max(0, raw))
                    } else 0
                    progressBar.progress = percentage
                    progressText.text =
                        ctx.getString(R.string.progress_text, completed, total)

                    val colorRes = if (completed == total && total > 0) {
                        R.color.green
                    } else {
                        R.color.blue_primary
                    }
                    progressBar.setIndicatorColor(ctx.getColor(colorRes))

                    lesson.coverImage.let { url ->
                        Glide.with(root.context)
                            .load(url)
                            .placeholder(R.drawable.icon_dummy_bab)
                            .error(R.drawable.icon_dummy_bab)
                            .into(imageView)
                    }
                    val textScaleRepository = TextScaleRepository(binding.root.context)
                    val scale = textScaleRepository.getScale()
                    TextScaleUtils.applyScaleToHierarchy(binding.root, scale)
                }
            } catch (e: Exception) {
                Log.e("StudentLessonAdapter", "Error in bind: ${e.message}")
            }
        }
    }

    fun cleanup() {
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