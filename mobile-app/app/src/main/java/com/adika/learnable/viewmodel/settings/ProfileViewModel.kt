package com.adika.learnable.viewmodel.settings

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.ProvinceDto
import com.adika.learnable.model.RegencyDto
import com.adika.learnable.model.User
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.ProfileRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val authRepository: AuthRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _userState = MutableLiveData<UserState>()
    val userState: LiveData<UserState> = _userState

    private val _uploadState = MutableLiveData<UploadState>()
    val uploadState: LiveData<UploadState> = _uploadState

    private val _provinces = MutableLiveData<List<ProvinceDto>>()
    val provinces: LiveData<List<ProvinceDto>> = _provinces

    private val _regencies = MutableLiveData<List<RegencyDto>>()
    val regencies: LiveData<List<RegencyDto>> = _regencies

    fun loadUserProfile() {
        viewModelScope.launch {
            _userState.value = UserState.Loading
            try {
                val user =
                    profileRepository.getUserData(profileRepository.getCurrentUserId())
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
                profileRepository.updateUserData(user)
                _userState.value = UserState.Success(user)
            } catch (e: Exception) {
                _userState.value = UserState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_update_profil)
                )
            }
        }
    }

    fun loadProvinces() {
        viewModelScope.launch {
            try {
                val provinces = profileRepository.fetchProvinces()
                _provinces.value = provinces
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading regencies", e)
                _provinces.value = emptyList()
            }
        }
    }

    fun loadRegencies(provinceId: String) {
        viewModelScope.launch {
            try {
                val regencies = profileRepository.fetchRegencies(provinceId)
                _regencies.value = regencies
            } catch (e: Exception) {
                Log.e("ProfileViewModel", "Error loading regencies", e)
                _regencies.value = emptyList()
            }
        }
    }

    fun uploadProfilePicture(file: File) {
        viewModelScope.launch {
            _uploadState.value = UploadState.Loading
            try {
                val userId = profileRepository.getCurrentUserId()
                val imageUrl = profileRepository.uploadProfilePicture(file, userId)
                val currentUser = profileRepository.getUserData(userId)
                val updatedUser = currentUser.copy(profilePicture = imageUrl)
                profileRepository.updateUserData(updatedUser)
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
                profileRepository.updatePassword(currentPassword, newPassword)
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