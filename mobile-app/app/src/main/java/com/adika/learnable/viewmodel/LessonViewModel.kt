package com.adika.learnable.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.StudentLessonProgress
import com.adika.learnable.model.SubBab
import com.adika.learnable.repository.LessonRepository
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.repository.SubBabRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val lessonRepository: LessonRepository,
    private val subBabRepository: SubBabRepository,
    private val studentProgressRepository: StudentProgressRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    private val _teacherState = MutableLiveData<TeacherState>()
    val teacherState: LiveData<TeacherState> = _teacherState

    private val _progressState = MutableLiveData<ProgressState>()
    val progressState: LiveData<ProgressState> = _progressState

    /// Student ///
    // Bagian untuk Lesson
    fun getLessonsBySubjectAndDisabilityType(idSubject: String, disabilityType: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val lessons = lessonRepository.getLessonsBySubjectAndDisabilityType(
                    idSubject,
                    disabilityType
                )
                Log.d("LessonViewModel", "Retrieved ${lessons.size} lessons")
                _studentState.value = StudentState.Success(
                    lessons = lessons,
                    selectedLesson = null,
                    subBabs = emptyList()
                )
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error getting lessons", e)
                _studentState.value = StudentState.Error(e.message ?: resourceProvider.getString(R.string.fail_load_lessons))
            }
        }
    }

    fun searchLessons(query: String, disabilityType: String, idSubject: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val lessons = lessonRepository.searchLessons(query, disabilityType, idSubject)
                _studentState.value = StudentState.Success(
                    lessons = lessons,
                    selectedLesson = null,
                    subBabs = emptyList()
                )
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(e.message ?: resourceProvider.getString(R.string.fail_find_lessons))
            }
        }
    }

    // Bagian sub bab
    fun loadSubBabsForLessonStudent(lessonId: String) {
        viewModelScope.launch {
            try {
                val currentState = _studentState.value
                if (currentState is StudentState.Success) {
                    val lessons = currentState.lessons
                    val selectedLesson = lessons.find { it.id == lessonId }

                    if (selectedLesson != null) {
                        val subBabs = subBabRepository.getSubBabByLesson(lessonId)
                        _studentState.value = currentState.copy(
                            selectedLesson = selectedLesson,
                            subBabs = subBabs
                        )
                        loadLessonProgress(lessonId)
                    } else {
                        Log.e("LessonViewModel", "Selected lesson not found: $lessonId")
                    }
                } else {
                    Log.e("LessonViewModel", "Invalid state for loading sub-babs: $currentState")
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error loading sub-babs", e)
                _studentState.value = StudentState.Error(e.message ?: resourceProvider.getString(R.string.fail_load_subbab))
            }
        }
    }

    /// Teacher ///
    // Bagian CRUD Lesson
    fun getLessonsByTeacherId(teacherId: String) {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                val lessons = lessonRepository.getLessonsByTeacherId(teacherId)
                _teacherState.value = TeacherState.Success(
                    lessons = lessons,
                    selectedLesson = null,
                    subBabs = emptyList()
                )
            } catch (e: Exception) {
                _teacherState.value = TeacherState.Error(e.message ?: resourceProvider.getString(R.string.fail_load_lessons))
            }
        }
    }

    fun addLesson(lesson: Lesson, onResult: (Result<Lesson>) -> Unit) {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                val newLesson = lessonRepository.addLesson(lesson)
                onResult(Result.success(newLesson))
                // Refresh lesson list
                getLessonsByTeacherId(lesson.teacherId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun updateLesson(lesson: Lesson, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                lessonRepository.updateLesson(lesson)
                onResult(Result.success(Unit))
                // Refresh lesson list
                getLessonsByTeacherId(lesson.teacherId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun deleteLesson(lessonId: String, teacherId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                // Delete all sub-babs first
                subBabRepository.deleteAllSubBabsForLesson(lessonId)
                // Then delete the lesson
                lessonRepository.deleteLesson(lessonId)
                onResult(Result.success(Unit))
                // Refresh lesson list
                getLessonsByTeacherId(teacherId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    // Bagian CRUD Sub Bab
    fun addSubBab(subBab: SubBab, onResult: (Result<SubBab>) -> Unit) {
        viewModelScope.launch {
            try {
                val newSubBab = subBabRepository.addSubBab(subBab)
                onResult(Result.success(newSubBab))
                // Refresh sub-bab list
                loadSubBabsForLessonTeacher(subBab.lessonId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun updateSubBab(subBab: SubBab, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                subBabRepository.updateSubBab(subBab)
                onResult(Result.success(Unit))
                // Refresh sub-bab list
                loadSubBabsForLessonTeacher(subBab.lessonId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun deleteSubBab(subBabId: String, lessonId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                subBabRepository.deleteSubBab(subBabId)
                onResult(Result.success(Unit))
                // Refresh sub-bab list
                loadSubBabsForLessonTeacher(lessonId)
            } catch (e: Exception) {
                onResult(Result.failure(e))
            }
        }
    }

    fun loadSubBabsForLessonTeacher(lessonId: String) {
        viewModelScope.launch {
            try {
                val currentState = _teacherState.value
                if (currentState is TeacherState.Success) {
                    val lessons = currentState.lessons
                    val selectedLesson = lessons.find { it.id == lessonId }

                    if (selectedLesson != null) {
                        val subBabs = subBabRepository.getSubBabByLesson(lessonId)
                        _teacherState.value = currentState.copy(
                            selectedLesson = selectedLesson,
                            subBabs = subBabs
                        )
                    } else {
                        Log.e("LessonViewModel", "Selected lesson not found: $lessonId")
                    }
                } else {
                    Log.e("LessonViewModel", "Invalid state for loading sub-babs: $currentState")
                }
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error loading sub-babs", e)
                _teacherState.value = TeacherState.Error(e.message ?: resourceProvider.getString(R.string.fail_load_subbab))
            }
        }
    }

    private fun loadLessonProgress(lessonId: String) {
        val studentId = studentProgressRepository.getCurrentUserId()
        viewModelScope.launch {
            _progressState.value = ProgressState.Loading
            try {
                val progress = studentProgressRepository.getStudentLessonProgress(studentId, lessonId)
                _progressState.value = ProgressState.Success(progress)
            } catch (e: Exception) {
                _progressState.value = ProgressState.Error(e.message ?: "Fail Load progress")
            }
        }
    }




    sealed class StudentState {
        data object Loading : StudentState()
        data class Success(
            val lessons: List<Lesson>,
            val selectedLesson: Lesson?,
            val subBabs: List<SubBab>
        ) : StudentState()

        data class Error(val message: String) : StudentState()
    }

    sealed class TeacherState {
        data object Loading : TeacherState()
        data class Success(
            val lessons: List<Lesson>,
            val selectedLesson: Lesson? = null,
            val subBabs: List<SubBab> = emptyList()
        ) : TeacherState()
        data class Error(val message: String) : TeacherState()
    }

    sealed class ProgressState {
        data object Loading : ProgressState()
        data class Success(val progress: StudentLessonProgress?) : ProgressState()
        data class Error(val message: String) : ProgressState()
    }
} 