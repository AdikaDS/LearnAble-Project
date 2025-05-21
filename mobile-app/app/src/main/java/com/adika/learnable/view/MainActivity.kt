package com.adika.learnable.view

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.adika.learnable.R
import com.adika.learnable.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        handleDestination()

    }


    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        navController.addOnDestinationChangedListener { _, destination, _ ->
            try {
                supportActionBar?.title = destination.label
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in navigation: ${e.message}")
            }
        }
    }

    private fun handleDestination() {
        val destination = intent.getStringExtra("destination")
        if (destination != null) {
            try {
                when (destination) {
                    "disability_selection" -> {
                        navController.navigate(R.id.disabilitySelectionFragment)
                    }
                    "student_dashboard" -> {
                        navController.navigate(R.id.studentDashboardFragment)
                    }
                    "teacher_dashboard" -> {
                        navController.navigate(R.id.teacherDashboardFragment)
                    }
                    "parent_dashboard" -> {
                        navController.navigate(R.id.parentDashboardFragment)
                    }
                    else -> {
                        Log.w("MainActivity", "Invalid destination: $destination")
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Navigation error: ${e.message}")
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        when (navController.currentDestination?.id) {
            R.id.studentDashboardFragment,
            R.id.teacherDashboardFragment,
            R.id.parentDashboardFragment -> {
                showExitConfirmationDialog()
            }
            else -> {
                super.onBackPressed()
            }
        }
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Keluar Aplikasi")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya") { _, _ ->
                finish()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        return try {
            navController.navigateUp() || super.onSupportNavigateUp()
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in navigate up: ${e.message}")
            super.onSupportNavigateUp()
        }
    }

}