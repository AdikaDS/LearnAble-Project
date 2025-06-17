package com.adika.learnable.repository

import com.adika.learnable.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserStudentRepository @Inject constructor(
    private val auth: FirebaseAuth,
    firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun updateDisabilityType(disabilityType: String): User {
        val userId = auth.currentUser?.uid ?: throw Exception("User tidak ditemukan")

        usersCollection.document(userId)
            .update("disabilityType", disabilityType)
            .await()

        val userDoc = usersCollection.document(userId).get().await()
        return userDoc.toObject(User::class.java)
            ?: throw Exception("Gagal mendapatkan data user")
    }

}