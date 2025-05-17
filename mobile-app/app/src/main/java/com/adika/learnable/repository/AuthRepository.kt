package com.adika.learnable.repository

import android.content.Context
import com.adika.learnable.model.User
import com.adika.learnable.util.ErrorMessages
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) {
    private val usersCollection = firestore.collection("users")

    suspend fun sendEmailVerification() {
        try {
            if (!isEmailVerified()) {
                auth.currentUser?.sendEmailVerification()?.await()
                throw Exception(ErrorMessages.getVerifyEmailSent(context))
            } else {
                throw Exception(ErrorMessages.getVerifyEmail(context))
            }
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getVerifyEmailFailed(context))
        }
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    fun getCurrentUserId(): String {
        return auth.currentUser?.uid ?: throw Exception(ErrorMessages.getAuthFailed(context))
    }

    suspend fun resetPassword(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e.message))
        }
    }

    suspend fun signUpWithEmailAndPassword(name: String, email: String, password: String): User {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        val user = result.user ?: throw Exception(ErrorMessages.getSignupFailed(context))

        val profileUpdates = UserProfileChangeRequest.Builder()
            .setDisplayName(name)
            .build()
        user.updateProfile(profileUpdates).await()

        val newUser = User(
            id = user.uid,
            email = email,
            name = name,
            role = "student",
            disabilityType = null,
            createdAt = Timestamp.now()
        )

        usersCollection.document(user.uid).set(newUser).await()
        sendEmailVerification()
        return newUser
    }

    suspend fun loginUser(email: String, password: String): User {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            return getUserData(result.user?.uid ?: throw Exception(ErrorMessages.getAuthFailed(context)))
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun signInWithGoogle(idToken: String): User {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        val user = result.user ?: throw Exception(ErrorMessages.getAuthFailed(context))

        val userDoc = usersCollection.document(user.uid).get().await()
        if (!userDoc.exists()) {
            val newUser = User(
                id = user.uid,
                email = user.email ?: "",
                name = user.displayName ?: "",
                role = "student", // Default role
                profilePicture = user.photoUrl.toString(),
                disabilityType = null
            )
            usersCollection.document(user.uid).set(newUser).await()
            return newUser
        }

        return userDoc.toObject(User::class.java) ?: throw Exception("Gagal mendapatkan data user")
    }

    suspend fun getUserData(userId: String): User {
        try {
            val userDoc = usersCollection.document(userId).get().await()
            return userDoc.toObject(User::class.java) ?: throw Exception(ErrorMessages.getAuthFailed(context))
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e.message))
        }
    }

    fun signOut() {
        auth.signOut()
    }
}