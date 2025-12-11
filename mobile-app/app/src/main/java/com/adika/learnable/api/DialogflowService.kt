package com.adika.learnable.api

import com.adika.learnable.model.DialogflowRequest
import com.adika.learnable.model.DialogflowResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface DialogflowService {
    @POST("v2/projects/{projectId}/agent/sessions/{sessionId}:detectIntent")
    suspend fun detectIntent(
        @Path("projectId") projectId: String,
        @Path("sessionId") sessionId: String,
        @Body body: DialogflowRequest,
        @Header("Authorization") auth: String
    ): Response<DialogflowResponse>
}