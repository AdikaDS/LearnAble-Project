package com.adika.learnable.api

import com.adika.learnable.model.ImgurResponse
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ImgurService {
    @Multipart
    @POST("image")
    suspend fun uploadImage(
        @Header("Authorization") auth: String,
        @Part image: MultipartBody.Part
    ): Response<ImgurResponse>
}