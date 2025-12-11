package com.adika.learnable.repository

import android.util.Log
import com.adika.learnable.api.SendEmailService
import com.adika.learnable.model.User
import com.adika.learnable.model.UserRegistrationRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAdminRepository @Inject constructor(
    firestore: FirebaseFirestore,
    private val emailService: SendEmailService
) {
    private val usersCollection = firestore.collection("users")
    private val TAG = "AdminRepository"

    suspend fun getParentAndTeacherUsers(): List<User> {
        try {
            Log.d(TAG, "Fetching parent and teacher users")

            val parentQuery = usersCollection
                .whereEqualTo("role", "parent")
                .get()
                .await()

            val teacherQuery = usersCollection
                .whereEqualTo("role", "teacher")
                .get()
                .await()

            val parentUsers = parentQuery.toObjects(User::class.java)
            val teacherUsers = teacherQuery.toObjects(User::class.java)

            val allUsers = (parentUsers + teacherUsers).sortedByDescending { it.createdAt }

            Log.d(
                TAG,
                "Found ${parentUsers.size} parent users and ${teacherUsers.size} teacher users"
            )
            return allUsers

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching parent and teacher users", e)
            throw e
        }
    }

    suspend fun searchParentAndTeacherUsers(query: String): List<User> {
        try {
            Log.d(TAG, "Searching parent and teacher users with query: $query")

            val allUsers = getParentAndTeacherUsers()

            val filteredUsers = allUsers.filter { user ->
                user.name.contains(query, ignoreCase = true)
            }

            Log.d(TAG, "Found ${filteredUsers.size} users matching query: $query")
            return filteredUsers

        } catch (e: Exception) {
            Log.e(TAG, "Error searching parent and teacher users", e)
            throw e
        }
    }

    suspend fun getParentAndTeacherUsersByStatus(isApproved: Boolean): List<User> {
        try {
            Log.d(TAG, "Fetching parent and teacher users with approval status: $isApproved")

            val allUsers = getParentAndTeacherUsers()
            val filteredUsers = allUsers.filter { it.isApproved == isApproved }

            Log.d(TAG, "Found ${filteredUsers.size} users with approval status: $isApproved")
            return filteredUsers

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users by approval status", e)
            throw e
        }
    }

    suspend fun getUsersByRole(role: String): List<User> {
        try {
            Log.d(TAG, "Fetching users with role: $role")

            if (role !in listOf("parent", "teacher")) {
                throw IllegalArgumentException("Role must be 'parent' or 'teacher'")
            }

            val query = usersCollection
                .whereEqualTo("role", role)
                .get()
                .await()

            val users = query.toObjects(User::class.java).sortedByDescending { it.createdAt }

            Log.d(TAG, "Found ${users.size} users with role: $role")
            return users

        } catch (e: Exception) {
            Log.e(TAG, "Error fetching users by role", e)
            throw e
        }
    }

    private suspend fun sendAdminNotificationApprove(name: String, email: String, role: String) {
        val normalizedRole = role.trim().lowercase()

        var delayMs = 400L
        repeat(3) { attempt ->
            try {
                val body = UserRegistrationRequest(
                    name = name,
                    email = email,
                    role = normalizedRole
                )

                val resp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    emailService.sendEmailApproveUser(body)
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

            if (attempt < 2) {
                kotlinx.coroutines.delay(delayMs)
                delayMs *= 2
            }
        }

        Log.w(TAG, "Notifikasi admin gagal setelah retry, lanjutkan proses tanpa blokir.")
    }

    private suspend fun sendAdminNotificationReject(name: String, email: String, role: String) {
        val normalizedRole = role.trim().lowercase()

        var delayMs = 400L
        repeat(3) { attempt ->
            try {
                val body = UserRegistrationRequest(
                    name = name,
                    email = email,
                    role = normalizedRole
                )

                val resp = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    emailService.sendEmailRejectUser(body)
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

            if (attempt < 2) {
                kotlinx.coroutines.delay(delayMs)
                delayMs *= 2
            }
        }

        Log.w(TAG, "Notifikasi admin gagal setelah retry, lanjutkan proses tanpa blokir.")
    }

    suspend fun approveUser(userId: String): Boolean {
        try {
            Log.d(TAG, "Approving user with ID: $userId")

            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists()) {
                Log.e(TAG, "User not found with ID: $userId")
                return false
            }

            val user = userDoc.toObject(User::class.java)
            if (user == null) {
                Log.e(TAG, "Failed to parse user data for ID: $userId")
                return false
            }

            usersCollection.document(userId).update(
                mapOf(
                    "approved" to true,
                    "approvedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()

            sendAdminNotificationApprove(user.name, user.email, user.role ?: "")

            Log.d(TAG, "User approved successfully: ${user.name}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error approving user with ID: $userId", e)
            throw e
        }
    }

    suspend fun rejectUser(userId: String): Boolean {
        try {
            Log.d(TAG, "Rejecting user with ID: $userId")

            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists()) {
                Log.e(TAG, "User not found with ID: $userId")
                return false
            }

            val user = userDoc.toObject(User::class.java)
            if (user == null) {
                Log.e(TAG, "Failed to parse user data for ID: $userId")
                return false
            }

            usersCollection.document(userId).update(
                mapOf(
                    "approved" to false,
                    "rejectedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                )
            ).await()

            sendAdminNotificationReject(user.name, user.email, user.role ?: "")

            Log.d(TAG, "User rejected successfully: ${user.name}")
            return true

        } catch (e: Exception) {
            Log.e(TAG, "Error rejecting user with ID: $userId", e)
            throw e
        }
    }

    suspend fun getUserById(userId: String): User? {
        try {
            Log.d(TAG, "Getting user by ID: $userId")

            val userDoc = usersCollection.document(userId).get().await()
            if (!userDoc.exists()) {
                Log.e(TAG, "User not found with ID: $userId")
                return null
            }

            val user = userDoc.toObject(User::class.java)
            Log.d(TAG, "User found: ${user?.name}")
            return user

        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by ID: $userId", e)
            throw e
        }
    }
}