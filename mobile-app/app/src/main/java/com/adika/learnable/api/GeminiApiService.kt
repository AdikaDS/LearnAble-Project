package com.adika.learnable.api

import com.adika.learnable.model.GeminiResultResponse
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.Response

interface GeminiApiService {
    @GET("check-gemini-result")
    suspend fun checkGeminiResult (
        @Query("cache_key") cacheKey: String
    ) : Response<GeminiResultResponse>
}