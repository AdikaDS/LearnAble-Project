package com.adika.learnable.view.dashboard.teacher.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogLessonDetailBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.util.EducationLevels
import com.bumptech.glide.Glide

class LessonDetailDialogFragment : DialogFragment() {
    private var _binding: DialogLessonDetailBinding? = null
    private val binding get() = _binding!!

    private var lesson: Lesson? = null
    private var onViewSubbabClick: ((Lesson) -> Unit)? = null

    companion object {
        fun newInstance(
            lesson: Lesson,
            onViewSubbabClick: ((Lesson) -> Unit)? = null
        ): LessonDetailDialogFragment {
            return LessonDetailDialogFragment().apply {
                this.lesson = lesson
                this.onViewSubbabClick = onViewSubbabClick
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLessonDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        lesson?.let { lesson ->
            val ctx = requireContext()

            binding.apply {

                tvLessonTitle.text = lesson.title

                val levelText = when (lesson.schoolLevel) {
                    EducationLevels.SD -> ctx.getString(R.string.elementarySchool)
                    EducationLevels.SMP -> ctx.getString(R.string.juniorHighSchool)
                    EducationLevels.SMA -> ctx.getString(R.string.seniorHighSchool)
                    else -> lesson.schoolLevel
                }
                tvSchoolLevel.text = levelText

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
                tvSubject.text = subjectName

                if (!lesson.coverImage.isNullOrBlank()) {
                    Glide.with(requireContext())
                        .load(lesson.coverImage)
                        .placeholder(R.drawable.icon_dummy_subject)
                        .error(R.drawable.icon_dummy_subject)
                        .into(ivIllustration)
                } else {
                    ivIllustration.setImageResource(R.drawable.icon_dummy_subject)
                }

                btnClose.setOnClickListener {
                    dismiss()
                }

                btnBack.setOnClickListener {
                    dismiss()
                }

                btnViewSubbab.setOnClickListener {
                    onViewSubbabClick?.invoke(lesson)
                    dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}