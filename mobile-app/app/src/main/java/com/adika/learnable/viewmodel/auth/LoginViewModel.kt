package com.adika.learnable.viewmodel.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.util.ErrorMessages
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _googleSignInState = MutableLiveData<GoogleSignInState>()
    val googleSignInState: LiveData<GoogleSignInState> = _googleSignInState

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val user = authRepository.loginUser(email, password)
                
                if (!authRepository.isEmailVerified()) {
                    _loginState.value = LoginState.Error(ErrorMessages.getVerifyEmail(context))
                    authRepository.sendEmailVerification()
                    return@launch
                }
                _loginState.value = LoginState.Success(user)
                checkUserRole()
            } catch (e: Exception) {
                _loginState.value =
                    LoginState.Error(ErrorMessages.getFirebaseErrorMessage(context, e.message))
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _googleSignInState.value = GoogleSignInState.Loading
            try {
                val user = authRepository.signInWithGoogle(idToken)
                _googleSignInState.value = GoogleSignInState.Success(user)

                checkUserRole()
            } catch (e: Exception) {
                _googleSignInState.value = GoogleSignInState.Error(
                    ErrorMessages.getFirebaseErrorMessage(
                        context,
                        e.message
                    )
                )
            }
        }
    }

    private fun checkUserRole() {
        viewModelScope.launch {
            try {
                val user = authRepository.getUserData(authRepository.getCurrentUserId())
                when (user.role) {
                    "student" -> {
                        if (user.disabilityType == null) {
                            _loginState.value = LoginState.NavigateToDisabilitySelection
                            _googleSignInState.value =
                                GoogleSignInState.NavigateToDisabilitySelection
                        } else {
                            _loginState.value = LoginState.NavigateToStudentDashboard
                            _googleSignInState.value = GoogleSignInState.NavigateToStudentDashboard
                        }
                    }

                    "teacher" -> {
                        _loginState.value = LoginState.NavigateToTeacherDashboard
                        _googleSignInState.value = GoogleSignInState.NavigateToTeacherDashboard
                    }

                    "parent" -> {
                        _loginState.value = LoginState.NavigateToParentDashboard
                        _googleSignInState.value = GoogleSignInState.NavigateToParentDashboard
                    }

                    else -> {
                        _loginState.value = LoginState.Error("Role tidak valid")
                        _googleSignInState.value = GoogleSignInState.Error("Role tidak valid")
                    }
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error(e.message ?: "Gagal mendapatkan data user")
                _googleSignInState.value =
                    GoogleSignInState.Error(e.message ?: "Gagal mendapatkan data user")
            }
        }
    }

    sealed class LoginState {
        data object Loading : LoginState()
        data class Success(val user: User) : LoginState()
        data class Error(val message: String) : LoginState()
        data object NavigateToDisabilitySelection : LoginState()
        data object NavigateToStudentDashboard : LoginState()
        data object NavigateToTeacherDashboard : LoginState()
        data object NavigateToParentDashboard : LoginState()
    }

    sealed class GoogleSignInState {
        data object Loading : GoogleSignInState()
        data class Success(val user: User) : GoogleSignInState()
        data class Error(val message: String) : GoogleSignInState()
        data object NavigateToDisabilitySelection : GoogleSignInState()
        data object NavigateToStudentDashboard : GoogleSignInState()
        data object NavigateToTeacherDashboard : GoogleSignInState()
        data object NavigateToParentDashboard : GoogleSignInState()
    }

} 