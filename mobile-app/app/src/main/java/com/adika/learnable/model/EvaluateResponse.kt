package com.adika.learnable.model

data class EvaluateResponse(
    val predictedAnswer: String,
    val similarityScores: Map<String, Float>,
    val bestScore: Float
)
