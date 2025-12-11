package com.adika.learnable.view.dashboard.student.quiz

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.adika.learnable.R
import com.adika.learnable.databinding.FragmentQuizResultBinding
import com.adika.learnable.model.QuestionType
import com.adika.learnable.model.Quiz
import com.adika.learnable.model.QuizResult
import com.adika.learnable.util.TimeUtils.formatTime
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.lesson.QuizViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuizResultFragment : BaseFragment() {
    private var _binding: FragmentQuizResultBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private val args: QuizResultFragmentArgs by navArgs()
    private var scoreAnimator: ValueAnimator? = null

    private var latestQuiz: Quiz? = null
    private var latestResult: QuizResult? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        setupClickListeners()
        getQuizResultData()

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack(R.id.studentSubBabDetailFragment, false)
        }

        setupTextScaling()
    }

    private fun setupObservers() {

        viewModel.quizState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.QuizState.Success -> {
                    latestQuiz = state.quiz
                    updateQuizDetails(state.quiz)

                    latestResult?.let { result ->
                        updateResultDetails(result, state.quiz)
                    }
                }

                else -> {}
            }
        }

        viewModel.resultState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.ResultState.Loading -> {
                    showLoading(true)
                }

                is QuizViewModel.ResultState.Success -> {
                    showLoading(false)
                    latestResult = state.result

                    // lagi ketika quizState.Success datang)
                    updateResultDetails(state.result, latestQuiz)
                }

                is QuizViewModel.ResultState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }

                else -> {}
            }
        }
    }

    private fun getQuizResultData() {

        val subBabId = args.subBab.id

        viewModel.getQuizBySubBabId(subBabId)

        viewModel.getStudentQuizResult(subBabId)
    }

    private fun updateQuizDetails(quiz: Quiz) {
        binding.apply {
            quizTitleText.text = quiz.title
            quizSubtitleText.text = args.subBab.title
        }
    }

    private fun updateResultDetails(
        result: QuizResult,
        quiz: Quiz? = (viewModel.quizState.value as? QuizViewModel.QuizState.Success)?.quiz
    ) {
        binding.apply {

            val score = result.score.toInt()
            scoreMessageText.text = getString(R.string.your_grade, score)
            scoreAnimator?.cancel()
            scoreAnimator = ValueAnimator.ofInt(0, score).apply {
                duration = 1000 // 1 detik
                addUpdateListener { animator ->
                    val value = animator.animatedValue as Int
                    _binding?.let { b ->
                        b.cpi.progress = value
                        b.tvPercent.text = value.toString()
                    }
                }
                start()
            }

            motivationText.text = when {
                score >= 80 -> getString(R.string.motivation1)
                score >= 60 -> getString(R.string.motivation2)
                else -> getString(R.string.motivation3)
            }

            if (quiz != null) {

                val isPassed = result.score >= quiz.passingScore
                if (isPassed) {
                    vibrationHelper.vibrateQuizPassed()
                } else {
                    vibrationHelper.vibrateQuizFailed()
                }

                var correctCount = 0
                var wrongCount = 0
                var pendingCount = 0

                quiz.questions.forEachIndexed { index, question ->
                    when (question.questionType) {
                        QuestionType.MULTIPLE_CHOICE -> {
                            val answer = result.answers.getOrNull(index) ?: -1
                            if (answer == question.correctAnswer) {
                                correctCount++
                            } else {
                                wrongCount++
                            }
                        }

                        QuestionType.ESSAY -> {
                            if (!result.essayAnswers[question.id].isNullOrBlank()) {
                                pendingCount++
                            }
                        }
                    }
                }

                correctCountText.text = correctCount.toString()
                wrongCountText.text = wrongCount.toString()
                pendingCountText.text = pendingCount.toString()
            }

            timeSpentText.text = formatTime(requireContext(), result.timeSpent)
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {

                findNavController().popBackStack(R.id.studentSubBabDetailFragment, false)
            }

            retryButton.setOnClickListener {

                val action = QuizResultFragmentDirections.actionQuizResultToQuizReview(
                    subBab = args.subBab
                )
                findNavController().navigate(action)
            }

            reviewButton.setOnClickListener {
                val currentResultState = viewModel.resultState.value
                val answersArray = if (currentResultState is QuizViewModel.ResultState.Success) {
                    currentResultState.result.answers.toIntArray()
                } else {
                    IntArray(0)
                }
                val essayAnswersArray =
                    if (currentResultState is QuizViewModel.ResultState.Success) {
                        currentResultState.result.essayAnswers.entries.map { "${it.key}::${it.value}" }
                            .toTypedArray()
                    } else {
                        emptyArray()
                    }

                val action =
                    QuizResultFragmentDirections.actionQuizResultFragmentToQuizExplanationFragment(
                        subBab = args.subBab,
                        answers = answersArray,
                        essayAnswers = essayAnswersArray
                    )
                findNavController().navigate(action)
            }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        scoreAnimator?.cancel()
        scoreAnimator = null
        _binding = null
    }
}