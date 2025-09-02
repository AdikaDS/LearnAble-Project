package com.adika.learnable.repository

import android.util.Log
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

    suspend fun searchStudent(
        query: String
    ): List<User> {
        try {
            val studentQuery = usersCollection.whereEqualTo("role", "student")

            val studentSnapshot = studentQuery.get().await()
            val student = studentSnapshot.toObjects(User::class.java)

            return student.filter { students ->
                students.name.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error searching lessons", e)
            throw e
        }
    }

}