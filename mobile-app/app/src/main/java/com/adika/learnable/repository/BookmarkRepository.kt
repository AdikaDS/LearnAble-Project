package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.model.Bookmark
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkRepository @Inject constructor(
    firestore: FirebaseFirestore
) {
    private val bookmarkCollection = firestore.collection("bookmarks")

    suspend fun getBookmarksByStudentId(studentId: String): List<Bookmark> {
        return try {
            val snapshot = bookmarkCollection
                .whereEqualTo("studentId", studentId)
                .orderBy("lastAccessedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.toObjects(Bookmark::class.java)
        } catch (e: Exception) {
            Log.e("BookmarkRepository", "Error getting bookmarks", e)
            emptyList()
        }
    }

    suspend fun addBookmark(bookmark: Bookmark): Result<String> {
        return try {
            val docRef = bookmarkCollection.document()
            val bookmarkWithId = bookmark.copy(id = docRef.id)
            docRef.set(bookmarkWithId).await()
            Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e("BookmarkRepository", "Error adding bookmark", e)
            Result.failure(e)
        }
    }

    suspend fun removeBookmark(bookmarkId: String): Result<Unit> {
        return try {
            bookmarkCollection.document(bookmarkId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("BookmarkRepository", "Error removing bookmark", e)
            Result.failure(e)
        }
    }

    suspend fun isBookmarked(studentId: String, lessonId: String, subBabId: String): Boolean {
        return try {
            val snapshot = bookmarkCollection
                .whereEqualTo("studentId", studentId)
                .whereEqualTo("lessonId", lessonId)
                .whereEqualTo("subBabId", subBabId)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e("BookmarkRepository", "Error checking bookmark status", e)
            false
        }
    }
}