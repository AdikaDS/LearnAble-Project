package com.adika.learnable.view

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.Credential
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.lifecycleScope
import com.adika.learnable.R
import com.adika.learnable.databinding.ActivitySignupBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    // Pesan validasi
    private val nameRequired by lazy { getString(R.string.name_required) }
    private val emailRequired by lazy { getString(R.string.email_required) }
    private val emailInvalid by lazy { getString(R.string.email_invalid) }
    private val passwordRequired by lazy { getString(R.string.password_required) }
    private val passwordMinLength by lazy { getString(R.string.password_min_length, MIN_PASSWORD_LENGTH) }
    private val passwordHaveUppercase by lazy { getString(R.string.password_have_uppercase) }
    private val passwordHaveLowercase by lazy { getString(R.string.password_have_lowercase) }
    private val passwordHaveNumber by lazy { getString(R.string.password_have_number) }
    private val passwordHaveSpecialChar by lazy { getString(R.string.password_have_special_char) }

    // Pesan error Firebase
    private val emailBadFormat by lazy { getString(R.string.email_bad_format) }
    private val passwordInvalid by lazy { getString(R.string.password_invalid) }
    private val emailExist by lazy { getString(R.string.email_exist) }
    private val networkError by lazy { getString(R.string.network_error) }
    private val invalidCredential by lazy { getString(R.string.invalid_credential) }
    private val tooManyAttempts by lazy { getString(R.string.too_many_attempts) }
    private val userDisabled by lazy { getString(R.string.user_disabled) }

    // Pesan umum
    private val verifyEmail by lazy { getString(R.string.verify_email) }
    private val verifiyEmailSent by lazy { getString(R.string.verify_email_sent) }
    private val verifyEmailFailed by lazy { getString(R.string.verify_email_failed) }
    private val authFailed by lazy { getString(R.string.auth_failed) }
    private val signupFailed by lazy { getString(R.string.signup_failed) }
    private val signupSuccess by lazy { getString(R.string.signup_success) }
    private val profileUpdateFailed by lazy { getString(R.string.profile_update_failed) }
    private val credentialError by lazy { getString(R.string.credential_error) }

    private companion object {
        private const val TAG = "SignUpActivity"
        private const val MIN_PASSWORD_LENGTH = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(baseContext)

        setupClickListeners()
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            if (currentUser.isEmailVerified) {
                navigateToMain()
            } else {
                showToast(verifyEmail)
                auth.signOut()
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnSignUp.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validateInputs(name, email, password)) {
                showLoading(false)
                return@setOnClickListener
            }

            showLoading(true)
            binding.btnSignUp.isEnabled = false
            binding.btnGoogleSignIn.isEnabled = false
            signUpUser(name, email, password)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            showLoading(true)
            binding.btnSignUp.isEnabled = false
            binding.btnGoogleSignIn.isEnabled = false
            launchCredentialManager()
        }

        binding.tvLogin.setOnClickListener {
            finish() // Return to LoginActivity
        }
    }

    private fun validatePassword(password: String): Boolean {
        if (password.isEmpty()) {
            binding.etPassword.error = passwordRequired
            binding.etPassword.requestFocus()
            return false
        }
        if (password.length < 8) {
            binding.etPassword.error = passwordMinLength
            binding.etPassword.requestFocus()
            return false
        }
        if (!password.any { it.isUpperCase() }) {
            binding.etPassword.error = passwordHaveUppercase
            binding.etPassword.requestFocus()
            return false
        }
        if (!password.any { it.isLowerCase() }) {
            binding.etPassword.error = passwordHaveLowercase
            binding.etPassword.requestFocus()
            return false
        }
        if (!password.any { it.isDigit() }) {
            binding.etPassword.error = passwordHaveNumber
            binding.etPassword.requestFocus()
            return false
        }
        if (!password.any { "!@#\$%^&*()-_=+[{]}|;:',<.>/?".contains(it) }) {
            binding.etPassword.error = passwordHaveSpecialChar
            binding.etPassword.requestFocus()
            return false
        }

        return true
    }

    private fun validateInputs(name: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = nameRequired
            binding.etName.requestFocus()
            return false
        }

        if (email.isEmpty()) {
            binding.etEmail.error = emailRequired
            binding.etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = emailInvalid
            binding.etEmail.requestFocus()
            return false
        }

        if (!validatePassword(password)) {
            return false
        }

        return true
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSignUp.isEnabled = !isLoading
        binding.btnGoogleSignIn.isEnabled = !isLoading
    }

    private fun launchCredentialManager() {
        val googleIdOption = GetGoogleIdOption.Builder()
            .setServerClientId(getString(R.string.web_client_id))
            .setFilterByAuthorizedAccounts(false)
            .build()

        val request = GetCredentialRequest.Builder()
            .addCredentialOption(googleIdOption)
            .build()

        lifecycleScope.launch {
            try {
                val result = credentialManager.getCredential(
                    context = this@SignUpActivity,
                    request = request
                )
                handleSignIn(result.credential)
            } catch (e: GetCredentialException) {
                Log.e(TAG, credentialError, e)
                showLoading(false)
            }
        }
    }

    private fun handleSignIn(credential: Credential) {
        if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            firebaseAuthWithGoogle(googleIdTokenCredential.idToken)
        } else {
            Log.w(TAG, "Kredensial bukan tipe Google ID!")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Simpan data ke Firestore
                        val userData = hashMapOf(
                            "displayName" to user.displayName,
                            "email" to user.email,
                            "uid" to user.uid,
                            "photoUrl" to (user.photoUrl?.toString() ?: ""),
                            "createdAt" to System.currentTimeMillis()
                        )

                        val firestore = FirebaseFirestore.getInstance()
                        firestore.collection("users").document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                Log.d(TAG, "Data Google user berhasil disimpan ke Firestore")
                                navigateToMain()
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Gagal menyimpan data Google user: ${e.message}")
                                showToast("Gagal menyimpan data profil")
                            }
                    }
                } else {
                    showToast("$authFailed: ${getErrorMessage(task.exception?.message)}")
                }
            }
    }

    private fun signUpUser(name: String, email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    sendEmailVerification()
                    updateUserProfileAndSaveToFirestore(name, email)
                } else {
                    showLoading(false)
                    showToast(getErrorMessage(task.exception?.message))
                }
            }
    }

    private fun updateUserProfileAndSaveToFirestore(fullName: String, email: String) {
        showLoading(true)

        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val user = auth.currentUser

        if (user != null) {
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()

            user.updateProfile(profileUpdates)
                .addOnCompleteListener { profileTask ->
                    if (profileTask.isSuccessful) {
                        val userData = hashMapOf(
                            "displayName" to fullName,
                            "email" to email,
                            "uid" to user.uid,
                            "createdAt" to System.currentTimeMillis()
                        )

                        firestore.collection("users").document(user.uid)
                            .set(userData)
                            .addOnSuccessListener {
                                showToast(signupSuccess)
                                auth.signOut()
                                navigateToLogin()
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                showToast("Gagal menyimpan data: ${e.message}")
                            }
                    } else {
                        showLoading(false)
                        showToast("$profileUpdateFailed: ${profileTask.exception?.message}")
                    }
                }
        } else {
            showLoading(false)
            showToast("User tidak ditemukan")
        }
    }

    private fun sendEmailVerification() {
        val user = auth.currentUser!!
        user.sendEmailVerification()
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    showToast(verifiyEmailSent)
                } else {
                    showToast(verifyEmailFailed)
                }
            }
    }

    private fun getErrorMessage(errorMessage: String?): String {
        return when (errorMessage) {
            "The email address is badly formatted." -> emailBadFormat
            "The password is invalid or the user does not have a password." -> passwordInvalid
            "The email address is already in use by another account." -> emailExist
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> networkError
            "The supplied auth credential is incorrect, malformed or has expired." -> invalidCredential
            "We have blocked all requests from this device due to unusual activity. Try again later." -> tooManyAttempts
            "The user account has been disabled by an administrator." -> userDisabled
            else -> "$signupFailed: $errorMessage"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finishAffinity() // Close all activities in the stack
    }

    private fun navigateToLogin() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
} 