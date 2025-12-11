package com.adika.learnable.viewmodel.lesson

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.Bookmark
import com.adika.learnable.model.QuizResult
import com.adika.learnable.model.StudentSubBabProgress
import com.adika.learnable.model.SubBab
import com.adika.learnable.repository.BookmarkRepository
import com.adika.learnable.repository.LessonRepository
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.repository.SubBabRepository
import com.adika.learnable.repository.SubjectRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentSubBabDetailViewModel @Inject constructor(
    private val subBabRepository: SubBabRepository,
    private val studentProgressRepository: StudentProgressRepository,
    private val bookmarkRepository: BookmarkRepository,
    private val lessonRepository: LessonRepository,
    private val subjectRepository: SubjectRepository,
    private val resourceProvider: ResourceProvider,
    private val quizRepository: com.adika.learnable.repository.QuizRepository
) : ViewModel() {

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    private val _progressState = MutableLiveData<ProgressState>()
    val progressState: LiveData<ProgressState> = _progressState

    private val _bookmarkState = MutableLiveData<BookmarkState>()
    val bookmarkState: LiveData<BookmarkState> = _bookmarkState

    fun getSubBab(subBabId: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val subBab = subBabRepository.getSubBab(subBabId)
                _studentState.value = StudentState.Success(subBab)
                loadProgressAndQuiz(subBabId)
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error getting sub-bab", e)
                _studentState.value = StudentState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_load_subbab)
                )
            }
        }
    }

    private fun loadProgressAndQuiz(subBabId: String) {
        viewModelScope.launch {
            _progressState.value = ProgressState.Loading
            try {
                val studentId = studentProgressRepository.getCurrentUserId()
                val progress =
                    studentProgressRepository.getStudentSubBabProgress(studentId, subBabId)
                val latestResult = quizRepository.getStudentQuizResult(studentId, subBabId)
                _progressState.value = ProgressState.Success(progress, latestResult)
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error loading progress", e)
                _progressState.value = ProgressState.Error(e.message ?: "Fail Load progress")
            }
        }
    }

    fun markMaterialAsCompleted(subBabId: String, materialType: String) {
        viewModelScope.launch {
            _progressState.value = ProgressState.Loading
            try {
                val studentId = studentProgressRepository.getCurrentUserId()
                val currentState = _studentState.value
                if (currentState is StudentState.Success) {
                    val subBab = currentState.subBab
                    studentProgressRepository.markMaterialAsCompleted(
                        studentId,
                        subBabId,
                        subBab.lessonId,
                        materialType
                    )

                    loadProgressAndQuiz(subBabId)
                }
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error marking material as completed", e)
                _progressState.value = ProgressState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_update_progress)
                )
            }
        }
    }

    fun updateTimeSpent(subBabId: String, seconds: Int) {
        viewModelScope.launch {
            _progressState.value = ProgressState.Loading
            try {
                val studentId = studentProgressRepository.getCurrentUserId()
                val currentState = _studentState.value
                if (currentState is StudentState.Success) {
                    val subBab = currentState.subBab
                    studentProgressRepository.updateTimeSpent(
                        studentId,
                        subBabId,
                        subBab.lessonId,
                        seconds
                    )

                    loadProgressAndQuiz(subBabId)
                }
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error updating time spent", e)
                _progressState.value = ProgressState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_update_progress)
                )
            }
        }
    }

    fun checkBookmarkStatus(subBabId: String) {
        viewModelScope.launch {
            try {
                val studentId = studentProgressRepository.getCurrentUserId()
                val currentState = _studentState.value
                if (currentState is StudentState.Success) {
                    val subBab = currentState.subBab
                    val isBookmarked =
                        bookmarkRepository.isBookmarked(studentId, subBab.lessonId, subBabId)
                    _bookmarkState.value = BookmarkState.Success(isBookmarked)
                }
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error checking bookmark status", e)
                _bookmarkState.value =
                    BookmarkState.Error(e.message ?: "Failed to check bookmark status")
            }
        }
    }

    fun toggleBookmark(subBabId: String, subjectId: String?) {
        viewModelScope.launch {
            try {
                val studentId = studentProgressRepository.getCurrentUserId()
                val currentState = _studentState.value
                if (currentState is StudentState.Success) {
                    val subBab = currentState.subBab

                    val isBookmarked =
                        bookmarkRepository.isBookmarked(studentId, subBab.lessonId, subBabId)

                    if (isBookmarked) {

                        val bookmarks = bookmarkRepository.getBookmarksByStudentId(studentId)
                        val bookmarkToRemove = bookmarks.find {
                            it.studentId == studentId &&
                                    it.lessonId == subBab.lessonId &&
                                    it.subBabId == subBabId
                        }

                        if (bookmarkToRemove != null) {
                            val result = bookmarkRepository.removeBookmark(bookmarkToRemove.id)
                            if (result.isSuccess) {
                                _bookmarkState.value = BookmarkState.Success(false)
                            } else {
                                _bookmarkState.value =
                                    BookmarkState.Error("Failed to remove bookmark")
                            }
                        } else {
                            _bookmarkState.value = BookmarkState.Success(false)
                        }
                    } else {

                        val lesson = lessonRepository.getLesson(subBab.lessonId)
                        if (lesson == null) {
                            _bookmarkState.value = BookmarkState.Error("Lesson not found")
                            return@launch
                        }

                        val subject =
                            subjectRepository.getSubjectById(subjectId ?: lesson.idSubject)
                        if (subject == null) {
                            _bookmarkState.value = BookmarkState.Error("Subject not found")
                            return@launch
                        }

                        val subBabProgress =
                            studentProgressRepository.getStudentSubBabProgress(studentId, subBabId)
                        val completedMaterials = subBabProgress?.completedMaterials ?: mapOf(
                            "pdf" to false,
                            "video" to false,
                            "quiz" to false
                        )

                        val bookmark = Bookmark(
                            studentId = studentId,
                            subjectId = subjectId ?: lesson.idSubject,
                            lessonId = subBab.lessonId,
                            subBabId = subBabId,
                            completedMaterials = completedMaterials,
                            lessonTitle = lesson.title,
                            subBabTitle = subBab.title,
                            subjectName = subject.name,
                            schoolLevel = lesson.schoolLevel,
                            coverImage = subBab.coverImage
                        )

                        val result = bookmarkRepository.addBookmark(bookmark)
                        if (result.isSuccess) {
                            _bookmarkState.value = BookmarkState.Success(true)
                        } else {
                            _bookmarkState.value = BookmarkState.Error("Failed to add bookmark")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error toggling bookmark", e)
                _bookmarkState.value = BookmarkState.Error(e.message ?: "Failed to toggle bookmark")
            }
        }
    }

    sealed class StudentState {
        data object Loading : StudentState()
        data class Success(val subBab: SubBab) : StudentState()
        data class Error(val message: String) : StudentState()
    }

    sealed class ProgressState {
        data object Loading : ProgressState()
        data class Success(
            val progress: StudentSubBabProgress?,
            val latestQuizResult: QuizResult?
        ) : ProgressState()

        data class Error(val message: String) : ProgressState()
    }

    sealed class BookmarkState {
        data object Loading : BookmarkState()
        data class Success(val isBookmarked: Boolean) : BookmarkState()
        data class Error(val message: String) : BookmarkState()
    }
} 