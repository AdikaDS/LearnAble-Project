package com.adika.learnable.model

import com.adika.learnable.R

data class VideoRecommendation(
    val id: String,
    val title: String,
    val subtitle: String,
    val videoUrl: String = "",
    val thumbnailRes: Int = R.drawable.ic_video_placeholder
)