package com.adika.learnable.util

import android.util.Log
import com.adika.learnable.api.ResendEmailService
import com.adika.learnable.model.ResendEmailRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmailService @Inject constructor(
    private val resendEmailService: ResendEmailService
) {

    companion object {
        private const val TAG = "EmailService"
        private const val FROM_EMAIL = "noreply@resend.dev"
    }

    suspend fun sendAdminNotification(userName: String, userEmail: String, userRole: String): Result<String> {
        return try {
            val message = when (userRole) {
                "teacher" -> "Seorang guru baru telah mendaftar dan memerlukan verifikasi"
                "parent" -> "Seorang orang tua baru telah mendaftar dan memerlukan verifikasi"
                else -> "Pengguna baru telah mendaftar dan memerlukan verifikasi"
            }

            val request = ResendEmailRequest(
                from = FROM_EMAIL,
                to = "learnableapplication@gmail.com",
                subject = "Permintaan Persetujuan Akun Baru $userRole",
                html = """
                    <p><strong>Nama:</strong> $userName</p>
                    <p><strong>Email:</strong> $userEmail</p>
                    <p><strong>Role:</strong> $userRole</p>
                    <p>$message</p>
                """.trimIndent()
            )
            val apiKey = "Bearer re_9QwskMuU_P4diSZgSFt2sK5tiycjHPXqY"
            val response = resendEmailService.sendEmail(apiKey, request)

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