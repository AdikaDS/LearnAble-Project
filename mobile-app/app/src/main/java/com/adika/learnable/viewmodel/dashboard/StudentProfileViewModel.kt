package com.adika.learnable.viewmodel.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.StudentLessonProgress
import com.adika.learnable.model.StudentOverallProgress
import com.adika.learnable.model.StudentSubjectProgress
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentProfileViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val studentProgressRepository: StudentProgressRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    private val _overallProgressState = MutableLiveData<OverallProgressState>()
    val overallProgressState: LiveData<OverallProgressState> = _overallProgressState

    private val _subjectProgressState = MutableLiveData<SubjectProgressState>()
    val subjectProgressState: LiveData<SubjectProgressState> = _subjectProgressState

    private val _lessonProgressState = MutableLiveData<LessonProgressState>()
    val lessonProgressState: LiveData<LessonProgressState> = _lessonProgressState

    fun loadStudentData(studentId: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val student = authRepository.getUserData(studentId)
                _studentState.value = StudentState.Success(student)
                loadOverallProgress(studentId)
                loadSubjectProgress(studentId)
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(e.message ?: resourceProvider.getString(R.string.fail_get_user_data))
            }
        }
    }

    private fun loadOverallProgress(studentId: String) {
        viewModelScope.launch {
            _overallProgressState.value = OverallProgressState.Loading
            try {
                val progress = studentProgressRepository.getStudentOverallProgress(studentId)
                _overallProgressState.value = OverallProgressState.Success(progress)
            } catch (e: Exception) {
                _overallProgressState.value = OverallProgressState.Error(e.message ?: resourceProvider.getString(R.string.fail_load_overall_progress))
            }
        }
    }

    private fun loadSubjectProgress(studentId: String) {
        viewModelScope.launch {
            _subjectProgressState.value = SubjectProgressState.Loading
            try {
                val progress = studentProgressRepository.getStudentSubjectProgress(studentId)
                _subjectProgressState.value = SubjectProgressState.Success(progress)
            } catch (e: Exception) {
                _subjectProgressState.value = SubjectProgressState.Error(e.message ?: resourceProvider.getString(R.string.fail_load_subject_progress))
            }
        }
    }

    private fun loadLessonProgress(studentId: String, lessonId: String) {
        viewModelScope.launch {
            _lessonProgressState.value = LessonProgressState.Loading
            try {
                val progress = studentProgressRepository.getStudentLessonProgress(studentId, lessonId)
                _lessonProgressState.value = LessonProgressState.Success(progress)
            } catch (e: Exception) {
                _lessonProgressState.value = LessonProgressState.Error(e.message ?: resourceProvider.getString(R.string.fail_load_lesson_progress))
            }
        }
    }

    sealed class OverallProgressState {
        data object Loading : OverallProgressState()
        data class Success(val progress: StudentOverallProgress?) : OverallProgressState()
        data class Error(val message: String) : OverallProgressState()
    }

    sealed class SubjectProgressState {
        data object Loading : SubjectProgressState()
        data class Success(val progress: List<StudentSubjectProgress>) : SubjectProgressState()
        data class Error(val message: String) : SubjectProgressState()
    }

    sealed class LessonProgressState {
        data object Loading : LessonProgressState()
        data class Success(val progress: StudentLessonProgress?) : LessonProgressState()
        data class Error(val message: String) : LessonProgressState()
    }

    sealed class StudentState {
        data object Loading : StudentState()
        data class Success(val student: User) : StudentState()
        data class Error(val message: String) : StudentState()
    }
} 