package com.adika.learnable.viewmodel.lesson

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.SubBab
import com.adika.learnable.repository.MaterialRepository
import com.adika.learnable.repository.SubBabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject

@HiltViewModel
class MaterialViewModel @Inject constructor(
    private val materialRepository: MaterialRepository,
    private val subBabRepository: SubBabRepository
) : ViewModel() {

    private val _materialState = MutableLiveData<MaterialState>()
    val materialState: LiveData<MaterialState> = _materialState

    private val _uploadProgress = MutableLiveData<UploadProgress>()
    val uploadProgress: LiveData<UploadProgress> = _uploadProgress

    private val _coverUploadState = MutableLiveData<CoverUploadState>()
    val coverUploadState: LiveData<CoverUploadState> = _coverUploadState

    fun createSubBabAndUploadFiles(
        subBab: SubBab,
        pendingUploads: Map<String, Pair<File, String>>
    ) {
        viewModelScope.launch {
            var createdSubBab: SubBab? = null
            val uploadedMaterials =
                mutableListOf<Triple<String, MaterialRepository.MaterialType, String>>() // objectKey, type, subBabId

            try {
                _materialState.value = MaterialState.Loading

                createdSubBab = withTimeoutOrNull(30000) { // 30 second timeout
                    subBabRepository.addSubBab(subBab)
                } ?: throw Exception("Timeout saat membuat SubBab")

                Log.d("MaterialViewModel", "SubBab berhasil dibuat dengan ID: ${createdSubBab.id}")

                Log.d("MaterialViewModel", "Jumlah file yang akan diupload: ${pendingUploads.size}")
                pendingUploads.forEach { (type, fileData) ->
                    Log.d("MaterialViewModel", "File type: $type, title: ${fileData.second}")
                }

                val totalFiles = pendingUploads.size
                var completedFiles = 0

                pendingUploads.forEach { (type, fileData) ->
                    try {
                        val (file, title) = fileData
                        if (!file.exists()) {
                            throw Exception("File tidak ditemukan: ${file.absolutePath}")
                        }

                        Log.d("MaterialViewModel", "Memulai upload file: $title dengan tipe: $type")

                        val materialType = MaterialRepository.MaterialType.valueOf(type)
                        val uploadedResource =
                            withTimeoutOrNull(60000) { // 60 second timeout per file
                                when (materialType) {
                                    MaterialRepository.MaterialType.VIDEO -> {
                                        materialRepository.uploadVideoMaterial(
                                            file,
                                            title,
                                            createdSubBab.id
                                        )
                                    }

                                    MaterialRepository.MaterialType.PDF -> {
                                        materialRepository.uploadPdfMaterial(
                                            file,
                                            title,
                                            createdSubBab.id
                                        )
                                    }

                                    MaterialRepository.MaterialType.SUBTITLE -> {
                                        materialRepository.uploadSubtitleMaterial(
                                            file,
                                            title,
                                            createdSubBab.id
                                        )
                                    }
                                }
                            } ?: throw Exception("Timeout saat upload file: $title")

                        val objectKey = when (uploadedResource) {
                            is com.adika.learnable.model.VideoResource -> uploadedResource.objectKey
                            is com.adika.learnable.model.PdfResource -> uploadedResource.objectKey
                            is com.adika.learnable.model.SubtitleResource -> uploadedResource.objectKey
                            else -> null
                        }

                        if (objectKey != null) {
                            uploadedMaterials.add(Triple(objectKey, materialType, createdSubBab.id))
                        }

                        completedFiles++
                        _uploadProgress.value = UploadProgress(
                            completed = completedFiles,
                            total = totalFiles,
                            message = "Mengupload file $completedFiles dari $totalFiles"
                        )

                    } catch (e: Exception) {
                        Log.e("MaterialViewModel", "Gagal upload file: ${fileData.second}", e)

                        rollbackUploadedMaterials(uploadedMaterials)

                        createdSubBab.let {
                            try {
                                subBabRepository.deleteSubBab(it.id)
                                Log.d("MaterialViewModel", "SubBab berhasil dihapus setelah error")
                            } catch (deleteError: Exception) {
                                Log.e(
                                    "MaterialViewModel",
                                    "Gagal menghapus SubBab setelah error",
                                    deleteError
                                )
                            }
                        }

                        _materialState.value = MaterialState.Error(
                            message = "Gagal upload ${fileData.second}: ${e.message}"
                        )
                        throw e
                    }
                }
            } catch (e: Exception) {
                Log.e("MaterialViewModel", "Gagal membuat SubBab atau upload file", e)

                if (uploadedMaterials.isNotEmpty()) {
                    rollbackUploadedMaterials(uploadedMaterials)
                }

                createdSubBab?.let {
                    try {
                        subBabRepository.deleteSubBab(it.id)
                        Log.d("MaterialViewModel", "SubBab berhasil dihapus setelah error")
                    } catch (deleteError: Exception) {
                        Log.e(
                            "MaterialViewModel",
                            "Gagal menghapus SubBab setelah error",
                            deleteError
                        )
                    }
                }

                _materialState.value = MaterialState.Error(
                    message = when (e) {
                        is CancellationException -> "Operasi dibatalkan"
                        else -> "Gagal membuat SubBab atau upload file: ${e.message}"
                    }
                )
            }
        }
    }

    private suspend fun rollbackUploadedMaterials(uploadedMaterials: List<Triple<String, MaterialRepository.MaterialType, String>>) {
        uploadedMaterials.forEach { (objectKey, type, subBabId) ->
            try {
                materialRepository.deleteMaterialByObjectKey(objectKey, type, subBabId)
                Log.d(
                    "MaterialViewModel",
                    "Berhasil rollback material: $objectKey (${type.name}) dari SubBab $subBabId"
                )
            } catch (e: Exception) {
                Log.e("MaterialViewModel", "Gagal rollback material: $objectKey", e)

            }
        }
    }

    fun updateSubBabAndUploadFiles(
        subBab: SubBab,
        pendingUploads: Map<String, Pair<File, String>>
    ) {
        viewModelScope.launch {
            var existingSubBab: SubBab? = null
            val uploadedMaterials =
                mutableListOf<Triple<String, MaterialRepository.MaterialType, String>>() // objectKey, type, subBabId

            try {
                _materialState.value = MaterialState.Loading

                existingSubBab = subBabRepository.getSubBab(subBab.id)

                val updatedMediaUrls = mutableMapOf<String, String>().apply {

                    putAll(existingSubBab.mediaUrls)

                    putAll(subBab.mediaUrls.filter { (_, value) -> value.isNotEmpty() })
                }

                val updatedSubBab = subBab.copy(mediaUrls = updatedMediaUrls)

                subBabRepository.updateSubBab(updatedSubBab)
                Log.d("MaterialViewModel", "SubBab berhasil diupdate: ${updatedSubBab.id}")

                Log.d("MaterialViewModel", "Jumlah file yang akan diupload: ${pendingUploads.size}")
                pendingUploads.forEach { (type, fileData) ->
                    Log.d("MaterialViewModel", "File type: $type, title: ${fileData.second}")
                }

                val totalFiles = pendingUploads.size
                var completedFiles = 0

                pendingUploads.forEach { (type, fileData) ->
                    try {
                        val (file, title) = fileData
                        if (!file.exists()) {
                            throw Exception("File tidak ditemukan: ${file.absolutePath}")
                        }

                        Log.d("MaterialViewModel", "Memulai upload file: $title dengan tipe: $type")

                        val materialType = MaterialRepository.MaterialType.valueOf(type)
                        val uploadedResource =
                            withTimeoutOrNull(60000) { // 60 second timeout per file
                                when (materialType) {
                                    MaterialRepository.MaterialType.VIDEO -> {
                                        materialRepository.uploadVideoMaterial(
                                            file,
                                            title,
                                            updatedSubBab.id
                                        )
                                    }

                                    MaterialRepository.MaterialType.PDF -> {
                                        materialRepository.uploadPdfMaterial(
                                            file,
                                            title,
                                            updatedSubBab.id
                                        )
                                    }

                                    MaterialRepository.MaterialType.SUBTITLE -> {
                                        materialRepository.uploadSubtitleMaterial(
                                            file,
                                            title,
                                            updatedSubBab.id
                                        )
                                    }
                                }
                            } ?: throw Exception("Timeout saat upload file: $title")

                        val objectKey = when (uploadedResource) {
                            is com.adika.learnable.model.VideoResource -> uploadedResource.objectKey
                            is com.adika.learnable.model.PdfResource -> uploadedResource.objectKey
                            is com.adika.learnable.model.SubtitleResource -> uploadedResource.objectKey
                            else -> null
                        }

                        if (objectKey != null) {
                            uploadedMaterials.add(Triple(objectKey, materialType, updatedSubBab.id))
                        }

                        completedFiles++
                        _uploadProgress.value = UploadProgress(
                            completed = completedFiles,
                            total = totalFiles,
                            message = "Mengupload file $completedFiles dari $totalFiles"
                        )

                    } catch (e: Exception) {
                        Log.e("MaterialViewModel", "Gagal upload file: ${fileData.second}", e)

                        rollbackUploadedMaterials(uploadedMaterials)

                        existingSubBab.let {
                            try {
                                subBabRepository.updateSubBab(it)
                                Log.d(
                                    "MaterialViewModel",
                                    "SubBab berhasil dirollback ke state sebelumnya"
                                )
                            } catch (rollbackError: Exception) {
                                Log.e("MaterialViewModel", "Gagal rollback SubBab", rollbackError)
                            }
                        }

                        _materialState.value = MaterialState.Error(
                            message = "Gagal upload ${fileData.second}: ${e.message}"
                        )
                        throw e
                    }
                }
            } catch (e: Exception) {
                Log.e("MaterialViewModel", "Gagal update SubBab atau upload file", e)

                if (uploadedMaterials.isNotEmpty()) {
                    rollbackUploadedMaterials(uploadedMaterials)
                }

                existingSubBab?.let {
                    try {
                        subBabRepository.updateSubBab(it)
                        Log.d("MaterialViewModel", "SubBab berhasil dirollback ke state sebelumnya")
                    } catch (rollbackError: Exception) {
                        Log.e("MaterialViewModel", "Gagal rollback SubBab", rollbackError)
                    }
                }

                _materialState.value = MaterialState.Error(
                    message = when (e) {
                        is CancellationException -> "Operasi dibatalkan"
                        else -> "Gagal update SubBab atau upload file: ${e.message}"
                    }
                )
            }
        }
    }

    fun deleteAllMaterialsForSubBab(subBabId: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            try {
                Log.d(
                    "MaterialViewModel",
                    "Memulai penghapusan semua material untuk SubBab: $subBabId"
                )

                withTimeoutOrNull(60000) { // 60 second timeout
                    materialRepository.deleteAllMaterialsForSubBab(subBabId)
                } ?: throw Exception("Timeout saat menghapus material")

                Log.d(
                    "MaterialViewModel",
                    "Berhasil menghapus semua material untuk SubBab: $subBabId"
                )
                onResult(Result.success(Unit))
            } catch (e: Exception) {
                Log.e("MaterialViewModel", "Gagal menghapus material untuk SubBab: $subBabId", e)
                onResult(Result.failure(e))
            }
        }
    }

    sealed class MaterialState {
        data object Loading : MaterialState()
        data class Success(
            val type: String,
            val title: String
        ) : MaterialState()

        data class Error(val message: String) : MaterialState()
    }

    data class UploadProgress(
        val completed: Int,
        val total: Int,
        val message: String
    )

    fun uploadSubBabCover(file: File, subBabId: String) {
        viewModelScope.launch {
            _coverUploadState.value = CoverUploadState.Loading
            try {
                val url = materialRepository.uploadSubBabCover(file, subBabId)
                _coverUploadState.value = CoverUploadState.Success(url)
            } catch (e: IllegalArgumentException) {
                _coverUploadState.value = CoverUploadState.Error(e.message ?: "Gagal upload cover")
            } catch (e: Exception) {
                _coverUploadState.value = CoverUploadState.Error(e.message ?: "Gagal upload cover")
            }
        }
    }

    sealed class CoverUploadState {
        data object Loading : CoverUploadState()
        data class Success(val url: String) : CoverUploadState()
        data class Error(val message: String) : CoverUploadState()
    }

    private val _quizMediaUploadState = MutableLiveData<QuizMediaUploadState>()
    val quizMediaUploadState: LiveData<QuizMediaUploadState> = _quizMediaUploadState

    fun uploadQuizMediaImage(file: File) {
        viewModelScope.launch {
            _quizMediaUploadState.value = QuizMediaUploadState.Loading
            try {
                val url = materialRepository.uploadQuizMediaImage(file)
                _quizMediaUploadState.value = QuizMediaUploadState.Success(url)
            } catch (e: IllegalArgumentException) {
                _quizMediaUploadState.value =
                    QuizMediaUploadState.Error(e.message ?: "Gagal upload gambar")
            } catch (e: Exception) {
                _quizMediaUploadState.value =
                    QuizMediaUploadState.Error(e.message ?: "Gagal upload gambar")
            }
        }
    }

    sealed class QuizMediaUploadState {
        data object Loading : QuizMediaUploadState()
        data class Success(val url: String) : QuizMediaUploadState()
        data class Error(val message: String) : QuizMediaUploadState()
    }
}