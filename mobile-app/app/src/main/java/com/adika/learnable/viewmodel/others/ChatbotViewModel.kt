package com.adika.learnable.viewmodel.others

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.R
import com.adika.learnable.model.DialogflowResponse
import com.adika.learnable.model.GeminiResultResponse
import com.adika.learnable.repository.ChatbotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val repository: ChatbotRepository,
    @ApplicationContext private val ctx: Context
) : ViewModel() {
    private val _token = MutableLiveData<String?>()
    val token: LiveData<String?> = _token

    private val _state = MutableLiveData<ChatBotState>()
    val state: LiveData<ChatBotState> = _state

    private var pollingJob: Job? = null

    fun fetchToken(apiKey: String = "rahasia123") {
        viewModelScope.launch {
            try {
                _state.value = ChatBotState.Loading
                val token = repository.getDialogflowToken(apiKey)
                _token.value = token
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true ->
                        ctx.getString(R.string.timeout_ai)

                    e.message?.contains("network", ignoreCase = true) == true ->
                        ctx.getString(R.string.failed_connect_ai_server)

                    else -> ctx.getString(R.string.failed_get_token, e.localizedMessage)

                }
                _state.value = ChatBotState.Error(errorMessage)
            }
        }
    }

    fun sendMessage(
        message: String,
        projectId: String,
        sessionId: String
    ) {
        val accessToken = _token.value
        if (accessToken.isNullOrBlank()) {
            _state.value = ChatBotState.Error(ctx.getString(R.string.token_not_ready))
            return
        }
        viewModelScope.launch {
            _state.value = ChatBotState.Loading
            val result =
                repository.sendMessageToDialogflow(message, accessToken, projectId, sessionId)

            if (result == null) {
                _state.value =
                    ChatBotState.Error(ctx.getString(R.string.failed_connect_ai_server))
            } else {
                _state.value = ChatBotState.SuccessDialogflow(result)
            }
        }

    }

    fun startPolling(cacheKey: String) {
        pollingJob?.cancel()
        _state.value = ChatBotState.Loading
        pollingJob = viewModelScope.launch {
            var attempts = 0
            while (attempts < MAX_ATTEMPTS) {
                delay(POLLING_INTERVAL_MS)
                val response = repository.checkGeminiResult(cacheKey)
                Log.d("Polling", "Polling attempt $attempts: status=${response?.status}")
                if (response?.status == "ready") {
                    _state.value = ChatBotState.SuccessGemini(response)
                    Log.d("Polling", "Gemini result status: ${response.status}")
                    return@launch
                }

                attempts++
            }
            _state.value =
                ChatBotState.Timeout(ctx.getString(R.string.answer_ai_not_reay_until, MAX_ATTEMPTS * POLLING_INTERVAL_MS / 1000))
        }
    }

    fun clearPolling() {
        pollingJob?.cancel()
        _state.value = ChatBotState.Idle
    }

    companion object {
        private const val MAX_ATTEMPTS = 5
        private const val POLLING_INTERVAL_MS = 3000L
    }

    sealed class ChatBotState {
        data object Loading : ChatBotState()
        data object Idle : ChatBotState()
        data class Timeout(val message: String = "Jawaban belum tersedia. Silakan coba lagi.") :
            ChatBotState()

        data class SuccessGemini(
            val response: GeminiResultResponse?,
            val uniqueId: Long = System.currentTimeMillis()
        ) : ChatBotState()

        data class SuccessDialogflow(val response: DialogflowResponse?) : ChatBotState()
        data class Error(val message: String) : ChatBotState()
    }
}