package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.model.Quiz
import com.adika.learnable.model.QuizResult
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuizRepository @Inject constructor(
    firestore: FirebaseFirestore
) {
    private val quizCollection = firestore.collection("quiz")
    private val quizResultCollection = firestore.collection("quiz_results")

    suspend fun getQuizBySubBabId(subBabId: String): Quiz? {
        try {
            val quizSnapshot = quizCollection
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            return if (quizSnapshot.documents.isNotEmpty()) {
                quizSnapshot.documents[0].toObject(Quiz::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error getting quiz", e)
            throw e
        }
    }

    suspend fun saveQuizResult(result: QuizResult) {
        try {
            val docRef = quizResultCollection.document()
            val resultWithId = result.copy(id = docRef.id)
            docRef.set(resultWithId).await()
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error saving quiz result", e)
            throw e
        }
    }

    suspend fun updateQuizResult(result: QuizResult) {
        try {
            quizResultCollection.document(result.id).set(result).await()
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error updating quiz result", e)
            throw e
        }
    }

    suspend fun getStudentQuizResults(studentId: String, subBabId: String): List<QuizResult> {
        try {
            val resultsSnapshot = quizResultCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            return resultsSnapshot.toObjects(QuizResult::class.java)
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error getting quiz results", e)
            throw e
        }
    }

    suspend fun getStudentQuizResult(studentId: String, subBabId: String): QuizResult? {
        try {
            val resultsSnapshot = quizResultCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            return if (resultsSnapshot.documents.isNotEmpty()) {
                resultsSnapshot.documents[0].toObject(QuizResult::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error getting quiz result", e)
            throw e
        }
    }

    suspend fun createQuiz(quiz: Quiz) {
        try {
            Log.d("QuizRepository", "Creating quiz in Firebase")
            Log.d("QuizRepository", "Quiz subBabId: ${quiz.subBabId}")
            Log.d("QuizRepository", "Quiz title: ${quiz.title}")
            Log.d("QuizRepository", "Quiz passingScore: ${quiz.passingScore}")
            Log.d("QuizRepository", "Quiz questions count: ${quiz.questions.size}")

            val docRef = quizCollection.document()
            val quizWithId = quiz.copy(id = docRef.id)

            Log.d("QuizRepository", "Quiz ID generated: ${quizWithId.id}")
            Log.d("QuizRepository", "Saving to Firestore document: ${docRef.path}")

            docRef.set(quizWithId).await()

            Log.d(
                "QuizRepository",
                "Quiz successfully saved to Firestore with ID: ${quizWithId.id}"
            )
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error creating quiz", e)
            Log.e("QuizRepository", "Error message: ${e.message}")
            Log.e("QuizRepository", "Error stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    suspend fun updateQuiz(quiz: Quiz) {
        try {
            quizCollection.document(quiz.id).set(quiz).await()
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error updating quiz", e)
            throw e
        }
    }

    suspend fun deleteQuizBySubBabId(subBabId: String) {
        try {
            val quizSnapshot = quizCollection
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            quizSnapshot.documents.forEach { doc ->
                doc.reference.delete().await()
            }
            Log.d("QuizRepository", "Successfully deleted quiz for subBabId: $subBabId")
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error deleting quiz by subBabId", e)
            throw e
        }
    }

    suspend fun deleteAllQuizzesForSubBabs(subBabIds: List<String>) {
        try {
            subBabIds.forEach { subBabId ->
                runCatching {
                    deleteQuizBySubBabId(subBabId)
                }
            }
            Log.d("QuizRepository", "Successfully deleted all quizzes for subBabs")
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error deleting all quizzes for subBabs", e)
            throw e
        }
    }

    suspend fun getAllQuizResultsBySubBabId(subBabId: String): List<QuizResult> {
        try {
            Log.d("QuizRepository", "getAllQuizResultsBySubBabId called with subBabId: $subBabId")
            val resultsSnapshot = quizResultCollection
                .whereEqualTo("subBabId", subBabId)
                .orderBy("completedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val results = resultsSnapshot.toObjects(QuizResult::class.java)
            Log.d("QuizRepository", "Found ${results.size} quiz results for subBabId: $subBabId")

            if (results.isEmpty()) {
                Log.w("QuizRepository", "No quiz results found for subBabId: $subBabId")
                // Try without orderBy to see if that's the issue
                val resultsWithoutOrder = quizResultCollection
                    .whereEqualTo("subBabId", subBabId)
                    .get()
                    .await()
                val results2 = resultsWithoutOrder.toObjects(QuizResult::class.java)
                Log.d("QuizRepository", "Without orderBy: Found ${results2.size} quiz results")
            }

            return results
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error getting all quiz results for subBabId: $subBabId", e)
            Log.e("QuizRepository", "Error message: ${e.message}")
            Log.e("QuizRepository", "Error stack trace: ${e.stackTraceToString()}")
            throw e
        }
    }

    suspend fun getQuizResultById(resultId: String): QuizResult? {
        try {
            val docSnapshot = quizResultCollection.document(resultId).get().await()
            return if (docSnapshot.exists()) {
                docSnapshot.toObject(QuizResult::class.java)
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error getting quiz result by ID", e)
            throw e
        }
    }
} 