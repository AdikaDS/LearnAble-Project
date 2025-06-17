package com.adika.learnable.repository

import android.media.MediaMetadataRetriever
import android.util.Log
import com.adika.learnable.model.AudioResource
import com.adika.learnable.model.PdfResource
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
    private val audioCollection = firestore.collection("audio_resource")
    private val pdfCollection = firestore.collection("pdf_resource")
    private val subBabCollection = firestore.collection("sub_bab")
    private val bucketName: String = "learnable-lessons-bucket"

    private fun generateUniqueObjectKey(type: MaterialType, originalFileName: String): String {
        val timestamp = System.currentTimeMillis()
        val sanitizedFileName = originalFileName.replace("[^a-zA-Z0-9.-]".toRegex(), "_")
        return "materials/${type.name.lowercase()}/$timestamp-$sanitizedFileName"
    }

    private fun validateFileFormat(file: File, type: MaterialType) {
        val validExtensions = when (type) {
            MaterialType.VIDEO -> listOf(".mp4", ".mov", ".avi")
            MaterialType.AUDIO -> listOf(".mp3", ".wav", ".m4a")
            MaterialType.PDF -> listOf(".pdf")
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

                    val mediaUrlKey = when (type) {
                        MaterialType.VIDEO -> "video"
                        MaterialType.PDF -> "pdfLesson"
                        MaterialType.AUDIO -> "audio"
                    }

                    subBabCollection.document(subBabId).update(mapOf("mediaUrls.$mediaUrlKey" to url)).await()
                    Log.d("MaterialRepository", "Berhasil update URL $mediaUrlKey di SubBab $subBabId")
                    success = true
                }
            } catch (e: Exception) {
                lastError = e
                retryCount++
                Log.w("MaterialRepository", "Percobaan $retryCount gagal update URL di SubBab", e)
                if (retryCount < 3) {
                    kotlinx.coroutines.delay(500) // Tunggu 500ms sebelum retry
                }
            }
        }

        if (!success) {
            Log.e("MaterialRepository", "Gagal update URL di SubBab setelah $retryCount percobaan", lastError)
            throw Exception("Gagal menyimpan URL: ${lastError?.message}")
        }
    }

    private fun generateS3Url(objectKey: String): String {
        return try {
            val url = s3Client.getUrl(bucketName, objectKey).toString()
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
                s3Client.putObject(PutObjectRequest(bucketName, objectKey, file))
                Log.d("MaterialRepository", "S3 upload completed successfully")
            } catch (e: Exception) {
                Log.e("MaterialRepository", "S3 upload failed", e)
                throw Exception("Gagal upload ke S3: ${e.message}")
            }
        }
    }

    private suspend fun uploadMaterial(
        file: File,
        title: String,
        subBabId: String,
        type: MaterialType
    ): Any {
        Log.d("MaterialRepository", "Mulai upload ${type.name.lowercase()}: ${file.name}")
        validateFileFormat(file, type)

        return withContext(Dispatchers.IO) {
            try {
                if (!file.exists()) {
                    throw FileNotFoundException("File tidak ditemukan: ${file.absolutePath}")
                }

                val objectKey = generateUniqueObjectKey(type, file.name)
                Log.d("MaterialRepository", "Generated object key: $objectKey")

                val duration = if (type == MaterialType.VIDEO || type == MaterialType.AUDIO) {
                    val metadata = getVideoAudioMetadata(file)
                    Log.d("MaterialRepository", "Got metadata: $metadata")
                    metadata["duration"] as Int
                } else {
                    0
                }

                // Upload ke S3
                uploadToS3(file, objectKey)

                // Generate URL
                val url = generateS3Url(objectKey)
                Log.d("MaterialRepository", "Generated URL: $url")

                // Update SubBab dengan retry
                Log.d("MaterialRepository", "Updating SubBab with ${type.name.lowercase()} URL...")
                updateSubBabMediaUrl(subBabId, type, url)

                // Simpan ke Firestore
                Log.d("MaterialRepository", "Saving ${type.name.lowercase()} resource to Firestore...")
                val resource = when (type) {
                    MaterialType.VIDEO -> VideoResource(id = "", title = title, bucketName = bucketName, objectKey = objectKey, duration = duration)
                    MaterialType.AUDIO -> AudioResource(id = "", title = title, bucketName = bucketName, objectKey = objectKey, duration = duration)
                    MaterialType.PDF -> PdfResource(id = "", title = title, bucketName = bucketName, objectKey = objectKey)
                }

                val docRef = when (type) {
                    MaterialType.VIDEO -> videoCollection.document()
                    MaterialType.AUDIO -> audioCollection.document()
                    MaterialType.PDF -> pdfCollection.document()
                }

                val finalResource = when(resource) {
                    is VideoResource -> resource.copy(id = docRef.id)
                    is AudioResource -> resource.copy(id = docRef.id)
                    is PdfResource -> resource.copy(id = docRef.id)
                }

                docRef.set(finalResource).await()

                Log.d("MaterialRepository", "Berhasil upload ${type.name.lowercase()}: ${file.name}")
                finalResource
            } catch (e: Exception) {
                Log.e("MaterialRepository", "Gagal upload ${type.name.lowercase()}: ${file.name}", e)
                throw e
            }
        }
    }

    suspend fun uploadVideoMaterial(file: File, title: String, subBabId: String): VideoResource {
        return uploadMaterial(file, title, subBabId, MaterialType.VIDEO) as VideoResource
    }

    suspend fun uploadAudioMaterial(file: File, title: String, subBabId: String): AudioResource {
        return uploadMaterial(file, title, subBabId, MaterialType.AUDIO) as AudioResource
    }

    suspend fun uploadPdfMaterial(file: File, title: String, subBabId: String): PdfResource {
        return uploadMaterial(file, title, subBabId, MaterialType.PDF) as PdfResource
    }

    private fun getVideoAudioMetadata(file: File): Map<String, Any> {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)

            mapOf("duration" to (retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toIntOrNull() ?: 0))
        } catch (e: Exception) {
            Log.w("VideoAudioMetadata", "Gagal mendapatkan metadata", e)
            mapOf("duration" to 0)
        }
    }

    enum class MaterialType {
        PDF, AUDIO, VIDEO
    }
}
