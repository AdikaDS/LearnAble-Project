package com.adika.learnable.viewmodel.settings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.adika.learnable.repository.LanguageRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor(
    private val languageRepository: LanguageRepository
) : ViewModel() {

    private val _languageCode = MutableLiveData<String>()
    val languageCode: LiveData<String> get() = _languageCode

    init {
        loadCurrentLanguage()
    }

    private fun loadCurrentLanguage() {
        val code = languageRepository.getCurrentLanguageCode()
        _languageCode.value = code
    }

    fun setLanguage(code: String) {
        languageRepository.applyLanguage(code)
        _languageCode.value = code
    }
}