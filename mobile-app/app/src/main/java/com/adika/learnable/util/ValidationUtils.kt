package com.adika.learnable.util

import android.content.Context
import android.util.Patterns
import android.widget.EditText
import com.adika.learnable.R
import com.adika.learnable.model.User
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

    fun validateDisabilitySelection(
        context: Context,
        inputLayout: TextInputLayout,
        selectedTag: Any?,
        fieldType: FieldType
    ): Boolean {
        return if ((selectedTag as? List<*>)?.isEmpty() != false) {
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
        DISABILITY(R.string.type_disability_required),
        SUBJECT(R.string.subject_required),
        SCHOOL_LEVEL(R.string.school_level_required),
        DIFFICULTY(R.string.difficulty_required)
    }

    private fun validateName(context: Context, name: String): ValidationResult {
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

    private fun validateConfirmPassword(
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

    fun validateUserData(context: Context, user: User): ValidationResult {
        val nameValidation = validateName(context, user.name)
        if (nameValidation is ValidationResult.Invalid) return nameValidation

        val emailValidation = validateEmail(context, user.email)
        if (emailValidation is ValidationResult.Invalid) return emailValidation

        val phoneValidation = validatePhone(context, user.phone)
        if (phoneValidation is ValidationResult.Invalid) return phoneValidation

        return ValidationResult.Valid
    }


    fun validateLoginData(
        context: Context,
        email: String,
        password: String,
        minPasswordLength: Int = 8
    ): ValidationResult {
        val emailValidation = validateEmail(context, email)
        if (emailValidation is ValidationResult.Invalid) return emailValidation

        val passwordValidation = validatePassword(context, password, minPasswordLength)
        if (passwordValidation is ValidationResult.Invalid) return passwordValidation

        return ValidationResult.Valid
    }

    fun validateSignupData(
        context: Context,
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        minPasswordLength: Int = 8
    ): ValidationResult {
        val nameValidation = validateName(context, name)
        if (nameValidation is ValidationResult.Invalid) return nameValidation

        val emailValidation = validateEmail(context, email)
        if (emailValidation is ValidationResult.Invalid) return emailValidation

        val passwordValidation = validatePassword(context, password, minPasswordLength)
        if (passwordValidation is ValidationResult.Invalid) return passwordValidation

        val confirmPasswordValidation = validateConfirmPassword(context, password, confirmPassword)
        if (confirmPasswordValidation is ValidationResult.Invalid) return confirmPasswordValidation

        return ValidationResult.Valid
    }

    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isValidPhone(phone: String): Boolean {
        val phoneRegex = "^(0|\\+62)[0-9]{9,12}$"
        return phone.matches(phoneRegex.toRegex())
    }
}