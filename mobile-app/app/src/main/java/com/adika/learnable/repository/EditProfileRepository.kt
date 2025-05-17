package com.adika.learnable.repository

import android.content.Context
import android.net.Uri
import com.adika.learnable.api.ApiConfig
import com.adika.learnable.model.ImgurResponse
import com.adika.learnable.model.User
import com.adika.learnable.util.ErrorMessages
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EditProfileRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
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
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e.message))
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

    fun uploadToImgur(uri: Uri, callback: (Result<String>) -> Unit) {
        try {
            // Convert Uri to File
            val inputStream = context.contentResolver.openInputStream(uri)
            val file = File.createTempFile("upload_", ".jpg", context.cacheDir)
            inputStream?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Create multipart request
            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

            // Upload to Imgur
            ApiConfig.imgurService.uploadImage("Client-ID ${ApiConfig.IMGUR_CLIENT_ID}", imagePart)
                .enqueue(object : Callback<ImgurResponse> {
                    override fun onResponse(
                        call: Call<ImgurResponse>,
                        response: Response<ImgurResponse>
                    ) {
                        if (response.isSuccessful) {
                            val imageUrl = response.body()?.data?.link
                            if (imageUrl != null) {
                                callback(Result.success(imageUrl))
                            } else {
                                callback(Result.failure(Exception("Gagal mendapatkan URL gambar")))
                            }
                        } else {
                            callback(Result.failure(Exception("Gagal upload gambar: ${response.code()}")))
                        }
                    }

                    override fun onFailure(call: Call<ImgurResponse>, t: Throwable) {
                        callback(Result.failure(Exception("Gagal upload gambar: ${t.message}")))
                    }
                })
        } catch (e: Exception) {
            callback(Result.failure(e))
        }
    }

} 