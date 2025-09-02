package com.adika.learnable.view

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.adika.learnable.databinding.ActivitySplashBinding
import com.adika.learnable.viewmodel.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint

@SuppressLint("CustomSplashScreen")
@AndroidEntryPoint
class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeNavigation()
        animateSplash()
    }

    private fun observeNavigation() {
        viewModel.navigationEvent.observe(this) { destination ->
            navigateToMainWithDestination(destination)
            finish()
        }
    }

    private fun animateSplash() {
        binding.animateSplash.playAnimation()
        binding.animateSplash.addAnimatorListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animation: Animator) {}

            override fun onAnimationEnd(animation: Animator) {
                viewModel.checkUserStatus()
            }

            override fun onAnimationCancel(animation: Animator) {}

            override fun onAnimationRepeat(animation: Animator) {}
        })

    }


    private fun navigateToMainWithDestination(destination: String) {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                // Jika ini forwarding dari deeplink, jangan kirim "destination" agar tidak override
                val fromDeeplink = intent?.getBooleanExtra("from_deeplink", false) == true
                if (!fromDeeplink) {
                    putExtra("destination", destination)
                }
                // forward extras dari deeplink jika ada
                intent?.let { source ->
                    if (source.getBooleanExtra("from_deeplink", false)) {
                        putExtra("from_deeplink", true)
                        putExtra("email_action_mode", source.getStringExtra("email_action_mode"))
                        putExtra("oobCode", source.getStringExtra("oobCode"))
                    }
                }
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
    }
} 