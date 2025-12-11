package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.api.DialogflowService
import com.adika.learnable.api.GeminiApiService
import com.adika.learnable.api.TokenDialogflowService
import com.adika.learnable.model.DialogflowResponse
import com.adika.learnable.model.DialogflowRequest
import com.adika.learnable.model.GeminiResultResponse
import com.adika.learnable.model.TextInput
import com.adika.learnable.model.QueryInput
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatbotRepository @Inject constructor(
    private val dialogflowService: DialogflowService,
    private val tokenDialogflowService: TokenDialogflowService,
    private val geminiApiService: GeminiApiService
){
    suspend fun sendMessageToDialogflow(
        message: String,
        accessToken: String,
        projectId: String,
        sessionId: String
    ): DialogflowResponse? {
        try {
            val requestBody = DialogflowRequest(
                queryInput = QueryInput(
                    text = TextInput(
                        text = message,
                        languageCode = "id"
                    )
                )
            )
            val response = dialogflowService.detectIntent(
                projectId = projectId,
                sessionId = sessionId,
                body = requestBody,
                auth = "Bearer $accessToken"
            )
            if (response.isSuccessful) {
                val result = response.body()
                Log.d("ChatbotRepository", "Full response: $result")
                Log.d("ChatbotRepository", result?.queryResult?.fulfillmentText ?: "no message")
                return result
            } else {
                Log.e("ChatbotRepository", "Code: ${response.code()}, Error: ${response.errorBody()?.string() ?: "Unknown error"}")
            }
        } catch (e: Exception) {
            Log.e("ChatbotRepository", "Exception: ", e)
        }
        return null
    }

    suspend fun getDialogflowToken(apiKey: String = "rahasia123"): String {
        return tokenDialogflowService.getToken(apiKey).accessToken
    }

    suspend fun checkGeminiResult(cacheKey: String): GeminiResultResponse? {
        return try {
            val response = geminiApiService.checkGeminiResult(cacheKey)
            if (response.isSuccessful) {
                return response.body()
            } else {Log.e("ChatbotRepository", "Check result error: ${response.code()} ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("ChatbotRepository", "Check result exception", e)
            null
        }
    }
}