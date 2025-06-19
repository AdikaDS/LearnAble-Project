package com.adika.learnable.viewmodel.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
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
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val KEY_ROLE = "user_role"
    }

    private val _loginState = MutableLiveData<LoginState>()
    val loginState: LiveData<LoginState> = _loginState

    private val _googleSignInState = MutableLiveData<GoogleSignInState>()
    val googleSignInState: LiveData<GoogleSignInState> = _googleSignInState

    private val _navigationState = MutableLiveData<NavigationState>()
    val navigationState: LiveData<NavigationState> = _navigationState

    val role: String?
        get() = savedStateHandle[KEY_ROLE]

    fun setRole(role: String) {
        savedStateHandle[KEY_ROLE] = role
    }

    private var currentGoogleToken: String? = null

    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            try {
                val user = authRepository.loginUser(email, password)

                if (!authRepository.isEmailVerified()) {
                    try {
                        authRepository.sendEmailVerification()
                        _loginState.value =
                            LoginState.Error(ErrorMessages.getVerifyEmailSent(context))
                    } catch (e: Exception) {
                        _loginState.value = LoginState.Error(ErrorMessages.getVerifyEmail(context))
                    }
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

    fun checkUserExists(idToken: String) {
        viewModelScope.launch {
            _googleSignInState.value = GoogleSignInState.Loading
            try {
                currentGoogleToken = idToken
                val userId = authRepository.getFirebaseUserIdFromToken(idToken)
                val user = authRepository.getUserData(userId)
                
                // User exists and has a role, proceed with sign in
                signInWithGoogle(idToken, user.role)
            } catch (e: Exception) {
                // User doesn't exist or error occurred, show role selection
                _googleSignInState.value = GoogleSignInState.ShowRoleSelection
            }
        }
    }

    private fun signInWithGoogle(idToken: String, role: String) {
        viewModelScope.launch {
            _googleSignInState.value = GoogleSignInState.Loading
            try {
                val user = authRepository.signInWithGoogle(idToken, role)
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

    fun signInWithStoredToken(role: String) {
        currentGoogleToken?.let { token ->
            signInWithGoogle(token, role)
        }
    }

    private fun checkUserRole() {
        viewModelScope.launch {
            try {
                val user = authRepository.getUserData(authRepository.getCurrentUserId())
                when (user.role) {
                    "student" -> {
                        if (user.disabilityType == null) {
                            _navigationState.value = NavigationState.NavigateToDisabilitySelection
                        } else {
                            _navigationState.value = NavigationState.NavigateToStudentDashboard
                        }
                    }

                    "teacher" -> {
                        if (user.isApproved) {
                            _navigationState.value = NavigationState.NavigateToTeacherDashboard
                        } else {
                            _navigationState.value = NavigationState.NavigateToAdminConfirmation
                        }
                    }

                    "parent" -> {
                        if (user.isApproved) {
                            _navigationState.value = NavigationState.NavigateToParentDashboard
                        } else {
                            _navigationState.value = NavigationState.NavigateToAdminConfirmation
                        }
                    }
                    else -> {
                        _googleSignInState.value = GoogleSignInState.ShowRoleSelection
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
        data class Error(val message: String) : GoogleSignInState()
        data object ShowRoleSelection : GoogleSignInState()
    }

    sealed class NavigationState {
        data object NavigateToDisabilitySelection : NavigationState()
        data object NavigateToStudentDashboard : NavigationState()
        data object NavigateToTeacherDashboard : NavigationState()
        data object NavigateToParentDashboard : NavigationState()
        data object NavigateToAdminConfirmation : NavigationState()
    }

} 