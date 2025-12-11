package com.adika.learnable.repository

import com.adika.learnable.api.FeedbackService
import com.adika.learnable.model.FeedbackReq
import com.adika.learnable.model.FeedbackResp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepository @Inject constructor(
    private val feedbackService: FeedbackService
){
    suspend fun submitFeedback(request: FeedbackReq): FeedbackResp = withContext(Dispatchers.IO) {
        try {
            feedbackService.submit(request)
        } catch (e: Exception) {
            FeedbackResp(
                ok = false,
                error = e.localizedMessage ?: "Unknown error"
            )
        }
    }
}