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

    private companion object {
        private const val TAG = "SignUpActivity"
        private const val MIN_PASSWORD_LENGTH = 6

        // Pesan validasi
        const val NAME_REQUIRED = "Nama harus diisi"
        const val EMAIL_REQUIRED = "Email harus diisi"
        const val EMAIL_INVALID = "Masukkan email yang valid"
        const val PASSWORD_REQUIRED = "Password harus diisi"
        const val PASSWORD_MIN_LENGTH = "Password minimal $MIN_PASSWORD_LENGTH karakter"

        // Pesan error Firebase
        const val EMAIL_BAD_FORMAT = "Format email tidak valid"
        const val PASSWORD_INVALID = "Password terlalu lemah"
        const val EMAIL_EXISTS = "Email sudah terdaftar"
        const val NETWORK_ERROR = "Terjadi kesalahan jaringan"
        const val INVALID_CREDENTIAL = "Email atau password salah"
        const val TOO_MANY_ATTEMPTS = "Terlalu banyak percobaan. Silakan coba lagi nanti"
        const val USER_DISABLED = "Akun ini telah dinonaktifkan"

        // Pesan umum
        const val VERIFY_EMAIL = "Silakan verifikasi email Anda terlebih dahulu"
        const val VERIFY_EMAIL_SENT = "Email verifikasi telah dikirim"
        const val VERIFY_EMAIL_FAILED = "Gagal mengirim email verifikasi"
        const val AUTH_FAILED = "Autentikasi gagal"
        const val SIGNUP_FAILED = "Pendaftaran gagal"
        const val SIGNUP_SUCCESS =
            "Pendaftaran berhasil. Silakan verifikasi email Anda sebelum login"
        const val PROFILE_UPDATE_FAILED = "Gagal memperbarui profil"
        const val CREDENTIAL_ERROR = "Tidak dapat mengambil kredensial pengguna"
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
                showToast(VERIFY_EMAIL)
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

    private fun validateInputs(name: String, email: String, password: String): Boolean {
        if (name.isEmpty()) {
            binding.etName.error = NAME_REQUIRED
            binding.etName.requestFocus()
            return false
        }

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

        if (password.length < MIN_PASSWORD_LENGTH) {
            binding.etPassword.error = PASSWORD_MIN_LENGTH
            binding.etPassword.requestFocus()
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
                    showToast("$AUTH_FAILED: ${getErrorMessage(task.exception?.message)}")
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
                                showToast(SIGNUP_SUCCESS)
                                auth.signOut()
                                navigateToLogin()
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                showToast("Gagal menyimpan data: ${e.message}")
                            }
                    } else {
                        showLoading(false)
                        showToast("$PROFILE_UPDATE_FAILED: ${profileTask.exception?.message}")
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
                    showToast(VERIFY_EMAIL_SENT)
                } else {
                    showToast(VERIFY_EMAIL_FAILED)
                }
            }
    }

    private fun getErrorMessage(errorMessage: String?): String {
        return when (errorMessage) {
            "The email address is badly formatted." -> EMAIL_BAD_FORMAT
            "The password is invalid or the user does not have a password." -> PASSWORD_INVALID
            "The email address is already in use by another account." -> EMAIL_EXISTS
            "A network error (such as timeout, interrupted connection or unreachable host) has occurred." -> NETWORK_ERROR
            "The supplied auth credential is incorrect, malformed or has expired." -> INVALID_CREDENTIAL
            "We have blocked all requests from this device due to unusual activity. Try again later." -> TOO_MANY_ATTEMPTS
            "The user account has been disabled by an administrator." -> USER_DISABLED
            else -> "$SIGNUP_FAILED: $errorMessage"
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