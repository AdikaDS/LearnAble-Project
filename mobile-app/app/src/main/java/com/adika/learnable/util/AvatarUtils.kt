package com.adika.learnable.util

import android.widget.ImageView
import androidx.core.net.toUri
import com.amulyakhare.textdrawable.TextDrawable
import com.amulyakhare.textdrawable.util.ColorGenerator
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions

object AvatarUtils {

    private val colorGenerator = ColorGenerator.MATERIAL

    fun getInitial(name: String): String {
        return name.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.first().uppercaseChar() }
            .joinToString("")
    }

    private fun getInitials(name: String?): String {
        if (name.isNullOrBlank()) return "?"
        val parts = name.trim().split(" ")
        return if (parts.size >= 2) {
            "${parts.first().first()}${parts.last().first()}"
        } else {
            parts.first().first().toString()
        }.uppercase()
    }

    private fun buildTextDrawable(name: String?, size: Int = 100): TextDrawable {
        val initials = getInitials(name)
        val color = colorGenerator.getColor(name ?: "unknown")

        return TextDrawable.builder()
            .beginConfig()
            .width(size)   // px
            .height(size)  // px
            .toUpperCase()
            .endConfig()
            .buildRound(initials, color)
    }

    fun loadAvatar(imageView: ImageView, name: String?, photoUrl: String?) {
        if (photoUrl.isNullOrBlank()) {

            imageView.post {
                val size = imageView.width.coerceAtLeast(imageView.height)
                val drawable = buildTextDrawable(name, size)
                imageView.setImageDrawable(drawable)
            }
        } else {
            Glide.with(imageView.context)
                .load(photoUrl.toUri())
                .apply(RequestOptions.circleCropTransform())
                .into(imageView)
        }
    }
}

fun ImageView.loadAvatar(name: String?, photoUrl: String?) {
    AvatarUtils.loadAvatar(this, name, photoUrl)
}