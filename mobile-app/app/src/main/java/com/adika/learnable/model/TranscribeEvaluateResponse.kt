package com.adika.learnable.model

data class TranscribeEvaluateResponse (
    val transcript: String,
    val evaluation: EvaluateResponse
)