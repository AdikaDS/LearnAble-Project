package com.adika.learnable.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _resetState = MutableLiveData<ResetState>()
    val resetState: LiveData<ResetState> = _resetState

    fun resetPassword(email: String) {
        viewModelScope.launch {
            _resetState.value = ResetState.Loading
            try {
                authRepository.resetPassword(email)
                _resetState.value = ResetState.Success
            } catch (e: Exception) {
                _resetState.value =
                    ResetState.Error(e.message ?: "Gagal mengirim email reset password")
            }
        }
    }

    sealed class ResetState {
        data object Loading : ResetState()
        data object Success : ResetState()
        data class Error(val message: String) : ResetState()
    }
} 