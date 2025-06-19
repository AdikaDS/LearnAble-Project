package com.adika.learnable.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val ttl: String = "",
    val kelas: String? = null,
    val phone: String = "",
    val disabilityType: String? = null,
    val profilePicture: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val parentId: String? = null,
    val studentIds: List<String>? = null,
    var isApproved: Boolean = false
)
