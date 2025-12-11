package com.adika.learnable.model

enum class QuizPageState { UNANSWERED, ANSWERED, CORRECT, INCORRECT }

data class QuizPage(
    val number: Int,
    var state: QuizPageState = QuizPageState.UNANSWERED
)