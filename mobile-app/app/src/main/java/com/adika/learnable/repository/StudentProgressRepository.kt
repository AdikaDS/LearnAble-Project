package com.adika.learnable.repository

import android.content.Context
import android.util.Log
import com.adika.learnable.model.StudentLessonProgress
import com.adika.learnable.model.StudentOverallProgress
import com.adika.learnable.model.StudentSubjectProgress
import com.adika.learnable.model.StudentSubBabProgress
import com.adika.learnable.util.ErrorMessages
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentProgressRepository @Inject constructor(
    private val auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val subBabProgressCollection = firestore.collection("student_subbab_progress")
    private val lessonProgressCollection = firestore.collection("student_lesson_progress")
    private val subjectProgressCollection = firestore.collection("student_subject_progress")
    private val overallProgressCollection = firestore.collection("student_overall_progress")

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw Exception(ErrorMessages.getAuthFailed(context))
    }

    // Get overall progress for a student
    suspend fun getStudentOverallProgress(studentId: String): StudentOverallProgress? {
        try {
            val snapshot = overallProgressCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            return if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents[0].toObject(StudentOverallProgress::class.java)
            }
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error getting overall progress", e)
            throw e
        }
    }

    // Update overall progress
    private suspend fun updateOverallProgress(studentId: String) {
        try {
            val subjectProgress = subjectProgressCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
                .toObjects(StudentSubjectProgress::class.java)

            if (subjectProgress.isNotEmpty()) {
                val totalSubjects = subjectProgress.size
                val completedSubjects = subjectProgress.count { it.progressPercentage == 100 }
                val overallProgressPercentage = if (totalSubjects > 0) {
                    subjectProgress.sumOf { it.progressPercentage } / totalSubjects
                } else 0

                val totalTimeSpent = subjectProgress.sumOf { it.totalTimeSpent }

                val overallProgress = StudentOverallProgress(
                    studentId = studentId,
                    studentName = "", // This should be fetched from user data
                    totalSubjects = totalSubjects,
                    completedSubjects = completedSubjects,
                    overallProgressPercentage = overallProgressPercentage,
                    totalTimeSpent = totalTimeSpent,
                    lastActivityDate = Timestamp.now(),
                    subjectProgress = subjectProgress
                )

                // Update or create overall progress
                val existingOverallProgress = overallProgressCollection
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .await()

                if (existingOverallProgress.isEmpty) {
                    overallProgressCollection.add(overallProgress).await()
                } else {
                    existingOverallProgress.documents[0].reference.set(overallProgress).await()
                }
            }
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error updating overall progress", e)
            throw e
        }
    }

    suspend fun getStudentSubjectProgress(studentId: String): List<StudentSubjectProgress> {
        try {
            val snapshot = subjectProgressCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()

            return snapshot.toObjects(StudentSubjectProgress::class.java)
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error getting subject progress", e)
            throw e
        }
    }

    // Update subject progress
    private suspend fun updateSubjectProgress(studentId: String, subjectId: String) {
        try {
            // Get all lesson progress for this subject
            val lessonProgress = lessonProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
                .toObjects(StudentLessonProgress::class.java)

            if (lessonProgress.isNotEmpty()) {
                val totalLessons = lessonProgress.size
                val completedLessons = lessonProgress.count { it.isCompleted }
                val progressPercentage = if (totalLessons > 0) {
                    (completedLessons * 100) / totalLessons
                } else 0

                val quizAverage = lessonProgress
                    .flatMap { it.quizScores }
                    .takeIf { it.isNotEmpty() }
                    ?.average()
                    ?.toFloat() ?: 0f

                val totalTimeSpent = lessonProgress.sumOf { it.totalTimeSpent }

                val subjectProgress = StudentSubjectProgress(
                    studentId = studentId,
                    subjectId = subjectId,
                    subjectName = lessonProgress.firstOrNull()?.subjectName ?: "",
                    progressPercentage = progressPercentage,
                    completedLessons = completedLessons,
                    totalLessons = totalLessons,
                    quizAverage = quizAverage,
                    lastUpdated = Timestamp.now(),
                    totalTimeSpent = totalTimeSpent,
                    lastActivityDate = Timestamp.now(),
                    lessonProgress = lessonProgress
                )

                // Update or create subject progress
                val existingSubjectProgress = subjectProgressCollection
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("subjectId", subjectId)
                    .get()
                    .await()

                if (existingSubjectProgress.isEmpty) {
                    subjectProgressCollection.add(subjectProgress).await()
                } else {
                    existingSubjectProgress.documents[0].reference.set(subjectProgress).await()
                }

                // Update overall progress
                updateOverallProgress(studentId)
            }
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error updating subject progress", e)
            throw e
        }
    }

    // Get lesson progress for a student
    suspend fun getStudentLessonProgress(
        studentId: String,
        lessonId: String
    ): StudentLessonProgress? {
        try {
            val snapshot = lessonProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("lessonId", lessonId)
                .get()
                .await()

            return if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents[0].toObject(StudentLessonProgress::class.java)
            }
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error getting lesson progress", e)
            throw e
        }
    }

    // Update lesson progress
    private suspend fun updateLessonProgress(studentId: String, lessonId: String) {
        try {
            val subBabProgress = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("lessonId", lessonId)
                .get()
                .await()
                .toObjects(StudentSubBabProgress::class.java)

            if (subBabProgress.isNotEmpty()) {
                val totalSubBabs = subBabProgress.size
                val completedSubBabs = subBabProgress.count { it.isCompleted }
                val progressPercentage = if (totalSubBabs > 0) {
                    (completedSubBabs * 100) / totalSubBabs
                } else 0

                val quizScores = subBabProgress
                    .filter { it.quizScore > 0f }
                    .map { it.quizScore }

                val totalTimeSpent = subBabProgress.sumOf { it.timeSpent }

                val lessonProgress = StudentLessonProgress(
                    studentId = studentId,
                    lessonId = lessonId,
                    progressPercentage = progressPercentage,
                    completedSubBabs = completedSubBabs,
                    totalSubBabs = totalSubBabs,
                    quizScores = quizScores,
                    lastActivityDate = Timestamp.now(),
                    totalTimeSpent = totalTimeSpent,
                    isCompleted = completedSubBabs == totalSubBabs
                )

                // Update or create lesson progress
                val lessonProgressDoc = lessonProgressCollection
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("lessonId", lessonId)
                    .get()
                    .await()

                if (lessonProgressDoc.isEmpty) {
                    lessonProgressCollection.add(lessonProgress).await()
                } else {
                    lessonProgressCollection.document(lessonProgressDoc.documents[0].id)
                        .set(lessonProgress)
                        .await()
                }

                // Update subject progress
                updateSubjectProgress(studentId, lessonProgress.subjectId)
            }
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error updating lesson progress", e)
            throw e
        }
    }

    suspend fun getSubBabProgress(studentId: String, subBabId: String): StudentSubBabProgress? {
        try {
            val snapshot = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            return if (snapshot.isEmpty) {
                null
            } else {
                snapshot.documents[0].toObject(StudentSubBabProgress::class.java)
            }
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error getting sub-bab progress", e)
            throw e
        }
    }

    suspend fun updateSubBabProgress(progress: StudentSubBabProgress) {
        try {
            // Cari dokumen yang sudah ada berdasarkan studentId dan subBabId
            val query = subBabProgressCollection
                .whereEqualTo("studentId", progress.studentId)
                .whereEqualTo("subBabId", progress.subBabId)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                // Jika dokumen sudah ada, update dokumen tersebut
                val existingDoc = query.documents[0]
                existingDoc.reference.set(progress.copy(id = existingDoc.id)).await()
            } else {
                // Jika dokumen belum ada, buat baru
                val docRef = subBabProgressCollection.document()
                val progressWithId = progress.copy(id = docRef.id)
                docRef.set(progressWithId).await()
            }
            
            // Update lesson progress
            updateLessonProgress(progress.studentId, progress.lessonId)
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error updating sub-bab progress", e)
            throw e
        }
    }

    suspend fun markMaterialAsCompleted(studentId: String, subBabId: String, lessonId: String, materialType: String) {
        try {
            // Cari dokumen progress yang sudah ada
            val query = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                // Update dokumen yang sudah ada
                val existingDoc = query.documents[0]
                val currentProgress = existingDoc.toObject(StudentSubBabProgress::class.java)
                
                if (currentProgress != null) {
                    // Update completedMaterials berdasarkan tipe material
                    val updatedCompletedMaterials = currentProgress.completedMaterials.toMutableMap().apply {
                        put(materialType, true)
                    }

                    // Cek apakah semua material sudah selesai
                    val isCompleted = updatedCompletedMaterials.all { it.value }

                    val updatedProgress = currentProgress.copy(
                        completedMaterials = updatedCompletedMaterials,
                        isCompleted = isCompleted,
                        lastActivityDate = Timestamp.now()
                    )
                    existingDoc.reference.set(updatedProgress).await()
                    
                    // Update lesson progress
                    updateLessonProgress(studentId, lessonId)
                }
            } else {
                // Buat dokumen baru jika belum ada
                val initialCompletedMaterials = mapOf(
                    "pdf" to (materialType == "pdf"),
                    "video" to (materialType == "video"),
                    "audio" to (materialType == "audio"),
                    "quiz" to (materialType == "quiz")
                )

                val docRef = subBabProgressCollection.document()
                val newProgress = StudentSubBabProgress(
                    id = docRef.id,
                    studentId = studentId,
                    subBabId = subBabId,
                    lessonId = lessonId,
                    completedMaterials = initialCompletedMaterials,
                    quizScore = 0f,
                    timeSpent = 0,
                    lastActivityDate = Timestamp.now(),
                    isCompleted = false
                )
                docRef.set(newProgress).await()
                
                // Update lesson progress
                updateLessonProgress(studentId, lessonId)
            }
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error marking material as completed", e)
            throw e
        }
    }

    suspend fun updateQuizScore(studentId: String, subBabId: String, lessonId: String, score: Float) {
        try {
            // Cari dokumen progress yang sudah ada
            val query = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                // Update dokumen yang sudah ada
                val existingDoc = query.documents[0]
                val currentProgress = existingDoc.toObject(StudentSubBabProgress::class.java)
                
                if (currentProgress != null) {
                    // Update quiz score dan completedMaterials
                    val updatedCompletedMaterials = currentProgress.completedMaterials.toMutableMap().apply {
                        put("quiz", true)
                    }

                    // Cek apakah semua material sudah selesai
                    val isCompleted = updatedCompletedMaterials.all { it.value }

                    val updatedProgress = currentProgress.copy(
                        quizScore = score,
                        completedMaterials = updatedCompletedMaterials,
                        isCompleted = isCompleted,
                        lastActivityDate = Timestamp.now()
                    )
                    existingDoc.reference.set(updatedProgress).await()
                    
                    // Update lesson progress
                    updateLessonProgress(studentId, lessonId)
                }
            } else {
                // Buat dokumen baru jika belum ada
                val initialCompletedMaterials = mapOf(
                    "pdf" to false,
                    "video" to false,
                    "audio" to false,
                    "quiz" to true
                )

                val docRef = subBabProgressCollection.document()
                val newProgress = StudentSubBabProgress(
                    id = docRef.id,
                    studentId = studentId,
                    subBabId = subBabId,
                    lessonId = lessonId,
                    completedMaterials = initialCompletedMaterials,
                    quizScore = score,
                    timeSpent = 0,
                    lastActivityDate = Timestamp.now(),
                    isCompleted = false
                )
                docRef.set(newProgress).await()
                
                // Update lesson progress
                updateLessonProgress(studentId, lessonId)
            }
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error updating quiz score", e)
            throw e
        }
    }

    suspend fun updateTimeSpent(studentId: String, subBabId: String, lessonId: String, minutes: Int) {
        try {
            // Cari dokumen progress yang sudah ada
            val query = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {
                // Update dokumen yang sudah ada
                val existingDoc = query.documents[0]
                val currentProgress = existingDoc.toObject(StudentSubBabProgress::class.java)
                
                if (currentProgress != null) {
                    val updatedProgress = currentProgress.copy(
                        timeSpent = currentProgress.timeSpent + minutes,
                        lastActivityDate = Timestamp.now()
                    )
                    existingDoc.reference.set(updatedProgress).await()
                    
                    // Update lesson progress
                    updateLessonProgress(studentId, lessonId)
                }
            } else {
                // Buat dokumen baru jika belum ada
                val initialCompletedMaterials = mapOf(
                    "pdf" to false,
                    "video" to false,
                    "audio" to false,
                    "quiz" to false
                )

                val docRef = subBabProgressCollection.document()
                val newProgress = StudentSubBabProgress(
                    id = docRef.id,
                    studentId = studentId,
                    subBabId = subBabId,
                    lessonId = lessonId,
                    completedMaterials = initialCompletedMaterials,
                    quizScore = 0f,
                    timeSpent = minutes,
                    lastActivityDate = Timestamp.now(),
                    isCompleted = false
                )
                docRef.set(newProgress).await()
                
                // Update lesson progress
                updateLessonProgress(studentId, lessonId)
            }
        } catch (e: Exception) {
            Log.e("StudentProgressRepository", "Error updating time spent", e)
            throw e
        }
    }
} 