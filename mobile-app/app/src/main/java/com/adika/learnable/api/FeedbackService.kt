package com.adika.learnable.api

import com.adika.learnable.model.FeedbackReq
import com.adika.learnable.model.FeedbackResp
import retrofit2.http.Body
import retrofit2.http.POST

interface FeedbackService {
    @POST("exec")
    suspend fun submit(@Body body: FeedbackReq): FeedbackResp
}