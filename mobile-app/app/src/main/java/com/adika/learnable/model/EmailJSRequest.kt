package com.adika.learnable.model

data class EmailJSRequest(
    val serviceId: String,
    val templateId: String,
    val userId: String,
    val templateParams: Map<String, String>
)

data class EmailJSResponse(
    val status: String
)
