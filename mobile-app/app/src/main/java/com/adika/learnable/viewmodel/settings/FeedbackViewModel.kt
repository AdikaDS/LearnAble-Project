package com.adika.learnable.viewmodel.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.FeedbackReq
import com.adika.learnable.repository.AuthRepository
import com.adika.learnable.repository.FeedbackRepository
import com.adika.learnable.util.NormalizeFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val repository: FeedbackRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _isSubmitting = MutableLiveData(false)
    val isSubmitting: LiveData<Boolean> = _isSubmitting

    private val _submitSuccess = MutableLiveData<Boolean?>(null)
    val submitSuccess: LiveData<Boolean?> = _submitSuccess

    private val _errorMessage = MutableLiveData<String?>(null)
    val errorMessage: LiveData<String?> = _errorMessage

    fun submit(
        subject: String,
        category: String,
        message: String,
        rating: Int?,
        appVersion: String,
        device: String
    ) {
        _isSubmitting.value = true
        _errorMessage.value = null
        viewModelScope.launch {
            try {
                val userId = authRepository.getCurrentUserId()
                val user = authRepository.getUserData(userId)

                val role = user.role?.let { NormalizeFirestore.unormalizeRoleFeedback(it) }
                val req = FeedbackReq(
                    name = user.name,
                    email = user.email,
                    role = role ?: "unknown",
                    subject = subject.trim(),
                    category = category.trim(),
                    message = message.trim(),
                    rating = rating,
                    appVersion = appVersion,
                    device = device
                )
                val resp = repository.submitFeedback(req)
                _isSubmitting.value = false
                if (resp.ok) {
                    _submitSuccess.value = true
                } else {
                    _submitSuccess.value = false
                    _errorMessage.value = resp.error ?: "Gagal mengirim feedback"
                }
            } catch (e: Exception) {
                _isSubmitting.value = false
                _submitSuccess.value = false
                _errorMessage.value = e.localizedMessage ?: "Gagal mengambil data pengguna"
            }
        }
    }

    fun resetState() {
        _submitSuccess.value = null
        _errorMessage.value = null
        _isSubmitting.value = false
    }
}