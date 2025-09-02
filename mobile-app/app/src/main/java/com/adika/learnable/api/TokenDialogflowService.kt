package com.adika.learnable.api

import com.adika.learnable.model.TokenResponse
import retrofit2.http.GET
import retrofit2.http.Header

interface TokenDialogflowService {
    @GET("get-dialogflow-token")
    suspend fun getToken(@Header("X-API-KEY") apiKey: String): TokenResponse
}