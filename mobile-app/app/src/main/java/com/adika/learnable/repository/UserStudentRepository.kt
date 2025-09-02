package com.adika.learnable.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStudentRepository @Inject constructor(
    private val auth: FirebaseAuth,
    firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

}