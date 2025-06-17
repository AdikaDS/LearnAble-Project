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
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val savedStateHandle: SavedStateHandle,
    @ApplicationContext private val context: Context
) : ViewModel() {

    companion object {
        private const val KEY_ROLE = "user_role"
    }

    private val _signupState = MutableLiveData<SignupState>()
    val signupState: LiveData<SignupState> = _signupState

    private val _googleSignUpState = MutableLiveData<GoogleSignUpState>()
    val googleSignUpState: LiveData<GoogleSignUpState> = _googleSignUpState

    val role: String?
        get() = savedStateHandle[KEY_ROLE]

    fun setRole(role: String) {
        savedStateHandle[KEY_ROLE] = role
    }

    fun signUpWithEmail(name: String, email: String, password: String, ttl: String, role: String) {
        viewModelScope.launch {
            _signupState.value = SignupState.Loading
            try {
                val result = authRepository.signUpWithEmailAndPassword(name, email, password, ttl, role)
                _signupState.value = SignupState.Success(result)
            } catch (e: Exception) {
                _signupState.value =
                    SignupState.Error(ErrorMessages.getFirebaseErrorMessage(context, e.message))
            }
        }
    }

    fun signUpWithGoogle(idToken: String, role: String) {
        viewModelScope.launch {
            _googleSignUpState.value = GoogleSignUpState.Loading
            try {
                val result = authRepository.signInWithGoogle(idToken, role)
                _googleSignUpState.value = GoogleSignUpState.Success(result)
            } catch (e: Exception) {
                _googleSignUpState.value = GoogleSignUpState.Error(
                    ErrorMessages.getFirebaseErrorMessage(
                        context,
                        e.message
                    )
                )
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
        data class Error(val message: String) : GoogleSignUpState()
    }
} 