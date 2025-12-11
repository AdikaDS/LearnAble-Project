package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.BuildConfig
import com.adika.learnable.model.Lesson
import com.amazonaws.services.s3.AmazonS3Client
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LessonRepository @Inject constructor(
    firestore: FirebaseFirestore,
    private val s3Client: AmazonS3Client
) {
    private val lessonsCollection = firestore.collection("lessons")

    suspend fun searchLessons(
        query: String,
        idSubject: String
    ): List<Lesson> {
        try {
            var lessonQuery: Query = lessonsCollection

            lessonQuery = lessonQuery.whereEqualTo("idSubject", idSubject)

            val lessonSnapshot = lessonQuery.get().await()
            val lessons = lessonSnapshot.toObjects(Lesson::class.java)

            return lessons.filter { lesson ->
                lesson.title.contains(query, ignoreCase = true)
            }
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error searching lessons", e)
            throw e
        }
    }

    suspend fun getLessonsBySubject(
        idSubject: String
    ): List<Lesson> {
        try {
            val lessonSnapshot = lessonsCollection
                .whereEqualTo("idSubject", idSubject)
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

    suspend fun getLessonCountBySubject(subjectId: String): Int {
        try {
            val snapshot = lessonsCollection
                .whereEqualTo("idSubject", subjectId)
                .get()
                .await()
            return snapshot.size()
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error counting lesson for subject $subjectId", e)
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

    suspend fun updateLessonTotalSubBab(lessonId: String, totalSubBab: Int) {
        try {
            lessonsCollection.document(lessonId)
                .update("totalSubBab", totalSubBab)
                .await()
            Log.d("LessonRepository", "Successfully updated totalSubBab for lesson $lessonId to $totalSubBab")
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error updating totalSubBab for lesson $lessonId", e)
            throw e
        }
    }

    suspend fun getLesson(lessonId: String): Lesson? {
        try {
            val document = lessonsCollection.document(lessonId).get().await()
            return document.toObject(Lesson::class.java)
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error getting lesson $lessonId", e)
            return null
        }
    }

    private fun generateCoverObjectKey(originalFileName: String): String {
        val sanitized = originalFileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return "lessonCover/$sanitized"
    }

    private fun generateS3Url(objectKey: String): String {
        return s3Client.getUrl(BuildConfig.S3_BUCKET_NAME, objectKey).toString()
    }

    private suspend fun uploadToS3(file: File, objectKey: String) {
        return withContext(Dispatchers.IO) {
            s3Client.putObject(BuildConfig.S3_BUCKET_NAME, objectKey, file)
        }
    }

    suspend fun uploadLessonCover(file: File): String {

        val ext = file.name.substringAfterLast('.', "").lowercase()
        if (ext !in listOf("jpg", "jpeg", "png", "webp")) {
            throw IllegalArgumentException("Format file tidak didukung. Hanya jpg, jpeg, png, webp")
        }
        val max = 5 * 1024 * 1024 // 5MB
        if (file.length() > max) {
            throw IllegalArgumentException("Ukuran file terlalu besar. Maksimal 5MB")
        }

        val objectKey = generateCoverObjectKey(file.name)
        uploadToS3(file, objectKey)
        return generateS3Url(objectKey)
    }

    private fun extractObjectKeyFromUrl(url: String?): String? {
        return url?.let {

            // Format: https://bucket.s3.region.amazonaws.com/objectKey
            val pattern = "https?://[^/]+/(.+)".toRegex()
            pattern.find(it)?.groupValues?.get(1)
        }
    }

    private suspend fun deleteFromS3(objectKey: String) {
        return withContext(Dispatchers.IO) {
            try {
                s3Client.deleteObject(BuildConfig.S3_BUCKET_NAME, objectKey)
                Log.d("LessonRepository", "Successfully deleted cover from S3: $objectKey")
            } catch (e: Exception) {
                Log.e("LessonRepository", "Error deleting cover from S3", e)
                throw Exception("Gagal delete cover dari S3: ${e.message}")
            }
        }
    }

    suspend fun deleteLessonCover(lessonId: String) {
        try {
            val lessonDoc = lessonsCollection.document(lessonId).get().await()
            if (!lessonDoc.exists()) return

            val coverUrl = lessonDoc.get("coverImage") as? String
            val coverKey = extractObjectKeyFromUrl(coverUrl)
            if (!coverKey.isNullOrBlank()) {
                deleteFromS3(coverKey)
            }
            Log.d("LessonRepository", "Successfully deleted lesson cover for lesson $lessonId")
        } catch (e: Exception) {
            Log.e("LessonRepository", "Error deleting lesson cover", e)
            throw e
        }
    }
} 