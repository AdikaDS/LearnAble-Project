package com.adika.learnable.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.Quiz
import com.adika.learnable.model.QuizResult
import com.adika.learnable.repository.QuizRepository
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.util.ResourceProvider
import com.google.firebase.Timestamp
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuizViewModel @Inject constructor(
    private val quizRepository: QuizRepository,
    private val studentProgressRepository: StudentProgressRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _quizState = MutableLiveData<QuizState>()
    val quizState: LiveData<QuizState> = _quizState

    private val _resultState = MutableLiveData<ResultState>()
    val resultState: LiveData<ResultState> = _resultState

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
                _quizState.value = QuizState.Error(e.message ?: resourceProvider.getString(R.string.error_loading_quiz))
            }
        }
    }

    fun submitQuiz(quiz: Quiz, answers: List<Int>, timeSpent: Int) {
        viewModelScope.launch {
            _resultState.value = ResultState.Loading
            try {
                val correctAnswers = answers.mapIndexed { index, answer ->
                    if (answer == quiz.questions[index].correctAnswer) 1 else 0
                }.sum()

                val score = (correctAnswers.toFloat() / quiz.questions.size) * 100
                val isPassed = score >= quiz.passingScore

                val result = QuizResult(
                    studentId = studentProgressRepository.getCurrentUserId(),
                    quizId = quiz.id,
                    subBabId = quiz.subBabId,
                    score = score,
                    answers = answers,
                    timeSpent = timeSpent,
                    completedAt = Timestamp.now(),
                    isPassed = isPassed
                )

                quizRepository.saveQuizResult(result)

                if (isPassed) {
                    studentProgressRepository.markMaterialAsCompleted(
                        studentProgressRepository.getCurrentUserId(),
                        quiz.subBabId,
                        "", // lessonId akan diisi oleh repository
                        "quiz"
                    )
                }

                _resultState.value = ResultState.Success(result)
            } catch (e: Exception) {
                _resultState.value = ResultState.Error(e.message ?: resourceProvider.getString(R.string.error_submitting_quiz))
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
                _resultState.value = ResultState.Error(e.message ?: resourceProvider.getString(R.string.error_loading_results))
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
} 