package com.adika.learnable.viewmodel.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.UserParentRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userParentRepository: UserParentRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _parentState = MutableLiveData<ParentState>()
    val parentState: LiveData<ParentState> = _parentState

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    fun loadUserData() {
        viewModelScope.launch {
            _parentState.value = ParentState.Loading
            try {
                val userParentId = authRepository.getUserData(authRepository.getCurrentUserId())
                _parentState.value = ParentState.Success(userParentId)
            } catch (e: Exception) {
                _parentState.value =
                    ParentState.Error(e.message ?: resourceProvider.getString(R.string.fail_get_user_data))
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
                    StudentState.Error(e.message ?: resourceProvider.getString(R.string.fail_get_user_data))
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
                    ?: throw Exception(resourceProvider.getString(R.string.fail_find_student))

                if (isStudentConnectedToOtherParent(student.id)) {
                    _studentState.value =
                        StudentState.Error(resourceProvider.getString(R.string.student_have_been_conected))
                    return@launch
                }

                userParentRepository.connectStudentWithParent(student.id, parentId)
                loadStudents()
                _studentState.value = StudentState.SuccessMessage(resourceProvider.getString(R.string.sucess_connect_student))
                
            } catch (e: Exception) {
                _studentState.value =
                    StudentState.Error(e.message ?: resourceProvider.getString(R.string.fail_connect_student))
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
} 