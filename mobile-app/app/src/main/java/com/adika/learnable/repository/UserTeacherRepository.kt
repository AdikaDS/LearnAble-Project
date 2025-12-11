package com.adika.learnable.repository

import com.adika.learnable.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserTeacherRepository @Inject constructor(
    firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun getAllStudent(): List<User> {
        val student = usersCollection
            .whereEqualTo("role", "student")
            .get()
            .await()
        return student.toObjects(User::class.java)
    }
}