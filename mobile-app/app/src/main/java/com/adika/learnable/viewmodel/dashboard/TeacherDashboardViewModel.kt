package com.adika.learnable.viewmodel.dashboard

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.UserTeacherRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TeacherDashboardViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userTeacherRepository: UserTeacherRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _teacherState = MutableLiveData<TeacherState>()
    val teacherState: LiveData<TeacherState> = _teacherState

    private val _studentState = MutableLiveData<StudentState>()
    val studentState: LiveData<StudentState> = _studentState

    fun loadUserData() {
        viewModelScope.launch {
            _teacherState.value = TeacherState.Loading
            try {
                val userParentId = authRepository.getUserData(authRepository.getCurrentUserId())
                _teacherState.value = TeacherState.Success(userParentId)
            } catch (e: Exception) {
                _teacherState.value =
                    TeacherState.Error(e.message ?: resourceProvider.getString(R.string.fail_get_user_data))
            }
        }
    }

    fun loadAllStudentData() {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val allStudentUser = userTeacherRepository.getAllStudent()
                _studentState.value = StudentState.Success(allStudentUser)
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(e.message ?: resourceProvider.getString(R.string.fail_get_user_data))
            }
        }
    }

    fun searchStudent(query: String) {
        viewModelScope.launch {
            _studentState.value = StudentState.Loading
            try {
                val lessons = userTeacherRepository.searchStudent(query)
                _studentState.value = StudentState.Success(lessons)
            } catch (e: Exception) {
                _studentState.value = StudentState.Error(e.message ?: resourceProvider.getString(R.string.fail_find_student))
            }
        }
    }


    sealed class StudentState {
        data object Loading : StudentState()
        data class Success(val students: List<User>) : StudentState()
        data class Error(val message: String) : StudentState()
    }

    sealed class TeacherState {
        data object Loading : TeacherState()
        data class Success(val teacher: User) : TeacherState()
        data class Error(val message: String) : TeacherState()
    }


} 