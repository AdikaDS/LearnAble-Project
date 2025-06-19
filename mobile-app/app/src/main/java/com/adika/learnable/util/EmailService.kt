package com.adika.learnable.util

import android.util.Log
import com.adika.learnable.api.EmailJSService
import com.adika.learnable.model.EmailJSRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailService @Inject constructor(
    private val emailJSService: EmailJSService
) {
    companion object {
        private const val TAG = "EmailService"
        // EmailJS Configuration
        private const val EMAILJS_PUBLIC_KEY = "NO3Oi2Oej2xFNUHr_"
        private const val EMAILJS_SERVICE_ID = "service_3zotwxq"
        private const val EMAILJS_TEMPLATE_ID = "template_urytd4h"
        
        private val VALID_ROLES = setOf("parent", "teacher")
    }

    suspend fun sendAdminNotification(userName: String, userEmail: String, userRole: String): Result<String> {
        return try {
            if (userRole !in VALID_ROLES) {
                Log.w(TAG, "Invalid role: $userRole")
                return Result.failure(Exception("Role tidak valid: $userRole"))
            }

            Log.d(TAG, "Sending admin notification for $userRole: $userName ($userEmail)")
            
            val templateParams = mapOf(
                "user_name" to userName,
                "user_email" to userEmail,
                "user_role" to userRole,
                "message" to when (userRole) {
                    "teacher" -> "Seorang guru baru telah mendaftar dan memerlukan verifikasi"
                    "parent" -> "Seorang orang tua baru telah mendaftar dan memerlukan verifikasi"
                    else -> "Pengguna baru telah mendaftar dan memerlukan verifikasi"
                }
            )

            val request = EmailJSRequest(
                serviceId = EMAILJS_SERVICE_ID,
                templateId = EMAILJS_TEMPLATE_ID,
                userId = EMAILJS_PUBLIC_KEY,
                templateParams = templateParams
            )

            val response = emailJSService.sendEmail(request)

            if (response.isSuccessful) {
                Log.d(TAG, "Admin notification sent successfully")
                Result.success("Notifikasi berhasil dikirim ke admin")
            } else {
                val errorMessage = "Gagal kirim: ${response.errorBody()?.string()}"
                Log.e(TAG, errorMessage)
                Result.failure(Exception(errorMessage))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending admin notification", e)
            Result.failure(e)
        }
    }
}