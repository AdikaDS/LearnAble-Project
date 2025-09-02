package com.adika.learnable.model

data class DialogflowRequest(
    val queryInput: QueryInput
)

data class QueryInput(
    val text: TextInput
)

data class TextInput(
    val text: String,
    val languageCode: String = "id" // ganti jika kamu pakai en-US
)
