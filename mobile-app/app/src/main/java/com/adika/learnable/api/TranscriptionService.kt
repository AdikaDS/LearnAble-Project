package com.adika.learnable.api

import com.adika.learnable.model.EvaluateRequest
import com.adika.learnable.model.EvaluateResponse
import com.adika.learnable.model.TranscribeEvaluateResponse
import com.adika.learnable.model.TranscriptionResponse
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TranscriptionService {
    @Multipart
    @POST("transcribe")
    suspend fun transcribeAudio(
        @Part audio: MultipartBody.Part
    ): Response<TranscriptionResponse>

    @Multipart
    @POST("transcribe-and-evaluate")
    suspend fun transcribeAndEvaluate(
        @Part file: MultipartBody.Part,
        @Part("question") question: RequestBody,
        @Part("options") options: List<RequestBody>
    ): Response<TranscribeEvaluateResponse>

    @POST("evaluate")
    suspend fun evaluateAnswer(@Body request: EvaluateRequest): Response<EvaluateResponse>
}