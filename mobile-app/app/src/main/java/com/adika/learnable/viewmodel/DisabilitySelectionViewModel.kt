package com.adika.learnable.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.User
import com.adika.learnable.repository.UserStudentRepository
import com.adika.learnable.util.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisabilitySelectionViewModel @Inject constructor(
    private val userStudentRepository: UserStudentRepository,
    private val resourceProvider: ResourceProvider
) : ViewModel() {

    private val _uiState = MutableLiveData<DisabilitySelectionState>()
    val uiState: LiveData<DisabilitySelectionState> = _uiState

    fun saveDisabilityType(disabilityType: String) {
        viewModelScope.launch {
            _uiState.value = DisabilitySelectionState.Loading
            try {
                val user = userStudentRepository.updateDisabilityType(disabilityType)
                _uiState.value = DisabilitySelectionState.Success(user)
            } catch (e: Exception) {
                _uiState.value = DisabilitySelectionState.Error(
                    e.message ?: resourceProvider.getString(R.string.fail_save_disability_type)
                )
            }
        }
    }

    sealed class DisabilitySelectionState {
        data object Loading : DisabilitySelectionState()
        data class Success(val user: User) : DisabilitySelectionState()
        data class Error(val message: String) : DisabilitySelectionState()
    }
}