package com.adika.learnable.api

import com.adika.learnable.model.ResendEmailRequest
import com.adika.learnable.model.ResendEmailResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface ResendEmailService {
    @POST("emails")
    suspend fun sendEmail(
        @Header("Authorization") auth: String,
        @Body request: ResendEmailRequest
    ): Response<ResendEmailResponse>
}

