package com.adika.learnable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adika.learnable.api.ApiConfig
import com.adika.learnable.model.TranscriptionResponse
import com.adika.learnable.util.AudioRecorder
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class RecordingViewModel(application: Application) : AndroidViewModel(application) {

    private val audioRecorder = AudioRecorder()
    private val _recordingState = MutableLiveData<RecordingState>(RecordingState.Idle)
    val recordingState: LiveData<RecordingState> = _recordingState

    private val _transcriptionResult = MutableLiveData<String?>()
    val transcriptionResult: LiveData<String?> = _transcriptionResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    fun startRecording() {
        val outputFile = File(getApplication<Application>().cacheDir, "recording.mp4")
        audioRecorder.start(outputFile)
        _recordingState.value = RecordingState.Recording
    }

    fun stopRecording() {
        val recordedFile = audioRecorder.stop()
        _recordingState.value = RecordingState.Idle
        recordedFile?.let { transcribeAudio(it) }
    }

    fun cancelRecording() {
        audioRecorder.cancel()
        _recordingState.value = RecordingState.Idle
    }

    private fun transcribeAudio(file: File) {
        _loading.value = true
        val requestFile = file.asRequestBody("audio/mp4".toMediaTypeOrNull())
        val audioPart = MultipartBody.Part.createFormData("file", file.name, requestFile)

        ApiConfig.transcribeService.transcribeAudio(audioPart)
            .enqueue(object : Callback<TranscriptionResponse> {
                override fun onResponse(
                    call: Call<TranscriptionResponse>,
                    response: Response<TranscriptionResponse>
                ) {
                    _loading.value = false
                    if (response.isSuccessful) {
                        _transcriptionResult.value = response.body()?.text
                    } else {
                        _transcriptionResult.value = "Error: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<TranscriptionResponse>, t: Throwable) {
                    _loading.value = false
                    _transcriptionResult.value = "Error: ${t.message}"
                }
            })
    }

    fun clearResult() {
        _transcriptionResult.value = null
    }
}

sealed class RecordingState {
    data object Idle : RecordingState()
    data object Recording : RecordingState()
    data object Transcribing : RecordingState()
}