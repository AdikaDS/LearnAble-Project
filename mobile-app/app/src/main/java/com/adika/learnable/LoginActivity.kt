package com.adika.learnable

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
import com.adika.learnable.databinding.ActivityLoginBinding
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager

    private companion object {
        private const val TAG = "LoginActivity"

        // Pesan validasi
        const val EMAIL_REQUIRED = "Email harus diisi"
        const val EMAIL_INVALID = "Masukkan email yang valid"
        const val PASSWORD_REQUIRED = "Password harus diisi"
        const val PASSWORD_MIN_LENGTH = "Password minimal 6 karakter"

        // Pesan error Firebase
        const val EMAIL_BAD_FORMAT = "Format email tidak valid"
        const val PASSWORD_INVALID = "Password salah"
        const val USER_NOT_FOUND = "Email tidak terdaftar"
        const val NETWORK_ERROR = "Terjadi kesalahan jaringan"
        const val INVALID_CREDENTIAL = "Email atau password salah"
        const val TOO_MANY_ATTEMPTS = "Terlalu banyak percobaan login. Silakan coba lagi nanti"
        const val USER_DISABLED = "Akun ini telah dinonaktifkan"

        // Pesan umum
        const val VERIFY_EMAIL = "Silakan verifikasi email Anda terlebih dahulu"
        const val AUTH_FAILED = "Autentikasi gagal"
        const val LOGIN_FAILED = "Login gagal"
        const val CREDENTIAL_ERROR = "Tidak dapat mengambil kredensial pengguna"
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

    private fun validateInputs(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = EMAIL_REQUIRED
            binding.etEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.etEmail.error = EMAIL_INVALID
            binding.etEmail.requestFocus()
            return false
        }

        if (password.isEmpty()) {
            binding.etPassword.error = PASSWORD_REQUIRED
            binding.etPassword.requestFocus()
            return false
        }

        if (password.length < 6) {
            binding.etPassword.error = PASSWORD_MIN_LENGTH
            binding.etPassword.requestFocus()
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
                Log.e(TAG, CREDENTIAL_ERROR, e)
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
                        if (user.isEmailVerified) {
                            Log.d(TAG, "signInWithCredential:success")
                            navigateToMain()
                        } else {
                            auth.signOut()
                            showToast(VERIFY_EMAIL)
                        }
                    }
                } else {
                    showToast("$AUTH_FAILED: ${getErrorMessage(task.exception?.message)}")
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
                            showToast(VERIFY_EMAIL)
                        }
                    }
                } else {
                    showToast(getErrorMessage(task.exception?.message))
                }
            }
    }

    private fun getErrorMessage(errorMessage: String?): String {
        return when (errorMessage) {
            "The email address is badly formatted." -> EMAIL_BAD_FORMAT
            "The password is invalid or the user does not have a password." -> PASSWORD_INVALID
            "There is no user record corresponding to this identifier. The user may have been deleted." -> USER_NOT_FOUND
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> NETWORK_ERROR
            "The supplied auth credential is incorrect, malformed or has expired." -> INVALID_CREDENTIAL
            "We have blocked all requests from this device due to unusual activity. Try again later." -> TOO_MANY_ATTEMPTS
            "The user account has been disabled by an administrator." -> USER_DISABLED
            else -> "$LOGIN_FAILED: $errorMessage"
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun navigateToMain() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
} 