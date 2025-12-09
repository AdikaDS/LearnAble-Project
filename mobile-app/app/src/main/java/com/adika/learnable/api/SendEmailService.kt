package com.adika.learnable.api

import com.adika.learnable.model.RegisterResponse
import com.adika.learnable.model.UserRegistrationRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface SendEmailService {
    @POST("email-admin-verification")
    suspend fun sendEmailToAdmin(
        @Body body: UserRegistrationRequest
    ): Response<RegisterResponse>
}