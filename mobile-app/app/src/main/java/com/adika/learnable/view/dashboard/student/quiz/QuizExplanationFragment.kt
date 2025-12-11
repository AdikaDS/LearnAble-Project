package com.adika.learnable.view.dashboard.student.quiz

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.PagerSnapHelper
import androidx.recyclerview.widget.RecyclerView
import com.adika.learnable.adapter.PageNumberQuizAdapter
import com.adika.learnable.adapter.QuizQuestionAdapter
import com.adika.learnable.databinding.FragmentQuizExplanationBinding
import com.adika.learnable.model.QuestionType
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.lesson.QuizViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuizExplanationFragment : BaseFragment() {
    private var _binding: FragmentQuizExplanationBinding? = null
    private val binding get() = _binding!!
    private val args: QuizExplanationFragmentArgs by navArgs()
    private val viewModel: QuizViewModel by viewModels()
    private lateinit var questionAdapter: QuizQuestionAdapter
    private lateinit var pageNumberAdapter: PageNumberQuizAdapter
    private var isInitialized = false
    private val essayAnswersMap: Map<String, String> by lazy { parseEssayAnswers() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizExplanationBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecycler()
        setupObservers()
        setupHeader()

        viewModel.getQuizBySubBabId(args.subBab.id)

        setupTextScaling()
    }

    private fun setupObservers() {
        viewModel.quizState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.QuizState.Loading -> {
                    showLoading(true)
                }

                is QuizViewModel.QuizState.Success -> {
                    showLoading(false)
                    binding.quizTitleText.text = state.quiz.title
                    setupOrUpdateContent(state)
                }

                is QuizViewModel.QuizState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }
    }

    private fun setupOrUpdateContent(state: QuizViewModel.QuizState.Success) {
        if (!isInitialized) {

            questionAdapter = QuizQuestionAdapter(
                onAnswerSelected = { _, _ -> }
            )
            questionAdapter.setSelectedAnswers(args.answers.toList())
            questionAdapter.setEssayAnswers(essayAnswersMap)
            questionAdapter.setReviewMode(state.quiz.questions)
            binding.questionsRecyclerView.apply {
                adapter = questionAdapter
                PagerSnapHelper().attachToRecyclerView(this)
            }

            pageNumberAdapter = PageNumberQuizAdapter { index ->
                smoothScrollToPosition(index)
            }
            binding.rvPageNumbers.adapter = pageNumberAdapter
            binding.rvPageNumbers.layoutManager =
                LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)

            binding.questionsRecyclerView.addOnScrollListener(object :
                RecyclerView.OnScrollListener() {
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

            isInitialized = true
        }

        questionAdapter.submitList(state.quiz.questions)
        questionAdapter.setEssayAnswers(essayAnswersMap)
        pageNumberAdapter.submitCount(state.quiz.questions.size)

        val selected = args.answers.toList()
        val correct = mutableSetOf<Int>()
        val wrong = mutableSetOf<Int>()
        val pending = mutableSetOf<Int>()
        state.quiz.questions.forEachIndexed { i, q ->
            when (q.questionType) {
                QuestionType.ESSAY -> pending.add(i)
                QuestionType.MULTIPLE_CHOICE -> {
                    val sel = selected.getOrNull(i) ?: -1
                    if (sel == q.correctAnswer) correct.add(i) else wrong.add(i)
                }
            }
        }
        pageNumberAdapter.applyResults(correct, wrong, pending)

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
                (current + 1).coerceAtMost((questionAdapter.itemCount - 1).coerceAtLeast(0))
            smoothScrollToPosition(target)
        }
    }

    private fun setupHeader() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun smoothScrollToPosition(index: Int) {
        binding.questionsRecyclerView.smoothScrollToPosition(index)
        pageNumberAdapter.updateSelectedIndex(index)
        binding.rvPageNumbers.smoothScrollToPosition(index)
    }

    private fun setupRecycler() {
        binding.questionsRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), RecyclerView.HORIZONTAL, false)
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    private fun parseEssayAnswers(): Map<String, String> {
        val raw = args.essayAnswers ?: return emptyMap()
        return raw.mapNotNull { entry ->
            val parts = entry.split("::", limit = 2)
            val key = parts.getOrNull(0)
            val value = parts.getOrNull(1) ?: ""
            key?.let { it to value }
        }.toMap()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}