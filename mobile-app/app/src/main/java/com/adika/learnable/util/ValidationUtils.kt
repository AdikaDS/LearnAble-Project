package com.adika.learnable.util

import android.content.Context
import android.util.Patterns
import android.widget.EditText
import com.adika.learnable.R
import com.google.android.material.textfield.TextInputLayout

object ValidationUtils {

    fun validateInputsData(
        context: Context,
        inputLayout: TextInputLayout,
        editText: EditText,
        fieldType: FieldType
    ): Boolean {
        return if (editText.text.isNullOrBlank()) {
            inputLayout.error = context.getString(fieldType.errorResId)
            false
        } else {
            inputLayout.error = null
            true
        }
    }

    enum class FieldType(val errorResId: Int) {
        TITLE(R.string.title_required),
        CONTENT(R.string.content_required),
        DURATION(R.string.duration_required),
        SUBJECT(R.string.subject_required),
        SCHOOL_LEVEL(R.string.school_level_required),
        DIFFICULTY(R.string.difficulty_required)
    }

    fun validateName(context: Context, name: String): ValidationResult {
        return when {
            name.isBlank() -> ValidationResult.Invalid(context.getString(R.string.name_required))
            else -> ValidationResult.Valid
        }
    }

    fun validateEmail(context: Context, email: String): ValidationResult {
        return when {
            email.isBlank() -> ValidationResult.Invalid(context.getString(R.string.email_required))
            !isValidEmail(email) -> ValidationResult.Invalid(context.getString(R.string.email_invalid))
            else -> ValidationResult.Valid
        }
    }

    private fun validatePhone(context: Context, phone: String): ValidationResult {
        return when {
            phone.isBlank() -> ValidationResult.Invalid(context.getString(R.string.phone_required))
            !isValidPhone(phone) -> ValidationResult.Invalid(context.getString(R.string.phone_invalid))
            else -> ValidationResult.Valid
        }
    }

    fun validatePasswordLogin(context: Context, password: String): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Invalid(context.getString(R.string.password_required))
            else -> ValidationResult.Valid
        }
    }

    fun validateNISN(context: Context, nisn: String): ValidationResult {
        return when {
            nisn.isBlank() -> ValidationResult.Invalid(context.getString(R.string.nisn_required))
            !isNISN(nisn) -> ValidationResult.Invalid(context.getString(R.string.nisn_invalid))
            else -> ValidationResult.Valid
        }
    }

    fun validateRole(context: Context, role: String): ValidationResult {
        return when {
            role.isBlank() -> ValidationResult.Invalid(context.getString(R.string.role_required))
            else -> ValidationResult.Valid
        }
    }

    fun validateNIP(context: Context, nip: String): ValidationResult {
        return when {
            nip.isBlank() -> ValidationResult.Invalid(context.getString(R.string.nip_required))
            !isNIP(nip) -> ValidationResult.Invalid(context.getString(R.string.nip_invalid))
            else -> ValidationResult.Valid
        }
    }

    fun validatePassword(
        context: Context,
        password: String,
        minLength: Int = 8,
        requireUppercase: Boolean = true,
        requireLowercase: Boolean = true,
        requireNumber: Boolean = true,
        requireSpecialChar: Boolean = true
    ): ValidationResult {
        return when {
            password.isBlank() -> ValidationResult.Invalid(context.getString(R.string.password_required))
            password.length < minLength -> ValidationResult.Invalid(
                context.getString(R.string.password_min_length, minLength)
            )

            requireUppercase && !password.any { it.isUpperCase() } ->
                ValidationResult.Invalid(context.getString(R.string.password_have_uppercase))

            requireLowercase && !password.any { it.isLowerCase() } ->
                ValidationResult.Invalid(context.getString(R.string.password_have_lowercase))

            requireNumber && !password.any { it.isDigit() } ->
                ValidationResult.Invalid(context.getString(R.string.password_have_number))

            requireSpecialChar && !password.any { "!@#\$%^&*()-_=+[{]}|;:',<.>/?".contains(it) } ->
                ValidationResult.Invalid(context.getString(R.string.password_have_special_char))

            else -> ValidationResult.Valid
        }
    }

    fun validateConfirmPassword(
        context: Context,
        password: String,
        confirmPassword: String
    ): ValidationResult {
        return when {
            confirmPassword.isBlank() -> ValidationResult.Invalid(context.getString(R.string.password_required))
            password != confirmPassword -> ValidationResult.Invalid(context.getString(R.string.password_match))
            else -> ValidationResult.Valid
        }
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        val phoneRegex = "^(0|\\+62)[0-9]{9,12}$"
        return phone.matches(phoneRegex.toRegex())
    }

    private fun isNISN(nisn: String): Boolean {
        return nisn.length == 10 && nisn.all { it.isDigit() }
    }

    private fun isNIP(nip: String): Boolean {
        return nip.length in 8..18 && nip.all { it.isDigit() }
    }
}