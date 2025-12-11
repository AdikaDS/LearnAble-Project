package com.adika.learnable.viewmodel.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.util.ErrorMessages
import com.adika.learnable.util.GoogleSignInResult
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

    private val _navigationState = MutableLiveData<NavigationState>()
    val navigationState: LiveData<NavigationState> = _navigationState

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val user = authRepository.loginUser(email, password)

                authRepository.checkAndNotifyAfterEmailVerification()

                if (!authRepository.isEmailVerified()) {
                    try {
                        authRepository.sendEmailVerification()
                        _loginState.value =
                            LoginState.Error(ErrorMessages.getVerifyEmailSent(context))
                    } catch (e: Exception) {
                        _loginState.value = LoginState.Error(ErrorMessages.getVerifyEmail(context))
                    }
                    authRepository.signOut()
                    return@launch
                }
                _loginState.value = LoginState.Success(user)
                checkUserRole()
            } catch (e: Exception) {
                _loginState.value =
                    LoginState.Error(ErrorMessages.getFirebaseErrorMessage(context, e))
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _googleSignInState.value = GoogleSignInState.Loading
            try {
                when (val result = authRepository.signInWithGoogle(idToken)) {
                    is GoogleSignInResult.NeedsMoreData -> {
                        _googleSignInState.value = GoogleSignInState.NeedMoreData(
                            result.user,
                            result.requiredFields
                        )
                    }

                    is GoogleSignInResult.Success -> {
                        _googleSignInState.value = GoogleSignInState.Success(result.user)

                        checkUserRole()
                    }
                }
            } catch (e: Exception) {
                _googleSignInState.value = GoogleSignInState.Error(
                    ErrorMessages.getFirebaseErrorMessage(
                        context,
                        e
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
                    "admin" -> {
                        _navigationState.value = NavigationState.NavigateToAdminDashboard
                    }

                    "student" -> {
                        _navigationState.value = NavigationState.NavigateToStudentDashboard
                    }

                    "teacher" -> {
                        if (user.isApproved) {
                            _navigationState.value = NavigationState.NavigateToTeacherDashboard
                        } else {
                            _navigationState.value = NavigationState.NavigateToAdminConfirmation
                        }
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
    }

    sealed class GoogleSignInState {
        data object Loading : GoogleSignInState()
        data class Success(val user: User) : GoogleSignInState()
        data class NeedMoreData(val user: User, val required: List<String>) : GoogleSignInState()
        data class Error(val message: String) : GoogleSignInState()
    }

    sealed class NavigationState {
        data object NavigateToAdminDashboard : NavigationState()
        data object NavigateToStudentDashboard : NavigationState()
        data object NavigateToTeacherDashboard : NavigationState()
        data object NavigateToAdminConfirmation : NavigationState()
    }
} 