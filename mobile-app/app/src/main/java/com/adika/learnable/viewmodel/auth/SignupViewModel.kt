package com.adika.learnable.viewmodel.auth

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.UserParentRepository
import com.adika.learnable.util.ErrorMessages
import com.adika.learnable.util.GoogleSignInResult
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SignupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val userParentRepository: UserParentRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _signupState = MutableLiveData<SignupState>()
    val signupState: LiveData<SignupState> = _signupState

    private val _googleSignUpState = MutableLiveData<GoogleSignUpState>()
    val googleSignUpState: LiveData<GoogleSignUpState> = _googleSignUpState

    private val _studentSearchState = MutableLiveData<StudentSearchState>()
    val studentSearchState: LiveData<StudentSearchState> = _studentSearchState

    fun signUpWithEmail(
        name: String,
        email: String,
        password: String,
        role: String,
        nomorInduk: String? = null
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
                        nomorInduk
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

    fun searchStudents(query: String) {
        viewModelScope.launch {
            _studentSearchState.value = StudentSearchState.Loading
            try {
                val students = userParentRepository.searchStudentByNameHybrid(query)
                _studentSearchState.value = StudentSearchState.Success(students)
            } catch (e: Exception) {
                _studentSearchState.value = StudentSearchState.Error(
                    e.message ?: context.getString(R.string.fail_find_student)
                )
            }
        }
    }

    fun connectStudentsToParent(parentId: String, studentIds: List<String>, onComplete: (Boolean, String?) -> Unit) {
        viewModelScope.launch {
            try {
                // Check if any students are already connected
                val unavailableStudents = userParentRepository.checkStudentsAvailability(studentIds)
                if (unavailableStudents.isNotEmpty()) {
                    val message = "Siswa berikut sudah terhubung dengan orangtua lain: ${unavailableStudents.joinToString(", ")}"
                    onComplete(false, message)
                    return@launch
                }

                for (studentId in studentIds) {
                    userParentRepository.connectStudentWithParent(studentId, parentId)
                }
                onComplete(true, null)
            } catch (e: Exception) {
                onComplete(false, e.message ?: context.getString(R.string.fail_connect_student))
            }
        }
    }

    fun completeAdditionalData(uid: String, role: String, nomorInduk: String?) {
        viewModelScope.launch {
            _googleSignUpState.value = GoogleSignUpState.Loading
            try {
                val user = authRepository.completeAdditionalData(uid, role, nomorInduk)
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

                    "parent" -> {
                        if (user.isApproved) {
                            _googleSignUpState.value = GoogleSignUpState.NavigateToParentDashboard
                        } else {
                            _googleSignUpState.value = GoogleSignUpState.NavigateToAdminConfirmation
                        }
                    }

                    else -> {
                        _googleSignUpState.value = GoogleSignUpState.Error("Role tidak valid: ${user.role}")
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
        data object NavigateToParentDashboard : GoogleSignUpState()
        data object NavigateToAdminConfirmation : GoogleSignUpState()
    }

    sealed class StudentSearchState {
        data object Loading : StudentSearchState()
        data class Success(val students: List<User>) : StudentSearchState()
        data class Error(val message: String) : StudentSearchState()
    }
} 