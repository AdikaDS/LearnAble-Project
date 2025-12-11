package com.adika.learnable.adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemQuizResponseBinding
import com.adika.learnable.model.QuizQuestion
import com.bumptech.glide.Glide

data class QuestionWithAnswer(
    val question: QuizQuestion,
    val studentAnswer: Int = -1,
    val essayAnswer: String? = null,
    val isGraded: Boolean = false,
    val isCorrect: Boolean? = null
)

class QuizResponseAdapter(
    private val onGradeEssay: (Int, Boolean) -> Unit
) : ListAdapter<QuestionWithAnswer, QuizResponseAdapter.ResponseViewHolder>(
    DiffCallback
) {

    object DiffCallback : DiffUtil.ItemCallback<QuestionWithAnswer>() {
        override fun areItemsTheSame(
            oldItem: QuestionWithAnswer,
            newItem: QuestionWithAnswer
        ): Boolean =
            oldItem.question.id == newItem.question.id

        override fun areContentsTheSame(
            oldItem: QuestionWithAnswer,
            newItem: QuestionWithAnswer
        ): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResponseViewHolder {
        val binding = ItemQuizResponseBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ResponseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ResponseViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ResponseViewHolder(
        private val binding: ItemQuizResponseBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: QuestionWithAnswer) {
            val question = item.question
            val studentAnswer = item.studentAnswer

            val isEssay = question.questionType == com.adika.learnable.model.QuestionType.ESSAY
            val position = adapterPosition

            binding.apply {

                if (isEssay && item.essayAnswer != null) {

                    essayAnswerContainer.visibility = View.VISIBLE
                    optionsContainer.visibility = View.GONE

                    tvEssayAnswer.text = item.essayAnswer

                    btnCorrect.setOnClickListener(null)
                    btnWrong.setOnClickListener(null)

                    btnCorrect.isEnabled = true
                    btnCorrect.isClickable = true
                    btnWrong.isEnabled = true
                    btnWrong.isClickable = true

                    btnCorrect.setOnClickListener {
                        Log.d(
                            "QuizResponseAdapter",
                            "btnCorrect clicked for position $position"
                        )
                        onGradeEssay(position, true)
                    }

                    btnWrong.setOnClickListener {
                        Log.d(
                            "QuizResponseAdapter",
                            "btnWrong clicked for position $position"
                        )
                        onGradeEssay(position, false)
                    }

                    Log.d(
                        "QuizResponseAdapter",
                        "Binding item: isGraded=${item.isGraded}, isCorrect=${item.isCorrect}"
                    )
                    if (item.isGraded && item.isCorrect != null) {
                        if (item.isCorrect == true) {

                            btnCorrect.alpha = 1.0f
                            btnWrong.alpha = 0.5f
                            Log.d("QuizResponseAdapter", "Item marked as correct")
                        } else {

                            btnWrong.alpha = 1.0f
                            btnCorrect.alpha = 0.5f
                            Log.d("QuizResponseAdapter", "Item marked as wrong")
                        }
                    } else {

                        btnCorrect.alpha = 1.0f
                        btnWrong.alpha = 1.0f
                        Log.d("QuizResponseAdapter", "Item not graded yet")
                    }
                } else {

                    essayAnswerContainer.visibility = View.GONE
                    optionsContainer.visibility = View.VISIBLE
                }

                tvQuestionText.text = question.question

                if (question.mediaQuestion.isNotBlank()) {
                    ivAttachment.visibility = View.VISIBLE
                    Glide.with(root.context)
                        .load(question.mediaQuestion)
                        .placeholder(R.drawable.icon_dummy_subject)
                        .error(R.drawable.icon_dummy_subject)
                        .into(ivAttachment)
                } else {
                    ivAttachment.visibility = View.GONE
                }

                val options = question.optionItems
                val correctAnswer = question.correctAnswer
                val greenColor = ContextCompat.getColor(root.context, R.color.green)
                val errorColor = ContextCompat.getColor(root.context, R.color.error)
                val defaultStrokeColor =
                    ContextCompat.getColor(root.context, R.color.stroke_card_quiz)

                if (options.size > 0) {
                    option1Card.visibility = View.VISIBLE
                    tvOption1.text = options[0].text

                    val isStudentAnswer = studentAnswer == 0
                    val isCorrectAnswer = correctAnswer == 0

                    if (isStudentAnswer) {
                        if (isCorrectAnswer) {
                            option1Card.strokeColor = greenColor
                            option1Card.strokeWidth = 2
                            ivOption1Correct.visibility = View.VISIBLE
                            ivOption1Correct.setImageResource(R.drawable.ic_checklist)
                            ivOption1Correct.setColorFilter(greenColor)
                        } else {
                            option1Card.strokeColor = errorColor
                            option1Card.strokeWidth = 2
                            ivOption1Correct.visibility = View.VISIBLE
                            ivOption1Correct.setImageResource(R.drawable.ic_cross)
                            ivOption1Correct.setColorFilter(errorColor)
                        }
                    } else if (isCorrectAnswer) {
                        option1Card.strokeColor = greenColor
                        option1Card.strokeWidth = 2
                        ivOption1Correct.visibility = View.VISIBLE
                        ivOption1Correct.setImageResource(R.drawable.ic_checklist)
                        ivOption1Correct.setColorFilter(greenColor)
                    } else {
                        option1Card.strokeColor = defaultStrokeColor
                        option1Card.strokeWidth = 1
                        ivOption1Correct.visibility = View.GONE
                    }

                    if (options[0].mediaUrl.isNotBlank()) {
                        ivOption1Media.visibility = View.VISIBLE
                        Glide.with(root.context)
                            .load(options[0].mediaUrl)
                            .placeholder(R.drawable.icon_dummy_subject)
                            .error(R.drawable.icon_dummy_subject)
                            .into(ivOption1Media)
                    } else {
                        ivOption1Media.visibility = View.GONE
                    }
                } else {
                    option1Card.visibility = View.GONE
                }

                if (options.size > 1) {
                    option2Card.visibility = View.VISIBLE
                    tvOption2.text = options[1].text

                    val isStudentAnswer = studentAnswer == 1
                    val isCorrectAnswer = correctAnswer == 1

                    if (isStudentAnswer) {
                        if (isCorrectAnswer) {
                            option2Card.strokeColor = greenColor
                            option2Card.strokeWidth = 2
                            ivOption2Correct.visibility = View.VISIBLE
                            ivOption2Correct.setImageResource(R.drawable.ic_checklist)
                            ivOption2Correct.setColorFilter(greenColor)
                        } else {
                            option2Card.strokeColor = errorColor
                            option2Card.strokeWidth = 2
                            ivOption2Correct.visibility = View.VISIBLE
                            ivOption2Correct.setImageResource(R.drawable.ic_cross)
                            ivOption2Correct.setColorFilter(errorColor)
                        }
                    } else if (isCorrectAnswer) {

                        option2Card.strokeColor = greenColor
                        option2Card.strokeWidth = 2
                        ivOption2Correct.visibility = View.VISIBLE
                        ivOption2Correct.setImageResource(R.drawable.ic_checklist)
                        ivOption2Correct.setColorFilter(greenColor)
                    } else {

                        option2Card.strokeColor = defaultStrokeColor
                        option2Card.strokeWidth = 1
                        ivOption2Correct.visibility = View.GONE
                    }

                    if (options[1].mediaUrl.isNotBlank()) {
                        ivOption2Media.visibility = View.VISIBLE
                        Glide.with(root.context)
                            .load(options[1].mediaUrl)
                            .placeholder(R.drawable.icon_dummy_subject)
                            .error(R.drawable.icon_dummy_subject)
                            .into(ivOption2Media)
                    } else {
                        ivOption2Media.visibility = View.GONE
                    }
                } else {
                    option2Card.visibility = View.GONE
                }

                if (options.size > 2) {
                    option3Card.visibility = View.VISIBLE
                    tvOption3.text = options[2].text

                    val isStudentAnswer = studentAnswer == 2
                    val isCorrectAnswer = correctAnswer == 2

                    if (isStudentAnswer) {

                        if (isCorrectAnswer) {

                            option3Card.strokeColor = greenColor
                            option3Card.strokeWidth = 2
                            ivOption3Correct.visibility = View.VISIBLE
                            ivOption3Correct.setImageResource(R.drawable.ic_checklist)
                            ivOption3Correct.setColorFilter(greenColor)
                        } else {

                            option3Card.strokeColor = errorColor
                            option3Card.strokeWidth = 2
                            ivOption3Correct.visibility = View.VISIBLE
                            ivOption3Correct.setImageResource(R.drawable.ic_cross)
                            ivOption3Correct.setColorFilter(errorColor)
                        }
                    } else if (isCorrectAnswer) {

                        option3Card.strokeColor = greenColor
                        option3Card.strokeWidth = 2
                        ivOption3Correct.visibility = View.VISIBLE
                        ivOption3Correct.setImageResource(R.drawable.ic_checklist)
                        ivOption3Correct.setColorFilter(greenColor)
                    } else {

                        option3Card.strokeColor = defaultStrokeColor
                        option3Card.strokeWidth = 1
                        ivOption3Correct.visibility = View.GONE
                    }

                    if (options[2].mediaUrl.isNotBlank()) {
                        ivOption3Media.visibility = View.VISIBLE
                        Glide.with(root.context)
                            .load(options[2].mediaUrl)
                            .placeholder(R.drawable.icon_dummy_subject)
                            .error(R.drawable.icon_dummy_subject)
                            .into(ivOption3Media)
                    } else {
                        ivOption3Media.visibility = View.GONE
                    }
                } else {
                    option3Card.visibility = View.GONE
                }

                if (options.size > 3) {
                    option4Card.visibility = View.VISIBLE
                    tvOption4.text = options[3].text

                    val isStudentAnswer = studentAnswer == 3
                    val isCorrectAnswer = correctAnswer == 3

                    if (isStudentAnswer) {

                        if (isCorrectAnswer) {

                            option4Card.strokeColor = greenColor
                            option4Card.strokeWidth = 2
                            ivOption4Correct.visibility = View.VISIBLE
                            ivOption4Correct.setImageResource(R.drawable.ic_checklist)
                            ivOption4Correct.setColorFilter(greenColor)
                        } else {

                            option4Card.strokeColor = errorColor
                            option4Card.strokeWidth = 2
                            ivOption4Correct.visibility = View.VISIBLE
                            ivOption4Correct.setImageResource(R.drawable.ic_cross)
                            ivOption4Correct.setColorFilter(errorColor)
                        }
                    } else if (isCorrectAnswer) {

                        option4Card.strokeColor = greenColor
                        option4Card.strokeWidth = 2
                        ivOption4Correct.visibility = View.VISIBLE
                        ivOption4Correct.setImageResource(R.drawable.ic_checklist)
                        ivOption4Correct.setColorFilter(greenColor)
                    } else {

                        option4Card.strokeColor = defaultStrokeColor
                        option4Card.strokeWidth = 1
                        ivOption4Correct.visibility = View.GONE
                    }

                    if (options[3].mediaUrl.isNotBlank()) {
                        ivOption4Media.visibility = View.VISIBLE
                        Glide.with(root.context)
                            .load(options[3].mediaUrl)
                            .placeholder(R.drawable.icon_dummy_subject)
                            .error(R.drawable.icon_dummy_subject)
                            .into(ivOption4Media)
                    } else {
                        ivOption4Media.visibility = View.GONE
                    }
                } else {
                    option4Card.visibility = View.GONE
                }
            }
        }
    }
}