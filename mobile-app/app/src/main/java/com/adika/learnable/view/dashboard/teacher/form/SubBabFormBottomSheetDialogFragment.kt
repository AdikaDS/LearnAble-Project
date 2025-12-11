package com.adika.learnable.view.dashboard.teacher.form

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogSubBabFormBinding
import com.adika.learnable.model.SubBab
import com.adika.learnable.repository.MaterialRepository
import com.adika.learnable.viewmodel.lesson.LessonViewModel
import com.adika.learnable.viewmodel.lesson.MaterialViewModel
import com.adika.learnable.viewmodel.lesson.QuizViewModel
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.io.FileOutputStream

@AndroidEntryPoint
class SubBabFormBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogSubBabFormBinding? = null
    private val binding get() = _binding!!

    private var extension: String = ""
    private var subBab: SubBab? = null
    private var lessonId: String? = null
    private var onSaveListener: ((SubBab) -> Unit)? = null
    private val lessonViewModel: LessonViewModel by viewModels()
    private val materialViewModel: MaterialViewModel by viewModels()
    private val quizViewModel: QuizViewModel by viewModels()

    private var coverImageUrl: String? = null
    private var selectedCoverFileName: String? = null
    private var selectedCoverFile: File? = null
    private var pendingCoverUpload = false

    private var selectedPdfFile: File? = null
    private var selectedPdfFileName: String? = null
    private var selectedVideoFile: File? = null
    private var selectedVideoFileName: String? = null
    private var selectedSubtitleFile: File? = null
    private var selectedSubtitleFileName: String? = null

    private var quizCreated = false
    private var quizExists = false // Track if quiz exists (from observer)
    private var existingQuizId: String? = null // Store quiz ID if exists
    private var savedSubBabId: String? = null

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri = result.data?.data ?: return@registerForActivityResult
                val file = uriToFile(uri)
                if (file != null) {
                    selectedCoverFile = file
                    selectedCoverFileName = file.name
                    coverImageUrl = null
                    binding.tvTitleCoverImageSubBab.text = selectedCoverFileName
                    updateSaveEnabled()
                } else {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.fail_up_picture),
                        Toast.LENGTH_SHORT
                    ).show()
                    updateSaveEnabled()
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(
                    requireContext(),
                    ImagePicker.getError(result.data),
                    Toast.LENGTH_SHORT
                ).show()
                updateSaveEnabled()
            }
        }

    private val pdfPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val file = uriToFile(it)
                if (file != null && file.name.endsWith(".pdf", ignoreCase = true)) {
                    selectedPdfFile = file
                    selectedPdfFileName = file.name
                    binding.tvTitleMaterial.text = selectedPdfFileName
                    updateSaveEnabled()
                } else {
                    showToast(getString(R.string.format_file_pdf))
                }
            }
        }

    private val videoPickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val file = uriToFile(it)
                if (file == null) {
                    showToast(getString(R.string.failed_read_video_file))
                    return@let
                }

                if (isValidVideoFile(file)) {
                    selectedVideoFile = file
                    selectedVideoFileName = file.name
                    binding.tvTitleVideo.text = selectedVideoFileName
                    updateSaveEnabled()
                } else {
                    val extension = file.name.substringAfterLast(".", "tidak diketahui")
                    showToast(getString(R.string.format_file_video, extension))
                }
            }
        }

    private val subtitlePickerLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            uri?.let {
                val file = uriToFile(it)
                if (file != null && file.name.endsWith(".srt", ignoreCase = true)) {
                    selectedSubtitleFile = file
                    selectedSubtitleFileName = file.name
                    binding.tvTitleSubtitle.text = selectedSubtitleFileName
                    updateSaveEnabled()
                } else {
                    showToast(getString(R.string.format_file_subtitle))
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)

        arguments?.let {
            subBab = it.getParcelable(ARG_SUBBAB)
            lessonId = it.getString(ARG_LESSON_ID)
        }
    }

    companion object {
        private const val ARG_SUBBAB = "subBab"
        private const val ARG_LESSON_ID = "lessonId"
        private const val PDF = "pdfLesson"
        private const val VIDEO = "video"
        fun newInstance(
            subBab: SubBab? = null,
            lessonId: String? = null,
            onSave: (SubBab) -> Unit
        ): SubBabFormBottomSheetDialogFragment {
            return SubBabFormBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_SUBBAB, subBab)
                    putString(ARG_LESSON_ID, lessonId)
                }
                this.onSaveListener = onSave
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogSubBabFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        fillExistingData()
        observeViewModel()
        checkExistingQuiz()
        updateSaveEnabled()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun setupViews() {
        binding.apply {
            tvTitle.text =
                if (subBab == null) getString(R.string.add_subbab) else getString(R.string.edit_subbab)

            btnSave.setOnClickListener {
                saveSubBab()
            }

            btnCancel.setOnClickListener {
                dismiss()
            }

            btnClose.setOnClickListener {
                dismiss()
            }

            btnUploadCoverSubBab.setOnClickListener {
                openImagePicker()
            }

            pdfSelectButton.setOnClickListener {
                pdfPickerLauncher.launch("application/pdf")
            }

            videoSelectButton.setOnClickListener {
                videoPickerLauncher.launch("video/*")
            }

            subtitleSelectButton.setOnClickListener {
                subtitlePickerLauncher.launch("*/*")
            }

            btnAddQuiz.setOnClickListener {

                val currentSubBabId = savedSubBabId ?: subBab?.id
                if (currentSubBabId.isNullOrEmpty()) {

                    saveSubBabForQuiz()
                } else {
                    openQuizDialog(currentSubBabId)
                }
            }

            etSubBabTitle.doAfterTextChanged {
                updateSaveEnabled()
            }

            tvUploadCover.text = HtmlCompat.fromHtml(
                getString(R.string.upload_cover),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            tvNameSubab.text = HtmlCompat.fromHtml(
                getString(R.string.name_subbab),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            tvUploadVideo.text = HtmlCompat.fromHtml(
                getString(R.string.learning_video),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            tvUploadMaterial.text = HtmlCompat.fromHtml(
                getString(R.string.learning_material),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            tvQuiz.text = HtmlCompat.fromHtml(
                getString(R.string.quiz_crud),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            ivDoneAddQuiz.visibility = View.GONE

            btnSave.isEnabled = false
        }
    }

    private fun fillExistingData() {
        subBab?.let {
            binding.apply {
                etSubBabTitle.setText(it.title)

                coverImageUrl = it.coverImage
                if (!coverImageUrl.isNullOrBlank()) {
                    tvTitleCoverImageSubBab.text = getString(R.string.cover_uploaded)
                }

                val pdfUrl = it.mediaUrls[PDF]
                if (!pdfUrl.isNullOrBlank()) {
                    tvTitleMaterial.text = getString(R.string.pdf_uploaded)
                }

                val videoUrl = it.mediaUrls[VIDEO]
                if (!videoUrl.isNullOrBlank()) {
                    tvTitleVideo.text = getString(R.string.video_uploaded)
                }

                if (it.subtitle.isNotBlank()) {
                    tvTitleSubtitle.text = getString(R.string.subtitle_uploaded)
                }
            }
        }
    }

    private fun observeViewModel() {

        materialViewModel.materialState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MaterialViewModel.MaterialState.Loading -> {
                    showProgress(true)
                }

                is MaterialViewModel.MaterialState.Success -> {

                }

                is MaterialViewModel.MaterialState.Error -> {
                    showProgress(false)
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = true
                }
            }
        }

        materialViewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            binding.apply {
                progressText.text = progress.message
                progressBar.progress = (progress.completed * 100 / progress.total).coerceAtMost(100)
            }
        }

        materialViewModel.coverUploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MaterialViewModel.CoverUploadState.Loading -> {
                    binding.btnUploadCoverSubBab.isEnabled = false
                    binding.btnSave.isEnabled = false
                }

                is MaterialViewModel.CoverUploadState.Success -> {
                    coverImageUrl = state.url
                    binding.btnUploadCoverSubBab.isEnabled = true
                    if (!pendingCoverUpload) {
                        updateSaveEnabled()
                    }
                }

                is MaterialViewModel.CoverUploadState.Error -> {
                    binding.btnUploadCoverSubBab.isEnabled = true
                    binding.btnSave.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    pendingCoverUpload = false
                }
            }
        }

        quizViewModel.quizState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.QuizState.Success -> {

                    quizExists = true
                    quizCreated = true // Also set this for consistency
                    existingQuizId = state.quiz.id // Store quiz ID for editing
                    updateSaveEnabled()
                }

                is QuizViewModel.QuizState.Error -> {

                    quizExists = false
                    existingQuizId = null // Clear quiz ID

                    updateSaveEnabled()
                }

                else -> {}
            }
        }
    }

    private fun onSubBabSaved(resultSubBab: SubBab) {
        savedSubBabId = resultSubBab.id
        onSaveListener?.invoke(resultSubBab)
        dismiss()
    }

    private fun saveSubBab() {
        val title = binding.etSubBabTitle.text.toString().trim()
        if (title.isEmpty()) {
            showToast(getString(R.string.fill_name_subbab_first))
            return
        }

        val currentLessonId = lessonId ?: subBab?.lessonId
        if (currentLessonId.isNullOrEmpty()) {
            getString(R.string.lesson_id_invalid)
            return
        }

        binding.btnSave.isEnabled = false

        val pendingUploads = mutableMapOf<String, Pair<File, String>>()

        selectedPdfFile?.let {
            pendingUploads[MaterialRepository.MaterialType.PDF.name] = Pair(it, it.name)
        }

        selectedVideoFile?.let {
            pendingUploads[MaterialRepository.MaterialType.VIDEO.name] = Pair(it, it.name)
        }

        selectedSubtitleFile?.let {
            pendingUploads[MaterialRepository.MaterialType.SUBTITLE.name] = Pair(it, it.name)
        }

        val newSubBab = createSubBabFromInputs(currentLessonId)

        val existingSubBabId = savedSubBabId ?: subBab?.id

        if (existingSubBabId == null) {

            lessonViewModel.addSubBab(newSubBab) { result ->
                result.fold(
                    onSuccess = { createdSubBab ->
                        savedSubBabId = createdSubBab.id

                        checkExistingQuiz()

                        if (selectedCoverFile != null) {
                            pendingCoverUpload = true
                            materialViewModel.uploadSubBabCover(
                                selectedCoverFile!!,
                                createdSubBab.id
                            )

                            setupCoverUploadObserver(createdSubBab, pendingUploads)
                        } else if (pendingUploads.isNotEmpty()) {

                            materialViewModel.createSubBabAndUploadFiles(
                                createdSubBab,
                                pendingUploads
                            )
                            observeMaterialUploadAndFinish(createdSubBab)
                        } else {

                            onSubBabSaved(createdSubBab)
                        }
                    },
                    onFailure = {
                        binding.btnSave.isEnabled = true
                        showToast(it.message ?: getString(R.string.failed_load_subbab))
                    }
                )
            }
        } else {

            val updatedSubBab = newSubBab.copy(id = existingSubBabId)

            if (selectedCoverFile != null) {
                pendingCoverUpload = true
                materialViewModel.uploadSubBabCover(selectedCoverFile!!, updatedSubBab.id)
                setupCoverUploadObserver(updatedSubBab, pendingUploads, isUpdate = true)
            } else if (pendingUploads.isNotEmpty()) {

                materialViewModel.updateSubBabAndUploadFiles(updatedSubBab, pendingUploads)
                observeMaterialUploadAndFinish(updatedSubBab)
            } else {

                lessonViewModel.updateSubBab(updatedSubBab) { result ->
                    result.fold(
                        onSuccess = {
                            onSubBabSaved(updatedSubBab)
                        },
                        onFailure = {
                            binding.btnSave.isEnabled = true
                            showToast(it.message ?: getString(R.string.failed_update_subbab))
                        }
                    )
                }
            }
        }
    }

    private fun setupCoverUploadObserver(
        subBab: SubBab,
        pendingUploads: Map<String, Pair<File, String>>,
        isUpdate: Boolean = false
    ) {
        var coverUploadHandled = false

        materialViewModel.coverUploadState.observe(viewLifecycleOwner) { state ->
            if (coverUploadHandled) return@observe

            when (state) {
                is MaterialViewModel.CoverUploadState.Success -> {
                    coverUploadHandled = true
                    pendingCoverUpload = false
                    val updatedSubBab = subBab.copy(coverImage = state.url)

                    lessonViewModel.updateSubBab(updatedSubBab) { result ->
                        result.fold(
                            onSuccess = {

                                if (pendingUploads.isNotEmpty()) {
                                    if (isUpdate) {
                                        materialViewModel.updateSubBabAndUploadFiles(
                                            updatedSubBab,
                                            pendingUploads
                                        )
                                    } else {
                                        materialViewModel.createSubBabAndUploadFiles(
                                            updatedSubBab,
                                            pendingUploads
                                        )
                                    }
                                    observeMaterialUploadAndFinish(updatedSubBab)
                                } else {

                                    onSubBabSaved(updatedSubBab)
                                }
                            },
                            onFailure = {
                                binding.btnSave.isEnabled = true
                                showToast(it.message ?: getString(R.string.failed_update_cover))
                            }
                        )
                    }
                }

                is MaterialViewModel.CoverUploadState.Error -> {
                    coverUploadHandled = true
                    pendingCoverUpload = false
                    binding.btnSave.isEnabled = true
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                }

                else -> {}
            }
        }
    }

    private fun observeMaterialUploadAndFinish(subBab: SubBab) {
        var uploadCompleted = false

        materialViewModel.uploadProgress.observe(viewLifecycleOwner) { progress ->
            if (progress.completed == progress.total && progress.total > 0 && !uploadCompleted) {
                uploadCompleted = true
                showProgress(false)

                checkExistingQuiz()
                onSubBabSaved(subBab)
            }
        }

        materialViewModel.materialState.observe(viewLifecycleOwner) { state ->
            if (state is MaterialViewModel.MaterialState.Error && !uploadCompleted) {
                binding.btnSave.isEnabled = true
                showProgress(false)
            }
        }
    }

    private fun createSubBabFromInputs(currentLessonId: String): SubBab {
        val mediaUrls = mutableMapOf<String, String>()

        subBab?.mediaUrls?.let {
            mediaUrls.putAll(it)
        }

        return SubBab(
            id = subBab?.id ?: "",
            lessonId = currentLessonId,
            title = binding.etSubBabTitle.text.toString().trim(),
            mediaUrls = mediaUrls,
            subtitle = subBab?.subtitle ?: ""
        )
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
            val inputStream = contentResolver.openInputStream(uri) ?: return null

            var fileName: String? = null

            try {
                val cursor = contentResolver.query(uri, null, null, null, null)
                cursor?.use {
                    if (it.moveToFirst()) {
                        val nameIndex =
                            it.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (nameIndex >= 0) {
                            fileName = it.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {

            }

            val mimeType = contentResolver.getType(uri)
            extension = when {
                mimeType?.contains("pdf") == true -> ".pdf"
                mimeType?.contains("video/mp4") == true -> ".mp4"
                mimeType?.contains("video/quicktime") == true || mimeType?.contains("video/x-quicktime") == true -> ".mov"
                mimeType?.contains("video/x-msvideo") == true -> ".avi"
                mimeType?.contains("video") == true -> {

                    fileName?.substringAfterLast(".", "")?.let {
                        if (it.lowercase() in listOf("mp4", "mov", "avi")) {
                            ".$it"
                        } else {
                            ".mp4" // default for video
                        }
                    } ?: ".mp4"
                }

                mimeType?.contains("text") == true -> ".srt"
                mimeType?.contains("image/jpeg") == true -> ".jpg"
                mimeType?.contains("image/png") == true -> ".png"
                mimeType?.contains("image/webp") == true -> ".webp"
                fileName != null -> {

                    fileName!!.substringAfterLast(".", "").let {
                        if (it.isNotEmpty()) ".$it" else ".tmp"
                    }
                }

                uri.path?.substringAfterLast(".") != null -> {
                    uri.path?.substringAfterLast(".")?.let { ".$it" } ?: ".tmp"
                }

                else -> ".tmp"
            }

            val tempFile = File.createTempFile("subbab_file_", extension, requireContext().cacheDir)
            FileOutputStream(tempFile).use { output ->
                inputStream.copyTo(output)
            }

            if (fileName != null && fileName!!.isNotEmpty()) {
                val renamedFile = fileName?.let { File(tempFile.parent, it) }
                if (renamedFile?.let { tempFile.renameTo(it) } == true) {
                    return renamedFile
                }
            }

            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isValidVideoFile(file: File): Boolean {
        val extension = file.name.substringAfterLast(".", "").lowercase()
        return extension in listOf("mp4", "mov", "avi")
    }

    private fun isFormValid(): Boolean {
        val titleFilled = !binding.etSubBabTitle.text.isNullOrBlank()

        val coverValid = selectedCoverFile != null || !coverImageUrl.isNullOrBlank()

        val pdfValid = selectedPdfFile != null ||
                (subBab?.mediaUrls?.get(PDF)?.isNotBlank() == true)

        val videoValid = selectedVideoFile != null ||
                (subBab?.mediaUrls?.get(VIDEO)?.isNotBlank() == true)

        val quizValid = quizCreated || quizExists

        return titleFilled && coverValid && pdfValid && videoValid && quizValid
    }

    private fun checkExistingQuiz() {
        val subBabId = savedSubBabId ?: subBab?.id
        if (subBabId != null) {
            quizViewModel.getQuizBySubBabId(subBabId)
        }
    }

    private fun updateSaveEnabled() {
        val isValid = isFormValid()
        binding.btnSave.isEnabled = isValid

        val hasQuiz = quizCreated || quizExists
        binding.ivDoneAddQuiz.visibility = if (hasQuiz) {
            View.VISIBLE
        } else {
            View.GONE
        }

        binding.btnAddQuiz.text = if (hasQuiz) {
            getString(R.string.edit_quiz)
        } else {
            getString(R.string.add_quiz)
        }
    }

    private fun showProgress(show: Boolean) {
        binding.apply {
            layoutProgress.visibility = if (show) View.VISIBLE else View.GONE
            if (show) {
                progressBar.progress = 0
                progressText.text = getString(R.string.processing)
            }
        }
    }

    private fun saveSubBabForQuiz() {
        val title = binding.etSubBabTitle.text.toString().trim()
        if (title.isEmpty()) {
            showToast(getString(R.string.fill_name_subbab_first))
            return
        }

        val currentLessonId = lessonId ?: subBab?.lessonId
        if (currentLessonId.isNullOrEmpty()) {
            showToast(getString(R.string.lesson_id_invalid))
            return
        }

        val minimalSubBab = createSubBabFromInputs(currentLessonId)

        if (savedSubBabId != null) {

            val updatedSubBab = minimalSubBab.copy(id = savedSubBabId!!)
            lessonViewModel.updateSubBab(updatedSubBab) { result ->
                result.fold(
                    onSuccess = {

                        checkExistingQuiz()

                        openQuizDialog(savedSubBabId!!)
                    },
                    onFailure = {
                        showToast(it.message ?: getString(R.string.failed_update_subbab))
                    }
                )
            }
        } else {

            lessonViewModel.addSubBab(minimalSubBab) { result ->
                result.fold(
                    onSuccess = { createdSubBab ->
                        savedSubBabId = createdSubBab.id

                        checkExistingQuiz()

                        openQuizDialog(createdSubBab.id)
                    },
                    onFailure = {
                        showToast(it.message ?: getString(R.string.failed_save_subbab))
                    }
                )
            }
        }
    }

    private fun openQuizDialog(subBabId: String) {

        val quizId = existingQuizId

        QuizFormBottomSheetDialogFragment.newInstance(
            quizId = quizId, // Pass quiz ID if exists (for edit), null for create
            subBabId = subBabId,
            onSave = { quiz ->

                quizCreated = true
                quizExists = true
                existingQuizId = quiz.id // Store quiz ID after save
                showToast(getString(R.string.quiz_success_added))

                updateSaveEnabled()

            }
        ).show(childFragmentManager, "QuizFormDialog")
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

