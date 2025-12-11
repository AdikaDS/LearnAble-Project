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
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _signupState = MutableLiveData<SignupState>()
    val signupState: LiveData<SignupState> = _signupState

    private val _googleSignUpState = MutableLiveData<GoogleSignUpState>()
    val googleSignUpState: LiveData<GoogleSignUpState> = _googleSignUpState

    fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        role: String,
        idNumber: String? = null
    ) {
        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            try {
                val result =
                    authRepository.signUpWithEmailAndPassword(
                        name,
                        email,
                        password,
                        role,
                        idNumber
                    )
                _signupState.value = SignupState.Success(result)
            } catch (e: Exception) {
                _signupState.value =
                    SignupState.Error(ErrorMessages.getFirebaseErrorMessage(context, e))
            }
        }
    }

    fun signUpWithGoogle(idToken: String) {
        viewModelScope.launch {
            _googleSignUpState.value = GoogleSignUpState.Loading
            try {
                when (val result = authRepository.signInWithGoogle(idToken)) {
                    is GoogleSignInResult.NeedsMoreData -> {
                        _googleSignUpState.value = GoogleSignUpState.NeedMoreData(
                            result.user,
                            result.requiredFields
                        )
                    }

                    is GoogleSignInResult.Success -> {
                        _googleSignUpState.value = GoogleSignUpState.Success(result.user)
                    }
                }

                checkUserRole()
            } catch (e: Exception) {
                _googleSignUpState.value = GoogleSignUpState.Error(
                    ErrorMessages.getFirebaseErrorMessage(
                        context,
                        e
                    )
                )
            }
        }
    }

    fun completeAdditionalData(uid: String, role: String, idNumber: String?) {
        viewModelScope.launch {
            _googleSignUpState.value = GoogleSignUpState.Loading
            try {
                val user = authRepository.completeAdditionalData(uid, role, idNumber)
                _googleSignUpState.value = GoogleSignUpState.Success(user)
                checkUserRole()
            } catch (e: Exception) {
                _googleSignUpState.value =
                    GoogleSignUpState.Error(ErrorMessages.getFirebaseErrorMessage(context, e))
            }
        }
    }

    private fun checkUserRole() {
        viewModelScope.launch {
            try {
                val user = authRepository.getUserData(authRepository.getCurrentUserId())
                when (user.role?.lowercase()) {
                    "student" -> {
                        _googleSignUpState.value = GoogleSignUpState.NavigateToStudentDashboard
                    }

                    "teacher" -> {
                        if (user.isApproved) {
                            _googleSignUpState.value = GoogleSignUpState.NavigateToTeacherDashboard
                        } else {
                            _googleSignUpState.value = GoogleSignUpState.NavigateToAdminConfirmation
                        }
                    }

                    else -> {
                        _googleSignUpState.value =
                            GoogleSignUpState.Error("Role tidak valid: ${user.role}")
                    }
                }
            } catch (e: Exception) {
                _googleSignUpState.value =
                    GoogleSignUpState.Error(e.message ?: "Gagal mendapatkan data user")
            }
        }
    }

    sealed class SignupState {
        data object Loading : SignupState()
        data class Success(val user: User) : SignupState()
        data class Error(val message: String) : SignupState()
    }

    sealed class GoogleSignUpState {
        data object Loading : GoogleSignUpState()
        data class Success(val user: User) : GoogleSignUpState()
        data class NeedMoreData(val user: User, val required: List<String>) : GoogleSignUpState()
        data class Error(val message: String) : GoogleSignUpState()

        data object NavigateToStudentDashboard : GoogleSignUpState()
        data object NavigateToTeacherDashboard : GoogleSignUpState()
        data object NavigateToAdminConfirmation : GoogleSignUpState()
    }
} 