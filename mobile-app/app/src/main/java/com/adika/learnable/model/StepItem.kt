package com.adika.learnable.model

data class StepItem(
    val key: String,
    val title: String,
    val isCompleted: Boolean,
    val scoreText: String? = null
)