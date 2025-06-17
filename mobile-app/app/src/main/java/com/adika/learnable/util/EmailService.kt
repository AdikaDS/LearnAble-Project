package com.adika.learnable.util

import com.adika.learnable.api.EmailJSService
import com.adika.learnable.model.EmailJSRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailService @Inject constructor(
    private val emailJSService: EmailJSService
) {
    companion object {
        private const val EMAILJS_PUBLIC_KEY = "NO3Oi2Oej2xFNUHr_" // Replace with your EmailJS public key
        private const val EMAILJS_SERVICE_ID = "service_3zotwxq" // Replace with your EmailJS service ID
        private const val EMAILJS_TEMPLATE_ID = "template_urytd4h" // Replace with your EmailJS template ID
    }

    suspend fun sendAdminNotification(userName: String, userEmail: String, userRole: String): Result<String> {
        return try {
            val templateParams = mapOf(
                "user_name" to userName,
                "user_email" to userEmail,
                "user_role" to userRole
            )

            val request = EmailJSRequest(
                serviceId = EMAILJS_SERVICE_ID,
                templateId = EMAILJS_TEMPLATE_ID,
                userId = EMAILJS_PUBLIC_KEY,
                templateParams = templateParams
            )

            val response = emailJSService.sendEmail(request)

            if (response.isSuccessful) {
                Result.success("Notifikasi berhasil dikirim ke admin")
            } else {
                Result.failure(Exception("Gagal kirim: ${response.errorBody()?.string()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}