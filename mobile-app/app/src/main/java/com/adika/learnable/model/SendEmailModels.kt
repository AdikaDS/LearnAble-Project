package com.adika.learnable.model

data class UserRegistrationRequest(
    val name: String,
    val email: String,
    val role: String
)

data class RegisterResponse(
    val status: String?,
    val message: String?,
    val user: RegisteredUser?
)

data class RegisteredUser(
    val email: String?,
    val role: String?,
    val name: String?
)