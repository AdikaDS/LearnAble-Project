package com.adika.learnable.view.dashboard.teacher.form

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogLessonFormBinding
import com.adika.learnable.model.Lesson
import com.adika.learnable.util.EducationLevels
import com.adika.learnable.viewmodel.dashboard.TeacherDashboardViewModel
import com.adika.learnable.viewmodel.lesson.LessonViewModel
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.io.File

@AndroidEntryPoint
class LessonFormBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogLessonFormBinding? = null
    private val binding get() = _binding!!

    private var lesson: Lesson? = null
    private var onSaveListener: ((Lesson) -> Unit)? = null
    private val teacherDashboardViewModel: TeacherDashboardViewModel by viewModels()
    private val lessonViewModel: LessonViewModel by viewModels()

    private var coverImageUrl: String? = null
    private var selectedCoverFileName: String? = null
    private var selectedCoverFile: File? = null
    private var pendingSaveAfterUpload = false

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri = result.data?.data ?: return@registerForActivityResult
                val file = uriToFile(uri)
                if (file != null) {
                    selectedCoverFile = file
                    selectedCoverFileName = file.name
                    coverImageUrl = null
                    binding.tvTitleCoverImage.text = selectedCoverFileName
                    updateSaveEnabled()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.fail_up_picture),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(
                    requireContext(),
                    ImagePicker.getError(result.data),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

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
        observeCoverUpload()
        updateSaveEnabled()
    }

    override fun onStart() {
        super.onStart()

        dialog?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun setupViews() {
        binding.apply {
            tvTitle.text =
                if (lesson == null) getString(R.string.add_lesson) else getString(R.string.edit_lesson)
            btnSave.setOnClickListener {

                if (selectedCoverFile != null && coverImageUrl.isNullOrBlank()) {
                    pendingSaveAfterUpload = true
                    btnSave.isEnabled = false
                    btnUploadCover.isEnabled = false
                    lessonViewModel.uploadLessonCover(selectedCoverFile!!)
                    return@setOnClickListener
                }

                val newLesson = createLessonFromInputs()
                onSaveListener?.invoke(newLesson)
                dismiss()
            }

            tvUploadCover.text = HtmlCompat.fromHtml(
                getString(R.string.upload_cover),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            tvNameLesson.text = HtmlCompat.fromHtml(
                getString(R.string.name_lesson),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            tvSchoolLevel.text = HtmlCompat.fromHtml(
                getString(R.string.school_level_crud),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
            tvSubject.text = HtmlCompat.fromHtml(
                getString(R.string.subject_crud),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            btnCancel.setOnClickListener {
                dismiss()
            }
            btnClose.setOnClickListener {
                dismiss()
            }

            btnUploadCover.setOnClickListener { openImagePicker() }

            btnSave.isEnabled = false

            titleEditText.doAfterTextChanged { updateSaveEnabled() }
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
                    0 -> EducationLevels.SD
                    1 -> EducationLevels.SMP
                    2 -> EducationLevels.SMA
                    else -> ""
                }
                tag = firebaseValue

                binding.subjectAutoComplete.setText("")
                binding.subjectAutoComplete.tag = null

                updateSubjectAdapter(firebaseValue)
                updateSaveEnabled()
            }
        }

        binding.subjectAutoComplete.isEnabled = false


    }

    private fun updateSubjectAdapter(schoolLevel: String) {
        val subjects = when (schoolLevel) {
            EducationLevels.SD -> resources.getStringArray(R.array.elementarySchoolSubjects)
            EducationLevels.SMP -> resources.getStringArray(R.array.juniorHighSchoolSubjects)
            EducationLevels.SMA -> resources.getStringArray(R.array.seniorHighSchoolSubjects)
            else -> emptyArray()
        }

        val adapter = ArrayAdapter(requireContext(), R.layout.item_dropdown, subjects)
        binding.subjectAutoComplete.apply {
            setAdapter(adapter)
            isEnabled = true
            setOnItemClickListener { _, _, position, _ ->
                val firebaseValue = when (schoolLevel) {
                    EducationLevels.SD -> when (position) {
                        0 -> EducationLevels.Subjects.SD.MATH
                        1 -> EducationLevels.Subjects.SD.BAHASA_INDONESIA
                        2 -> EducationLevels.Subjects.SD.NATURAL_AND_SOCIAL_SCIENCE
                        else -> ""
                    }

                    EducationLevels.SMP -> when (position) {
                        0 -> EducationLevels.Subjects.SMP.MATH
                        1 -> EducationLevels.Subjects.SMP.BAHASA_INDONESIA
                        2 -> EducationLevels.Subjects.SMP.NATURAL_SCIENCE
                        else -> ""
                    }

                    EducationLevels.SMA -> when (position) {
                        0 -> EducationLevels.Subjects.SMA.MATH
                        1 -> EducationLevels.Subjects.SMA.BAHASA_INDONESIA
                        2 -> EducationLevels.Subjects.SMA.NATURAL_SCIENCE
                        else -> ""
                    }

                    else -> ""
                }
                tag = firebaseValue
                setText(subjects[position], false)
                updateSaveEnabled()
            }
        }
    }

    private fun fillExistingData() {
        lesson?.let {
            binding.titleEditText.setText(it.title)
            coverImageUrl = it.coverImage
            if (!coverImageUrl.isNullOrBlank()) {
                binding.btnUploadCover.text = getString(R.string.upload_picture)
            }
            updateSaveEnabled()

            val schoolLevelPosition = when (it.schoolLevel) {
                EducationLevels.SD -> 0
                EducationLevels.SMP -> 1
                EducationLevels.SMA -> 2
                else -> 0
            }
            binding.schoolLevelAutoComplete.setText(
                resources.getStringArray(R.array.school_levels)[schoolLevelPosition],
                false
            )
            binding.schoolLevelAutoComplete.tag = it.schoolLevel

            updateSubjectAdapter(it.schoolLevel)

            when (it.schoolLevel) {
                EducationLevels.SD -> {
                    val subjectPosition = when (it.idSubject) {
                        EducationLevels.Subjects.SD.MATH -> 0
                        EducationLevels.Subjects.SD.BAHASA_INDONESIA -> 1
                        EducationLevels.Subjects.SD.NATURAL_AND_SOCIAL_SCIENCE -> 2
                        else -> 0
                    }
                    binding.subjectAutoComplete.setText(
                        resources.getStringArray(R.array.elementarySchoolSubjects)[subjectPosition],
                        false
                    )
                }

                EducationLevels.SMP -> {
                    val subjectPosition = when (it.idSubject) {
                        EducationLevels.Subjects.SMP.MATH -> 0
                        EducationLevels.Subjects.SMP.BAHASA_INDONESIA -> 1
                        EducationLevels.Subjects.SMP.NATURAL_SCIENCE -> 2
                        else -> 0
                    }
                    binding.subjectAutoComplete.setText(
                        resources.getStringArray(R.array.juniorHighSchoolSubjects)[subjectPosition],
                        false
                    )
                }

                EducationLevels.SMA -> {
                    val subjectPosition = when (it.idSubject) {
                        EducationLevels.Subjects.SMA.MATH -> 0
                        EducationLevels.Subjects.SMA.BAHASA_INDONESIA -> 1
                        EducationLevels.Subjects.SMA.NATURAL_SCIENCE -> 2
                        else -> 0
                    }
                    binding.subjectAutoComplete.setText(
                        resources.getStringArray(R.array.seniorHighSchoolSubjects)[subjectPosition],
                        false
                    )
                }
            }
            binding.subjectAutoComplete.tag = it.idSubject
        }
    }


    private fun createLessonFromInputs(): Lesson {
        val currentTeacherId =
            (teacherDashboardViewModel.teacherState.value as? TeacherDashboardViewModel.TeacherState.Success)?.teacher?.id
                ?: ""

        return Lesson(
            id = lesson?.id ?: "",
            title = binding.titleEditText.text.toString(),
            idSubject = binding.subjectAutoComplete.tag.toString(),
            schoolLevel = binding.schoolLevelAutoComplete.tag.toString(),
            teacherId = currentTeacherId,
            coverImage = coverImageUrl
        )
    }

    private fun getData() {
        teacherDashboardViewModel.loadUserData()
    }

    private fun openImagePicker() {
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(1080, 1080)
            .createIntent { intent ->
                imagePickerLauncher.launch(intent)
            }
    }

    private fun uriToFile(uri: Uri): File? {
        return try {
            val contentResolver = requireContext().contentResolver
            val type = contentResolver.getType(uri)
            val extension = when (type) {
                "image/jpeg" -> ".jpg"
                "image/png" -> ".png"
                "image/webp" -> ".webp"
                else -> ".jpg"
            }
            val tempFile =
                File.createTempFile("lesson_cover_", extension, requireContext().cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            null
        }
    }

    private fun observeCoverUpload() {
        lessonViewModel.coverUploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is LessonViewModel.CoverUploadState.Loading -> {
                    binding.btnUploadCover.isEnabled = false
                    binding.btnSave.isEnabled = false
                }

                is LessonViewModel.CoverUploadState.Success -> {
                    coverImageUrl = state.url
                    binding.btnUploadCover.text = getString(R.string.upload_picture)
                    selectedCoverFileName?.let { binding.tvTitleCoverImage.text = it }
                    binding.btnUploadCover.isEnabled = true

                    if (pendingSaveAfterUpload) {
                        pendingSaveAfterUpload = false
                        val newLesson = createLessonFromInputs()
                        onSaveListener?.invoke(newLesson)
                        dismiss()
                    } else {
                        updateSaveEnabled()
                    }
                }

                is LessonViewModel.CoverUploadState.Error -> {
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    binding.btnUploadCover.isEnabled = true
                    pendingSaveAfterUpload = false
                    updateSaveEnabled()
                }
            }
        }
    }

    private fun isFormValid(): Boolean {
        val titleFilled = !binding.titleEditText.text.isNullOrBlank()
        val schoolLevelFilled =
            (binding.schoolLevelAutoComplete.tag as? String)?.isNotBlank() == true
        val subjectFilled = (binding.subjectAutoComplete.tag as? String)?.isNotBlank() == true
        val coverFilled = !coverImageUrl.isNullOrBlank() || selectedCoverFile != null
        return titleFilled && schoolLevelFilled && subjectFilled && coverFilled
    }

    private fun updateSaveEnabled() {
        binding.btnSave.isEnabled = isFormValid()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 