package com.adika.learnable.view.dashboard.teacher.form

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RadioButton
import androidx.core.text.HtmlCompat
import androidx.core.widget.doAfterTextChanged
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemQuestionCardBinding
import com.adika.learnable.model.OptionItem
import com.adika.learnable.model.QuestionType
import com.google.android.material.textfield.TextInputEditText

class QuestionCardViewHolder(
    val itemView: View,
    private var questionCardData: QuizFormBottomSheetDialogFragment.QuestionCardData,
    private val onQuestionUpdated: (QuizFormBottomSheetDialogFragment.QuestionCardData) -> Unit,
    private val onImagePickerRequest: (QuizFormBottomSheetDialogFragment.ImageUploadType, Int?) -> Unit
) {
    private val binding = ItemQuestionCardBinding.bind(itemView)
    private var onDeleteClickListener: (() -> Unit)? = null

    init {
        setupViews()
        updateQuestionType()
        fillExistingData()
    }

    private fun setupViews() {
        binding.apply {

            btnMC.setOnClickListener {
                questionCardData.isMultipleChoice = true
                questionCardData.question =
                    questionCardData.question.copy(questionType = QuestionType.MULTIPLE_CHOICE)
                updateQuestionType()
                onQuestionUpdated(questionCardData)
            }

            btnEssay.setOnClickListener {
                questionCardData.isMultipleChoice = false
                questionCardData.question =
                    questionCardData.question.copy(
                        questionType = QuestionType.ESSAY,
                        optionItems = emptyList(),
                        correctAnswer = 0
                    )
                updateQuestionType()
                onQuestionUpdated(questionCardData)
            }

            btnDelete.setOnClickListener {
                onDeleteClickListener?.invoke()
            }

            etQuestionMC.doAfterTextChanged { text ->
                questionCardData.question =
                    questionCardData.question.copy(question = text.toString())
                onQuestionUpdated(questionCardData)
            }

            etQuestionEssay.doAfterTextChanged { text ->
                questionCardData.question =
                    questionCardData.question.copy(question = text.toString())
                onQuestionUpdated(questionCardData)
            }

            setupOption(1, etOption1, radioOption1, btnAttachOption1, ivCorrectOption1)
            setupOption(2, etOption2, radioOption2, btnAttachOption2, ivCorrectOption2)
            setupOption(3, etOption3, radioOption3, btnAttachOption3, ivCorrectOption3)
            setupOption(4, etOption4, radioOption4, btnAttachOption4, ivCorrectOption4)

            etMCExplanation.doAfterTextChanged { text ->
                questionCardData.question =
                    questionCardData.question.copy(explanation = text.toString())
                onQuestionUpdated(questionCardData)
            }

            etEssayExplanation.doAfterTextChanged { text ->
                questionCardData.question =
                    questionCardData.question.copy(explanation = text.toString())
                onQuestionUpdated(questionCardData)
            }

            btnAttachQuestionMC.setOnClickListener {
                onImagePickerRequest(
                    QuizFormBottomSheetDialogFragment.ImageUploadType.QUESTION_MEDIA,
                    null
                )
            }

            btnAttachQuestionEssay.setOnClickListener {
                onImagePickerRequest(
                    QuizFormBottomSheetDialogFragment.ImageUploadType.QUESTION_MEDIA,
                    null
                )
            }

            btnAttachMCExplanation.setOnClickListener {
                onImagePickerRequest(
                    QuizFormBottomSheetDialogFragment.ImageUploadType.EXPLANATION_MEDIA,
                    null
                )
            }

            btnAttachEssayExplanation.setOnClickListener {
                onImagePickerRequest(
                    QuizFormBottomSheetDialogFragment.ImageUploadType.EXPLANATION_MEDIA,
                    null
                )
            }

            tvQuestionMC.text = HtmlCompat.fromHtml(
                itemView.context.getString(R.string.question),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            tvQuestionEssay.text = HtmlCompat.fromHtml(
                itemView.context.getString(R.string.question),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            tvExplanationMCQuestion.text = HtmlCompat.fromHtml(
                itemView.context.getString(R.string.explanation_answer),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )

            tvExplanationEssayQuestion.text = HtmlCompat.fromHtml(
                itemView.context.getString(R.string.explanation_answer),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }
    }

    private fun setupOption(
        optionIndex: Int,
        editText: TextInputEditText,
        radioButton: RadioButton,
        attachButton: ImageButton,
        correctIndicator: ImageView
    ) {
        editText.doAfterTextChanged { text ->
            val optionItems = questionCardData.question.optionItems.toMutableList()

            while (optionItems.size < optionIndex) {
                optionItems.add(OptionItem())
            }
            if (optionItems.size == optionIndex - 1) {
                optionItems.add(OptionItem(text = text.toString()))
            } else {
                optionItems[optionIndex - 1] =
                    optionItems[optionIndex - 1].copy(text = text.toString())
            }
            questionCardData.question = questionCardData.question.copy(optionItems = optionItems)
            updateOptionMediaIndicator(optionIndex - 1, correctIndicator)
            onQuestionUpdated(questionCardData)
        }

        radioButton.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                questionCardData.question =
                    questionCardData.question.copy(correctAnswer = optionIndex - 1)
                updateCorrectAnswerIndicators()
                onQuestionUpdated(questionCardData)
            }
        }

        attachButton.setOnClickListener {
            onImagePickerRequest(
                QuizFormBottomSheetDialogFragment.ImageUploadType.OPTION_MEDIA,
                optionIndex - 1
            )
        }

        updateOptionMediaIndicator(optionIndex - 1, correctIndicator)
    }

    private fun updateOptionMediaIndicator(optionIndex: Int, correctIndicator: ImageView) {
        val optionItems = questionCardData.question.optionItems
        if (optionIndex < optionItems.size) {
            val option = optionItems[optionIndex]

            correctIndicator.visibility = if (option.mediaUrl.isNotBlank()) {
                View.VISIBLE
            } else {
                View.GONE
            }
        } else {
            correctIndicator.visibility = View.GONE
        }
    }

    private fun updateQuestionType() {
        binding.apply {
            val ctx = root.context
            if (questionCardData.isMultipleChoice) {

                btnMC.setTextColor(ctx.getColor(R.color.button_blue))
                btnMC.backgroundTintList = ctx.getColorStateList(R.color.button_blue)
                btnEssay.setTextColor(ctx.getColor(R.color.grey))
                btnEssay.backgroundTintList = ctx.getColorStateList(R.color.grey)

                layoutMC.visibility = View.VISIBLE
                layoutEssay.visibility = View.GONE
            } else {

                btnMC.setTextColor(ctx.getColor(R.color.grey))
                btnMC.backgroundTintList = ctx.getColorStateList(R.color.grey)
                btnEssay.setTextColor(ctx.getColor(R.color.button_blue))
                btnEssay.backgroundTintList = ctx.getColorStateList(R.color.button_blue)

                layoutMC.visibility = View.GONE
                layoutEssay.visibility = View.VISIBLE
            }
        }
    }

    private fun updateCorrectAnswerIndicators() {
        binding.apply {
            val correctAnswer = questionCardData.question.correctAnswer

            radioOption1.isChecked = correctAnswer == 0
            radioOption2.isChecked = correctAnswer == 1
            radioOption3.isChecked = correctAnswer == 2
            radioOption4.isChecked = correctAnswer == 3

        }
    }

    private fun updateAllOptionMediaIndicators() {
        binding.apply {
            updateOptionMediaIndicator(0, ivCorrectOption1)
            updateOptionMediaIndicator(1, ivCorrectOption2)
            updateOptionMediaIndicator(2, ivCorrectOption3)
            updateOptionMediaIndicator(3, ivCorrectOption4)
        }
    }

    private fun fillExistingData() {
        val question = questionCardData.question

        binding.apply {

            etQuestionMC.setText(question.question)
            etQuestionEssay.setText(question.question)

            if (question.optionItems.isNotEmpty()) {
                if (question.optionItems.size > 0) etOption1.setText(question.optionItems[0].text)
                if (question.optionItems.size > 1) etOption2.setText(question.optionItems[1].text)
                if (question.optionItems.size > 2) etOption3.setText(question.optionItems[2].text)
                if (question.optionItems.size > 3) etOption4.setText(question.optionItems[3].text)
            }

            etMCExplanation.setText(question.explanation)
            etEssayExplanation.setText(question.explanation)

            updateCorrectAnswerIndicators()

            updateAllOptionMediaIndicators()

            updateQuestionAndExplanationMediaIndicators(question)
        }
    }

    private fun updateQuestionAndExplanationMediaIndicators(question: com.adika.learnable.model.QuizQuestion) {
        binding.apply {

            ivDoneQuestionMCAttach.visibility = if (question.mediaQuestion.isNotBlank()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            ivDoneQuestionEssayAttach.visibility = if (question.mediaQuestion.isNotBlank()) {
                View.VISIBLE
            } else {
                View.GONE
            }

            ivDoneQuestionExplanationMCAttach.visibility =
                if (question.mediaExplanation.isNotBlank()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

            ivDoneQuestionExplanationEssayAttach.visibility =
                if (question.mediaExplanation.isNotBlank()) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
        }
    }

    fun setOnDeleteClickListener(listener: () -> Unit) {
        onDeleteClickListener = listener
    }

    fun updateQuestionCardData(newData: QuizFormBottomSheetDialogFragment.QuestionCardData) {
        questionCardData = newData
        fillExistingData()
    }
}