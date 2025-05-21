package com.adika.learnable.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep the splash screen visible while we check user status
        splashScreen.setKeepOnScreenCondition { true }

        checkUserStatus()
    }

    private fun checkUserStatus() {
        lifecycleScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser != null) {
                    // Verify that the user data is still valid
                    try {
                        val userDoc = FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUser.uid)
                            .get()
                            .await()

                        if (!userDoc.exists()) {
                            // User document doesn't exist, clear auth state
                            FirebaseAuth.getInstance().signOut()
                            navigateToMainWithDestination("login")
                            return@launch
                        }

                        val user = userDoc.toObject(com.adika.learnable.model.User::class.java)
                        if (user == null) {
                            // Invalid user data, clear auth state
                            FirebaseAuth.getInstance().signOut()
                            navigateToMainWithDestination("login")
                            return@launch
                        }

                        // User data is valid, proceed with navigation
                        when (user.role) {
                            "student" -> {
                                if (user.disabilityType == null) {
                                    navigateToMainWithDestination("disability_selection")
                                } else {
                                    navigateToMainWithDestination("student_dashboard")
                                }
                            }
                            "teacher" -> navigateToMainWithDestination("teacher_dashboard")
                            "parent" -> navigateToMainWithDestination("parent_dashboard")
                            else -> navigateToMainWithDestination("login")
                        }
                    } catch (e: Exception) {
                        // Error occurred, clear auth state
                        FirebaseAuth.getInstance().signOut()
                        navigateToMainWithDestination("login")
                    }
                } else {
                    // No user logged in, go to login
                    navigateToMainWithDestination("login")
                }
            } catch (e: Exception) {
                // Error occurred, go to login
                navigateToMainWithDestination("login")
            } finally {
                finish()
            }
        }
    }


    private fun navigateToMainWithDestination(destination: String) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                putExtra("destination", destination)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
} 