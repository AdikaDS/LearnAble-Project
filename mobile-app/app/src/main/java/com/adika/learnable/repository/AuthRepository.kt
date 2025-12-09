package com.adika.learnable.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import com.adika.learnable.R
import com.adika.learnable.api.SendEmailService
import com.adika.learnable.model.User
import com.adika.learnable.model.UserRegistrationRequest
import com.adika.learnable.util.ErrorMessages
import com.adika.learnable.util.GoogleSignInResult
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
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
    private val emailService: SendEmailService,
    @ApplicationContext private val context: Context
) {
    private val usersCollection = firestore.collection("users")
    private val TAG = "AuthRepository"

    suspend fun checkAndNotifyAfterEmailVerification() {
        val firebaseUser = auth.currentUser ?: throw Exception(ErrorMessages.getAuthFailed(context))
        firebaseUser.reload().await() // penting: refresh status
        val isVerified = firebaseUser.isEmailVerified
        val uid = firebaseUser.uid

        val docRef = usersCollection.document(uid)
        val snap = docRef.get().await()
        if (!snap.exists()) throw Exception(ErrorMessages.getAuthFailed(context))

        val role = (snap.getString("role") ?: "").trim().lowercase()
        val name = snap.getString("name") ?: (firebaseUser.displayName ?: "")
        val email = snap.getString("email") ?: (firebaseUser.email ?: "")
        val adminNotified = snap.getBoolean("adminNotified") ?: false

        // Simpan mirror status (opsional, berguna untuk UI)
        docRef.update("emailVerified", isVerified).await()

        if (isVerified && !adminNotified && (role == "parent" || role == "teacher")) {
            // Kirim hanya sekali
            sendAdminNotificationIfNeeded(name, email, role)
            docRef.update(
                mapOf(
                    "adminNotified" to true,
                    "adminNotifiedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        }
    }

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
            val hostingDomain = "https://learnable-22a3b.firebaseapp.com"
            val settings = ActionCodeSettings.newBuilder()
                .setUrl("$hostingDomain/continue")
                .setHandleCodeInApp(true)
                .setAndroidPackageName(context.packageName, /* p1 = */true, /* p2 = */null)
                .build()

            auth.setLanguageCode("id")
            auth.sendPasswordResetEmail(email, settings).await()
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e))
        }
    }

    /**
     * Verifikasi oobCode dari link email reset.
     * Return: email milik akun yang akan di-reset.
     */
    suspend fun verifyResetCode(oobCode: String): String {
        return try {
            auth.verifyPasswordResetCode(oobCode).await()
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e))
        }
    }

    /**
     * Konfirmasi password baru dengan oobCode yang valid.
     */
    suspend fun confirmPasswordReset(oobCode: String, newPassword: String) {
        try {
            auth.confirmPasswordReset(oobCode, newPassword).await()
        } catch (e: Exception) {
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e))
        }
    }

    private suspend fun sendAdminNotificationIfNeeded(name: String, email: String, role: String) {
        val normalizedRole = role.trim().lowercase()
        if (normalizedRole != "parent" && normalizedRole != "teacher") {
            Log.d(TAG, "Role '$role' tidak perlu notifikasi admin, skip.")
            return
        }

        // Retry ringan 3x dengan exponential backoff
        var delayMs = 400L
        repeat(3) { attempt ->
            try {
                // Panggil endpoint backend (POST /email-admin-verification)
                val body = UserRegistrationRequest(
                    name = name,
                    email = email,
                    role = normalizedRole
                )

                // Jalankan di IO dispatcher
                val resp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    emailService.sendEmailToAdmin(body)
                }

                if (resp.isSuccessful) {
                    val data = resp.body()
                    Log.i(TAG, "Notifikasi admin OK: status=${data?.status}, msg=${data?.message}")
                    return // sukses, hentikan retry
                } else {
                    val err = resp.errorBody()?.string()
                    Log.w(TAG, "Gagal kirim notifikasi (HTTP ${resp.code()}): $err")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception kirim notifikasi admin (attempt ${attempt + 1})", e)
            }

            // Kalau belum sukses dan masih ada jatah retry, tunda dulu
            if (attempt < 2) {
                kotlinx.coroutines.delay(delayMs)
                delayMs *= 2
            }
        }

        // Jangan lempar error—biarkan proses pendaftaran tetap lanjut
        Log.w(TAG, "Notifikasi admin gagal setelah retry, lanjutkan proses tanpa blokir.")
    }

    suspend fun signUpWithEmailAndPassword(
        name: String,
        email: String,
        password: String,
        role: String,
        nomorInduk: String? = null
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
                email = email,
                name = name,
                role = role,
                nomorInduk = nomorInduk,
                profileCompleted = true
            )

            usersCollection.document(user.uid).set(newUser).await()
            usersCollection.document(user.uid).update(
                mapOf(
                    "adminNotified" to false,
                    "emailVerified" to false
                )
            ).await()

            sendEmailVerification()

            Log.d(TAG, "Sign up successful for user: $email with profileCompleted: true")
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
            // Re-throw exception dengan message asli dari Firebase
            throw e
        }
    }

    suspend fun signInWithGoogle(idToken: String): GoogleSignInResult {
        try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception(ErrorMessages.getAuthFailed(context))

            user.reload().await()
            val isVerified = user.isEmailVerified

            val docRef = usersCollection.document(user.uid)
            val userDoc = docRef.get().await()

            if (!userDoc.exists()) {
                // Ini adalah pendaftaran baru
                Log.d(TAG, "Creating new user document for Google sign in")
                val newUser = User(
                    id = user.uid,
                    email = user.email ?: "",
                    name = user.displayName ?: "",
                    role = null,
                    profilePicture = user.photoUrl.toString(),
                    nomorInduk = null,
                    profileCompleted = false
                )
                docRef.set(newUser).await()

                docRef.update(
                    mapOf(
                        "adminNotified" to false,
                        "emailVerified" to isVerified
                    )
                ).await()

                return GoogleSignInResult.NeedsMoreData(newUser, listOf("role"))

            } else {
                // user sudah ada
                val existing = userDoc.toObject(User::class.java)
                    ?: throw Exception(context.getString(R.string.fail_load_user_data))


                // cek kelengkapan
                val required = mutableListOf<String>()
                if (existing.role.isNullOrBlank()) {
                    required += "role"
                } else {
                    when (existing.role.lowercase()) {
                        "student" -> if (existing.nomorInduk.isNullOrBlank()) required += "nomorInduk"
                        "teacher" -> if (existing.nomorInduk.isNullOrBlank())  required += "nomorInduk"
                    }
                }

                return if (required.isEmpty()) {
                    // Lengkap → tandai completed jika belum
                    if (!existing.profileCompleted) {
                        docRef.update("profileCompleted", true).await()
                        Log.d(TAG, "Profile marked as completed for user: ${existing.email}")
                    }
                    GoogleSignInResult.Success(existing.copy(profileCompleted = true))
                } else {
                    // Masih butuh data tambahan
                    if (existing.profileCompleted) {
                        docRef.update("profileCompleted", false).await()
                        Log.d(TAG, "Profile marked as incomplete for user: ${existing.email}, required: $required")
                    }
                    GoogleSignInResult.NeedsMoreData(existing.copy(profileCompleted = false), required)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google sign in failed", e)
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e))
        }
    }

    suspend fun completeAdditionalData(
        uid: String,
        role: String,
        nomorInduk: String? = null
    ): User {
        val docRef = usersCollection.document(uid)
        val updates = mutableMapOf<String, Any>(
            "role" to role,
            "profileCompleted" to true
        )

        when (role.lowercase()) {
            "student" -> {
                require(!nomorInduk.isNullOrBlank()) { context.getString(R.string.nisn_required) }
                updates["nomorInduk"] = nomorInduk
            }
            "teacher" -> {
                require(!nomorInduk.isNullOrBlank()) { context.getString(R.string.nip_required) }
                updates["nomorInduk"] = nomorInduk
            }
            "parent" -> {
                // Parent tidak memerlukan nomor induk
            }
        }

        docRef.update(updates).await()

        // Notify admin setelah role diketahui (parent/teacher)
        if (role.equals("parent", true) || role.equals("teacher", true)) {
            val latest = docRef.get().await().toObject(User::class.java)
            if (latest != null) {
                sendAdminNotificationIfNeeded(latest.name, latest.email, role)
                docRef.update(
                    mapOf(
                        "adminNotified" to true,
                        "adminNotifiedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
        }
        
        // Ambil data user yang sudah di-update
        val updatedUser = docRef.get().await().toObject(User::class.java)
            ?: throw Exception(context.getString(R.string.fail_load_user_data))
            
        Log.d(TAG, "Complete additional data successful for user: ${updatedUser.email}, role: ${updatedUser.role}, profileCompleted: ${updatedUser.profileCompleted}")
        
        return updatedUser
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
            throw Exception(ErrorMessages.getFirebaseErrorMessage(context, e))
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