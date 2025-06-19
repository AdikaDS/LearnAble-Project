package com.adika.learnable.model

data class ResendEmailRequest(
    val from: String,
    val to: String,
    val subject: String,
    val html: String
)

data class ResendEmailResponse(
    val message: String
)
