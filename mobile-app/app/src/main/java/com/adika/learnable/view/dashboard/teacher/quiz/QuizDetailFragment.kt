package com.adika.learnable.view.dashboard.teacher.quiz

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import android.widget.ArrayAdapter
import com.adika.learnable.R
import com.adika.learnable.adapter.QuestionWithAnswer
import com.adika.learnable.adapter.QuizQuestionDetailAdapter
import com.adika.learnable.adapter.QuizResponseAdapter
import com.adika.learnable.databinding.FragmentQuizDetailBinding
import com.adika.learnable.model.QuestionType
import com.adika.learnable.view.dashboard.teacher.dialog.ConfirmAnswerDialogFragment
import com.adika.learnable.model.QuizResult
import com.adika.learnable.view.core.BaseFragment
import com.adika.learnable.viewmodel.lesson.QuizViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuizDetailFragment : BaseFragment() {
    private var _binding: FragmentQuizDetailBinding? = null
    private val binding get() = _binding!!
    private val args: QuizDetailFragmentArgs by navArgs()
    private val viewModel: QuizViewModel by viewModels()
    
    private lateinit var questionAdapter: QuizQuestionDetailAdapter
    private lateinit var responseAdapter: QuizResponseAdapter
    private var currentTab = TAB_PERTANYAAN
    private var currentQuiz: com.adika.learnable.model.Quiz? = null
    private var quizResults: List<QuizResult> = emptyList()
    private var selectedQuizResult: QuizResult? = null
    private val studentNamesMap = mutableMapOf<String, String>()
    private var studentDropdownAdapter: ArrayAdapter<String>? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentQuizDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupTabNavigation()
        setupClickListeners()
        setupObservers()
        setupStudentDropdown()
        loadQuiz()
        setupTextScaling()
    }

    private fun setupRecyclerView() {
        questionAdapter = QuizQuestionDetailAdapter()
        
        binding.rvQuestions.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = questionAdapter
            isNestedScrollingEnabled = false
        }

        responseAdapter = QuizResponseAdapter { position, isCorrect ->
            showConfirmAnswerDialog(position, isCorrect)
        }
        
        binding.rvQuestionsWithAnswers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = responseAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupTabNavigation() {
        binding.btnTabPertanyaan.setOnClickListener {
            switchTab(TAB_PERTANYAAN)
        }
        
        binding.btnTabRespon.setOnClickListener {
            switchTab(TAB_RESPON)
        }

        switchTab(TAB_PERTANYAAN)
    }

    private fun switchTab(tab: Int) {
        currentTab = tab
        
        when (tab) {
            TAB_PERTANYAAN -> {

                binding.btnTabPertanyaan.apply {
                    setBackgroundResource(R.drawable.bg_button_blue)
                    setTextColor(requireContext().getColor(R.color.white))
                }
                binding.btnTabRespon.apply {
                    setBackgroundResource(R.drawable.bg_outline_button)
                    setTextColor(requireContext().getColor(R.color.text_primary))
                }

                binding.scrollViewPertanyaan.visibility = View.VISIBLE
                binding.scrollViewRespon.visibility = View.GONE
            }
            TAB_RESPON -> {

                binding.btnTabPertanyaan.apply {
                    setBackgroundResource(R.drawable.bg_outline_button)
                    setTextColor(requireContext().getColor(R.color.text_primary))
                }
                binding.btnTabRespon.apply {
                    setBackgroundResource(R.drawable.bg_button_blue)
                    setTextColor(requireContext().getColor(R.color.white))
                }

                binding.scrollViewPertanyaan.visibility = View.GONE
                binding.scrollViewRespon.visibility = View.VISIBLE

                loadQuizResults()
                binding.btnSimpan.visibility = View.VISIBLE
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }
        
        binding.btnKembali.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnSimpan.setOnClickListener {
            val quiz = currentQuiz ?: return@setOnClickListener
            val result = selectedQuizResult ?: return@setOnClickListener

            viewModel.recalculateAndUpdateScore(result.id, quiz)

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
                    currentQuiz = state.quiz
                    displayQuestions(state.quiz.questions)
                }
                is QuizViewModel.QuizState.Error -> {
                    showLoading(false)
                    showToast(state.message)
                }
            }
        }

        viewModel.resultState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.ResultState.Success -> {

                    if (selectedQuizResult?.id == state.result.id) {
                        Log.d("QuizDetailFragment", "Result updated: score=${state.result.score}, essayGrading=${state.result.essayGrading}")
                        selectedQuizResult = state.result

                        displayQuestionsWithAnswers()

                        showToast("Nilai berhasil diperbarui: ${String.format("%.1f", state.result.score)}")
                    }
                }
                is QuizViewModel.ResultState.Error -> {
                    Log.e("QuizDetailFragment", "Error updating result: ${state.message}")
                    showToast(state.message)
                }
                else -> {}
            }
        }

        viewModel.quizResultsState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.QuizResultsState.Loading -> {
                    Log.d("QuizDetailFragment", "Loading quiz results...")
                }
                is QuizViewModel.QuizResultsState.Success -> {
                    Log.d("QuizDetailFragment", "Quiz results loaded: ${state.results.size} results")
                    quizResults = state.results

                    state.results.forEachIndexed { index, result ->
                        Log.d("QuizDetailFragment", "Result $index: studentId=${result.studentId}")
                    }
                    
                    updateStudentDropdown(state.results)

                    val uniqueStudentIds = state.results.map { it.studentId }.distinct()
                    Log.d("QuizDetailFragment", "Unique student IDs: ${uniqueStudentIds.size} - $uniqueStudentIds")
                    if (uniqueStudentIds.isNotEmpty()) {
                        viewModel.loadMultipleUsers(uniqueStudentIds)
                    } else {
                        Log.w("QuizDetailFragment", "No unique student IDs found!")
                    }
                }
                is QuizViewModel.QuizResultsState.Error -> {
                    Log.e("QuizDetailFragment", "Error loading quiz results: ${state.message}")
                    showToast(state.message)
                }
            }
        }
        
        viewModel.loadedUsers.observe(viewLifecycleOwner) { usersMap ->
            Log.d("QuizDetailFragment", "Loaded users map: ${usersMap.size} users")
            usersMap.forEach { (userId, user) ->
                Log.d("QuizDetailFragment", "User loaded: userId=$userId, name=${user.name}")
            }

            usersMap.forEach { (userId, user) ->
                studentNamesMap[userId] = user.name
            }
            
            Log.d("QuizDetailFragment", "Student names map size: ${studentNamesMap.size}")
            studentNamesMap.forEach { (id, name) ->
                Log.d("QuizDetailFragment", "Map entry: $id -> $name")
            }
            
            updateStudentDropdownAdapter()
        }

        viewModel.selectedStudentState.observe(viewLifecycleOwner) { state ->
            when (state) {
                is QuizViewModel.SelectedStudentState.Loading -> {

                }
                is QuizViewModel.SelectedStudentState.Success -> {

                    studentNamesMap[state.user.id] = state.user.name

                    // This ensures the dropdown is refreshed
                    updateStudentDropdownAdapter()

                    if (selectedQuizResult?.studentId == state.user.id) {
                        displayStudentInfo(state.user)
                        displayQuestionsWithAnswers()
                    }
                }
                is QuizViewModel.SelectedStudentState.Error -> {

                    Log.e("QuizDetailFragment", "Error loading user: ${state.message}")

                }
            }
        }
    }

    private fun loadQuiz() {
        val subBabId = args.subBabId
        if (subBabId.isNotEmpty()) {
            viewModel.getQuizBySubBabId(subBabId)
        } else {
            showToast(getString(R.string.data_not_completed))
        }
    }

    private fun displayQuestions(questions: List<com.adika.learnable.model.QuizQuestion>) {
        questionAdapter.submitList(questions)
    }

    private fun loadQuizResults() {
        val subBabId = args.subBabId
        Log.d("QuizDetailFragment", "loadQuizResults called with subBabId: $subBabId")
        if (subBabId.isNotEmpty()) {
            viewModel.getAllQuizResultsBySubBabId(subBabId)
        } else {
            Log.w("QuizDetailFragment", "subBabId is empty!")
        }
    }

    private fun setupStudentDropdown() {
        binding.actvStudentDropdown.setOnItemClickListener { _, _, position, _ ->
            val selectedResult = quizResults[position]
            selectedQuizResult = selectedResult

            val studentName = studentNamesMap[selectedResult.studentId]
            if (studentName != null) {

                viewModel.getUserById(selectedResult.studentId)
            } else {

                viewModel.getUserById(selectedResult.studentId)
            }
        }
    }

    private fun updateStudentDropdown(results: List<QuizResult>) {
        Log.d("QuizDetailFragment", "updateStudentDropdown called with ${results.size} results")

        val studentNames = results.map { "Loading..." }
        
        Log.d("QuizDetailFragment", "Creating adapter with ${studentNames.size} items")
        studentDropdownAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_dropdown_item_1line,
            studentNames.toMutableList()
        )
        binding.actvStudentDropdown.setAdapter(studentDropdownAdapter)
        Log.d("QuizDetailFragment", "Adapter set to AutoCompleteTextView")
    }

    private fun updateStudentDropdownAdapter() {
        Log.d("QuizDetailFragment", "updateStudentDropdownAdapter called")
        Log.d("QuizDetailFragment", "quizResults.size = ${quizResults.size}")
        Log.d("QuizDetailFragment", "studentDropdownAdapter != null = ${studentDropdownAdapter != null}")
        Log.d("QuizDetailFragment", "studentNamesMap.size = ${studentNamesMap.size}")
        
        if (quizResults.isEmpty() || studentDropdownAdapter == null) {
            Log.w("QuizDetailFragment", "Cannot update adapter: quizResults=${quizResults.size}, adapter=${studentDropdownAdapter != null}")
            return
        }
        
        val studentNames = quizResults.map { result ->
            val name = studentNamesMap[result.studentId] ?: "Loading..."
            Log.d("QuizDetailFragment", "Result studentId=${result.studentId}, name=$name")
            name
        }
        
        Log.d("QuizDetailFragment", "Updating adapter with ${studentNames.size} names")
        Log.d("QuizDetailFragment", "Student names: $studentNames")

        studentDropdownAdapter?.let { adapter ->
            adapter.clear()
            adapter.addAll(studentNames)
            adapter.notifyDataSetChanged()
            Log.d("QuizDetailFragment", "Adapter updated successfully. Current count: ${adapter.count}")
        } ?: run {
            Log.e("QuizDetailFragment", "Adapter is null!")
        }
    }

    private fun displayStudentInfo(user: com.adika.learnable.model.User) {
        binding.cardStudentInfo.visibility = View.VISIBLE
        binding.tvStudentFullName.text = user.name
        binding.tvStudentGrade.text = user.studentData.grade.ifBlank { "-" }
    }

    private fun displayQuestionsWithAnswers() {
        val quiz = currentQuiz ?: return
        val result = selectedQuizResult ?: return

        val questionsWithAnswers = quiz.questions.mapIndexed { index, question ->
            val studentAnswer = if (index < result.answers.size) {
                result.answers[index]
            } else {
                -1
            }

            val isEssay = question.questionType == QuestionType.ESSAY

            val essayAnswer = if (isEssay) {
                result.essayAnswers[question.id]?.takeIf { it.isNotBlank() }
            } else null

            val isGraded = if (isEssay) {
                result.essayGrading.containsKey(question.id)
            } else {
                false
            }
            val isCorrect = if (isEssay && isGraded) {
                result.essayGrading[question.id]
            } else {
                null
            }
            
            QuestionWithAnswer(
                question = question,
                studentAnswer = studentAnswer,
                essayAnswer = essayAnswer,
                isGraded = isGraded,
                isCorrect = isCorrect
            )
        }

        responseAdapter.submitList(questionsWithAnswers)
    }

    private fun showConfirmAnswerDialog(position: Int, isCorrect: Boolean) {
        val quiz = currentQuiz ?: return
        val result = selectedQuizResult ?: return

        val currentList = responseAdapter.currentList
        if (position >= currentList.size) return
        
        val item = currentList[position]
        val question = item.question

        val isEssay = question.questionType == QuestionType.ESSAY
        if (!isEssay) return

        val gradingValue = isCorrect
        
        val dialog = ConfirmAnswerDialogFragment.newInstance(gradingValue) { confirmedValue ->
            viewModel.gradeEssayAnswer(
                resultId = result.id,
                questionId = question.id,
                isCorrect = confirmedValue, // This will be false when "salah" is clicked
                quiz = quiz
            )

            // But we update immediately for better UX
            val updatedList = responseAdapter.currentList.toMutableList()
            if (position < updatedList.size) {
                val updatedItem = updatedList[position].copy(
                    isGraded = true,
                    isCorrect = confirmedValue // This will be false when "salah" is clicked
                )
                updatedList[position] = updatedItem
                responseAdapter.submitList(updatedList)
                
                Log.d("QuizDetailFragment", "UI updated immediately: isGraded=true, isCorrect=$confirmedValue")
                showToast(if (confirmedValue) "Jawaban ditandai benar" else "Jawaban ditandai salah")
            } else {
                Log.e("QuizDetailFragment", "Position $position is out of bounds, list size: ${updatedList.size}")
            }
        }
        dialog.show(parentFragmentManager, "ConfirmAnswerDialog")
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val TAB_PERTANYAAN = 0
        private const val TAB_RESPON = 1
    }
}

