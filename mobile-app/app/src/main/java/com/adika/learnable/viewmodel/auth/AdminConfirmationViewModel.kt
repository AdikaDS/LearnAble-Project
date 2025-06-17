package com.adika.learnable.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminConfirmationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _approvalState = MutableLiveData<ApprovalState>()
    val approvalState: LiveData<ApprovalState> = _approvalState

    private var userRole: String = ""

    fun checkApprovalStatus() {
        viewModelScope.launch {
            _approvalState.value = ApprovalState.Loading
            try {
                val user = authRepository.getUserData(authRepository.getCurrentUserId())
                userRole = user.role

                when (user.role) {
                    "student" -> {
                        _approvalState.value = ApprovalState.Approved
                    }
                    "teacher", "parent" -> {
                        if (user.isApproved) {
                            _approvalState.value = ApprovalState.Approved
                        } else {
                            _approvalState.value = ApprovalState.NotApproved
                        }
                    }
                    else -> {
                        _approvalState.value = ApprovalState.Error(
                            resourceProvider.getString(R.string.invalid_role)
                        )
                    }
                }
            } catch (e: Exception) {
                _approvalState.value = ApprovalState.Error(
                    e.message ?: resourceProvider.getString(R.string.unknown_error)
                )
            }
        }
    }

    fun getUserRole(): String = userRole

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
            } catch (e: Exception) {
                _approvalState.value =
                    ApprovalState.Error(e.message ?: resourceProvider.getString(R.string.fail_logout))
            }
        }
    }

    sealed class ApprovalState {
        data object Loading : ApprovalState()
        data object Approved : ApprovalState()
        data object NotApproved : ApprovalState()
        data class Error(val message: String) : ApprovalState()
    }
} 