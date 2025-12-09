package com.adika.learnable.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    companion object {
        private const val STUDENT = "student"
        private const val TEACHER = "teacher"
        private const val PARENT = "parent"
        private const val STUDENT_DASHBOARD = "student_dashboard"
        private const val PARENT_DASHBOARD = "parent_dashboard"
        private const val TEACHER_DASHBOARD = "teacher_dashboard"
        private const val ADMIN_CONFIRMATION = "admin_confirmation"
        private const val LOGIN = "login"
    }

    private val _navigationEvent = MutableLiveData<String>()
    val navigationEvent: LiveData<String> get() = _navigationEvent

    fun checkUserStatus() {
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val user = authRepository.getUserData(userId)

                when (user.role) {
                    STUDENT -> {
                        _navigationEvent.value = STUDENT_DASHBOARD
                    }

                    TEACHER -> {
                        if (user.isApproved) {
                            _navigationEvent.value = TEACHER_DASHBOARD
                        } else {
                            _navigationEvent.value = ADMIN_CONFIRMATION
                        }
                    }

                    PARENT -> {
                        if (user.isApproved) {
                            _navigationEvent.value = PARENT_DASHBOARD
                        } else {
                            _navigationEvent.value = ADMIN_CONFIRMATION
                        }
                    }

                    else -> _navigationEvent.value = LOGIN
                }
            } catch (e: Exception) {
                _navigationEvent.value = LOGIN
            }
        }
    }
}
