package com.adika.learnable.view.auth.resetpassword

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.adika.learnable.view.SplashActivity
import dagger.hilt.android.AndroidEntryPoint
import androidx.core.net.toUri

@AndroidEntryPoint
class DeepLinkActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val top = intent?.data
        // coba tingkat atas
        var mode = top?.getQueryParameter("mode")
        var oob = top?.getQueryParameter("oobCode")

        // kalau kosong, cek nested "link=" (sering terjadi)
        if (mode.isNullOrBlank() || oob.isNullOrBlank()) {
            top?.getQueryParameter("link")?.let { nested ->
                val uri = nested.toUri()
                mode = uri.getQueryParameter("mode")
                oob = uri.getQueryParameter("oobCode")
            }
        }

        // (opsional) log buat pastikan ke-trigger
        Log.d(
            "DeepLink",
            "top=$top, mode=$mode, oob=${if (oob.isNullOrBlank()) "null" else "..."}"
        )

        // Teruskan ke SplashActivity dulu
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
}