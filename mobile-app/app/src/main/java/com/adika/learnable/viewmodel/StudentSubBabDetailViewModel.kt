package com.adika.learnable.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.StudentSubBabProgress
import com.adika.learnable.model.SubBab
import com.adika.learnable.repository.StudentProgressRepository
import com.adika.learnable.repository.SubBabRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudentSubBabDetailViewModel @Inject constructor(
    private val subBabRepository: SubBabRepository,
    private val studentProgressRepository: StudentProgressRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    private val _progressState = MutableLiveData<ProgressState>()
    val progressState: LiveData<ProgressState> = _progressState

    fun getSubBab(subBabId: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val subBab = subBabRepository.getSubBab(subBabId)
                _studentState.value = StudentState.Success(subBab)
                loadProgress(subBabId)
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error getting sub-bab", e)
                _studentState.value = StudentState.Error(e.message ?: resourceProvider.getString(R.string.fail_load_subbab))
            }
        }
    }

    private fun loadProgress(subBabId: String) {
        viewModelScope.launch {
            _progressState.value = ProgressState.Loading
            try {
                val studentId = studentProgressRepository.getCurrentUserId()
                val progress = studentProgressRepository.getSubBabProgress(studentId, subBabId)
                _progressState.value = ProgressState.Success(progress)
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
                    studentProgressRepository.markMaterialAsCompleted(studentId, subBabId, subBab.lessonId, materialType)
                    // Refresh progress after update
                    loadProgress(subBabId)
                }
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error marking material as completed", e)
                _progressState.value = ProgressState.Error(e.message ?: resourceProvider.getString(R.string.fail_update_progress))
            }
        }
    }

    fun updateQuizScore(subBabId: String, score: Float) {
        viewModelScope.launch {
            _progressState.value = ProgressState.Loading
            try {
                val studentId = studentProgressRepository.getCurrentUserId()
                val currentState = _studentState.value
                if (currentState is StudentState.Success) {
                    val subBab = currentState.subBab
                    studentProgressRepository.updateQuizScore(studentId, subBabId, subBab.lessonId, score)
                    // Refresh progress after update
                    loadProgress(subBabId)
                }
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error updating quiz score", e)
                _progressState.value = ProgressState.Error(e.message ?: resourceProvider.getString(R.string.fail_update_progress))
            }
        }
    }

    fun updateTimeSpent(subBabId: String, minutes: Int) {
        viewModelScope.launch {
            _progressState.value = ProgressState.Loading
            try {
                val studentId = studentProgressRepository.getCurrentUserId()
                val currentState = _studentState.value
                if (currentState is StudentState.Success) {
                    val subBab = currentState.subBab
                    studentProgressRepository.updateTimeSpent(studentId, subBabId, subBab.lessonId, minutes)
                    // Refresh progress after update
                    loadProgress(subBabId)
                }
            } catch (e: Exception) {
                Log.e("StudentSubBabDetailViewModel", "Error updating time spent", e)
                _progressState.value = ProgressState.Error(e.message ?: resourceProvider.getString(R.string.fail_update_progress))
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
        data class Success(val progress: StudentSubBabProgress?) : ProgressState()
        data class Error(val message: String) : ProgressState()
    }
} 