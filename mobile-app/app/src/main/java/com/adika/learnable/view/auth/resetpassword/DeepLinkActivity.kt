package com.adika.learnable.view.auth.resetpassword

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.adika.learnable.util.LanguageUtils
import com.adika.learnable.view.core.SplashActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val top = intent?.data
        var mode = top?.getQueryParameter("mode")
        var oob = top?.getQueryParameter("oobCode")

        if (mode.isNullOrBlank() || oob.isNullOrBlank()) {
            top?.getQueryParameter("link")?.let { nested ->
                val uri = nested.toUri()
                mode = uri.getQueryParameter("mode")
                oob = uri.getQueryParameter("oobCode")
            }
        }

        startActivity(
            Intent(this, SplashActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("from_deeplink", true)
                putExtra("email_action_mode", mode)
                putExtra("oobCode", oob)
            }
        )
        finish()
    }

    override fun attachBaseContext(newBase: Context) {
        val languageCode = LanguageUtils.getLanguagePreference(newBase)
        val context = LanguageUtils.changeLanguage(newBase, languageCode)
        super.attachBaseContext(context)
    }
}