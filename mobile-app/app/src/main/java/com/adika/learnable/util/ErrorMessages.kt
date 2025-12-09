package com.adika.learnable.util

import android.content.Context
import com.adika.learnable.R

object ErrorMessages {
    // Pesan error Firebase
    private fun getEmailBadFormat(context: Context) = context.getString(R.string.email_bad_format)
    private fun getPasswordInvalid(context: Context) = context.getString(R.string.password_invalid)
    private fun getEmailExist(context: Context) = context.getString(R.string.email_exist)
    private fun getNetworkError(context: Context) = context.getString(R.string.network_error)
    private fun getInvalidCredential(context: Context) = context.getString(R.string.invalid_credential)
    private fun getTooManyAttempts(context: Context) = context.getString(R.string.too_many_attempts)
    private fun getUserDisabled(context: Context) = context.getString(R.string.user_disabled)

    // Pesan umum
    fun getVerifyEmail(context: Context) = context.getString(R.string.verify_email)
    fun getVerifyEmailSent(context: Context) = context.getString(R.string.verify_email_sent)
    fun getVerifyEmailFailed(context: Context) = context.getString(R.string.verify_email_failed)
    fun getAuthFailed(context: Context) = context.getString(R.string.auth_failed)
    fun getSignupFailed(context: Context) = context.getString(R.string.signup_failed)
    fun getProfileUpdateFailed(context: Context) = context.getString(R.string.profile_update_failed)
    fun getPasswordUpdateFailed(context: Context) = context.getString(R.string.password_update_failed)

    // Helper function untuk menangani error Firebase berbasis pesan (legacy, exact match)
    fun getFirebaseErrorMessage(context: Context, errorMessage: String?): String {
        return when (errorMessage) {
            "The email address is badly formatted." -> getEmailBadFormat(context)
            "The password is invalid or the user does not have a password." -> getInvalidCredential(context)
            "There is no user record corresponding to this identifier." -> getInvalidCredential(context)
            "The email address is already in use by another account." -> getEmailExist(context)
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> getNetworkError(context)
            "The user account has been disabled by an administrator." -> getUserDisabled(context)
            "Too many unsuccessful login attempts. Please try again later." -> getTooManyAttempts(context)
            "The credential is invalid or has expired." -> getInvalidCredential(context)
            // Tambahan error message yang sering muncul
            "INVALID_LOGIN_CREDENTIALS" -> getInvalidCredential(context)
            "INVALID_PASSWORD" -> getInvalidCredential(context)
            "USER_NOT_FOUND" -> getInvalidCredential(context)
            else -> getAuthFailed(context)
        }
    }

    // Versi yang direkomendasikan: mapping berdasarkan tipe dan kode error Firebase
    fun getFirebaseErrorMessage(context: Context, throwable: Throwable?): String {
        if (throwable == null) return getAuthFailed(context)

        // Hindari dependency keras pada Firebase di layer util: cek via class name & errorCode via refleksi
        val clazz = throwable::class.java.name

        // FirebaseNetworkException
        if (clazz == "com.google.firebase.FirebaseNetworkException") {
            return getNetworkError(context)
        }

        // FirebaseTooManyRequestsException
        if (clazz == "com.google.firebase.auth.FirebaseTooManyRequestsException") {
            return getTooManyAttempts(context)
        }

        // FirebaseAuthException (memiliki field errorCode)
        if (throwable is com.google.firebase.auth.FirebaseAuthException) {
            return when (throwable.errorCode) {
                // Email/password login common
                "ERROR_INVALID_EMAIL",
                "ERROR_WRONG_PASSWORD",
                "ERROR_INVALID_CREDENTIAL",
                "ERROR_USER_NOT_FOUND" -> getInvalidCredential(context)

                "ERROR_USER_DISABLED" -> getUserDisabled(context)
                "ERROR_TOO_MANY_REQUESTS" -> getTooManyAttempts(context)
                // Password reset action code errors
                "ERROR_INVALID_ACTION_CODE" -> context.getString(R.string.reset_code_invalid)
                "ERROR_EXPIRED_ACTION_CODE" -> context.getString(R.string.reset_code_expired)
                // New password validation during confirm
                "ERROR_WEAK_PASSWORD" -> context.getString(R.string.password_too_weak)
                else -> getAuthFailed(context)
            }
        }

        // Fallback ke pengecekan string bila bukan exception Firebase yang dikenal
        val message = throwable.message
        return getFirebaseErrorMessage(context, message)
    }
}