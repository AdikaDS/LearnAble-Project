package com.adika.learnable.repository

import android.content.Context
import android.util.Log
import com.adika.learnable.BuildConfig
import com.adika.learnable.model.User
import com.adika.learnable.util.ErrorMessages
import com.amazonaws.services.s3.AmazonS3Client
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditProfileRepository @Inject constructor(
    private val auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    private val s3Client: AmazonS3Client,
    @ApplicationContext private val context: Context
) {
    private val usersCollection = firestore.collection("users")

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw Exception(ErrorMessages.getAuthFailed(context))
    }

    suspend fun getUserData(userId: String): User {
        try {
            val userDoc = usersCollection.document(userId).get().await()
            return userDoc.toObject(User::class.java)
                ?: throw Exception(ErrorMessages.getAuthFailed(context))
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e))
        }
    }

    suspend fun updateUserData(user: User) {
        try {
            usersCollection.document(user.id).set(user).await()
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getProfileUpdateFailed(context))
        }
    }

    suspend fun updatePassword(currentPassword: String, newPassword: String) {
        try {
            val user = auth.currentUser ?: throw Exception(ErrorMessages.getAuthFailed(context))

            // Reauthenticate user before changing password
            val credential = EmailAuthProvider
                .getCredential(user.email!!, currentPassword)
            user.reauthenticate(credential).await()

            // Update password
            user.updatePassword(newPassword).await()
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getPasswordUpdateFailed(context))
        }
    }

    private fun generateUniqueObjectKey(originalFileName: String): String {
        val sanitizedFileName = originalFileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return "pictureProfile/$sanitizedFileName"
    }

    private fun generateS3Url(objectKey: String): String {
        return try {
            val url = s3Client.getUrl(BuildConfig.S3_BUCKET_NAME, objectKey).toString()
            Log.d("EditProfileRepository", "Generated S3 URL: $url")
            url
        } catch (e: Exception) {
            Log.e("EditProfileRepository", "Error generating S3 URL", e)
            throw Exception("Gagal generate URL: ${e.message}")
        }
    }

    private suspend fun uploadToS3(file: File, objectKey: String) {
        return withContext(Dispatchers.IO) {
            try {
                s3Client.putObject(BuildConfig.S3_BUCKET_NAME, objectKey, file)
                Log.d("EditProfileRepository", "S3 upload completed successfully")
            } catch (e: Exception) {
                Log.e("EditProfileRepository", "S3 upload failed", e)
                throw Exception("Gagal upload ke S3: ${e.message}")
            }
        }
    }

    private suspend fun deleteFromS3(objectKey: String) {
        return withContext(Dispatchers.IO) {
            try {
                s3Client.deleteObject(BuildConfig.S3_BUCKET_NAME, objectKey)
                Log.d("EditProfileRepository", "S3 delete completed successfully")
            } catch (e: Exception) {
                Log.e("EditProfileRepository", "S3 delete failed", e)
                throw Exception("Gagal delete dari S3: ${e.message}")
            }
        }
    }

    private suspend fun getExistingObjectKey(userId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val userDoc = usersCollection.document(userId).get().await()
                userDoc.toObject(User::class.java)?.profilePicture
            } catch (e: Exception) {
                Log.e("EditProfileRepository", "Gagal memuat object lama", e)
                null
            }
        }
    }

    private fun extractObjectKeyFromUrl(url: String): String {
        return url.substringAfter(".amazonaws.com/")
    }

    suspend fun uploadProfilePicture(
        file: File,
        userId: String
    ): String {
        // Validasi format file dan ukuran
        val fileExtension = file.name.substringAfterLast('.', "").lowercase()
        if (fileExtension !in listOf("jpg", "jpeg", "png", "webp")) {
            throw IllegalArgumentException("Format file tidak didukung. Hanya mendukung: jpg, jpeg, png, webp.")
        }

        val maxSizeBytes = 5 * 1024 * 1024 // 5MB
        if (file.length() > maxSizeBytes) {
            throw IllegalArgumentException("Ukuran file terlalu besar. Maksimal 5MB.")
        }

        val oldUrl = getExistingObjectKey(userId)
        val oldObjectKey = oldUrl?.let { extractObjectKeyFromUrl(it) }
        if (!oldObjectKey.isNullOrEmpty()) deleteFromS3(oldObjectKey)

        val objectKey = generateUniqueObjectKey(file.name)
        uploadToS3(file, objectKey)

        val url = generateS3Url(objectKey)

        val user = getUserData(userId).copy(profilePicture = url)
        updateUserData(user)
        return url
    }
}