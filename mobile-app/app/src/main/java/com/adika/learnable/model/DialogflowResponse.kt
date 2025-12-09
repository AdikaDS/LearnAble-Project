package com.adika.learnable.model

import com.google.gson.annotations.SerializedName

data class DialogflowResponse(
    val queryResult: QueryResult?
)

data class TokenResponse(
    @SerializedName("access_token")
    val accessToken: String
)

data class QueryResult(
    val fulfillmentText: String?,
    val fulfillmentMessages: List<FulfillmentMessage>?,
    val outputContexts: List<OutputContext>?
)

data class OutputContext(
    val name: String,
    val parameters: Map<String, Any>?
)

data class FulfillmentMessage(
    val text: TextContent?,
    val payload: RichPayload?
)

data class TextContent(
    val text: List<String>?
)

data class RichPayload(
    val richContent: List<List<ChipOption>>?
)

data class ChipOption(
    val type: String?,
    val options: List<Chip>?
)

data class Chip(
    val text: String?
)

