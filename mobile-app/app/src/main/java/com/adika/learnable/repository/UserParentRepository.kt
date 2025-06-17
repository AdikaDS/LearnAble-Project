package com.adika.learnable.repository

import com.adika.learnable.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserParentRepository @Inject constructor(
    firestore: FirebaseFirestore
) {
    private val usersCollection = firestore.collection("users")

    suspend fun connectStudentWithParent(studentId: String, parentId: String) {
        // Update student document with parent ID
        usersCollection.document(studentId)
            .update("parentId", parentId)
            .await()

        // Get current student IDs of parent
        val parentDoc = usersCollection.document(parentId).get().await()
        val parent = parentDoc.toObject(User::class.java)
            ?: throw Exception("Parent not found")

        // Add student ID to parent's studentIds list
        val updatedStudentIds = parent.studentIds?.toMutableList()
        if (updatedStudentIds != null) {
            if (!updatedStudentIds.contains(studentId)) {
                updatedStudentIds.add(studentId)
                usersCollection.document(parentId)
                    .update("studentIds", updatedStudentIds)
                    .await()
            }
        }
    }

    suspend fun getStudentsByParentId(parentId: String): List<User> {
        val students = usersCollection
            .whereEqualTo("parentId", parentId)
            .get()
            .await()
        return students.toObjects(User::class.java)
    }

    suspend fun getParentByStudentId(studentId: String): User? {
        val studentDoc = usersCollection.document(studentId).get().await()
        val student = studentDoc.toObject(User::class.java)
            ?: throw Exception("Student not found")

        if (student.parentId?.isBlank() == true) {
            return null
        }

        val parentDoc = student.parentId?.let { usersCollection.document(it).get().await() }
        return parentDoc?.toObject(User::class.java)
    }

    suspend fun findStudentEmail(email: String): User? {
        val studentQuery = usersCollection
            .whereEqualTo("email", email)
            .whereEqualTo("role", "student")
            .get()
            .await()

        return if (studentQuery.documents.isNotEmpty()) {
            studentQuery.documents[0].toObject(User::class.java)
        } else {
            null
        }
    }
}