package com.adika.learnable.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val profilePicture: String = "",
    val phone: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val parentId: String = "",
    val studentIds: List<String> = listOf(),
    val disabilityType: String? = null
)
