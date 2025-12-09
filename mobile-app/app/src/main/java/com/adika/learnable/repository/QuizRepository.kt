package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.model.Quiz
import com.adika.learnable.model.QuizResult
import com.google.firebase.firestore.FirebaseFirestore
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

    suspend fun getStudentQuizResults(studentId: String, subBabId: String): List<QuizResult> {
        try {
            val resultsSnapshot = quizResultCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("subBabId", subBabId)
                .orderBy("completedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .await()

            return resultsSnapshot.toObjects(QuizResult::class.java)
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error getting quiz results", e)
            throw e
        }
    }

    suspend fun createQuiz(quiz: Quiz) {
        try {
            val docRef = quizCollection.document()
            val quizWithId = quiz.copy(id = docRef.id)
            docRef.set(quizWithId).await()
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error creating quiz", e)
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

    suspend fun deleteQuiz(quizId: String) {
        try {
            quizCollection.document(quizId).delete().await()
        } catch (e: Exception) {
            Log.e("QuizRepository", "Error deleting quiz", e)
            throw e
        }
    }
} 