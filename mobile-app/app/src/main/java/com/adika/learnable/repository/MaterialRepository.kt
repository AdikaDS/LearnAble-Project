package com.adika.learnable.repository

import android.media.MediaMetadataRetriever
import android.util.Log
import com.adika.learnable.BuildConfig
import com.adika.learnable.model.PdfResource
import com.adika.learnable.model.SubtitleResource
import com.adika.learnable.model.VideoResource
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.PutObjectRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaterialRepository @Inject constructor(
    firestore: FirebaseFirestore,
    private val s3Client: AmazonS3Client
) {
    private val videoCollection = firestore.collection("video_resource")
    private val pdfCollection = firestore.collection("pdf_resource")
    private val subtitleCollection = firestore.collection("subtitle_resource")
    private val subBabCollection = firestore.collection("sub_bab")

    private fun generateUniqueObjectKey(type: MaterialType, originalFileName: String): String {
        val timestamp = System.currentTimeMillis()
        val sanitizedFileName = originalFileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return "materials/${type.name.lowercase()}/$timestamp-$sanitizedFileName"
    }

    private fun generateCoverObjectKey(originalFileName: String): String {
        val timestamp = System.currentTimeMillis()
        val sanitizedFileName = originalFileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return "subbabCover/$timestamp-$sanitizedFileName"
    }
    
    private fun generateQuizMediaObjectKey(originalFileName: String): String {
        val timestamp = System.currentTimeMillis()
        val sanitizedFileName = originalFileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return "quizMedia/$timestamp-$sanitizedFileName"
    }

    private fun validateFileFormat(file: File, type: MaterialType) {
        val validExtensions = when (type) {
            MaterialType.VIDEO -> listOf(".mp4", ".mov", ".avi")
            MaterialType.PDF -> listOf(".pdf")
            MaterialType.SUBTITLE -> listOf(".srt", ".vtt")
        }

        if (!validExtensions.any { file.name.endsWith(it, ignoreCase = true) }) {
            throw IllegalArgumentException("Format file tidak didukung. Format yang didukung: ${validExtensions.joinToString()}")
        }
    }

    private suspend fun updateSubBabMediaUrl(subBabId: String, type: MaterialType, url: String) {
        var retryCount = 0
        var success = false
        var lastError: Exception? = null

        while (!success && retryCount < 3) {
            try {
                withContext(Dispatchers.IO) {
                    val subBabDoc = subBabCollection.document(subBabId).get().await()
                    if (!subBabDoc.exists()) {
                        throw Exception("SubBab tidak ditemukan")
                    }

                    if (type == MaterialType.SUBTITLE) {
                        subBabCollection.document(subBabId)
                            .update(mapOf("subtitle" to url)).await()
                    } else {
                        val mediaUrlKey = when (type) {
                            MaterialType.VIDEO -> "video"
                            MaterialType.PDF -> "pdfLesson"
                            else -> ""
                        }
                        subBabCollection.document(subBabId)
                            .update(mapOf("mediaUrls.$mediaUrlKey" to url)).await()
                    }
                    Log.d(
                        "MaterialRepository",
                        "Berhasil update URL ${type.name.lowercase()} di SubBab $subBabId"
                    )
                    success = true
                }
            } catch (e: Exception) {
                lastError = e
                retryCount++
                Log.w("MaterialRepository", "Percobaan $retryCount gagal update URL di SubBab", e)
                if (retryCount < 3) {
                    kotlinx.coroutines.delay(500)
                }
            }
        }

        if (!success) {
            Log.e(
                "MaterialRepository",
                "Gagal update URL di SubBab setelah $retryCount percobaan",
                lastError
            )
            throw Exception("Gagal menyimpan URL: ${lastError?.message}")
        }
    }

    private fun generateS3Url(objectKey: String): String {
        return try {
            val url = s3Client.getUrl(BuildConfig.S3_BUCKET_NAME, objectKey).toString()
            Log.d("MaterialRepository", "Generated S3 URL: $url")
            url
        } catch (e: Exception) {
            Log.e("MaterialRepository", "Error generating S3 URL", e)
            throw Exception("Gagal generate URL: ${e.message}")
        }
    }

    private suspend fun uploadToS3(file: File, objectKey: String) {
        return withContext(Dispatchers.IO) {
            try {
                s3Client.putObject(PutObjectRequest(BuildConfig.S3_BUCKET_NAME, objectKey, file))
                Log.d("MaterialRepository", "S3 upload completed successfully")
            } catch (e: Exception) {
                Log.e("MaterialRepository", "S3 upload failed", e)
                throw Exception("Gagal upload ke S3: ${e.message}")
            }
        }
    }

    private fun extractObjectKeyFromUrl(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val u = java.net.URL(url)
            u.path.removePrefix("/")
        } catch (e: Exception) {

            val marker = ".amazonaws.com/"
            val idx = url.indexOf(marker)
            if (idx >= 0 && idx + marker.length < url.length) {
                url.substring(idx + marker.length)
            } else null
        }
    }

    suspend fun deleteAllMaterialsForSubBab(subBabId: String) {

        val subBabDoc = subBabCollection.document(subBabId).get().await()
        if (!subBabDoc.exists()) return

        runCatching {
            val videoUrl = subBabDoc.get("mediaUrls.video") as? String
            val videoKey = extractObjectKeyFromUrl(videoUrl)
            if (!videoKey.isNullOrBlank()) {

                deleteFromS3(videoKey)

                val snap = videoCollection.whereEqualTo("objectKey", videoKey).get().await()
                for (doc in snap.documents) {
                    videoCollection.document(doc.id).delete().await()
                }
            }
        }

        runCatching {
            val pdfUrl = subBabDoc.get("mediaUrls.pdfLesson") as? String
            val pdfKey = extractObjectKeyFromUrl(pdfUrl)
            if (!pdfKey.isNullOrBlank()) {
                deleteFromS3(pdfKey)
                val snap = pdfCollection.whereEqualTo("objectKey", pdfKey).get().await()
                for (doc in snap.documents) {
                    pdfCollection.document(doc.id).delete().await()
                }
            }
        }

        runCatching {
            val subtitleUrl = subBabDoc.get("subtitle") as? String
            val subtitleKey = extractObjectKeyFromUrl(subtitleUrl)
            if (!subtitleKey.isNullOrBlank()) {
                deleteFromS3(subtitleKey)
                val snap = subtitleCollection.whereEqualTo("objectKey", subtitleKey).get().await()
                for (doc in snap.documents) {
                    subtitleCollection.document(doc.id).delete().await()
                }
            }
        }

        runCatching {
            val coverUrl = subBabDoc.get("coverImage") as? String
            val coverKey = extractObjectKeyFromUrl(coverUrl)
            if (!coverKey.isNullOrBlank()) {
                deleteFromS3(coverKey)
            }
        }

        subBabCollection.document(subBabId).update(
            mapOf(
                "mediaUrls.video" to "",
                "mediaUrls.pdfLesson" to "",
                "subtitle" to "",
                "coverImage" to ""
            )
        ).await()
    }

    suspend fun uploadQuizMediaImage(file: File): String {
        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) {
                    throw FileNotFoundException("File tidak ditemukan: ${file.absolutePath}")
                }
                
                val objectKey = generateQuizMediaObjectKey(file.name)
                Log.d("MaterialRepository", "Generated quiz media object key: $objectKey")

                uploadToS3(file, objectKey)

                val url = generateS3Url(objectKey)
                Log.d("MaterialRepository", "Generated quiz media URL: $url")
                
                url
            } catch (e: Exception) {
                Log.e("MaterialRepository", "Gagal upload quiz media image: ${file.name}", e)
                throw e
            }
        }
    }
    
    suspend fun uploadSubBabCover(file: File, subBabId: String): String {

        val ext = file.name.substringAfterLast('.', "").lowercase()
        if (ext !in listOf("jpg", "jpeg", "png", "webp")) {
            throw IllegalArgumentException("Format file tidak didukung. Hanya jpg, jpeg, png, webp")
        }
        val max = 10 * 1024 * 1024
        if (file.length() > max) throw IllegalArgumentException("Ukuran file terlalu besar. Maksimal 5MB")

        val objectKey = generateCoverObjectKey(file.name)
        uploadToS3(file, objectKey)
        val url = generateS3Url(objectKey)

        subBabCollection.document(subBabId).update(mapOf("coverImage" to url)).await()
        return url
    }

    private suspend fun deleteFromS3(objectKey: String) {
        return withContext(Dispatchers.IO) {
            try {
                s3Client.deleteObject(BuildConfig.S3_BUCKET_NAME, objectKey)
                Log.d("MaterialRepository", "S3 delete completed successfully")
            } catch (e: Exception) {
                Log.e("MaterialRepository", "S3 delete failed", e)
                throw Exception("Gagal delete dari S3: ${e.message}")
            }
        }
    }

    suspend fun deleteMaterialByObjectKey(objectKey: String, type: MaterialType, subBabId: String) {
        withContext(Dispatchers.IO) {
            try {

                deleteFromS3(objectKey)

                val collection = when (type) {
                    MaterialType.VIDEO -> videoCollection
                    MaterialType.PDF -> pdfCollection
                    MaterialType.SUBTITLE -> subtitleCollection
                }
                
                val snap = collection.whereEqualTo("objectKey", objectKey).get().await()
                for (doc in snap.documents) {
                    collection.document(doc.id).delete().await()
                }

                if (type == MaterialType.SUBTITLE) {
                    subBabCollection.document(subBabId).update(mapOf("subtitle" to "")).await()
                } else {
                    val mediaUrlKey = when (type) {
                        MaterialType.VIDEO -> "video"
                        MaterialType.PDF -> "pdfLesson"
                        else -> ""
                    }
                    subBabCollection.document(subBabId).update(mapOf("mediaUrls.$mediaUrlKey" to "")).await()
                }
                
                Log.d("MaterialRepository", "Berhasil menghapus material $objectKey dengan tipe ${type.name} dan membersihkan mediaUrls di SubBab")
            } catch (e: Exception) {
                Log.e("MaterialRepository", "Gagal menghapus material $objectKey", e)

            }
        }
    }

    private suspend fun getExistingObjectKey(type: MaterialType, id: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val docSnapshot = when (type) {
                    MaterialType.VIDEO -> videoCollection.document(id).get().await()
                    MaterialType.PDF -> pdfCollection.document(id).get().await()
                    MaterialType.SUBTITLE -> subtitleCollection.document(id).get().await()
                }

                if (!docSnapshot.exists()) return@withContext null

                when (type) {
                    MaterialType.VIDEO -> docSnapshot.toObject(VideoResource::class.java)?.objectKey
                    MaterialType.PDF -> docSnapshot.toObject(PdfResource::class.java)?.objectKey
                    MaterialType.SUBTITLE -> docSnapshot.toObject(SubtitleResource::class.java)?.objectKey
                }
            } catch (e: Exception) {
                Log.e("Material Repository", "Gagal memuat object lama", e)
                null
            }
        }
    }

    private suspend fun uploadMaterial(
        file: File,
        title: String,
        subBabId: String,
        type: MaterialType,
        existingMaterialId: String? = null
    ): Any {
        Log.d(
            "MaterialRepository",
            "Mulai upload ${type.name.lowercase()}: ${file.name}, existingID = $existingMaterialId"
        )
        validateFileFormat(file, type)

        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) {
                    throw FileNotFoundException("File tidak ditemukan: ${file.absolutePath}")
                }

                if (existingMaterialId != null) {
                    val oldObjectKey = getExistingObjectKey(type, existingMaterialId)
                    if (!oldObjectKey.isNullOrEmpty()) {
                        Log.d("MaterialRepository", "Menghapus file lama di S3: $oldObjectKey")
                        deleteFromS3(oldObjectKey)
                    }
                }
                val objectKey = generateUniqueObjectKey(type, file.name)
                Log.d("MaterialRepository", "Generated object key: $objectKey")

                val duration = if (type == MaterialType.VIDEO) {
                    val metadata = getVideoAudioMetadata(file)
                    Log.d("MaterialRepository", "Got metadata: $metadata")
                    metadata["duration"] as Int
                } else {
                    0
                }

                uploadToS3(file, objectKey)

                val url = generateS3Url(objectKey)
                Log.d("MaterialRepository", "Generated URL: $url")

                Log.d("MaterialRepository", "Updating SubBab with ${type.name.lowercase()} URL...")
                updateSubBabMediaUrl(subBabId, type, url)

                Log.d(
                    "MaterialRepository",
                    "Saving ${type.name.lowercase()} resource to Firestore..."
                )
                val newResource = when (type) {
                    MaterialType.VIDEO -> VideoResource(
                        id = "",
                        title = title,
                        bucketName = BuildConfig.S3_BUCKET_NAME,
                        objectKey = objectKey,
                        duration = duration
                    )

                    MaterialType.PDF -> PdfResource(
                        id = "",
                        title = title,
                        bucketName = BuildConfig.S3_BUCKET_NAME,
                        objectKey = objectKey
                    )

                    MaterialType.SUBTITLE -> SubtitleResource(
                        id = "",
                        title = title,
                        bucketName = BuildConfig.S3_BUCKET_NAME,
                        objectKey = objectKey
                    )
                }

                val docRef = when (type) {
                    MaterialType.VIDEO -> if (!existingMaterialId.isNullOrBlank()) videoCollection.document(
                        existingMaterialId
                    ) else videoCollection.document()

                    MaterialType.PDF -> if (!existingMaterialId.isNullOrBlank()) pdfCollection.document(
                        existingMaterialId
                    ) else pdfCollection.document()

                    MaterialType.SUBTITLE -> if (!existingMaterialId.isNullOrBlank()) subtitleCollection.document(
                        existingMaterialId
                    ) else subtitleCollection.document()
                }

                val finalResource = when (newResource) {
                    is VideoResource -> newResource.copy(id = docRef.id)
                    is PdfResource -> newResource.copy(id = docRef.id)
                    is SubtitleResource -> newResource.copy(id = docRef.id)
                }

                docRef.set(finalResource).await()

                Log.d(
                    "MaterialRepository",
                    "Berhasil upload ${type.name.lowercase()}: ${file.name}"
                )
                finalResource
            } catch (e: Exception) {
                Log.e(
                    "MaterialRepository",
                    "Gagal upload ${type.name.lowercase()}: ${file.name}",
                    e
                )
                throw e
            }
        }
    }

    suspend fun uploadVideoMaterial(file: File, title: String, subBabId: String): VideoResource {
        return uploadMaterial(file, title, subBabId, MaterialType.VIDEO) as VideoResource
    }

    suspend fun uploadPdfMaterial(file: File, title: String, subBabId: String): PdfResource {
        return uploadMaterial(file, title, subBabId, MaterialType.PDF) as PdfResource
    }

    suspend fun uploadSubtitleMaterial(file: File, title: String, subBabId: String): String {
        return uploadMaterial(file, title, subBabId, MaterialType.SUBTITLE) as String
    }

    private fun getVideoAudioMetadata(file: File): Map<String, Any> {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            mapOf(
                "duration" to (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                    ?.toIntOrNull() ?: 0)
            )
        } catch (e: Exception) {
            Log.w("VideoAudioMetadata", "Gagal mendapatkan metadata", e)
            mapOf("duration" to 0)
        }
    }

    enum class MaterialType {
        PDF, VIDEO, SUBTITLE
    }
}