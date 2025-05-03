package com.adika.learnable.api

import com.adika.learnable.model.TranscriptionResponse
import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface TrancribeService {
    @Multipart
    @POST("transcribe")
    fun transcribeAudio(
        @Part audio: MultipartBody.Part
    ): Call<TranscriptionResponse>
}