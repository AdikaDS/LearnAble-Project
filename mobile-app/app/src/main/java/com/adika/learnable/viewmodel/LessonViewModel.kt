package com.adika.learnable.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.Lesson
import com.adika.learnable.repository.LessonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LessonViewModel @Inject constructor(
    private val lessonRepository: LessonRepository
) : ViewModel() {

    private val _uiState = MutableLiveData<LessonState>()
    val uiState: LiveData<LessonState> = _uiState

    private val _lessons = MutableLiveData<List<Lesson>>()
    val lessons: LiveData<List<Lesson>> = _lessons

    private val _totalLessons = MutableLiveData<Int>()
    val totalLessons: LiveData<Int> = _totalLessons

    fun getLessonsBySubjectAndDisabilityType(idSubject: String, disabilityType: String) {
        Log.d("LessonViewModel", "Getting lessons for subject: $idSubject, disability: $disabilityType")
        viewModelScope.launch {
            _uiState.value = LessonState.Loading
            try {
                val lessons = lessonRepository.getLessonsBySubjectAndDisabilityType(
                    idSubject,
                    disabilityType
                )
                Log.d("LessonViewModel", "Retrieved ${lessons.size} lessons")
                lessons.forEach { lesson ->
                    Log.d("LessonViewModel", "Lesson: ${lesson.title}, Subject: ${lesson.idSubject}, Disabilities: ${lesson.disabilityTypes}")
                }
                _lessons.value = lessons
                _totalLessons.value = lessons.size
                _uiState.value = LessonState.Success(null)
            } catch (e: Exception) {
                Log.e("LessonViewModel", "Error getting lessons", e)
                _uiState.value = LessonState.Error(e.message ?: "Gagal mengambil materi")
            }
        }
    }

    fun searchLessons(query: String, disabilityType: String? = null, idSubject: String? = null) {
        viewModelScope.launch {
            _uiState.value = LessonState.Loading
            try {
                val lessons = lessonRepository.searchLessons(query, disabilityType, idSubject)
                _lessons.value = lessons
                _uiState.value = LessonState.Success(null)
            } catch (e: Exception) {
                _uiState.value = LessonState.Error(e.message ?: "Gagal mencari materi")
            }
        }
    }

    sealed class LessonState {
        data object Loading : LessonState()
        data class Success(val lesson: Lesson?) : LessonState()
        data class Error(val message: String) : LessonState()
    }
} 