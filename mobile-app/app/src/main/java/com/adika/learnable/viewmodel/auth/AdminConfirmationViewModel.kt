package com.adika.learnable.viewmodel.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
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

    init {
        observeUserApprovalStatus()
    }

    private fun observeUserApprovalStatus() {
        viewModelScope.launch {
            _approvalState.value = ApprovalState.State(ApprovalState.Status.LOADING)
            authRepository.observeUserData(authRepository.getCurrentUserId())
                .map { user ->
                    userRole = user.role.toString()

                    when (user.role) {
                        "student" -> ApprovalState.State(ApprovalState.Status.APPROVED)
                        "teacher", "parent" -> {
                            if (user.isApproved) ApprovalState.State(ApprovalState.Status.APPROVED)
                            else ApprovalState.State(ApprovalState.Status.NOT_APPROVED)
                        }

                        else -> ApprovalState.State(
                            ApprovalState.Status.ERROR,
                            resourceProvider.getString(R.string.invalid_role)
                        )
                    }
                }
                .catch { e ->
                    _approvalState.value = ApprovalState.State(
                        ApprovalState.Status.ERROR,
                        e.message ?: resourceProvider.getString(R.string.unknown_error)
                    )
                }
                .collect { state ->
                    _approvalState.value = state
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
                    ApprovalState.State(
                        ApprovalState.Status.ERROR,
                        e.message ?: resourceProvider.getString(R.string.fail_logout)
                    )
            }
        }
    }

    sealed class ApprovalState {
        data class State(val status: Status, val message: String? = null) : ApprovalState()

        enum class Status {
            LOADING,
            APPROVED,
            NOT_APPROVED,
            ERROR
        }
    }

}