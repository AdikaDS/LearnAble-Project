package com.adika.learnable.repository

import android.content.Context
import android.content.Intent
import android.util.Log
import com.adika.learnable.R
import com.adika.learnable.model.Lesson
import com.adika.learnable.model.StudentLessonProgress
import com.adika.learnable.model.StudentOverallProgress
import com.adika.learnable.model.StudentSubBabProgress
import com.adika.learnable.model.StudentSubjectProgress
import com.adika.learnable.receiver.LearningProgressReceiver
import com.adika.learnable.util.ErrorMessages
import com.adika.learnable.util.NormalizeSchoolLevel.formatSchoolLevel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentProgressRepository @Inject constructor(
    private val auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context,
    private val notificationRepository: NotificationRepository
) {
    companion object {
        private const val TAG = "StudentProgressRepository"
        private const val MATERIAL_PDF = "pdf"
        private const val MATERIAL_VIDEO = "video"
        private const val MATERIAL_QUIZ = "quiz"
    }

    private val subBabProgressCollection = firestore.collection("student_subbab_progress")
    private val lessonProgressCollection = firestore.collection("student_lesson_progress")
    private val subjectProgressCollection = firestore.collection("student_subject_progress")
    private val overallProgressCollection = firestore.collection("student_overall_progress")
    private val lessonsCollection = firestore.collection("lessons")
    private val subBabCollection = firestore.collection("sub_bab")

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException(
            ErrorMessages.getAuthFailed(
                context
            )
        )
    }

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
            Log.e(TAG, "Error getting overall progress", e)
            throw e
        }
    }

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
                    (completedSubjects * 100) / totalSubjects
                } else 0

                val totalTimeSpent = subjectProgress.sumOf { it.totalTimeSpent }

                val quizAverage = calculateQuizAverage(
                    subjectProgress
                        .filter { it.quizAverage > 0f }
                        .map { it.quizAverage }
                )

                val streak = calculateStreak(subjectProgress.map { it.lastActivityDate })

                val existingOverallProgress = overallProgressCollection
                    .whereEqualTo("studentId", studentId)
                    .get()
                    .await()

                if (existingOverallProgress.isEmpty) {

                    val overallProgressDocRef = overallProgressCollection.document()
                    val overallProgress = StudentOverallProgress(
                        id = overallProgressDocRef.id,
                        studentId = studentId,
                        totalSubjects = totalSubjects,
                        completedSubjects = completedSubjects,
                        overallProgressPercentage = overallProgressPercentage,
                        totalTimeSpent = totalTimeSpent,
                        quizAverage = quizAverage,
                        streak = streak,
                        lastActivityDate = Timestamp.now(),
                        subjectProgress = subjectProgress
                    )
                    overallProgressCollection.add(overallProgress).await()
                } else {

                    val existingDoc = existingOverallProgress.documents[0]
                    val overallProgress = StudentOverallProgress(
                        id = existingDoc.id,
                        studentId = studentId,
                        totalSubjects = totalSubjects,
                        completedSubjects = completedSubjects,
                        overallProgressPercentage = overallProgressPercentage,
                        totalTimeSpent = totalTimeSpent,
                        quizAverage = quizAverage,
                        streak = streak,
                        lastActivityDate = Timestamp.now(),
                        subjectProgress = subjectProgress
                    )
                    existingDoc.reference.set(overallProgress).await()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating overall progress", e)
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
            Log.e(TAG, "Error getting subject progress", e)
            throw e
        }
    }

    suspend fun getStudentSubjectProgressBySubjectId(
        studentId: String,
        subjectId: String
    ): StudentSubjectProgress? {
        try {
            val snapshot = subjectProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()

            return snapshot.documents.firstOrNull()?.toObject(StudentSubjectProgress::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subject progress by subjectId", e)
            throw e
        }
    }

    private suspend fun updateSubjectProgress(studentId: String, subjectId: String) {
        try {

            val lessonProgress = lessonProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subjectId", subjectId)
                .get()
                .await()
                .toObjects(StudentLessonProgress::class.java)

            if (lessonProgress.isNotEmpty()) {
                val totalLessons = lessonProgress.size
                val completedLessons = lessonProgress.count { it.isCompleted }
                val progressPercentage =
                    (completedLessons * 100) / totalLessons


                val quizAverage = calculateQuizAverageFromLessonProgress(lessonProgress)

                val totalTimeSpent = lessonProgress.sumOf { it.totalTimeSpent }

                val streak = calculateStreak(lessonProgress.map { it.lastActivityDate })

                val existingSubjectProgress = subjectProgressCollection
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("subjectId", subjectId)
                    .get()
                    .await()

                if (existingSubjectProgress.isEmpty) {

                    val subjectProgressDocRef = subjectProgressCollection.document()
                    val subjectProgress = StudentSubjectProgress(
                        id = subjectProgressDocRef.id,
                        studentId = studentId,
                        subjectId = subjectId,
                        progressPercentage = progressPercentage,
                        completedLessons = completedLessons,
                        totalLessons = totalLessons,
                        quizAverage = quizAverage,
                        lastUpdated = Timestamp.now(),
                        streak = streak,
                        totalTimeSpent = totalTimeSpent,
                        lastActivityDate = Timestamp.now(),
                        lessonProgress = lessonProgress
                    )
                    subjectProgressCollection.add(subjectProgress).await()
                } else {

                    val existingDoc = existingSubjectProgress.documents[0]
                    val subjectProgress = StudentSubjectProgress(
                        id = existingDoc.id,
                        studentId = studentId,
                        subjectId = subjectId,
                        progressPercentage = progressPercentage,
                        completedLessons = completedLessons,
                        totalLessons = totalLessons,
                        quizAverage = quizAverage,
                        lastUpdated = Timestamp.now(),
                        streak = streak,
                        totalTimeSpent = totalTimeSpent,
                        lastActivityDate = Timestamp.now(),
                        lessonProgress = lessonProgress
                    )
                    existingDoc.reference.set(subjectProgress).await()
                }

                updateOverallProgress(studentId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating subject progress", e)
            throw e
        }
    }

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

            return snapshot.documents.firstOrNull()?.toObject(StudentLessonProgress::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting lesson progress", e)
            throw e
        }
    }

    private suspend fun updateLessonProgress(studentId: String, lessonId: String) {
        try {
            val subBabProgress = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("lessonId", lessonId)
                .get()
                .await()
                .toObjects(StudentSubBabProgress::class.java)

            if (subBabProgress.isNotEmpty()) {

                val lesson = try {
                    lessonsCollection.document(lessonId).get().await().toObject(Lesson::class.java)
                } catch (e: Exception) {
                    Log.w(TAG, "Unable to fetch lesson $lessonId; proceeding with defaults", e)
                    null
                }

                val totalSubBabs = subBabProgress.size
                val completedSubBabs = subBabProgress.count { it.isCompleted }
                val progressPercentage = if (totalSubBabs > 0) {
                    (completedSubBabs * 100) / totalSubBabs
                } else 0

                val quizScores = subBabProgress
                    .filter { it.quizScore > 0f }
                    .map { it.quizScore }

                val totalTimeSpent = subBabProgress.sumOf { it.timeSpent }

                val quizAverage = calculateQuizAverageFromSubBabProgress(subBabProgress)

                val lessonProgressDoc = lessonProgressCollection
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("lessonId", lessonId)
                    .get()
                    .await()

                if (lessonProgressDoc.isEmpty) {

                    val lessonProgressDocRef = lessonProgressCollection.document()
                    val lessonProgress = StudentLessonProgress(
                        id = lessonProgressDocRef.id,
                        studentId = studentId,
                        lessonId = lessonId,
                        lessonTitle = lesson?.title ?: "",
                        subjectId = lesson?.idSubject ?: "",
                        progressPercentage = progressPercentage,
                        completedSubBabs = completedSubBabs,
                        totalSubBabs = totalSubBabs,
                        subBabProgress = subBabProgress,
                        quizAverage = quizAverage,
                        quizScores = quizScores,
                        lastActivityDate = Timestamp.now(),
                        totalTimeSpent = totalTimeSpent,
                        isCompleted = completedSubBabs == totalSubBabs
                    )
                    lessonProgressCollection.add(lessonProgress).await()
                } else {

                    val existingDoc = lessonProgressDoc.documents[0]
                    val lessonProgress = StudentLessonProgress(
                        id = existingDoc.id,
                        studentId = studentId,
                        lessonId = lessonId,
                        lessonTitle = lesson?.title ?: "",
                        subjectId = lesson?.idSubject ?: "",
                        progressPercentage = progressPercentage,
                        completedSubBabs = completedSubBabs,
                        totalSubBabs = totalSubBabs,
                        subBabProgress = subBabProgress,
                        quizAverage = quizAverage,
                        quizScores = quizScores,
                        lastActivityDate = Timestamp.now(),
                        totalTimeSpent = totalTimeSpent,
                        isCompleted = completedSubBabs == totalSubBabs
                    )
                    lessonProgressCollection.document(existingDoc.id)
                        .set(lessonProgress)
                        .await()
                }

                val subjectIdForUpdate = lesson?.idSubject ?: ""
                if (subjectIdForUpdate.isNotEmpty()) {
                    updateSubjectProgress(studentId, subjectIdForUpdate)
                } else {
                    Log.w(
                        TAG,
                        "Skipped subject progress update: missing subjectId for lesson $lessonId"
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating lesson progress", e)
            throw e
        }
    }

    suspend fun getStudentSubBabProgress(
        studentId: String,
        subBabId: String
    ): StudentSubBabProgress? {
        try {
            val snapshot = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            return snapshot.documents.firstOrNull()?.toObject(StudentSubBabProgress::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting sub-bab progress", e)
            throw e
        }
    }

    suspend fun getAllStudentSubBabProgress(studentId: String): List<StudentSubBabProgress> {
        try {
            val snapshot = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .get()
                .await()
            val list = snapshot.toObjects(StudentSubBabProgress::class.java)
            Log.d(TAG, "Fetched subbab progress count=${list.size}")
            return list
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all sub-bab progress", e)
            throw e
        }
    }

    suspend fun getLessonById(lessonId: String): Lesson? {
        return try {
            val l = lessonsCollection.document(lessonId).get().await().toObject(Lesson::class.java)
            if (l == null) Log.w(TAG, "Lesson not found for id=$lessonId")
            l
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching lesson $lessonId", e)
            null
        }
    }

    suspend fun getSubBabById(subBabId: String): com.adika.learnable.model.SubBab? {
        return try {
            val subBab = subBabCollection.document(subBabId).get().await()
                .toObject(com.adika.learnable.model.SubBab::class.java)
            if (subBab == null) Log.w(TAG, "SubBab not found for id=$subBabId")
            subBab
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching subBab $subBabId", e)
            null
        }
    }

    suspend fun markMaterialAsCompleted(
        studentId: String,
        subBabId: String,
        lessonId: String,
        materialType: String
    ) {
        try {

            val query = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {

                val existingDoc = query.documents[0]
                val currentProgress = existingDoc.toObject(StudentSubBabProgress::class.java)

                if (currentProgress != null) {

                    val updatedCompletedMaterials =
                        currentProgress.completedMaterials.toMutableMap().apply {
                            put(materialType, true)
                        }

                    val isCompleted = updatedCompletedMaterials.all { it.value }

                    val updatedProgress = currentProgress.copy(
                        completedMaterials = updatedCompletedMaterials,
                        isCompleted = isCompleted,
                        lastActivityDate = Timestamp.now()
                    )
                    existingDoc.reference.set(updatedProgress).await()

                    updateLessonProgress(studentId, lessonId)

                    if (isCompleted && !currentProgress.isCompleted) {
                        triggerSubBabCompletionNotification(subBabId, lessonId)

                        createSubBabCompletionNotification(studentId, subBabId, lessonId)
                    }
                }
            } else {

                val initialCompletedMaterials = mapOf(
                    MATERIAL_PDF to (materialType == MATERIAL_PDF),
                    MATERIAL_VIDEO to (materialType == MATERIAL_VIDEO),
                    MATERIAL_QUIZ to (materialType == MATERIAL_QUIZ)
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

                updateLessonProgress(studentId, lessonId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error marking material as completed", e)
            throw e
        }
    }

    suspend fun updateQuizScore(
        studentId: String,
        subBabId: String,
        lessonId: String,
        score: Float
    ) {
        try {

            val query = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {

                val existingDoc = query.documents[0]
                val currentProgress = existingDoc.toObject(StudentSubBabProgress::class.java)

                if (currentProgress != null) {

                    val updatedCompletedMaterials =
                        currentProgress.completedMaterials.toMutableMap().apply {
                            put(MATERIAL_QUIZ, true)
                        }

                    val isCompleted = updatedCompletedMaterials.all { it.value }

                    val updatedProgress = currentProgress.copy(
                        quizScore = score,
                        completedMaterials = updatedCompletedMaterials,
                        isCompleted = isCompleted,
                        lastActivityDate = Timestamp.now()
                    )
                    existingDoc.reference.set(updatedProgress).await()

                    updateLessonProgress(studentId, lessonId)

                    if (isCompleted && !currentProgress.isCompleted) {
                        triggerSubBabCompletionNotification(subBabId, lessonId)

                        createSubBabCompletionNotification(studentId, subBabId, lessonId)
                    }
                }
            } else {

                val initialCompletedMaterials = mapOf(
                    MATERIAL_PDF to false,
                    MATERIAL_VIDEO to false,
                    MATERIAL_QUIZ to true
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

                updateLessonProgress(studentId, lessonId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating quiz score", e)
            throw e
        }
    }

    suspend fun updateTimeSpent(
        studentId: String,
        subBabId: String,
        lessonId: String,
        seconds: Int
    ) {
        try {

            val query = subBabProgressCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            if (query.documents.isNotEmpty()) {

                val existingDoc = query.documents[0]
                val currentProgress = existingDoc.toObject(StudentSubBabProgress::class.java)

                if (currentProgress != null) {
                    val updatedProgress = currentProgress.copy(
                        timeSpent = currentProgress.timeSpent + seconds,
                        lastActivityDate = Timestamp.now()
                    )
                    existingDoc.reference.set(updatedProgress).await()

                    updateLessonProgress(studentId, lessonId)
                }
            } else {

                val initialCompletedMaterials = mapOf(
                    MATERIAL_PDF to false,
                    MATERIAL_VIDEO to false,
                    MATERIAL_QUIZ to false
                )

                val docRef = subBabProgressCollection.document()
                val newProgress = StudentSubBabProgress(
                    id = docRef.id,
                    studentId = studentId,
                    subBabId = subBabId,
                    lessonId = lessonId,
                    completedMaterials = initialCompletedMaterials,
                    quizScore = 0f,
                    timeSpent = seconds,
                    lastActivityDate = Timestamp.now(),
                    isCompleted = false
                )
                docRef.set(newProgress).await()

                updateLessonProgress(studentId, lessonId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating time spent", e)
            throw e
        }
    }

    private fun calculateStreak(activityDates: List<Timestamp>): Int {
        if (activityDates.isEmpty()) return 0

        val dates = activityDates.map { it.toDate() }.sorted()

        var streak = 0
        val calendar = Calendar.getInstance()
        val today = calendar.time

        calendar.time = today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        var currentDate = calendar.time

        val hasActivityToday = dates.any {
            val activityDate = Calendar.getInstance().apply { time = it }
            activityDate.get(Calendar.YEAR) == calendar.get(Calendar.YEAR) &&
                    activityDate.get(Calendar.DAY_OF_YEAR) == calendar.get(Calendar.DAY_OF_YEAR)
        }

        if (!hasActivityToday) {

            calendar.add(Calendar.DAY_OF_MONTH, -1)
            currentDate = calendar.time
        }

        while (true) {
            val hasActivityOnDate = dates.any {
                val activityDate = Calendar.getInstance().apply { time = it }
                val checkDate = Calendar.getInstance().apply { time = currentDate }
                activityDate.get(Calendar.YEAR) == checkDate.get(Calendar.YEAR) &&
                        activityDate.get(Calendar.DAY_OF_YEAR) == checkDate.get(Calendar.DAY_OF_YEAR)
            }

            if (hasActivityOnDate) {
                streak++
                calendar.add(Calendar.DAY_OF_MONTH, -1)
                currentDate = calendar.time
            } else {
                break
            }
        }

        return streak
    }

    /**
     * Calculate quiz average from a list of quiz scores
     * Filters out invalid scores (0 or negative) and calculates the average
     */
    private fun calculateQuizAverage(quizScores: List<Float>): Float {
        val validScores = quizScores.filter { it > 0f }
        return if (validScores.isNotEmpty()) {
            validScores.average().toFloat()
        } else {
            0f
        }
    }

    /**
     * Calculate quiz average from StudentSubBabProgress list
     */
    private fun calculateQuizAverageFromSubBabProgress(subBabProgressList: List<StudentSubBabProgress>): Float {
        val quizScores = subBabProgressList
            .filter { it.quizScore > 0f }
            .map { it.quizScore }
        return calculateQuizAverage(quizScores)
    }

    /**
     * Calculate quiz average from StudentLessonProgress list
     */
    private fun calculateQuizAverageFromLessonProgress(lessonProgressList: List<StudentLessonProgress>): Float {
        val quizScores = lessonProgressList
            .flatMap { it.quizScores }
            .filter { it > 0f }
        return calculateQuizAverage(quizScores)
    }

    /**
     * Trigger notification when a subbab is completed
     */
    private fun triggerSubBabCompletionNotification(subBabId: String, lessonId: String) {
        try {
            val intent = Intent(context, LearningProgressReceiver::class.java).apply {
                action = LearningProgressReceiver.ACTION_SUBBAB_COMPLETED
                putExtra(LearningProgressReceiver.EXTRA_SUBBAB_ID, subBabId)
                putExtra(LearningProgressReceiver.EXTRA_LESSON_ID, lessonId)
            }
            context.sendBroadcast(intent)
            Log.d(TAG, "SubBab completion notification triggered for: $subBabId")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering subbab completion notification", e)
        }
    }

    /**
     * Create notification in database when subbab is completed
     */
    private suspend fun createSubBabCompletionNotification(
        studentId: String,
        subBabId: String,
        lessonId: String
    ) {
        try {

            val lesson = getLessonById(lessonId)
            val subBab = getSubBabById(subBabId)

            val lessonTitle = lesson?.title ?: context.getString(R.string.lesson)
            val subBabTitle = subBab?.title ?: context.getString(R.string.subbab)
            val schoolLevel = formatSchoolLevel(context, lesson?.schoolLevel)

            val message = context.getString(
                R.string.lesson_completion_message,
                lessonTitle,
                schoolLevel,
                subBabTitle
            )

            notificationRepository.createNotification(
                userId = studentId,
                title = "Penyelesaian SubBab",
                message = message,
                type = "subbab_completion",
                data = mapOf(
                    "subBabId" to subBabId,
                    "lessonId" to lessonId,
                    "subBabTitle" to subBabTitle,
                    "lessonTitle" to lessonTitle
                )
            )

            Log.d(TAG, "SubBab completion notification created in database")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating subbab completion notification", e)
        }
    }
} 