package com.adika.learnable.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.EditProfileRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val editProfileRepository: EditProfileRepository,
    private val authRepository: AuthRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _userState = MutableLiveData<UserState>()
    val userState: LiveData<UserState> = _userState

    private val _uploadState = MutableLiveData<UploadState>()
    val uploadState: LiveData<UploadState> = _uploadState

    fun loadUserProfile() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                val user =
                    editProfileRepository.getUserData(editProfileRepository.getCurrentUserId())
                _userState.value = UserState.Success(user)
            } catch (e: Exception) {
                _userState.value = UserState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_load_profil)
                )
            }
        }
    }

    fun updateUserProfile(user: User) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                editProfileRepository.updateUserData(user)
                _userState.value = UserState.Success(user)
            } catch (e: Exception) {
                _userState.value = UserState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_update_profil)
                )
            }
        }
    }

    fun uploadProfilePicture(file: File) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                val userId = editProfileRepository.getCurrentUserId()
                val imageUrl = editProfileRepository.uploadProfilePicture(file, userId)
                val currentUser = editProfileRepository.getUserData(userId)
                val updatedUser = currentUser.copy(profilePicture = imageUrl)
                editProfileRepository.updateUserData(updatedUser)
                _uploadState.value = UploadState.Success(imageUrl)
                _userState.value = UserState.Success(updatedUser)
            } catch (e: IllegalArgumentException) {
                _uploadState.value = UploadState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_up_picture)
                )
            } catch (e: Exception) {
                _uploadState.value = UploadState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_up_picture)
                )
            }
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                editProfileRepository.updatePassword(currentPassword, newPassword)
                _userState.value = UserState.PasswordUpdated
            } catch (e: Exception) {
                _userState.value = UserState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_update_password)
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                authRepository.signOut()
                _userState.value = UserState.Success(null)
            } catch (e: Exception) {
                _userState.value =
                    UserState.Error(e.message ?: resourceProvider.getString(R.string.fail_logout))
            }
        }
    }

    sealed class UserState {
        data object Loading : UserState()
        data class Success(val user: User?) : UserState()
        data class Error(val message: String) : UserState()
        data object PasswordUpdated : UserState()
    }

    sealed class UploadState {
        data object Loading : UploadState()
        data class Success(val imageUrl: String) : UploadState()
        data class Error(val message: String) : UploadState()
    }
} 