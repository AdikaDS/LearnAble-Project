package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.model.Notification
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationRepository @Inject constructor(
    private val auth: FirebaseAuth,
    firestore: FirebaseFirestore
) {
    companion object {
        private const val TAG = "NotificationRepository"
    }

    private val notificationsCollection = firestore.collection("notifications")

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
    }

    suspend fun getNotifications(userId: String): List<Notification> {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val base = doc.toObject(Notification::class.java) ?: return@mapNotNull null
                val effectiveIsRead =
                    doc.getBoolean("isRead") ?: doc.getBoolean("read") ?: base.isRead
                base.copy(id = doc.id, isRead = effectiveIsRead)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting notifications", e)
            emptyList()
        }
    }

    suspend fun markAsRead(notificationId: String) {
        try {
            notificationsCollection.document(notificationId)
                .update("read", true)
                .await()
            Log.d(TAG, "Notification marked as read: $notificationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking notification as read", e)
        }
    }

    suspend fun getUnreadCount(userId: String): Int {
        return try {
            val snapshot = notificationsCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("read", false)
                .get()
                .await()

            snapshot.size()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count", e)
            0
        }
    }

    suspend fun createNotification(
        userId: String,
        title: String,
        message: String,
        type: String,
        data: Map<String, Any> = emptyMap()
    ) {
        try {
            val docRef = notificationsCollection.document()
            val notification = Notification(
                id = docRef.id,
                userId = userId,
                title = title,
                message = message,
                type = type,
                isRead = false,
                createdAt = Timestamp.now(),
                data = data
            )

            docRef.set(notification).await()
            Log.d(TAG, "Notification created for user: $userId")
        } catch (e: Exception) {
            Log.e(TAG, "Error creating notification", e)
        }
    }
}
