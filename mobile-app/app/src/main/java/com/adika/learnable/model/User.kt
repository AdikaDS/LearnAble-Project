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
    val parentId: String = "", // Untuk student
    val studentIds: List<String> = listOf(), // Untuk parent
    val disabilityType: String? = null // "tunarungu" atau "tunanetra", kosong jika tidak ada
)
