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
import com.adika.learnable.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var db: FirebaseFirestore

    // Pesan validasi
    private val emailRequired by lazy { getString(R.string.email_required) }
    private val emailInvalid by lazy { getString(R.string.email_invalid) }
    private val passwordRequired by lazy { getString(R.string.password_required) }
    private val passwordMinLength by lazy { getString(R.string.password_min_length, MIN_PASSWORD_LENGTH) }

    // Pesan error Firebase
    private val emailBadFormat by lazy { getString(R.string.email_bad_format) }
    private val passwordInvalid by lazy { getString(R.string.password_invalid) }
    private val emailDosentExist by lazy { getString(R.string.email_doesnt_exist) }
    private val networkError by lazy { getString(R.string.network_error) }
    private val invalidCredential by lazy { getString(R.string.invalid_credential) }
    private val tooManyAttempts by lazy { getString(R.string.too_many_attempts) }
    private val userDisabled by lazy { getString(R.string.user_disabled) }
    private val loginFailed by lazy { getString(R.string.login_failed) }

    // Pesan umum
    private val verifyEmail by lazy { getString(R.string.verify_email) }
    private val authFailed by lazy { getString(R.string.auth_failed) }
    private val credentialError by lazy { getString(R.string.credential_error) }

    private companion object {
        private const val TAG = "LoginActivity"
        private const val MIN_PASSWORD_LENGTH = 8
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(baseContext)

        setupClickListeners()
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
        if (currentUser != null) {
            navigateToMain()
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!validateInputs(email, password)) {
                showLoading(false)
                return@setOnClickListener
            }

            showLoading(true)
            binding.btnLogin.isEnabled = false
            binding.btnGoogleSignIn.isEnabled = false
            loginUser(email, password)
        }

        binding.btnGoogleSignIn.setOnClickListener {
            showLoading(true)
            binding.btnLogin.isEnabled = false
            binding.btnGoogleSignIn.isEnabled = false
            launchCredentialManager()
        }

        binding.tvSignUp.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
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

        return true
    }

    private fun validateInputs(email: String, password: String): Boolean {
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
        binding.btnLogin.isEnabled = !isLoading
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
                    context = this@LoginActivity,
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
                            "photoUrl" to (user.photoUrl?.toString() ?: "")
                        )

                        db.collection("users").document(user.uid)
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

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                showLoading(false)
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        if (user.isEmailVerified) {
                            Log.d(TAG, "signInWithCredential:success")
                            navigateToMain()
                        } else {
                            auth.signOut()
                            showToast(verifyEmail)
                        }
                    }
                } else {
                    showToast(getErrorMessage(task.exception?.message))
                }
            }
    }

    private fun getErrorMessage(errorMessage: String?): String {
        return when (errorMessage) {
            "The email address is badly formatted." -> emailBadFormat
            "The password is invalid or the user does not have a password." -> passwordInvalid
            "There is no user record corresponding to this identifier. The user may have been deleted." -> emailDosentExist
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> networkError
            "The supplied auth credential is incorrect, malformed or has expired." -> invalidCredential
            "We have blocked all requests from this device due to unusual activity. Try again later." -> tooManyAttempts
            "The user account has been disabled by an administrator." -> userDisabled
            else -> "$loginFailed: $errorMessage"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, EditProfileActivity::class.java))
        finish()
    }
} 