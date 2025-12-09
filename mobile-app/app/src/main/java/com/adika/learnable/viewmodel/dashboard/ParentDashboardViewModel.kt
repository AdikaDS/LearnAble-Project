package com.adika.learnable.viewmodel.dashboard

import android.content.Context
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
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userParentRepository: UserParentRepository,
    private val resourceProvider: ResourceProvider,
    @ApplicationContext private val context: Context
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

    fun connectStudentsToParent(parentId: String, studentIds: List<String>, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                for (studentId in studentIds) {
                    userParentRepository.connectStudentWithParent(studentId, parentId)
                }
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message ?: context.getString(R.string.fail_connect_student))
            }
        }
    }

    fun searchStudents(query: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val students = userParentRepository.searchStudentByNameHybrid(query)
                _studentState.value = StudentState.Success(students)
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(
                    e.message ?: context.getString(R.string.fail_find_student)
                )
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