package com.adika.learnable.viewmodel.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.LearningProgress
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.UserParentRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userParentRepository: UserParentRepository
) : ViewModel() {

    private val _parentState = MutableLiveData<ParentState>()
    val parentState: LiveData<ParentState> = _parentState

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    private val _studentProgressState = MutableLiveData<StudentProgressState>()
    val studentProgressState: LiveData<StudentProgressState> = _studentProgressState

    fun loadUserData() {
        viewModelScope.launch {
            _parentState.value = ParentState.Loading
            try {
                val userParentId = authRepository.getUserData(authRepository.getCurrentUserId())
                _parentState.value = ParentState.Success(userParentId)
            } catch (e: Exception) {
                _parentState.value =
                    ParentState.Error(e.message ?: "Terjadi kesalahan saat memuat profil")
            }
        }
    }

    fun loadStudents() {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val students = userParentRepository.getStudentsByParentId(authRepository.getCurrentUserId())
                _studentState.value = StudentState.Success(students)
            } catch (e: Exception) {
                _studentState.value =
                    StudentState.Error(e.message ?: "Terjadi kesalahan saat memuat data siswa")
            }
        }
    }

    fun loadStudentProgress(studentId: String) {
        viewModelScope.launch {
            _studentProgressState.value = StudentProgressState.Loading
            try {
                val progressList = userParentRepository.getStudentProgress(studentId)
                _studentProgressState.value = StudentProgressState.Success(progressList)
            } catch (e: Exception) {
                _studentProgressState.value = StudentProgressState.Error(
                    e.message ?: "Terjadi kesalahan saat memuat progress belajar"
                )
            }
        }
    }

    private suspend fun isStudentConnectedToOtherParent(studentId: String): Boolean {
        val parent = userParentRepository.getParentByStudentId(studentId)
        return parent != null && parent.id != authRepository.getCurrentUserId()
    }

    fun connectStudent(studentEmail: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val parentId = authRepository.getCurrentUserId()
                
                val student = userParentRepository.findStudentEmail(studentEmail)
                    ?: throw Exception("Siswa dengan email tersebut tidak ditemukan")

                if (isStudentConnectedToOtherParent(student.id)) {
                    _studentState.value =
                        StudentState.Error("Siswa ini sudah terhubung dengan orang tua lain")
                    return@launch
                }

                userParentRepository.connectStudentWithParent(student.id, parentId)
                loadStudents()
                _studentState.value = StudentState.SuccessMessage("Berhasil terhubung dengan siswa")
                
            } catch (e: Exception) {
                _studentState.value =
                    StudentState.Error(e.message ?: "Terjadi kesalahan saat menghubungkan siswa")
            }
        }
    }

    sealed class ParentState {
        data object Loading : ParentState()
        data class Success(val parent: User) : ParentState()
        data class Error(val message: String) : ParentState()
    }

    sealed class StudentState {
        data object Loading : StudentState()
        data class Success(val students: List<User>) : StudentState()
        data class Error(val message: String) : StudentState()
        data class SuccessMessage(val message: String) : StudentState()
    }

    sealed class StudentProgressState {
        data object Loading : StudentProgressState()
        data class Success(val learningProgressList: List<LearningProgress>) : StudentProgressState()
        data class Error(val message: String) : StudentProgressState()
    }
} 