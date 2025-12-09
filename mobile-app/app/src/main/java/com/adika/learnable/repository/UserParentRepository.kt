package com.adika.learnable.repository

import android.util.Log
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
        // Check if student already has a parent
        val studentDoc = usersCollection.document(studentId).get().await()
        val student = studentDoc.toObject(User::class.java)
            ?: throw Exception("Student not found")
        
        if (student.parentId?.isNotBlank() == true) {
            throw Exception("Student already has a parent")
        }

        // Update student document with parent ID
        usersCollection.document(studentId)
            .update("parentId", parentId)
            .await()

        // Get current student IDs of parent
        val parentDoc = usersCollection.document(parentId).get().await()
        val parent = parentDoc.toObject(User::class.java)
            ?: throw Exception("Parent not found")

        // Add student ID to parent's studentIds list
        val updatedStudentIds = parent.studentIds?.toMutableList() ?: mutableListOf()
        if (!updatedStudentIds.contains(studentId)) {
            updatedStudentIds.add(studentId)
            usersCollection.document(parentId)
                .update("studentIds", updatedStudentIds)
                .await()
        }

    }

    suspend fun checkStudentsAvailability(studentIds: List<String>): List<String> {
        val unavailableStudents = mutableListOf<String>()
        
        for (studentId in studentIds) {
            val studentDoc = usersCollection.document(studentId).get().await()
            val student = studentDoc.toObject(User::class.java)
                ?: continue
            
            if (student.parentId?.isNotBlank() == true) {
                unavailableStudents.add(student.name)
            }
        }
        
        return unavailableStudents
    }

    suspend fun getStudentsByParentId(parentId: String): List<User> {
        val students = usersCollection
            .whereEqualTo("parentId", parentId)
            .get()
            .await()
        return students.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.copy(id = doc.id)
        }
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
            studentQuery.documents[0].toObject(User::class.java)?.copy(id = studentQuery.documents[0].id)
        } else {
            null
        }
    }

    /**
     * Hybrid search: fetch students (role == "student") and filter by name contains query (case-insensitive).
     * This avoids composite index requirements on Firestore while remaining reasonably efficient for small datasets.
     */
    suspend fun searchStudentByNameHybrid(query: String): List<User> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return emptyList()

        return try {
            val snapshot = usersCollection
                .whereEqualTo("role", "student")
                .get()
                .await()

            val lower = trimmed.lowercase()
            snapshot.documents.mapNotNull { doc ->
                doc.toObject(User::class.java)?.copy(id = doc.id)
            }.filter { it.name.lowercase().contains(lower) }
        } catch (e: Exception) {
            Log.e("UserParentRepository", "Failed searching students", e)
            emptyList()
        }
    }
}