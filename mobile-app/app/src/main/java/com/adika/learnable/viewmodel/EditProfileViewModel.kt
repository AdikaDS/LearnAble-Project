package com.adika.learnable.viewmodel

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.EditProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val editProfileRepository: EditProfileRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _userState = MutableLiveData<UserState>()
    val userState: LiveData<UserState> = _userState

    private val _uploadState = MutableLiveData<UploadState>()
    val uploadState: LiveData<UploadState> = _uploadState

    fun loadUserProfile() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                val user = editProfileRepository.getUserData(editProfileRepository.getCurrentUserId())
                _userState.value = UserState.Success(user)
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Terjadi kesalahan saat memuat profil")
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
                _userState.value = UserState.Error(e.message ?: "Terjadi kesalahan saat memperbarui profil")
            }
        }
    }

    fun uploadProfilePicture(uri: Uri) {
        _uploadState.value = UploadState.Loading
        editProfileRepository.uploadToImgur(uri) { result ->
            result.fold(
                onSuccess = { imageUrl ->
                    // Update profil user dengan URL gambar baru
                    viewModelScope.launch {
                        try {
                            val currentUser = editProfileRepository.getUserData(editProfileRepository.getCurrentUserId())
                            val updatedUser = currentUser.copy(profilePicture = imageUrl)
                            editProfileRepository.updateUserData(updatedUser)
                            _uploadState.value = UploadState.Success(imageUrl)
                            _userState.value = UserState.Success(updatedUser)
                        } catch (e: Exception) {
                            _uploadState.value = UploadState.Error(e.message ?: "Gagal memperbarui profil")
                        }
                    }
                },
                onFailure = { error ->
                    _uploadState.value = UploadState.Error(error.message ?: "Gagal upload gambar")
                }
            )
        }
    }

    fun updatePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                editProfileRepository.updatePassword(currentPassword, newPassword)
                _userState.value = UserState.PasswordUpdated
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Terjadi kesalahan saat memperbarui password")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _userState.value = UserState.Loading
                authRepository.signOut()
                _userState.value = UserState.Success(null)
            } catch (e: Exception) {
                _userState.value = UserState.Error(e.message ?: "Terjadi kesalahan saat logout")
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