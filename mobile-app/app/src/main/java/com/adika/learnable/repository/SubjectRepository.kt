package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.model.Subject
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepository @Inject constructor(
    firestore: FirebaseFirestore
) {
    private val subjectsCollection = firestore.collection("subjects")
    private val subjectCache = mutableMapOf<String, List<Subject>>()

    suspend fun getSubjectsBySchoolLevel(
        schoolLevel: String
    ): List<Subject> {

        subjectCache[schoolLevel]?.let {
            Log.d("SubjectRepository", "Returning cached subjects for $schoolLevel")
            return it
        }

        try {
            val subjectSnapshot = subjectsCollection
                .whereEqualTo("schoolLevel", schoolLevel)
                .get()
                .await()

            val subjects = subjectSnapshot.toObjects(Subject::class.java)

            subjectCache[schoolLevel] = subjects
            return subjects
        } catch (e: Exception) {
            Log.e("SubjectRepository", "Error getting subjects", e)
            throw e
        }
    }

    suspend fun getSubjectById(subjectId: String): Subject? = try {
        val snapshot = subjectsCollection
            .whereEqualTo("idSubject", subjectId)
            .get()
            .await()

        snapshot.toObjects(Subject::class.java).firstOrNull()
    } catch (e: Exception) {
        Log.e("SubjectRepository", "Error getting subject by ID", e)
        null
    }
}