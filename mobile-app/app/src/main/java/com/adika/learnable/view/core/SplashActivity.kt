package com.adika.learnable.view.core

import android.animation.Animator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.adika.learnable.databinding.ActivitySplashBinding
import com.adika.learnable.viewmodel.others.SplashViewModel
import dagger.hilt.android.AndroidEntryPoint
import com.adika.learnable.util.LanguageUtils

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

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LanguageUtils.getLanguagePreference(newBase)
        val context = LanguageUtils.changeLanguage(newBase, languageCode)
        super.attachBaseContext(context)
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

                val fromDeeplink = intent?.getBooleanExtra("from_deeplink", false) == true
                if (!fromDeeplink) {
                    putExtra("destination", destination)
                }

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