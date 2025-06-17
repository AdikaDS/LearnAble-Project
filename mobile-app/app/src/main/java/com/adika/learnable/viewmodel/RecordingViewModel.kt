package com.adika.learnable.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.repository.TranscriptionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

@HiltViewModel
class RecordingViewModel @Inject constructor(
    private val transcriptionRepository: TranscriptionRepository
) : ViewModel() {

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _transcriptionResult = MutableStateFlow<String?>(null)
    val transcriptionResult: StateFlow<String?> = _transcriptionResult.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    fun startRecording() {
        viewModelScope.launch {
            transcriptionRepository.startRecording().collect {
                _recordingState.value = RecordingState.Recording
            }
        }
    }

    fun stopRecording() {
        viewModelScope.launch {
            transcriptionRepository.stopRecording().collect { file ->
                _recordingState.value = RecordingState.Idle
                file?.let { transcribeAudio(it) }
            }
        }
    }

    fun cancelRecording() {
        viewModelScope.launch {
            transcriptionRepository.cancelRecording().collect {
                _recordingState.value = RecordingState.Idle
            }
        }
    }

    private fun transcribeAudio(file: File) {
        _recordingState.value = RecordingState.Transcribing
        viewModelScope.launch {
            transcriptionRepository.transcribeAudio(file).collect { result ->
                result.fold(
                    onSuccess = { text ->
                        _transcriptionResult.value = text
                    },
                    onFailure = { error ->
                        _transcriptionResult.value = "Error: ${error.message}"
                    }
                )
                _recordingState.value = RecordingState.Idle
            }
        }
    }
}

sealed class RecordingState {
    data object Idle : RecordingState()
    data object Recording : RecordingState()
    data object Transcribing : RecordingState()
}