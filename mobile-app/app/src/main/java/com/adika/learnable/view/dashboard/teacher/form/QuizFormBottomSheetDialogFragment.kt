package com.adika.learnable.view.dashboard.teacher.form

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.viewModels
import com.adika.learnable.R
import com.adika.learnable.databinding.DialogQuizFormBinding
import com.adika.learnable.model.OptionItem
import com.adika.learnable.model.QuestionType
import com.adika.learnable.model.Quiz
import com.adika.learnable.model.QuizQuestion
import com.adika.learnable.util.VibrationHelper
import com.adika.learnable.viewmodel.lesson.MaterialViewModel
import com.adika.learnable.viewmodel.lesson.QuizViewModel
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import java.io.File
import java.util.UUID

@AndroidEntryPoint
class QuizFormBottomSheetDialogFragment : BottomSheetDialogFragment() {
    private var _binding: DialogQuizFormBinding? = null
    private val binding get() = _binding!!

    private var quiz: Quiz? = null
    private var subBabId: String? = null
    private var onSaveListener: ((Quiz) -> Unit)? = null
    private val quizViewModel: QuizViewModel by viewModels()
    private val materialViewModel: MaterialViewModel by viewModels()

    private val questionCards = mutableListOf<QuestionCardData>()
    private var quizLoaded = false
    private val vibrationHelper by lazy {
        VibrationHelper(requireContext())
    }

