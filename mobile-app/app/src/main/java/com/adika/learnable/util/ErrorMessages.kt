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

    // Helper function untuk menangani error Firebase
    fun getFirebaseErrorMessage(context: Context, errorMessage: String?): String {
        return when (errorMessage) {
            "The email address is badly formatted." -> getEmailBadFormat(context)
            "The password is invalid or the user does not have a password." -> getPasswordInvalid(context)
            "There is no user record corresponding to this identifier." -> getEmailExist(context)
            "The email address is already in use by another account." -> getEmailExist(context)
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> getNetworkError(context)
            "The user account has been disabled by an administrator." -> getUserDisabled(context)
            "Too many unsuccessful login attempts. Please try again later." -> getTooManyAttempts(context)
            "The credential is invalid or has expired." -> getInvalidCredential(context)
            else -> getAuthFailed(context)
        }
    }
}