package com.adika.learnable.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import kotlinx.parcelize.Parcelize

@Parcelize
data class User(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val role: String? = null,
    val idNumber: String? = null,
    val ttl: String? = null,
    val phoneNumber: String? = null,
    val profilePicture: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val gender: String? = null,
    val studentData: StudentData = StudentData(),
    val profileCompleted: Boolean = false,
    var isApproved: Boolean = false
) : Parcelable

@Parcelize
data class StudentData(
    val address: String = "",
    val provinceAddress: String = "",
    val cityAddress: String = "",
    val grade: String = "",
    val nameParent: String = "",
    val phoneNumberParent: String = ""
) : Parcelable