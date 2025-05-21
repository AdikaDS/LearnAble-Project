package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.model.Subject
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepository @Inject constructor(
    firestore: FirebaseFirestore,
    private val lessonRepository: LessonRepository
) {
    private val subjectsCollection = firestore.collection("subjects")
    private val subjectCache = mutableMapOf<String, List<Subject>>()

    suspend fun getSubjectsBySchoolLevel(
        schoolLevel: String,
        disabilityType: String
    ): List<Subject> {
        val cacheKey = "$schoolLevel-$disabilityType"
        
        // Return cached data if available
        subjectCache[cacheKey]?.let {
            Log.d("SubjectRepository", "Returning cached subjects for $cacheKey")
            return it
        }

        try {
            val subjectSnapshot = subjectsCollection
                .whereEqualTo("schoolLevel", schoolLevel)
                .get()
                .await()

            val subjects = subjectSnapshot.toObjects(Subject::class.java)

            val processedSubjects = subjects.map { subject ->
                val lessons = lessonRepository.getLessonsBySubjectAndDisabilityType(
                    idSubject = subject.idSubject,
                    disabilityType = disabilityType
                )

                val totalLessons = lessons.size
                Log.d(
                    "SubjectRepository",
                    "Subject ${subject.name} has $totalLessons lessons for disability type: $disabilityType"
                )

                updateTotalLesson(subject.idSubject, totalLessons)
                subject.copy(totalLessons = totalLessons)
            }

            // Cache the processed subjects
            subjectCache[cacheKey] = processedSubjects
            return processedSubjects
        } catch (e: Exception) {
            Log.e("SubjectRepository", "Error getting subjects", e)
            throw e
        }
    }

    private suspend fun updateTotalLesson(idSubject: String, totalLessons: Int) {
        try {
            val subjectQuery = subjectsCollection
                .whereEqualTo("idSubject", idSubject)
                .get()
                .await()
            if (subjectQuery.documents.isNotEmpty()) {
                val documentId = subjectQuery.documents[0].id
                subjectsCollection.document(documentId)
                    .update("totalLessons", totalLessons)
                    .await()
                Log.d(
                    "SubjectRepository",
                    "Successfully updated totalLessons for subject $idSubject to $totalLessons"
                )
            } else {
                Log.e("SubjectRepository", "Subject with idSubject $idSubject not found")
            }
        } catch (e: Exception) {
            Log.e("SubjectRepository", "Error updating totalLessons for subject $idSubject", e)
            throw e
        }
    }
} 