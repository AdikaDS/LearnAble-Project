package com.adika.learnable.model

import com.google.firebase.Timestamp

data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String? = null,
    val nomorInduk: String? = null,
    val ttl: String? = null,
    val kelas: String? = null,
    val phone: String? = null,
    val profilePicture: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val parentId: String? = null,
    val studentIds: List<String>? = null,
    val profileCompleted: Boolean = false,
    var isApproved: Boolean = false
)
