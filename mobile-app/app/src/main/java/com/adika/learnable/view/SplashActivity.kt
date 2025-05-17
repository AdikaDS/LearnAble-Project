package com.adika.learnable.view

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.adika.learnable.repository.AuthRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {
    @Inject
    lateinit var authRepository: AuthRepository

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
                    // User is logged in, check if they have selected disability type
                    val userDoc = FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .await()

                    val user = userDoc.toObject(com.adika.learnable.model.User::class.java)
                    if (user != null) {
                        when (user.role) {
                            "student" -> {
                                if (user.disabilityType == null) {
                                    // Student hasn't selected disability type
                                    navigateToMainWithDestination("disability_selection")
                                } else {
                                    // Student has selected disability type
                                    navigateToMainWithDestination("student_dashboard")
                                }
                            }
                            "teacher" -> {
                                // Teacher goes directly to their dashboard
                                navigateToMainWithDestination("teacher_dashboard")
                            }
                            "parent" -> {
                                // Parent goes directly to their dashboard
                                navigateToMainWithDestination("parent_dashboard")
                            }
                            else -> {
                                // Invalid role, go to login
                                navigateToMainWithDestination("login")
                            }
                        }
                    } else {
                        // User document not found, go to login
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