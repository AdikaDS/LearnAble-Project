package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.databinding.ItemQuizQuestionDetailBinding
import com.adika.learnable.model.QuestionType
import com.adika.learnable.model.QuizQuestion
import com.bumptech.glide.Glide

class QuizQuestionDetailAdapter : ListAdapter<QuizQuestion, QuizQuestionDetailAdapter.QuestionDetailViewHolder>(
    DiffCallback
) {

    object DiffCallback : DiffUtil.ItemCallback<QuizQuestion>() {
        override fun areItemsTheSame(oldItem: QuizQuestion, newItem: QuizQuestion): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: QuizQuestion, newItem: QuizQuestion): Boolean =
            oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QuestionDetailViewHolder {
        val binding = ItemQuizQuestionDetailBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return QuestionDetailViewHolder(binding)
    }

    override fun onBindViewHolder(holder: QuestionDetailViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class QuestionDetailViewHolder(
        private val binding: ItemQuizQuestionDetailBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(question: QuizQuestion) {
            binding.apply {

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

                val defaultStrokeColor = ContextCompat.getColor(root.context, R.color.stroke_card_quiz)
                option1Card.strokeColor = defaultStrokeColor
                option2Card.strokeColor = defaultStrokeColor
                option3Card.strokeColor = defaultStrokeColor
                option4Card.strokeColor = defaultStrokeColor

                val options = question.optionItems
                val correctAnswer = question.correctAnswer
                val greenColor = ContextCompat.getColor(root.context, R.color.green)

                if (options.size > 0) {
                    option1Card.visibility = View.VISIBLE
                    tvOption1.text = options[0].text

                    if (correctAnswer == 0) {
                        option1Card.strokeColor = greenColor
                        option1Card.strokeWidth = 2
                    } else {
                        option1Card.strokeWidth = 1
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

                    if (correctAnswer == 1) {
                        option2Card.strokeColor = greenColor
                        option2Card.strokeWidth = 2
                    } else {
                        option2Card.strokeWidth = 1
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

                    if (correctAnswer == 2) {
                        option3Card.strokeColor = greenColor
                        option3Card.strokeWidth = 2
                    } else {
                        option3Card.strokeWidth = 1
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

                    if (correctAnswer == 3) {
                        option4Card.strokeColor = greenColor
                        option4Card.strokeWidth = 2
                    } else {
                        option4Card.strokeWidth = 1
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

                if (question.explanation.isNotBlank()) {
                    explanationTitle.visibility = View.VISIBLE
                    explanationText.visibility = View.VISIBLE
                    explanationText.text = question.explanation

                    if (question.questionType == QuestionType.MULTIPLE_CHOICE) {
                        val letter = when (correctAnswer) {
                            0 -> "A"
                            1 -> "B"
                            2 -> "C"
                            3 -> "D"
                            else -> "-"
                        }
                        rightAnswer.visibility = View.VISIBLE
                        rightAnswer.text = root.context.getString(R.string.right_answer, letter)
                    } else {

                        rightAnswer.visibility = View.GONE
                    }

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
                } else {
                    explanationTitle.visibility = View.GONE
                    explanationText.visibility = View.GONE
                    rightAnswer.visibility = View.GONE
                    mediaExplanation.visibility = View.GONE
                }
            }
        }
    }
}