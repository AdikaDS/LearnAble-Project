package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.model.Lesson
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonRepository @Inject constructor(
    firestore: FirebaseFirestore
) {
    private val lessonsCollection = firestore.collection("lessons")

    suspend fun getLessonsBySubjectAndDisabilityType(
        idSubject: String,
        disabilityType: String
    ): List<Lesson> {
        Log.d(
            "LessonRepository",
            "Getting lessons for subject: $idSubject, disability: $disabilityType"
        )
        try {
            val lessonSnapshot = lessonsCollection
                .whereEqualTo("idSubject", idSubject)
                .whereArrayContains("disabilityTypes", disabilityType)
                .get()
                .await()

            val lessons = lessonSnapshot.toObjects(Lesson::class.java)

            Log.d("LessonRepository", "Found ${lessons.size} matching lessons")
            return lessons
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error getting lessons", e)
            throw e
        }

    }

    suspend fun searchLessons(
        query: String,
        disabilityType: String,
        idSubject: String
    ): List<Lesson> {
        try {
            var lessonQuery: Query = lessonsCollection

            lessonQuery = lessonQuery.whereArrayContains("disabilityTypes", disabilityType)

            lessonQuery = lessonQuery.whereEqualTo("idSubject", idSubject)

            val lessonSnapshot = lessonQuery.get().await()
            val lessons = lessonSnapshot.toObjects(Lesson::class.java)

            return lessons.filter { lesson ->
                lesson.title.contains(query, ignoreCase = true) ||
                        lesson.content.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error searching lessons", e)
            throw e
        }
    }

    suspend fun getLessonsByTeacherId(teacherId: String): List<Lesson> {
        try {
            val lessonSnapshot = lessonsCollection
                .whereEqualTo("teacherId", teacherId)
                .get()
                .await()
            return lessonSnapshot.toObjects(Lesson::class.java)
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error getting lessons by teacherId", e)
            throw e
        }
    }

    suspend fun addLesson(lesson: Lesson): Lesson {
        try {
            val docRef = lessonsCollection.document()
            val lessonWithId = lesson.copy(id = docRef.id)
            docRef.set(lessonWithId).await()
            return lessonWithId
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error adding lesson", e)
            throw e
        }
    }

    suspend fun updateLesson(lesson: Lesson) {
        try {
            lessonsCollection.document(lesson.id).set(lesson).await()
            Log.d("LessonRepository", "Successfully updated lesson ${lesson.id}")
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error updating lesson", e)
            throw e
        }
    }

    suspend fun deleteLesson(lessonId: String) {
        try {
            lessonsCollection.document(lessonId).delete().await()
            Log.d("LessonRepository", "Successfully deleted lesson $lessonId")
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error deleting lesson", e)
            throw e
        }
    }

} 