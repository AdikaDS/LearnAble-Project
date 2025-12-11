package com.adika.learnable.repository

import com.adika.learnable.model.SubBab
import com.adika.learnable.model.VideoRecommendation
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudentDashboardRepository @Inject constructor(
    firestore: FirebaseFirestore
) {
    private val subBabCollection = firestore.collection("sub_bab")
    private val lessonsCollection = firestore.collection("lessons")

    suspend fun getRandomVideoRecommendations(limit: Int = 3): List<VideoRecommendation> {

        val subBabSnapshot = subBabCollection.get().await()
        val subBabs = subBabSnapshot.documents.mapNotNull { doc ->
            val model = doc.toObject(SubBab::class.java)
            model?.copy(id = model.id.ifBlank { doc.id })
        }.filter { sub ->
            val videoUrl = sub.mediaUrls["video"] ?: ""
            videoUrl.isNotBlank()
        }

        if (subBabs.isEmpty()) return emptyList()

        val picked = subBabs.shuffled().take(limit)

        val lessonIds = picked.map { it.lessonId }.toSet()
        val lessonTitleById = mutableMapOf<String, String>()
        for (id in lessonIds) {
            val doc = lessonsCollection.document(id).get().await()
            lessonTitleById[id] = doc.getString("title") ?: ""
        }

        return picked.map { sub ->
            val subtitle = lessonTitleById[sub.lessonId] ?: ""
            VideoRecommendation(
                id = sub.id,
                title = sub.title,
                subtitle = subtitle,
                videoUrl = sub.mediaUrls["video"] ?: ""
            )
        }
    }
}