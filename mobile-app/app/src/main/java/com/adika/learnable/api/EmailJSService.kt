package com.adika.learnable.api

import com.adika.learnable.model.EmailJSRequest
import com.adika.learnable.model.EmailJSResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface EmailJSService {
    @Headers("Content-Type: application/json")
    @POST("api/v1.0/email/send")
    suspend fun sendEmail(@Body body: EmailJSRequest): Response<EmailJSResponse>
}
