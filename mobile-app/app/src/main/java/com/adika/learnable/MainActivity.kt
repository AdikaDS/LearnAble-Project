package com.adika.learnable

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.ClearCredentialException
import androidx.lifecycle.lifecycleScope
import com.adika.learnable.databinding.ActivityMainBinding
import com.bumptech.glide.Glide
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.launch
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var credentialManager: CredentialManager
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var storageRef: StorageReference

    private companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Firebase services
        auth = FirebaseAuth.getInstance()
        credentialManager = CredentialManager.create(this)
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        storageRef = storage.reference

        setupToolbar()
        setupClickListeners()
        loadUserData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.btnChangePhoto.setOnClickListener {
            openImagePicker()
        }

        binding.btnSave.setOnClickListener {
            updateProfile()
        }
    }

    private fun logout() {
        // Sign out from Firebase
        auth.signOut()

        // Clear stored credentials
        lifecycleScope.launch {
            try {
                credentialManager.clearCredentialState(
                    ClearCredentialStateRequest()
                )
            } catch (e: ClearCredentialException) {
                Log.e(TAG, "Failed to clear credentials", e)
            }
        }

        // Navigate to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun openImagePicker() {
        ImagePicker.with(this)
            .crop()                    // Crop image
            .compress(1024)           // Final image size will be less than 1 MB
            .maxResultSize(1080, 1080) // Final image resolution will be less than 1080 x 1080
            .start()
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            val uri: Uri = data?.data!!
            uploadProfilePhoto(uri)
        } else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadUserData() {
        showLoading(true)
        val user = auth.currentUser
        if (user != null) {
            // Load data from Firestore
            firestore.collection("users").document(user.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val displayName = document.getString("displayName")
                        val email = document.getString("email")
                        val phoneNumber = document.getString("phoneNumber")
                        val photoUrl = document.getString("photoUrl")

                        // Update UI
                        binding.etName.setText(displayName)
                        binding.etEmail.setText(email)
                        binding.etPhone.setText(phoneNumber)

                        // Load profile photo if exists
                        if (!photoUrl.isNullOrEmpty()) {
                            Glide.with(this)
                                .load(photoUrl)
                                .placeholder(R.drawable.ic_launcher_background)
                                .error(R.drawable.ic_launcher_background)
                                .into(binding.ivProfile)
                        }
                    }
                    showLoading(false)
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showToast("Gagal memuat data profil: ${e.message}")
                }
        }
    }

    private fun updateProfile() {
        val user = auth.currentUser
        if (user != null) {
            showLoading(true)

            val newName = binding.etName.text.toString().trim()
            val newPhoneNumber = binding.etPhone.text.toString().trim()

            // Update Firestore
            val updates = hashMapOf<String, Any>(
                "displayName" to newName,
                "phoneNumber" to newPhoneNumber
            )

            firestore.collection("users").document(user.uid)
                .update(updates)
                .addOnSuccessListener {
                    // Update Firebase Auth profile
                    val profileUpdates = UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build()

                    user.updateProfile(profileUpdates)
                        .addOnCompleteListener { task ->
                            showLoading(false)
                            if (task.isSuccessful) {
                                showToast("Profil berhasil diperbarui")
                            } else {
                                showToast("Gagal memperbarui profil: ${task.exception?.message}")
                            }
                        }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showToast("Gagal memperbarui profil: ${e.message}")
                }
        }
    }

    private fun uploadProfilePhoto(imageUri: Uri) {
        showLoading(true)
        val user = auth.currentUser
        if (user != null) {
            val imageRef = storageRef.child("profile_images/${user.uid}/${UUID.randomUUID()}")

            imageRef.putFile(imageUri)
                .addOnSuccessListener { taskSnapshot ->
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        // Update Firestore with new photo URL
                        firestore.collection("users").document(user.uid)
                            .update("photoUrl", uri.toString())
                            .addOnSuccessListener {
                                // Update Firebase Auth profile
                                val profileUpdates = UserProfileChangeRequest.Builder()
                                    .setPhotoUri(uri)
                                    .build()

                                user.updateProfile(profileUpdates)
                                    .addOnCompleteListener { task ->
                                        showLoading(false)
                                        if (task.isSuccessful) {
                                            showToast("Foto profil berhasil diperbarui")
                                            // Update image view with new photo
                                            Glide.with(this)
                                                .load(uri)
                                                .placeholder(R.drawable.ic_launcher_background)
                                                .error(R.drawable.ic_launcher_background)
                                                .into(binding.ivProfile)
                                        } else {
                                            showToast("Gagal memperbarui foto profil: ${task.exception?.message}")
                                        }
                                    }
                            }
                            .addOnFailureListener { e ->
                                showLoading(false)
                                showToast("Gagal menyimpan URL foto: ${e.message}")
                            }
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    showToast("Gagal mengunggah foto: ${e.message}")
                }
        }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !isLoading
        binding.btnChangePhoto.isEnabled = !isLoading
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}