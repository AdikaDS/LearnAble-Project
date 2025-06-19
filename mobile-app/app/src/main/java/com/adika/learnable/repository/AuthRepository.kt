package com.adika.learnable.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.adika.learnable.R
import com.adika.learnable.model.User
import com.adika.learnable.util.EmailService
import com.adika.learnable.util.ErrorMessages
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    private val emailService: EmailService,
    @ApplicationContext private val context: Context
) {
    private val usersCollection = firestore.collection("users")
    private val TAG = "AuthRepository"

    suspend fun sendEmailVerification() {
        try {
            auth.currentUser?.sendEmailVerification()?.await()
            return
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getVerifyEmailFailed(context))
        }
    }

    fun isEmailVerified(): Boolean {
        return auth.currentUser?.isEmailVerified ?: false
    }

    fun getCurrentUserId(): String {
        val userId = auth.currentUser?.uid
        Log.d("AuthRepository", "Current user ID: $userId")
        return userId ?: throw Exception(ErrorMessages.getAuthFailed(context))
    }

    suspend fun resetPassword(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e.message))
        }
    }

    private suspend fun sendAdminNotificationIfNeeded(name: String, email: String, role: String) {
        if (role == "parent" || role == "teacher") {
            try {
                Log.d(TAG, "Sending admin notification for $role: $name ($email)")
                emailService.sendAdminNotification(name, email, role)
                    .onSuccess { Log.d(TAG, "Admin notification sent successfully") }
                    .onFailure { Log.e(TAG, "Failed to send admin notification", it) }
            } catch (e: Exception) {
                Log.e(TAG, "Error sending admin notification", e)
                // We don't throw the exception here to prevent blocking the registration process
            }
        }
    }

    suspend fun signUpWithEmailAndPassword(
        name: String,
        email: String,
        password: String,
        ttl: String,
        role: String
    ): User {
        try {
            Log.d(TAG, "Starting sign up process for user: $email with role: $role")
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception(ErrorMessages.getSignupFailed(context))

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build()
            user.updateProfile(profileUpdates).await()

            val newUser = User(
                id = user.uid,
                ttl = ttl,
                email = email,
                name = name,
                role = role
            )

            usersCollection.document(user.uid).set(newUser).await()
            sendAdminNotificationIfNeeded(name, email, role)
            sendEmailVerification()
            
            Log.d(TAG, "Sign up successful for user: $email")
            return newUser
        } catch (e: Exception) {
            Log.e(TAG, "Sign up failed for user: $email", e)
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e.message))
        }
    }

    fun observeUserData(userId: String): Flow<User> = callbackFlow {
        val docRef = usersCollection.document(userId)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                if (user != null) {
                    trySend(user).isSuccess
                }
            }
        }

        awaitClose { listener.remove() }
    }

    suspend fun loginUser(email: String, password: String): User {
        try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            return getUserData(
                result.user?.uid ?: throw Exception(
                    ErrorMessages.getAuthFailed(
                        context
                    )
                )
            )
        } catch (e: Exception) {
            throw Exception(e.message)
        }
    }

    suspend fun signInWithGoogle(idToken: String, role: String): User {
        try {
            Log.d(TAG, "Starting Google sign in process with role: $role")
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception(ErrorMessages.getAuthFailed(context))

            val userDoc = usersCollection.document(user.uid).get().await()
            if (!userDoc.exists()) {
                // Ini adalah pendaftaran baru
                Log.d(TAG, "Creating new user document for Google sign in")
                val newUser = User(
                    id = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "",
                    role = role,
                    profilePicture = user.photoUrl.toString()
                )
                usersCollection.document(user.uid).set(newUser).await()
                
                // Kirim notifikasi ke admin hanya untuk pendaftaran baru
                user.email?.let { email ->
                    user.displayName?.let { name ->
                        sendAdminNotificationIfNeeded(name, email, role)
                    }
                }
                
                return newUser
            } else {
                // Ini adalah login untuk user yang sudah ada
                Log.d(TAG, "Google sign in successful for existing user")
                return userDoc.toObject(User::class.java)
                    ?: throw Exception(context.getString(R.string.fail_load_user_data))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e.message))
        }
    }

    suspend fun getFirebaseUserIdFromToken(idToken: String): String {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user?.uid ?: throw Exception("User ID tidak ditemukan")
    }


    suspend fun getUserData(userId: String): User {
        try {
            Log.d("AuthRepository", "Fetching user data for ID: $userId")
            val userDoc = usersCollection.document(userId).get().await()

            if (!userDoc.exists()) {
                Log.e("AuthRepository", "User document does not exist for ID: $userId")
                throw Exception(ErrorMessages.getAuthFailed(context))
            }

            val user = userDoc.toObject(User::class.java)
            Log.d("AuthRepository", "User data retrieved: $user")

            if (user == null) {
                Log.e("AuthRepository", "Failed to convert document to User object")
                throw Exception(ErrorMessages.getAuthFailed(context))
            }

            return user
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error getting user data", e)
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e.message))
        }
    }

    suspend fun signOut() {
        try {
            if (auth.currentUser != null) {
                auth.signOut()
                clearAllUserData()
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error during sign out", e)
            throw e
        }
    }

    private suspend fun clearAllUserData() {
        try {
            // Clear any stored credentials
            val clearRequest = ClearCredentialStateRequest()
            val credentialManager = CredentialManager.create(context)
            credentialManager.clearCredentialState(clearRequest)

            // Clear all shared preferences
            context.getSharedPreferences("student_dashboard", Context.MODE_PRIVATE).edit { clear() }
            context.getSharedPreferences("teacher_dashboard", Context.MODE_PRIVATE).edit { clear() }
            context.getSharedPreferences("parent_dashboard", Context.MODE_PRIVATE).edit { clear() }
//            context.getSharedPreferences("user_preferences", Context.MODE_PRIVATE).edit { clear() }
//            context.getSharedPreferences("auth_preferences", Context.MODE_PRIVATE).edit { clear() }

        } catch (e: Exception) {
            Log.e("AuthRepository", "Error clearing user data", e)
            throw e
        }
    }
}