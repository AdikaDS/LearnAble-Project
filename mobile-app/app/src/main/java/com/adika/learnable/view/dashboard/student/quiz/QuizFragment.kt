package com.adika.learnable.view.dashboard.student.quiz

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.CountDownTimer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.R
import com.adika.learnable.adapter.PageNumberQuizAdapter
import com.adika.learnable.adapter.QuizQuestionAdapter
import com.adika.learnable.databinding.FragmentQuizBinding
import com.adika.learnable.model.QuestionType
import com.adika.learnable.model.Quiz
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.lesson.QuizViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuizFragment : BaseFragment() {
    private var _binding: FragmentQuizBinding? = null
    private val binding get() = _binding!!
    private val viewModel: QuizViewModel by viewModels()
    private val args: QuizFragmentArgs by navArgs()
    private lateinit var quizQuestionAdapter: QuizQuestionAdapter
    private lateinit var pageNumberAdapter: PageNumberQuizAdapter
    private var countDownTimer: CountDownTimer? = null
    private var timeSpent = 0
    private var quizStartTime: Long = 0L

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
        setupTextScaling()
        setupRecyclerView()
        setupObservers()
        setupSubmitButton()

        binding.btnBack.setOnClickListener {
            findNavController().popBackStack(R.id.studentSubBabDetailFragment, false)
        }

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner) {
            findNavController().popBackStack(R.id.studentSubBabDetailFragment, false)
        }

        viewModel.getQuizBySubBabId(args.subBab.id)
    }


    private fun setupRecyclerView() {

        quizQuestionAdapter = QuizQuestionAdapter(
            onAnswerSelected = { position, _ ->
                pageNumberAdapter.markAnswered(position)
            },
            onEssayAnswerChanged = { position, hasAnswer ->
                if (hasAnswer) {
                    pageNumberAdapter.markAnswered(position)
                } else {
                    pageNumberAdapter.markUnanswered(position)
                }
            }
        )
        binding.questionsRecyclerView.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = quizQuestionAdapter
            PagerSnapHelper().attachToRecyclerView(this)
        }

        pageNumberAdapter = PageNumberQuizAdapter { index ->
            smoothScrollToPosition(index)
        }
        binding.rvPageNumbers.apply {
            layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
            adapter = pageNumberAdapter
        }

        binding.questionsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    val lm = recyclerView.layoutManager as LinearLayoutManager
                    val position = lm.findFirstCompletelyVisibleItemPosition()
                        .takeIf { it != RecyclerView.NO_POSITION }
                        ?: lm.findFirstVisibleItemPosition()
                    if (position != RecyclerView.NO_POSITION) {
                        pageNumberAdapter.updateSelectedIndex(position)
                        binding.rvPageNumbers.smoothScrollToPosition(position)
                    }
                }
            }
        })

        binding.btnPrevious.setOnClickListener {
            val lm = binding.questionsRecyclerView.layoutManager as LinearLayoutManager
            val current = lm.findFirstVisibleItemPosition()
            val target = (current - 1).coerceAtLeast(0)
            smoothScrollToPosition(target)
        }
        binding.btnNext.setOnClickListener {
            val lm = binding.questionsRecyclerView.layoutManager as LinearLayoutManager
            val current = lm.findFirstVisibleItemPosition()
            val target =
                (current + 1).coerceAtMost((quizQuestionAdapter.itemCount - 1).coerceAtLeast(0))
            smoothScrollToPosition(target)
        }

    }

    private fun smoothScrollToPosition(index: Int) {
        binding.questionsRecyclerView.smoothScrollToPosition(index)
        pageNumberAdapter.updateSelectedIndex(index)
        binding.rvPageNumbers.smoothScrollToPosition(index)
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
                    navigateToQuizResult()
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
            val quizState = viewModel.quizState.value
            if (quizState !is QuizViewModel.QuizState.Success) return@setOnClickListener

            val answers = quizQuestionAdapter.getSelectedAnswers()
            val essayAnswers = quizQuestionAdapter.getEssayAnswers()

            val hasUnansweredMultipleChoice =
                quizState.quiz.questions.withIndex().any { (index, question) ->
                    question.questionType != QuestionType.ESSAY && (answers.getOrNull(index)
                        ?: -1) == -1
                }

            val hasUnansweredEssay = quizState.quiz.questions
                .filter { it.questionType == QuestionType.ESSAY }
                .any { essayAnswers[it.id].isNullOrBlank() }

            if (hasUnansweredMultipleChoice || hasUnansweredEssay) {
                showToast(getString(R.string.answer_all_questions))
            } else {
                val elapsedSeconds = if (timeSpent > 0) {
                    timeSpent
                } else if (quizStartTime > 0L) {
                    ((System.currentTimeMillis() - quizStartTime) / 1000L).toInt().coerceAtLeast(0)
                } else {
                    0
                }
                viewModel.submitQuiz(
                    quizState.quiz,
                    answers,
                    essayAnswers,
                    elapsedSeconds
                )
            }
        }
    }

    private fun updateUI(quiz: Quiz) {
        binding.apply {
            quizTitleText.text = quiz.title
            quizQuestionAdapter.submitList(quiz.questions)
            pageNumberAdapter.submitCount(quiz.questions.size)
            if (quizStartTime == 0L) {
                quizStartTime = System.currentTimeMillis()
                startElapsedTimer()
            }
        }
    }

    private fun navigateToQuizResult() {
        val action = QuizFragmentDirections.actionQuizFragmentToQuizResultFragment(
            subBab = args.subBab
        )
        findNavController().navigate(action)
    }

    private fun startElapsedTimer() {
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000L) {
            @SuppressLint("DefaultLocale")
            override fun onTick(millisUntilFinished: Long) {
                timeSpent++
                val minutes = timeSpent / 60
                val seconds = timeSpent % 60
                binding.timerText.text = getString(
                    R.string.time_elapsed,
                    String.format("%02d:%02d", minutes, seconds)
                )
            }

            override fun onFinish() {

                binding.timerText.text = getString(R.string.time_elapsed, "00:00")
            }
        }.start()
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