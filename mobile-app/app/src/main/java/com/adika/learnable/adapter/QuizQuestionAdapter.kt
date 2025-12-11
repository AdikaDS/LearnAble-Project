package com.adika.learnable.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemQuizQuestionBinding
import com.adika.learnable.model.OptionItem
import com.adika.learnable.model.QuestionType
import com.adika.learnable.model.QuizQuestion
import com.adika.learnable.repository.TextScaleRepository
import com.adika.learnable.util.TextScaleUtils
import com.bumptech.glide.Glide

class QuizQuestionAdapter(
    private val onAnswerSelected: (Int, Int) -> Unit,
    private val onEssayAnswerChanged: (Int, Boolean) -> Unit = { _, _ -> }
) : ListAdapter<QuizQuestion, QuizQuestionAdapter.QuizQuestionViewHolder>(QuizQuestionDiffCallback()) {

    private val selectedAnswers = mutableMapOf<Int, Int>()
    private var isReviewMode = false
    private var correctAnswers = mutableMapOf<Int, Int>()
    private val essayAnswers = mutableMapOf<String, String>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuizQuestionViewHolder {
        val binding = ItemQuizQuestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuizQuestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuizQuestionViewHolder, position: Int) {
        val question = getItem(position)
        holder.bind(
            question,
            position,
            selectedAnswers[position] ?: -1,
            isReviewMode,
            correctAnswers[position] ?: -1
        )
    }

    fun getSelectedAnswers(): List<Int> {
        return List(currentList.size) { index ->
            selectedAnswers[index] ?: -1
        }
    }

    fun getEssayAnswers(): Map<String, String> = essayAnswers.toMap()

    @SuppressLint("NotifyDataSetChanged")
    fun setEssayAnswers(answers: Map<String, String>) {
        essayAnswers.clear()
        essayAnswers.putAll(answers)
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setSelectedAnswers(answers: List<Int>) {
        selectedAnswers.clear()
        answers.forEachIndexed { index, value ->
            selectedAnswers[index] = value
        }
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun setReviewMode(questions: List<QuizQuestion>) {
        isReviewMode = true
        correctAnswers.clear()
        questions.forEachIndexed { index, question ->
            correctAnswers[index] = question.correctAnswer
        }
        notifyDataSetChanged()
    }

    inner class QuizQuestionViewHolder(
        private val binding: ItemQuizQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private var essayWatcher: TextWatcher? = null

        @SuppressLint("SetTextI18n")
        fun bind(
            question: QuizQuestion,
            position: Int,
            selectedOption: Int,
            isReviewMode: Boolean,
            correctAnswer: Int
        ) {
            binding.apply {
                questionText.text = question.question
                if (question.mediaQuestion.isNotBlank()) {
                    mediaQuestion.visibility = View.VISIBLE
                    Glide.with(root.context)
                        .load(question.mediaQuestion)
                        .placeholder(R.drawable.ic_video_placeholder)
                        .error(R.drawable.ic_error)
                        .into(mediaQuestion)
                } else {
                    mediaQuestion.visibility = View.GONE
                }

                when (question.questionType) {
                    QuestionType.ESSAY -> {
                        showEssayInput(question, position, isReviewMode)
                    }

                    QuestionType.MULTIPLE_CHOICE -> {
                        showMultipleChoice(
                            question,
                            position,
                            isReviewMode,
                            selectedOption,
                            correctAnswer
                        )
                    }
                }
                if (isReviewMode) {
                    explanationTitle.visibility = View.VISIBLE
                    explanationText.visibility = View.VISIBLE
                    explanationText.text = question.explanation
                    if (question.mediaExplanation.isNotBlank()) {
                        mediaExplanation.visibility = View.VISIBLE
                        Glide.with(root.context)
                            .load(question.mediaExplanation)
                            .placeholder(R.drawable.ic_video_placeholder)
                            .error(R.drawable.ic_error)
                            .into(mediaExplanation)
                    } else {
                        mediaExplanation.visibility = View.GONE
                    }

                    if (question.questionType == QuestionType.MULTIPLE_CHOICE) {
                        rightAnswer.visibility = View.VISIBLE
                        wcText.visibility = View.VISIBLE
                        val isCorrect = selectedOption == correctAnswer
                        wcText.setText(if (isCorrect) R.string.answer_correct else R.string.answer_wrong)
                        wcText.setTextColor(
                            ContextCompat.getColor(
                                root.context,
                                if (isCorrect) R.color.green else R.color.error
                            )
                        )

                        val letter = when (correctAnswer) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            3 -> "D"
                            else -> "-"
                        }
                        rightAnswer.text = root.context.getString(R.string.right_answer, letter)
                    } else {
                        rightAnswer.visibility = View.GONE
                        wcText.visibility = View.GONE
                    }
                } else {
                    explanationTitle.visibility = View.GONE
                    explanationText.visibility = View.GONE
                    mediaExplanation.visibility = View.GONE
                    wcText.visibility = View.GONE
                    rightAnswer.visibility = View.GONE
                }
                val textScaleRepository = TextScaleRepository(binding.root.context)
                val scale = textScaleRepository.getScale()
                TextScaleUtils.applyScaleToHierarchy(binding.root, scale)
            }
        }

        @SuppressLint("SetTextI18n")
        private fun showMultipleChoice(
            question: QuizQuestion,
            position: Int,
            isReviewMode: Boolean,
            selectedOption: Int,
            correctAnswer: Int
        ) {
            binding.apply {
                optionsContainer.visibility = View.VISIBLE
                essayInputLayout.visibility = View.GONE
                essayWatcher?.let {
                    essayAnswerInput.removeTextChangedListener(it)
                    essayWatcher = null
                }
                essayAnswerInput.text = null
                essayAnswerInput.clearFocus()
            }

            if (question.optionItems.size >= 4) {
                binding.option1Text.text = "A. ${question.optionItems[0].text}"
                binding.option2Text.text = "B. ${question.optionItems[1].text}"
                binding.option3Text.text = "C. ${question.optionItems[2].text}"
                binding.option4Text.text = "D. ${question.optionItems[3].text}"
            } else {
                binding.option1Text.text = "A. ${question.optionItems.getOrNull(0)?.text ?: ""}"
                binding.option2Text.text = "B. ${question.optionItems.getOrNull(1)?.text ?: ""}"
                binding.option3Text.text = "C. ${question.optionItems.getOrNull(2)?.text ?: ""}"
                binding.option4Text.text = "D. ${question.optionItems.getOrNull(3)?.text ?: ""}"
            }

            handleOptionMedia(question.optionItems.getOrNull(0), binding.option1Image)
            handleOptionMedia(question.optionItems.getOrNull(1), binding.option2Image)
            handleOptionMedia(question.optionItems.getOrNull(2), binding.option3Image)
            handleOptionMedia(question.optionItems.getOrNull(3), binding.option4Image)

            setupCardClickListeners(position, isReviewMode)
            updateCardStates(selectedOption, isReviewMode, correctAnswer)
        }

        private fun showEssayInput(
            question: QuizQuestion,
            position: Int,
            isReviewMode: Boolean
        ) {
            binding.apply {
                optionsContainer.visibility = View.GONE
                essayInputLayout.visibility = View.VISIBLE
                essayAnswerInput.isEnabled = !isReviewMode

                essayWatcher?.let {
                    essayAnswerInput.removeTextChangedListener(it)
                    essayWatcher = null
                }
                val existingAnswer = essayAnswers[question.id] ?: ""
                if (essayAnswerInput.text?.toString() != existingAnswer) {
                    essayAnswerInput.setText(existingAnswer)
                    essayAnswerInput.text?.let { editable ->
                        essayAnswerInput.setSelection(editable.length)
                    }
                }
                onEssayAnswerChanged(position, existingAnswer.isNotBlank())

                if (!isReviewMode) {
                    essayWatcher = object : TextWatcher {
                        override fun beforeTextChanged(
                            s: CharSequence?,
                            start: Int,
                            count: Int,
                            after: Int
                        ) {
                        }

                        override fun onTextChanged(
                            s: CharSequence?,
                            start: Int,
                            before: Int,
                            count: Int
                        ) {
                        }

                        override fun afterTextChanged(s: Editable?) {
                            val value = s?.toString() ?: ""
                            essayAnswers[question.id] = value
                            onEssayAnswerChanged(position, value.isNotBlank())
                        }
                    }
                    essayAnswerInput.addTextChangedListener(essayWatcher)
                }
            }
        }

        private fun setupCardClickListeners(position: Int, isReviewMode: Boolean) {
            if (isReviewMode) return // Tidak bisa klik saat review mode

            binding.apply {
                option1Card.setOnClickListener { selectOption(position, 0) }
                option2Card.setOnClickListener { selectOption(position, 1) }
                option3Card.setOnClickListener { selectOption(position, 2) }
                option4Card.setOnClickListener { selectOption(position, 3) }
            }
        }

        private fun selectOption(position: Int, optionIndex: Int) {
            selectedAnswers[position] = optionIndex
            onAnswerSelected(position, optionIndex)
            updateCardStates(optionIndex, false, -1)
        }

        private fun updateCardStates(
            selectedOption: Int,
            isReviewMode: Boolean,
            correctAnswer: Int
        ) {
            val context = binding.root.context
            resetAllCards(context)

            if (isReviewMode && correctAnswer != -1) {
                updateCardsForReview(context, selectedOption, correctAnswer)
            } else if (selectedOption != -1) {
                updateCardsForSelection(context, selectedOption)
            }
        }

        private fun resetAllCards(context: Context) {
            val defaultStrokeColor = ContextCompat.getColor(context, R.color.stroke_card_quiz)
            binding.apply {
                option1Card.strokeColor = defaultStrokeColor
                option2Card.strokeColor = defaultStrokeColor
                option3Card.strokeColor = defaultStrokeColor
                option4Card.strokeColor = defaultStrokeColor
            }
        }

        private fun updateCardsForSelection(context: Context, selectedOption: Int) {
            val blueColor = ContextCompat.getColor(context, R.color.blue_primary)
            binding.apply {
                when (selectedOption) {
                    0 -> option1Card.strokeColor = blueColor
                    1 -> option2Card.strokeColor = blueColor
                    2 -> option3Card.strokeColor = blueColor
                    3 -> option4Card.strokeColor = blueColor
                }
            }
        }

        private fun updateCardsForReview(
            context: Context,
            selectedOption: Int,
            correctAnswer: Int
        ) {
            val greenColor = ContextCompat.getColor(context, R.color.green)
            val redColor = ContextCompat.getColor(context, R.color.error)

            binding.apply {
                when (selectedOption) {
                    0 -> option1Card.strokeColor =
                        if (selectedOption == correctAnswer) greenColor else redColor

                    1 -> option2Card.strokeColor =
                        if (selectedOption == correctAnswer) greenColor else redColor

                    2 -> option3Card.strokeColor =
                        if (selectedOption == correctAnswer) greenColor else redColor

                    3 -> option4Card.strokeColor =
                        if (selectedOption == correctAnswer) greenColor else redColor
                }

                if (selectedOption != correctAnswer) {
                    when (correctAnswer) {
                        0 -> option1Card.strokeColor = greenColor
                        1 -> option2Card.strokeColor = greenColor
                        2 -> option3Card.strokeColor = greenColor
                        3 -> option4Card.strokeColor = greenColor
                    }
                }
            }
        }

        private fun handleOptionMedia(
            optionItem: OptionItem?,
            imageView: ImageView
        ) {
            if (optionItem != null && optionItem.mediaUrl.isNotBlank()) {
                imageView.visibility = View.VISIBLE
                Glide.with(imageView.context)
                    .load(optionItem.mediaUrl)
                    .placeholder(R.drawable.ic_video_placeholder)
                    .error(R.drawable.ic_error)
                    .into(imageView)
            } else {
                imageView.visibility = View.GONE
                imageView.setImageDrawable(null)
            }
        }
    }

    private class QuizQuestionDiffCallback : DiffUtil.ItemCallback<QuizQuestion>() {
        override fun areItemsTheSame(oldItem: QuizQuestion, newItem: QuizQuestion): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: QuizQuestion, newItem: QuizQuestion): Boolean {
            return oldItem == newItem
        }
    }
}