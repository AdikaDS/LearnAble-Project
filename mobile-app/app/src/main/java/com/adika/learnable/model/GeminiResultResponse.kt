package com.adika.learnable.model

data class GeminiResultResponse(
    val status: String,
    val fulfillmentMessages: List<FulfillmentMessage>?,
    val outputContexts: List<OutputContext>? = null
)