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
class ResetPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    // State untuk langkah verifikasi oobCode → dapat email
    private val _verifyState = MutableLiveData<VerifyState>()
    val verifyState: LiveData<VerifyState> = _verifyState

    // State untuk langkah konfirmasi password baru
    private val _confirmState = MutableLiveData<ConfirmState>()
    val confirmState: LiveData<ConfirmState> = _confirmState

    fun verifyResetCode(oobCode: String) {
        viewModelScope.launch {
            _verifyState.value = VerifyState.Loading
            try {
                val email = authRepository.verifyResetCode(oobCode)
                _verifyState.value = VerifyState.Success(email)
            } catch (e: Exception) {
                _verifyState.value = VerifyState.Error(
                    e.message ?: "Kode reset tidak valid atau kadaluarsa"
                )
            }
        }
    }

    fun confirmPassword(oobCode: String, newPassword: String) {
        viewModelScope.launch {
            _confirmState.value = ConfirmState.Loading
            try {
                authRepository.confirmPasswordReset(oobCode, newPassword)
                _confirmState.value = ConfirmState.Success
            } catch (e: Exception) {
                _confirmState.value = ConfirmState.Error(
                    e.message ?: "Gagal mengubah password"
                )
            }
        }
    }

    sealed class VerifyState {
        data object Loading : VerifyState()
        data class Success(val email: String) : VerifyState()
        data class Error(val message: String) : VerifyState()
    }

    sealed class ConfirmState {
        data object Loading : ConfirmState()
        data object Success : ConfirmState()
        data class Error(val message: String) : ConfirmState()
    }

}
