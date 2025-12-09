package com.adika.learnable.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.databinding.ItemQuizQuestionBinding
import com.adika.learnable.model.QuizQuestion
import com.adika.learnable.viewmodel.RecordingState
import com.adika.learnable.viewmodel.RecordingViewModel
import kotlinx.coroutines.launch

class QuizQuestionAdapter(
    private val onAnswerSelected: (Int, Int) -> Unit, // position, selectedOption
    private val recordingViewModel: RecordingViewModel
) : ListAdapter<QuizQuestion, QuizQuestionAdapter.QuizQuestionViewHolder>(QuizQuestionDiffCallback()) {

    private val selectedAnswers = mutableMapOf<Int, Int>()

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
        holder.bind(question, position, selectedAnswers[position] ?: -1)
    }

    fun getSelectedAnswers(): List<Int> {
        return List(currentList.size) { index ->
            selectedAnswers[index] ?: -1
        }
    }

    inner class QuizQuestionViewHolder(
        private val binding: ItemQuizQuestionBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(question: QuizQuestion, position: Int, selectedOption: Int) {
            binding.apply {
                questionNumberText.text = "Pertanyaan ${position + 1}"
                questionText.text = question.question

                // Set options
                option1RadioButton.text = question.options[0]
                option2RadioButton.text = question.options[1]
                option3RadioButton.text = question.options[2]
                option4RadioButton.text = question.options[3]

                // Set selected option
                optionsRadioGroup.setOnCheckedChangeListener(null)
                when (selectedOption) {
                    0 -> option1RadioButton.isChecked = true
                    1 -> option2RadioButton.isChecked = true
                    2 -> option3RadioButton.isChecked = true
                    3 -> option4RadioButton.isChecked = true
                    else -> optionsRadioGroup.clearCheck()
                }

                // Handle option selection
                optionsRadioGroup.setOnCheckedChangeListener { _, checkedId ->
                    val selectedIndex = when (checkedId) {
                        option1RadioButton.id -> 0
                        option2RadioButton.id -> 1
                        option3RadioButton.id -> 2
                        option4RadioButton.id -> 3
                        else -> -1
                    }
                    if (selectedIndex != -1) {
                        selectedAnswers[position] = selectedIndex
                        onAnswerSelected(position, selectedIndex)
                    }
                }

                // Setup voice recording button
                recordButton.setOnClickListener {
                    when (recordingViewModel.recordingState.value) {
                        is RecordingState.Idle -> {
                            recordingViewModel.startRecording()
                        }
                        is RecordingState.Recording -> {
                            recordingViewModel.stopRecording()
                        }
                        else -> { /* Do nothing */ }
                    }
                }

                // Observe recording state
                (itemView.context as? LifecycleOwner)?.let { lifecycleOwner ->
                    lifecycleOwner.lifecycleScope.launch {
                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            recordingViewModel.recordingState.collect { state ->
                                recordButton.text = when (state) {
                                    is RecordingState.Idle -> "Start Recording"
                                    is RecordingState.Recording -> "Stop Recording"
                                    is RecordingState.Transcribing -> "Transcribing..."
                                }
                                recordButton.isEnabled = state !is RecordingState.Transcribing
                            }
                        }
                    }
                }

                // Observe transcription
                (itemView.context as? LifecycleOwner)?.let { lifecycleOwner ->
                    lifecycleOwner.lifecycleScope.launch {
                        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                            recordingViewModel.transcriptionResult.collect { text ->
                                text?.let {
                                    transcribedText.text = it
                                    
                                    // Try to match transcription with options
                                    val transcribedText = it.lowercase()
                                    val selectedIndex = when {
                                        transcribedText.contains("a") || transcribedText.contains("satu") -> 0
                                        transcribedText.contains("b") || transcribedText.contains("dua") -> 1
                                        transcribedText.contains("c") || transcribedText.contains("tiga") -> 2
                                        transcribedText.contains("d") || transcribedText.contains("empat") -> 3
                                        else -> -1
                                    }

                                    if (selectedIndex != -1) {
                                        selectedAnswers[position] = selectedIndex
                                        onAnswerSelected(position, selectedIndex)
                                        // Update radio button selection
                                        when (selectedIndex) {
                                            0 -> option1RadioButton.isChecked = true
                                            1 -> option2RadioButton.isChecked = true
                                            2 -> option3RadioButton.isChecked = true
                                            3 -> option4RadioButton.isChecked = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
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