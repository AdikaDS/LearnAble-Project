package com.adika.learnable.util

import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import android.util.LruCache
import android.widget.ImageView
import androidx.core.graphics.scale
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.adika.learnable.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

object VideoThumbnailUtils {

    private const val TAG = "VideoThumbnailUtils"
    private val memoryCache: LruCache<String, Bitmap> by lazy {
        val maxMemoryKb = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSizeKb = maxMemoryKb / 8
        object : LruCache<String, Bitmap>(cacheSizeKb) {
            override fun sizeOf(key: String, value: Bitmap): Int {
                return value.byteCount / 1024
            }
        }
    }

    suspend fun generateThumbnail(
        videoFile: File,
        timeInMs: Long = 1000L,
        width: Int = 320,
        height: Int = 240
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            if (!videoFile.exists()) {
                Log.e(TAG, "Video file tidak ditemukan: ${videoFile.absolutePath}")
                return@withContext null
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)

            val bitmap = retriever.getFrameAtTime(
                timeInMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )

            retriever.release()

            if (bitmap != null) {

                if (bitmap.width != width || bitmap.height != height) {
                    val resizedBitmap = bitmap.scale(width, height)
                    bitmap.recycle() // Free memory dari bitmap asli
                    return@withContext resizedBitmap
                }
                return@withContext bitmap
            } else {
                Log.e(TAG, "Gagal mengambil frame dari video")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail: ${e.message}", e)
            return@withContext null
        }
    }

    private suspend fun generateThumbnailFromUrl(
        videoUrl: String,
        timeInMs: Long = 1000L,
        width: Int = 320,
        height: Int = 240
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Generating thumbnail from URL: $videoUrl at time: $timeInMs")
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoUrl, HashMap<String, String>())

            val bitmap = retriever.getFrameAtTime(
                timeInMs * 1000,
                MediaMetadataRetriever.OPTION_CLOSEST_SYNC
            )

            retriever.release()

            if (bitmap != null) {
                Log.d(
                    TAG,
                    "Frame extracted successfully, original size: ${bitmap.width}x${bitmap.height}"
                )

                if (bitmap.width != width || bitmap.height != height) {
                    val resizedBitmap = bitmap.scale(width, height)
                    bitmap.recycle()
                    Log.d(TAG, "Bitmap resized to: ${resizedBitmap.width}x${resizedBitmap.height}")
                    return@withContext resizedBitmap
                }
                Log.d(TAG, "Bitmap used as-is: ${bitmap.width}x${bitmap.height}")
                return@withContext bitmap
            } else {
                Log.e(TAG, "Gagal mengambil frame dari video URL: $videoUrl")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail from URL: $videoUrl, error: ${e.message}", e)
            return@withContext null
        }
    }

    private fun loadVideoThumbnail(
        target: ImageView,
        videoUrl: String?,
        scope: CoroutineScope,
        placeholderRes: Int = R.drawable.ic_video_placeholder,
        timeInMs: Long = 1000L,
        width: Int = 320,
        height: Int = 240,
        onLoadComplete: (() -> Unit)? = null
    ) {
        target.setImageResource(placeholderRes)
        if (videoUrl.isNullOrBlank()) {
            onLoadComplete?.invoke()
            return
        }

        val cacheKey = "$videoUrl|$timeInMs|$width|$height"
        Log.d(TAG, "Loading thumbnail for: $videoUrl, cache key: $cacheKey")

        memoryCache.get(cacheKey)?.let { cached ->
            Log.d(TAG, "Cache hit for: $cacheKey")
            target.setImageBitmap(cached)
            onLoadComplete?.invoke()
            return
        }

        Log.d(TAG, "Cache miss for: $cacheKey, generating thumbnail...")

        scope.launch(Dispatchers.Main) {
            val bmp = try {
                generateThumbnailFromUrl(
                    videoUrl = videoUrl,
                    timeInMs = timeInMs,
                    width = width,
                    height = height
                )
            } catch (e: Exception) {
                null
            }
            if (bmp != null) {
                Log.d(TAG, "Thumbnail generated successfully for: $videoUrl")
                memoryCache.put(cacheKey, bmp)
                target.setImageBitmap(bmp)
            } else {
                Log.e(TAG, "Failed to generate thumbnail for: $videoUrl")
                target.setImageResource(placeholderRes)
            }
            onLoadComplete?.invoke()
        }
    }

    fun loadVideoThumbnail(
        target: ImageView,
        videoUrl: String?,
        placeholderRes: Int = R.drawable.ic_video_placeholder,
        timeInMs: Long = 1000L,
        width: Int = 320,
        height: Int = 240,
        onLoadComplete: (() -> Unit)? = null
    ) {
        val owner = target.findViewTreeLifecycleOwner()
        if (owner != null) {
            loadVideoThumbnail(
                target = target,
                videoUrl = videoUrl,
                scope = owner.lifecycleScope,
                placeholderRes = placeholderRes,
                timeInMs = timeInMs,
                width = width,
                height = height,
                onLoadComplete = onLoadComplete
            )
        } else {
            loadVideoThumbnail(
                target = target,
                videoUrl = videoUrl,
                scope = CoroutineScope(Dispatchers.Main),
                placeholderRes = placeholderRes,
                timeInMs = timeInMs,
                width = width,
                height = height,
                onLoadComplete = onLoadComplete
            )
        }
    }

    suspend fun generateMultipleThumbnails(
        videoFile: File,
        count: Int = 5,
        width: Int = 160,
        height: Int = 120
    ): List<Bitmap> = withContext(Dispatchers.IO) {
        try {
            if (!videoFile.exists()) {
                Log.e(TAG, "Video file tidak ditemukan: ${videoFile.absolutePath}")
                return@withContext emptyList()
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L

            val thumbnails = mutableListOf<Bitmap>()

            if (duration > 0) {
                val interval = duration / count

                for (i in 0 until count) {
                    val timeInMs = i * interval
                    val bitmap = retriever.getFrameAtTime(
                        timeInMs * 1000,
                        MediaMetadataRetriever.OPTION_CLOSEST_SYNC
                    )

                    if (bitmap != null) {
                        val resizedBitmap = bitmap.scale(width, height)
                        bitmap.recycle()
                        thumbnails.add(resizedBitmap)
                    }
                }
            }

            retriever.release()
            return@withContext thumbnails

        } catch (e: Exception) {
            Log.e(TAG, "Error generating multiple thumbnails: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    suspend fun getVideoDuration(videoFile: File): Long = withContext(Dispatchers.IO) {
        try {
            if (!videoFile.exists()) {
                return@withContext 0L
            }

            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(videoFile.absolutePath)

            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            retriever.release()

            return@withContext duration / 1000 // Convert to seconds

        } catch (e: Exception) {
            Log.e(TAG, "Error getting video duration: ${e.message}", e)
            return@withContext 0L
        }
    }
}