    private val imagePickerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val uri: Uri? = result.data?.data
                if (uri != null) {
                    val file = uriToFile(uri)
                    if (file != null) {
                        handleImageUpload(file)
                    } else {
                        showToast(getString(R.string.failed_process_pict))
                    }
                }
            } else if (result.resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(
                    requireContext(),
                    ImagePicker.getError(result.data),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    private var currentImageUploadContext: ImageUploadContext? = null

    data class ImageUploadContext(
        val questionCardId: String,
        val uploadType: ImageUploadType,
        val optionIndex: Int? = null
    )

    enum class ImageUploadType {
        QUESTION_MEDIA,
        OPTION_MEDIA,
        EXPLANATION_MEDIA
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)

        arguments?.let {
            subBabId = it.getString(ARG_SUBBAB_ID)
        }
    }

    companion object {
        private const val ARG_QUIZ_ID = "quizId"
        private const val ARG_SUBBAB_ID = "subBabId"

        fun newInstance(
            quizId: String? = null,
            subBabId: String? = null,
            onSave: (Quiz) -> Unit
        ): QuizFormBottomSheetDialogFragment {
            return QuizFormBottomSheetDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_QUIZ_ID, quizId)
                    putString(ARG_SUBBAB_ID, subBabId)
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
        _binding = DialogQuizFormBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        observeViewModel()
        fillExistingData()
        updateSaveEnabled()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
    }

    private fun setupViews() {
        binding.apply {

            tvTitle.text = getString(R.string.add_quiz)

            btnSave.setOnClickListener {
                vibrationHelper.vibrateClick()
                saveQuiz()
            }

            btnCancel.setOnClickListener {
                vibrationHelper.vibrateClick()
                dismiss()
            }

            btnClose.setOnClickListener {
                vibrationHelper.vibrateClick()
                dismiss()
            }

            btnBack.setOnClickListener {
                vibrationHelper.vibrateClick()
                dismiss()
            }

            btnAddQuestion.setOnClickListener {
                vibrationHelper.vibrateClick()
                addQuestionCard()
            }

            etPassingScore.doAfterTextChanged {
                updateSaveEnabled()
            }

            tvPassingScoreLabel.text = HtmlCompat.fromHtml(
                getString(R.string.passing_score),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            btnSave.isEnabled = false
        }
    }

    private fun observeViewModel() {

        quizViewModel.quizState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.QuizState.Success -> {

                    if (!quizLoaded) {
                        quizLoaded = true
                        quiz = state.quiz
                        binding.apply {

                            tvTitle.text = getString(R.string.edit_quiz)
                            etPassingScore.setText(state.quiz.passingScore.toInt().toString())

                            questionCards.clear()
                            binding.containerQuestions.removeAllViews()

                            state.quiz.questions.forEach { question ->
                                val questionCard = QuestionCardData(
                                    question = question,
                                    isMultipleChoice = question.questionType != QuestionType.ESSAY
                                )
                                questionCards.add(questionCard)
                                addQuestionCardView(questionCard)
                            }
                            updateSaveEnabled()
                        }
                    }
                }

                else -> {}
            }
        }
    }

    private fun fillExistingData() {
        val quizId = arguments?.getString(ARG_QUIZ_ID)
        if (quizId != null && subBabId != null) {

            quizViewModel.getQuizBySubBabId(subBabId!!)
        } else {

            binding.tvTitle.text = getString(R.string.add_quiz)
        }
    }

    private fun addQuestionCard() {
        val questionCard = QuestionCardData(
            question = QuizQuestion(
                id = UUID.randomUUID().toString(),
                question = "",
                mediaQuestion = "",
                optionItems = emptyList(),
                correctAnswer = 0,
                explanation = "",
                mediaExplanation = "",
                questionType = QuestionType.MULTIPLE_CHOICE
            ),
            isMultipleChoice = true
        )
        questionCards.add(questionCard)
        addQuestionCardView(questionCard)
        updateSaveEnabled()
    }

    private fun addQuestionCardView(questionCard: QuestionCardData): QuestionCardViewHolder {
        val cardView = LayoutInflater.from(requireContext())
            .inflate(R.layout.item_question_card, binding.containerQuestions, false)

        val holder = QuestionCardViewHolder(
            itemView = cardView,
            questionCardData = questionCard,
            onQuestionUpdated = { updatedCard ->
                val index = questionCards.indexOfFirst { it.id == updatedCard.id }
                if (index != -1) {
                    questionCards[index] = updatedCard
                }
                updateSaveEnabled()
            },
            onImagePickerRequest = { uploadType, optionIndex ->
                currentImageUploadContext = ImageUploadContext(
                    questionCardId = questionCard.id,
                    uploadType = uploadType,
                    optionIndex = optionIndex
                )
                openImagePicker()
            }
        )

        holder.setOnDeleteClickListener {
            val index = questionCards.indexOfFirst { it.id == questionCard.id }
            if (index != -1) {
                questionCards.removeAt(index)
                binding.containerQuestions.removeView(cardView)
                updateSaveEnabled()
            }
        }

        binding.containerQuestions.addView(cardView)
        cardView.tag = holder
        return holder
    }

    private fun saveQuiz() {
        val passingScoreText = binding.etPassingScore.text.toString().trim()
        if (passingScoreText.isEmpty()) {
            vibrationHelper.vibrateError()
            showToast(getString(R.string.passed_score_must_be_fill))
            return
        }

        val passingScore = passingScoreText.toFloatOrNull()
        if (passingScore == null || passingScore < 0 || passingScore > 100) {
            vibrationHelper.vibrateError()
            showToast(getString(R.string.passed_score_inbetween))
            return
        }

        if (questionCards.isEmpty()) {
            vibrationHelper.vibrateError()
            showToast(getString(R.string.minimun_question))
            return
        }

        val validatedQuestions = mutableListOf<QuizQuestion>()
        for ((index, card) in questionCards.withIndex()) {
            val question = card.question

            if (question.question.isBlank()) {
                vibrationHelper.vibrateError()
                showToast(getString(R.string.question_must_be_fill, index + 1))
                return
            }

            if (card.isMultipleChoice) {

                if (question.optionItems.size < 2) {
                    vibrationHelper.vibrateError()
                    showToast(getString(R.string.question_more_then1, index + 1))
                    return
                }

                val hasFilledOptions = question.optionItems.count { it.text.isNotBlank() } >= 2
                if (!hasFilledOptions) {
                    vibrationHelper.vibrateError()
                    showToast(getString(R.string.question_more_then1, index + 1))
                    return
                }

                if (question.correctAnswer < 0 || question.correctAnswer >= question.optionItems.size) {
                    vibrationHelper.vibrateError()
                    showToast(getString(R.string.question_must_be_have_answer, index + 1))
                    return
                }
            }

            if (question.explanation.isBlank()) {
                vibrationHelper.vibrateError()
                showToast(getString(R.string.explanation_must_be_filled, index + 1))
                return
            }

            val finalQuestion = if (card.isMultipleChoice) {
                question.copy(questionType = QuestionType.MULTIPLE_CHOICE)
            } else {
                question.copy(
                    questionType = QuestionType.ESSAY,
                    optionItems = emptyList(),
                    correctAnswer = 0
                )
            }

            validatedQuestions.add(finalQuestion)
        }

        val currentSubBabId = subBabId ?: quiz?.subBabId
        if (currentSubBabId.isNullOrEmpty()) {
            vibrationHelper.vibrateError()
            showToast(getString(R.string.subbab_id_not_valid))
            return
        }

        binding.btnSave.isEnabled = false

        val quizId = arguments?.getString(ARG_QUIZ_ID) ?: quiz?.id
        val newQuiz = Quiz(
            id = quizId ?: "",
            subBabId = currentSubBabId,
            title = "",
            questions = validatedQuestions,
            passingScore = passingScore
        )

        if (quizId.isNullOrEmpty()) {

            quizViewModel.createQuiz(newQuiz) { result ->
                result.fold(
                    onSuccess = {
                        vibrationHelper.vibrateSuccess()
                        onSaveListener?.invoke(it)
                        dismiss()
                    },
                    onFailure = {
                        vibrationHelper.vibrateError()
                        binding.btnSave.isEnabled = true
                        showToast(it.message ?: getString(R.string.failed_load_quiz))
                    }
                )
            }
        } else {

            quizViewModel.updateQuiz(newQuiz) { result ->
                result.fold(
                    onSuccess = {
                        vibrationHelper.vibrateSuccess()
                        onSaveListener?.invoke(newQuiz)
                        dismiss()
                    },
                    onFailure = {
                        vibrationHelper.vibrateError()
                        binding.btnSave.isEnabled = true
                        showToast(it.message ?: getString(R.string.failed_update_quiz))
                    }
                )
            }
        }
    }

    private fun isFormValid(): Boolean {
        val passingScoreFilled = !binding.etPassingScore.text.isNullOrBlank()
        val hasQuestions = questionCards.isNotEmpty()
        return passingScoreFilled && hasQuestions
    }

    private fun updateSaveEnabled() {
        binding.btnSave.isEnabled = isFormValid()
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
            val tempFile = File.createTempFile("quiz_media_", extension, requireContext().cacheDir)
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            tempFile
        } catch (e: Exception) {
            Log.e("QuizForm", "Error converting URI to file", e)
            null
        }
    }

    private fun handleImageUpload(file: File) {
        val context = currentImageUploadContext ?: return

        binding.btnSave.isEnabled = false

        materialViewModel.uploadQuizMediaImage(file)

        materialViewModel.quizMediaUploadState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is MaterialViewModel.QuizMediaUploadState.Success -> {
                    updateQuestionCardWithMedia(context, state.url)
                    binding.btnSave.isEnabled = isFormValid()
                    materialViewModel.quizMediaUploadState.removeObservers(viewLifecycleOwner)
                }

                is MaterialViewModel.QuizMediaUploadState.Error -> {
                    vibrationHelper.vibrateError()
                    Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                    binding.btnSave.isEnabled = isFormValid()
                    materialViewModel.quizMediaUploadState.removeObservers(viewLifecycleOwner)
                }

                else -> {}
            }
        }
    }

    private fun updateQuestionCardWithMedia(context: ImageUploadContext, mediaUrl: String) {
        val questionCardIndex = questionCards.indexOfFirst { it.id == context.questionCardId }
        if (questionCardIndex == -1) return

        val questionCard = questionCards[questionCardIndex]
        val question = questionCard.question

        when (context.uploadType) {
            ImageUploadType.QUESTION_MEDIA -> {
                val updatedQuestion = question.copy(mediaQuestion = mediaUrl)
                questionCards[questionCardIndex] = questionCard.copy(question = updatedQuestion)
            }

            ImageUploadType.OPTION_MEDIA -> {
                val optionIndex = context.optionIndex ?: return
                val optionItems = question.optionItems.toMutableList()
                while (optionItems.size <= optionIndex) {
                    optionItems.add(OptionItem())
                }
                optionItems[optionIndex] = optionItems[optionIndex].copy(mediaUrl = mediaUrl)
                val updatedQuestion = question.copy(optionItems = optionItems)
                questionCards[questionCardIndex] = questionCard.copy(question = updatedQuestion)
            }

            ImageUploadType.EXPLANATION_MEDIA -> {
                val updatedQuestion = question.copy(mediaExplanation = mediaUrl)
                questionCards[questionCardIndex] = questionCard.copy(question = updatedQuestion)
            }
        }

        refreshQuestionCardView(context.questionCardId)
    }

    private fun refreshQuestionCardView(questionCardId: String) {

        val questionCardIndex = questionCards.indexOfFirst { it.id == questionCardId }
        if (questionCardIndex == -1) return

        val cardView = binding.containerQuestions.getChildAt(questionCardIndex)
        if (cardView != null) {

            val existingHolder = cardView.tag as? QuestionCardViewHolder
            if (existingHolder != null) {

                val questionCard = questionCards[questionCardIndex]
                existingHolder.updateQuestionCardData(questionCard)
            } else {

                binding.containerQuestions.removeViewAt(questionCardIndex)
                val questionCard = questionCards[questionCardIndex]
                addQuestionCardView(questionCard)

                val newIndex = binding.containerQuestions.childCount - 1
                if (newIndex != questionCardIndex && newIndex > questionCardIndex) {
                    val viewToMove = binding.containerQuestions.getChildAt(newIndex)
                    binding.containerQuestions.removeViewAt(newIndex)
                    binding.containerQuestions.addView(viewToMove, questionCardIndex)
                }
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class QuestionCardData(
        val id: String = UUID.randomUUID().toString(),
        var question: QuizQuestion,
        var isMultipleChoice: Boolean = true
    )
}

