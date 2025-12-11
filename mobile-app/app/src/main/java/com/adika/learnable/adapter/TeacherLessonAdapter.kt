package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemTeacherDashboardLessonBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.util.EducationLevels

class TeacherLessonAdapter(
    private val onViewClick: (Lesson) -> Unit,
    private val onDetailClick: (Lesson) -> Unit,
    private val onEditClick: (Lesson) -> Unit,
    private val onDeleteClick: (Lesson) -> Unit
) : ListAdapter<Lesson, TeacherLessonAdapter.ViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<Lesson>() {
        override fun areItemsTheSame(oldItem: Lesson, newItem: Lesson): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Lesson, newItem: Lesson): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemTeacherDashboardLessonBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemTeacherDashboardLessonBinding
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(lesson: Lesson) {
            val ctx = binding.root.context
            binding.apply {

                root.setOnClickListener {
                    onViewClick(lesson)
                }

                tvTitle.text = lesson.title
                val levelText = when (lesson.schoolLevel) {
                    EducationLevels.SD -> ctx.getString(R.string.elementarySchool)
                    EducationLevels.SMP -> ctx.getString(R.string.juniorHighSchool)
                    EducationLevels.SMA -> ctx.getString(R.string.seniorHighSchool)
                    else -> lesson.schoolLevel
                }

                val subjectName = when (lesson.schoolLevel) {
                    EducationLevels.SD -> when (lesson.idSubject) {
                        EducationLevels.Subjects.SD.MATH -> ctx.getString(R.string.math)
                        EducationLevels.Subjects.SD.BAHASA_INDONESIA -> ctx.getString(R.string.indonesianLanguage)
                        EducationLevels.Subjects.SD.NATURAL_AND_SOCIAL_SCIENCE -> ctx.getString(R.string.naturalAndSocialScience)
                        else -> lesson.idSubject
                    }

                    EducationLevels.SMP -> when (lesson.idSubject) {
                        EducationLevels.Subjects.SMP.MATH -> ctx.getString(R.string.math)
                        EducationLevels.Subjects.SMP.BAHASA_INDONESIA -> ctx.getString(R.string.indonesianLanguage)
                        EducationLevels.Subjects.SMP.NATURAL_SCIENCE -> ctx.getString(R.string.naturalScience)
                        else -> lesson.idSubject
                    }

                    EducationLevels.SMA -> when (lesson.idSubject) {
                        EducationLevels.Subjects.SMA.MATH -> ctx.getString(R.string.math)
                        EducationLevels.Subjects.SMA.BAHASA_INDONESIA -> ctx.getString(R.string.indonesianLanguage)
                        EducationLevels.Subjects.SMA.NATURAL_SCIENCE -> ctx.getString(R.string.naturalScience)
                        else -> lesson.idSubject
                    }

                    else -> lesson.idSubject
                }

                tvSubjectLevel.text =
                    ctx.getString(R.string.string_connected, subjectName, levelText)

                btnDetail.setOnClickListener { onDetailClick(lesson) }
                btnEdit.setOnClickListener { onEditClick(lesson) }
                btnDelete.setOnClickListener { onDeleteClick(lesson) }
            }
        }
    }
}