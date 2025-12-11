package com.adika.learnable.model

data class FeedbackReq(
    val name: String,
    val email: String,
    val role: String,
    val subject: String,
    val category: String,
    val message: String,
    val rating: Int?,
    val appVersion: String,
    val device: String,
    val token: String = "LearnAble-25"
)

data class FeedbackResp(val ok: Boolean, val error: String?)