package com.adika.learnable.viewmodel.lesson

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.QuestionType
import com.adika.learnable.model.Quiz
import com.adika.learnable.model.QuizResult
import com.adika.learnable.model.User
import com.adika.learnable.repository.QuizRepository
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.repository.SubBabRepository
import com.adika.learnable.repository.UserAdminRepository
import com.adika.learnable.util.ResourceProvider
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val subBabRepository: SubBabRepository,
    private val studentProgressRepository: StudentProgressRepository,
    private val userAdminRepository: UserAdminRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _quizState = MutableLiveData<QuizState>()
    val quizState: LiveData<QuizState> = _quizState

    private val _resultState = MutableLiveData<ResultState>()
    val resultState: LiveData<ResultState> = _resultState

    private val _quizResultsState = MutableLiveData<QuizResultsState>()
    val quizResultsState: LiveData<QuizResultsState> = _quizResultsState

    private val _selectedStudentState = MutableLiveData<SelectedStudentState>()
    val selectedStudentState: LiveData<SelectedStudentState> = _selectedStudentState

    private val _loadedUsers = MutableLiveData<Map<String, User>>()
    val loadedUsers: LiveData<Map<String, User>> = _loadedUsers

    fun getQuizBySubBabId(subBabId: String) {
        viewModelScope.launch {
            _quizState.value = QuizState.Loading
            try {
                val quiz = quizRepository.getQuizBySubBabId(subBabId)
                _quizState.value = if (quiz != null) {
                    QuizState.Success(quiz)
                } else {
                    QuizState.Error(resourceProvider.getString(R.string.quiz_not_found))
                }
            } catch (e: Exception) {
                _quizState.value = QuizState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_loading_quiz)
                )
            }
        }
    }

    fun submitQuiz(
        quiz: Quiz,
        answers: List<Int>,
        essayAnswers: Map<String, String>,
        timeSpent: Int
    ) {
        viewModelScope.launch {
            _resultState.value = ResultState.Loading
            try {
                val totalEvaluable = quiz.questions.count { it.questionType != QuestionType.ESSAY }
                val correctAnswers = quiz.questions.withIndex()
                    .filter { (_, question) -> question.questionType != QuestionType.ESSAY }
                    .map { (index, question) ->
                        val answer = answers.getOrNull(index) ?: -1
                        if (answer == question.correctAnswer) 1 else 0
                    }.sum()

                val score = if (totalEvaluable > 0) {
                    (correctAnswers.toFloat() / totalEvaluable) * 100
                } else {
                    0f
                }
                val isPassed = score >= quiz.passingScore

                val currentUserId = studentProgressRepository.getCurrentUserId()

                val existingResult =
                    quizRepository.getStudentQuizResult(currentUserId, quiz.subBabId)

                val result = // Update existing result - preserve the original ID
                    existingResult?.copy(
                        score = score,
                        answers = answers,
                        essayAnswers = essayAnswers,
                        essayGrading = existingResult.essayGrading, // Preserve existing grading
                        timeSpent = timeSpent,
                        completedAt = Timestamp.now(),
                        isPassed = isPassed
                    )
                        ?: // Create new result
                        QuizResult(
                            studentId = currentUserId,
                            quizId = quiz.id,
                            subBabId = quiz.subBabId,
                            score = score,
                            answers = answers,
                            essayAnswers = essayAnswers,
                            essayGrading = emptyMap(), // Initialize empty grading
                            timeSpent = timeSpent,
                            completedAt = Timestamp.now(),
                            isPassed = isPassed
                        )

                if (existingResult != null) {
                    quizRepository.updateQuizResult(result)
                } else {
                    quizRepository.saveQuizResult(result)
                }

                val subBab = subBabRepository.getSubBab(quiz.subBabId)
                studentProgressRepository.updateQuizScore(
                    currentUserId,
                    quiz.subBabId,
                    subBab.lessonId,
                    score
                )

                _resultState.value = ResultState.Success(result)
            } catch (e: Exception) {
                _resultState.value = ResultState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_submitting_quiz)
                )
            }
        }
    }

    fun getStudentQuizResults(subBabId: String) {
        viewModelScope.launch {
            _resultState.value = ResultState.Loading
            try {
                val results = quizRepository.getStudentQuizResults(
                    studentProgressRepository.getCurrentUserId(),
                    subBabId
                )
                _resultState.value = ResultState.History(results)
            } catch (e: Exception) {
                _resultState.value = ResultState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_loading_results)
                )
            }
        }
    }

    fun getStudentQuizResult(subBabId: String) {
        viewModelScope.launch {
            _resultState.value = ResultState.Loading
            try {
                val result = quizRepository.getStudentQuizResult(
                    studentProgressRepository.getCurrentUserId(),
                    subBabId
                )
                _resultState.value = if (result != null) {
                    ResultState.Success(result)
                } else {
                    ResultState.Error("No quiz result found")
                }
            } catch (e: Exception) {
                _resultState.value = ResultState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_loading_results)
                )
            }
        }
    }

    fun createQuiz(quiz: Quiz, onResult: (Result<Quiz>) -> Unit) {
        viewModelScope.launch {
            _quizState.value = QuizState.Loading
            try {
                quizRepository.createQuiz(quiz)

                val createdQuiz = quizRepository.getQuizBySubBabId(quiz.subBabId)
                    ?: throw Exception("Failed to retrieve created quiz")
                _quizState.value = QuizState.Success(createdQuiz)
                onResult(Result.success(createdQuiz))
            } catch (e: Exception) {
                val errorMessage =
                    e.message ?: resourceProvider.getString(R.string.error_loading_quiz)
                _quizState.value = QuizState.Error(errorMessage)
                onResult(Result.failure(e))
            }
        }
    }

    fun updateQuiz(quiz: Quiz, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _quizState.value = QuizState.Loading
            try {
                quizRepository.updateQuiz(quiz)
                _quizState.value = QuizState.Success(quiz)
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                val errorMessage =
                    e.message ?: resourceProvider.getString(R.string.error_loading_quiz)
                _quizState.value = QuizState.Error(errorMessage)
                onResult(Result.failure(e))
            }
        }
    }

    sealed class QuizState {
        data object Loading : QuizState()
        data class Success(val quiz: Quiz) : QuizState()
        data class Error(val message: String) : QuizState()
    }

    sealed class ResultState {
        data object Loading : ResultState()
        data class Success(val result: QuizResult) : ResultState()
        data class History(val results: List<QuizResult>) : ResultState()
        data class Error(val message: String) : ResultState()
    }

    fun getAllQuizResultsBySubBabId(subBabId: String) {
        viewModelScope.launch {
            Log.d(
                "QuizViewModel",
                "getAllQuizResultsBySubBabId called with subBabId: $subBabId"
            )
            _quizResultsState.value = QuizResultsState.Loading
            try {
                val results = quizRepository.getAllQuizResultsBySubBabId(subBabId)
                Log.d("QuizViewModel", "Quiz results loaded: ${results.size} results")
                results.forEachIndexed { index, result ->
                    Log.d(
                        "QuizViewModel",
                        "Result $index: id=${result.id}, studentId=${result.studentId}, quizId=${result.quizId}"
                    )
                }
                _quizResultsState.value = QuizResultsState.Success(results)
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error loading quiz results: ${e.message}", e)
                _quizResultsState.value = QuizResultsState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_loading_results)
                )
            }
        }
    }

    fun getUserById(userId: String) {
        viewModelScope.launch {
            _selectedStudentState.value = SelectedStudentState.Loading
            try {
                val user = userAdminRepository.getUserById(userId)
                if (user != null) {
                    _selectedStudentState.value = SelectedStudentState.Success(user)

                    val currentMap = _loadedUsers.value?.toMutableMap() ?: mutableMapOf()
                    currentMap[userId] = user
                    _loadedUsers.value = currentMap
                } else {
                    _selectedStudentState.value = SelectedStudentState.Error("User not found")
                }
            } catch (e: Exception) {
                _selectedStudentState.value = SelectedStudentState.Error(
                    e.message ?: "Error loading user"
                )
            }
        }
    }

    fun loadMultipleUsers(userIds: List<String>) {
        viewModelScope.launch {
            Log.d(
                "QuizViewModel",
                "loadMultipleUsers called with ${userIds.size} user IDs: $userIds"
            )
            val usersMap = mutableMapOf<String, User>()
            userIds.forEach { userId ->
                try {
                    Log.d("QuizViewModel", "Loading user: $userId")
                    val user = userAdminRepository.getUserById(userId)
                    if (user != null) {
                        usersMap[userId] = user
                        Log.d(
                            "QuizViewModel",
                            "User loaded successfully: $userId -> ${user.name}"
                        )
                    } else {
                        Log.w("QuizViewModel", "User not found: $userId")
                    }
                } catch (e: Exception) {
                    Log.e(
                        "QuizViewModel",
                        "Error loading user $userId: ${e.message}",
                        e
                    )
                }
            }
            Log.d(
                "QuizViewModel",
                "Finished loading users. Total loaded: ${usersMap.size}"
            )
            _loadedUsers.value = usersMap
        }
    }

    sealed class QuizResultsState {
        data object Loading : QuizResultsState()
        data class Success(val results: List<QuizResult>) : QuizResultsState()
        data class Error(val message: String) : QuizResultsState()
    }

    sealed class SelectedStudentState {
        data object Loading : SelectedStudentState()
        data class Success(val user: User) : SelectedStudentState()
        data class Error(val message: String) : SelectedStudentState()
    }

    fun gradeEssayAnswer(resultId: String, questionId: String, isCorrect: Boolean, quiz: Quiz) {
        viewModelScope.launch {
            try {
                Log.d(
                    "QuizViewModel",
                    "gradeEssayAnswer: resultId=$resultId, questionId=$questionId, isCorrect=$isCorrect"
                )

                val currentResult = quizRepository.getQuizResultById(resultId)
                    ?: throw Exception("Quiz result not found")

                Log.d(
                    "QuizViewModel",
                    "Current essayGrading: ${currentResult.essayGrading}"
                )

                val updatedEssayGrading = currentResult.essayGrading.toMutableMap()
                updatedEssayGrading[questionId] = isCorrect

                Log.d("QuizViewModel", "Updated essayGrading: $updatedEssayGrading")

                val scores = calculateQuizScores(quiz, currentResult.answers, updatedEssayGrading)

                Log.d(
                    "QuizViewModel",
                    "Calculated scores: multipleChoice=${scores.multipleChoiceScore}, essay=${scores.essayScore}, total=${scores.totalScore}"
                )

                val updatedResult = currentResult.copy(
                    essayGrading = updatedEssayGrading,
                    score = scores.totalScore,
                    isPassed = scores.totalScore >= quiz.passingScore
                )

                quizRepository.updateQuizResult(updatedResult)

                Log.d(
                    "QuizViewModel",
                    "Result updated successfully: score=${updatedResult.score}, essayGrading=${updatedResult.essayGrading}"
                )

                val subBab = subBabRepository.getSubBab(quiz.subBabId)
                studentProgressRepository.updateQuizScore(
                    currentResult.studentId,
                    quiz.subBabId,
                    subBab.lessonId,
                    scores.totalScore
                )

                _resultState.value = ResultState.Success(updatedResult)
            } catch (e: Exception) {
                Log.e("QuizViewModel", "Error grading essay: ${e.message}", e)
                _resultState.value = ResultState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_grading_essay)
                )
            }
        }
    }

    private fun calculateQuizScores(
        quiz: Quiz,
        answers: List<Int>,
        essayGrading: Map<String, Boolean>
    ): QuizScores {

        val multipleChoiceQuestions = quiz.questions
            .withIndex()
            .filter { (_, question) -> question.questionType != QuestionType.ESSAY }

        val totalMultipleChoice = multipleChoiceQuestions.size
        val correctMultipleChoice = multipleChoiceQuestions.count { (index, question) ->
            val answer = answers.getOrNull(index) ?: -1
            answer == question.correctAnswer
        }

        val multipleChoiceScore = if (totalMultipleChoice > 0) {
            (correctMultipleChoice.toFloat() / totalMultipleChoice) * 100
        } else {
            0f
        }

        val essayQuestions = quiz.questions.filter { it.questionType == QuestionType.ESSAY }
        val totalEssay = essayQuestions.size

        if (totalEssay == 0) {

            return QuizScores(
                multipleChoiceScore = multipleChoiceScore,
                essayScore = 0f,
                totalScore = multipleChoiceScore
            )
        }

        val correctEssay = essayQuestions.count { question ->
            essayGrading[question.id] == true
        }


        val essayScore = (correctEssay.toFloat() / totalEssay) * 100

        val totalScore = if (totalMultipleChoice > 0 && totalEssay > 0) {

            (multipleChoiceScore + essayScore) / 2
        } else if (totalMultipleChoice > 0) {

            multipleChoiceScore
        } else {

            essayScore
        }

        return QuizScores(
            multipleChoiceScore = multipleChoiceScore,
            essayScore = essayScore,
            totalScore = totalScore
        )
    }

    fun recalculateAndUpdateScore(resultId: String, quiz: Quiz) {
        viewModelScope.launch {
            try {

                val currentResult = quizRepository.getQuizResultById(resultId)
                    ?: throw Exception("Quiz result not found")

                val scores =
                    calculateQuizScores(quiz, currentResult.answers, currentResult.essayGrading)

                val updatedResult = currentResult.copy(
                    score = scores.totalScore,
                    isPassed = scores.totalScore >= quiz.passingScore
                )

                quizRepository.updateQuizResult(updatedResult)

                val subBab = subBabRepository.getSubBab(quiz.subBabId)
                studentProgressRepository.updateQuizScore(
                    currentResult.studentId,
                    quiz.subBabId,
                    subBab.lessonId,
                    scores.totalScore
                )

                _resultState.value = ResultState.Success(updatedResult)
            } catch (e: Exception) {
                _resultState.value = ResultState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_grading_essay)
                )
            }
        }
    }

    private data class QuizScores(
        val multipleChoiceScore: Float,
        val essayScore: Float,
        val totalScore: Float
    )
} 