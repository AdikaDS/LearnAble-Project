package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.model.SubBab
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubBabRepository @Inject constructor(
    firestore: FirebaseFirestore
) {
    private val collection = firestore.collection("sub_bab")

    suspend fun getSubBab (subBabId: String) : SubBab {
        try {
            val document = collection
                .document(subBabId)
                .get()
                .await()

            val subBab = document.toObject(SubBab::class.java)
            return subBab!!
        } catch (e: Exception) {
            Log.e("SubBabRepository", "Error getting sub-babs", e)
            throw e
        }
    }

    suspend fun getSubBabByLesson(lessonId: String): List<SubBab> {
        try {
            Log.d("SubBabRepository", "Querying sub-babs for lesson $lessonId")
            val subBabSnapshot = collection
                .whereEqualTo("lessonId", lessonId)
                .get()
                .await()
            
            val subBab = subBabSnapshot.toObjects(SubBab::class.java)

            return subBab
        } catch (e: Exception) {
            Log.e("SubBabRepository", "Error getting sub-babs for lesson $lessonId", e)
            throw e
        }
    }

    suspend fun addSubBab(subBab: SubBab) : SubBab {
        try {
            val docRef = collection.document()
            val subBabWithId = subBab.copy(id = docRef.id)
            docRef.set(subBabWithId).await()
            return subBabWithId
        } catch (e: Exception) {
            Log.e("SubBabRepository", "Error adding sub-bab", e)
            throw e
        }
    }


    suspend fun updateSubBab(subBab: SubBab) {
        try {
            // Ensure lessonId is preserved
            collection.document(subBab.id).set(subBab).await()
            Log.d("SubBabRepository", "Successfully updated sub-bab ${subBab.id}")
        } catch (e: Exception) {
            Log.e("SubBabRepository", "Error updating sub-bab", e)
            throw e
        }
    }

    suspend fun deleteSubBab(subBabId: String) {
        try {
            collection.document(subBabId).delete().await()
            Log.d("SubBabRepository", "Successfully deleted sub-bab $subBabId")
        } catch (e: Exception) {
            Log.e("SubBabRepository", "Error deleting sub-bab", e)
            throw e
        }
    }

    suspend fun deleteAllSubBabsForLesson(lessonId: String) {
        try {
            val subBabs = getSubBabByLesson(lessonId)
            val batch = collection.firestore.batch()

            subBabs.forEach { subBab ->
                val docRef = collection.document(subBab.id)
                batch.delete(docRef)
            }

            batch.commit().await()
            Log.d("SubBabRepository", "Successfully deleted all sub-babs for lesson $lessonId")
        } catch (e: Exception) {
            Log.e("SubBabRepository", "Error deleting all sub-babs for lesson $lessonId", e)
            throw e
        }
    }
}