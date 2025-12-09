package com.adika.learnable.view.dashboard.teacher

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.viewModels
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogLessonFormBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.util.ValidationUtils
import com.adika.learnable.viewmodel.dashboard.TeacherDashboardViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LessonFormBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogLessonFormBinding? = null
    private val binding get() = _binding!!

    private var lesson: Lesson? = null
    private var onSaveListener: ((Lesson) -> Unit)? = null
    private val teacherDashboardViewModel: TeacherDashboardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)
    }

    companion object {
        fun newInstance(
            lesson: Lesson? = null,
            onSave: (Lesson) -> Unit
        ): LessonFormBottomSheetDialogFragment {
            return LessonFormBottomSheetDialogFragment().apply {
                this.lesson = lesson
                this.onSaveListener = onSave
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogLessonFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        setupDropdowns()
        fillExistingData()
        getData()
    }

    private fun setupViews() {
        binding.tvTitle.text = if (lesson == null) getString(R.string.add_lesson) else getString(R.string.edit_lesson)
        binding.btnSave.setOnClickListener {
            if (validateInputs()) {
                val newLesson = createLessonFromInputs()
                onSaveListener?.invoke(newLesson)
                dismiss()
            }
        }

        binding.btnCancel.setOnClickListener {
            dismiss()
        }
    }

    private fun setupDropdowns() {
        val schoolLevels = resources.getStringArray(R.array.school_levels)
        val schoolLevelsAdapter =
            ArrayAdapter(requireContext(), R.layout.item_dropdown, schoolLevels)
        binding.schoolLevelAutoComplete.apply {
            setAdapter(schoolLevelsAdapter)
            setOnItemClickListener { _, _, position, _ ->
                val firebaseValue = when (position) {
                    0 -> "sd"
                    1 -> "smp"
                    2 -> "sma"
                    else -> ""
                }
                tag = firebaseValue

                binding.subjectAutoComplete.setText("")
                binding.subjectAutoComplete.tag = null

                updateSubjectAdapter(firebaseValue)
            }
        }

        binding.subjectAutoComplete.isEnabled = false

        val difficulties = resources.getStringArray(R.array.difficulties)
        val difficultiesAdapter =
            ArrayAdapter(requireContext(), R.layout.item_dropdown, difficulties)
        binding.difficultyAutoComplete.apply {
            setAdapter(difficultiesAdapter)
            setOnItemClickListener { _, _, position, _ ->
                val firebaseValue = when (position) {
                    0 -> "easy"
                    1 -> "medium"
                    2 -> "hard"
                    else -> ""
                }
                tag = firebaseValue
            }
        }
    }

    private fun updateSubjectAdapter(schoolLevel: String) {
        val subjects = when (schoolLevel) {
            "sd" -> resources.getStringArray(R.array.elementarySchoolSubjects)
            "smp" -> resources.getStringArray(R.array.juniorHighSchoolSubjects)
            "sma" -> resources.getStringArray(R.array.juniorHighSchoolSubjects) // Sementara
            else -> emptyArray()
        }

        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, subjects)
        binding.subjectAutoComplete.apply {
            setAdapter(adapter)
            isEnabled = true
            setOnItemClickListener { _, _, position, _ ->
                val firebaseValue = when (schoolLevel) {
                    "sd" -> when (position) {
                        0 -> "mathES"
                        1 -> "bahasaIndonesiaES"
                        2 -> "naturalAndSocialScienceES"
                        else -> ""
                    }
                    "smp", "sma" -> when (position) {
                        0 -> "mathJHS"
                        1 -> "bahasaIndonesiaJHS"
                        2 -> "naturalScienceJHS"
                        else -> ""
                    }
                    else -> ""
                }
                tag = firebaseValue
                // Tampilkan teks dari string array
                setText(subjects[position], false)
            }
        }
    }

    private fun fillExistingData() {
        lesson?.let {
            binding.titleEditText.setText(it.title)
            binding.contentEditText.setText(it.content)
            binding.durationEditText.setText(it.duration.toString())

            val schoolLevelPosition = when (it.schoolLevel) {
                "sd" -> 0
                "smp" -> 1
                "sma" -> 2
                else -> 0
            }
            binding.schoolLevelAutoComplete.setText(
                resources.getStringArray(R.array.school_levels)[schoolLevelPosition],
                false
            )
            binding.schoolLevelAutoComplete.tag = it.schoolLevel

            updateSubjectAdapter(it.schoolLevel)

            // Set subject text dan tag berdasarkan school level
            when (it.schoolLevel) {
                "sd" -> {
                    val subjectPosition = when (it.idSubject) {
                        "mathES" -> 0
                        "bahasaIndonesiaES" -> 1
                        "naturalAndSocialScienceES" -> 2
                        else -> 0
                    }
                    binding.subjectAutoComplete.setText(
                        resources.getStringArray(R.array.elementarySchoolSubjects)[subjectPosition],
                        false
                    )
                }
                "smp", "sma" -> {
                    val subjectPosition = when (it.idSubject) {
                        "mathJHS" -> 0
                        "bahasaIndonesiaJHS" -> 1
                        "naturalScienceJHS" -> 2
                        else -> 0
                    }
                    binding.subjectAutoComplete.setText(
                        resources.getStringArray(R.array.juniorHighSchoolSubjects)[subjectPosition],
                        false
                    )
                }
            }
            binding.subjectAutoComplete.tag = it.idSubject

            val difficultyPosition = when (it.difficulty) {
                "easy" -> 0
                "medium" -> 1
                "hard" -> 2
                else -> 0
            }
            binding.difficultyAutoComplete.setText(
                resources.getStringArray(R.array.difficulties)[difficultyPosition],
                false
            )
            binding.difficultyAutoComplete.tag = it.difficulty
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true

        if (!ValidationUtils.validateInputsData(
                requireContext(),
                binding.titleInputLayout,
                binding.titleEditText,
                ValidationUtils.FieldType.TITLE
            )
        ) isValid = false

        if (!ValidationUtils.validateInputsData(
                requireContext(),
                binding.contentInputLayout,
                binding.contentEditText,
                ValidationUtils.FieldType.CONTENT
            )
        ) isValid = false

        if (!ValidationUtils.validateInputsData(
                requireContext(),
                binding.durationInputLayout,
                binding.durationEditText,
                ValidationUtils.FieldType.DURATION
            )
        ) isValid = false

        if (!ValidationUtils.validateInputsData(
                requireContext(),
                binding.subjectInputLayout,
                binding.subjectAutoComplete,
                ValidationUtils.FieldType.SUBJECT
            )
        ) isValid = false

        if (!ValidationUtils.validateInputsData(
                requireContext(),
                binding.schoolLevelInputLayout,
                binding.schoolLevelAutoComplete,
                ValidationUtils.FieldType.SCHOOL_LEVEL
            )
        ) isValid = false

        if (!ValidationUtils.validateInputsData(
                requireContext(),
                binding.difficultyInputLayout,
                binding.difficultyAutoComplete,
                ValidationUtils.FieldType.DIFFICULTY
            )
        ) isValid = false

        return isValid
    }

    private fun createLessonFromInputs(): Lesson {
        val currentTeacherId =
            (teacherDashboardViewModel.teacherState.value as? TeacherDashboardViewModel.TeacherState.Success)?.teacher?.id
                ?: ""

        return Lesson(
            id = lesson?.id ?: "",
            title = binding.titleEditText.text.toString(),
            content = binding.contentEditText.text.toString(),
            idSubject = binding.subjectAutoComplete.tag.toString(),
            schoolLevel = binding.schoolLevelAutoComplete.tag.toString(),
            duration = binding.durationEditText.text.toString().toIntOrNull() ?: 0,
            difficulty = binding.difficultyAutoComplete.tag.toString(),
            teacherId = currentTeacherId
        )
    }

    private fun getData() {
        teacherDashboardViewModel.loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 