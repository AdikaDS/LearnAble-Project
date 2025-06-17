package com.adika.learnable.view.dashboard.student.quiz

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.adika.learnable.R
import com.adika.learnable.adapter.QuizQuestionAdapter
import com.adika.learnable.databinding.FragmentQuizBinding
import com.adika.learnable.model.Quiz
import com.adika.learnable.model.QuizResult
import com.adika.learnable.viewmodel.QuizViewModel
import com.adika.learnable.viewmodel.RecordingViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class QuizFragment : Fragment() {
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private val recordingViewModel: RecordingViewModel by viewModels()
    private val args: QuizFragmentArgs by navArgs()
    private lateinit var quizQuestionAdapter: QuizQuestionAdapter
    private var countDownTimer: CountDownTimer? = null
    private var timeSpent = 0

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(requireContext(), "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        checkAndRequestPermission()
        setupRecyclerView()
        setupObservers()
        setupSubmitButton()

        viewModel.getQuizBySubBabId(args.subBabId)
    }

    private fun checkAndRequestPermission() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED -> {
                // Permission already granted
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        }
    }

    private fun setupRecyclerView() {
        quizQuestionAdapter = QuizQuestionAdapter (
            onAnswerSelected = {_, _ -> },
            recordingViewModel = recordingViewModel
        )
        binding.questionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = quizQuestionAdapter
        }
    }

    private fun setupObservers() {
        viewModel.quizState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.QuizState.Loading -> {
                    showLoading(true)
                }
                is QuizViewModel.QuizState.Success -> {
                    showLoading(false)
                    updateUI(state.quiz)
                    startTimer(state.quiz.timeLimit)
                }
                is QuizViewModel.QuizState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }

        viewModel.resultState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.ResultState.Loading -> {
                    showLoading(true)
                }
                is QuizViewModel.ResultState.Success -> {
                    showLoading(false)
                    showResult(state.result)
                }
                is QuizViewModel.ResultState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
                else -> {}
            }
        }
    }

    private fun setupSubmitButton() {
        binding.submitButton.setOnClickListener {
            val answers = quizQuestionAdapter.getSelectedAnswers()
            if (answers.any { it == -1 }) {
                showToast(getString(R.string.answer_all_questions))
            } else {
                viewModel.quizState.value?.let { state ->
                    if (state is QuizViewModel.QuizState.Success) {
                        viewModel.submitQuiz(state.quiz, answers, timeSpent)
                    }
                }
            }
        }
    }

    private fun updateUI(quiz: Quiz) {
        binding.apply {
            quizTitleText.text = quiz.title
            quizDescriptionText.text = quiz.description
            quizQuestionAdapter.submitList(quiz.questions)
        }
    }

    private fun startTimer(timeLimit: Int) {
        if (timeLimit <= 0) return

        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(
            timeLimit * 60 * 1000L, // Convert minutes to milliseconds
            1000 // Update every second
        ) {
            override fun onTick(millisUntilFinished: Long) {
                timeSpent++
                val minutes = TimeUnit.MILLISECONDS.toMinutes(millisUntilFinished)
                val seconds = TimeUnit.MILLISECONDS.toSeconds(millisUntilFinished) % 60
                binding.timerText.text = getString(
                    R.string.time_remaining,
                    String.format("%02d:%02d", minutes, seconds)
                )
            }

            override fun onFinish() {
                binding.timerText.text = getString(R.string.time_up)
                binding.submitButton.performClick()
            }
        }.start()
    }

    private fun showResult(result: QuizResult) {
        val message = if (result.isPassed) {
            getString(R.string.quiz_passed, result.score)
        } else {
            getString(R.string.quiz_failed, result.score)
        }
        showToast(message)
        findNavController().navigateUp()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
        _binding = null
    }
} 