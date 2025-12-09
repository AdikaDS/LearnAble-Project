package com.adika.learnable.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.adika.learnable.model.SubBab
import com.adika.learnable.repository.MaterialRepository
import com.adika.learnable.repository.SubBabRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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

    fun createSubBabAndUploadFiles(subBab: SubBab, pendingUploads: Map<String, Pair<File, String>>) {
        viewModelScope.launch {
            try {
                _materialState.value = MaterialState.Loading

                // Buat SubBab baru dengan timeout
                val createdSubBab = withTimeoutOrNull(30000) { // 30 second timeout
                    subBabRepository.addSubBab(subBab)
                } ?: throw Exception("Timeout saat membuat SubBab")

                Log.d("MaterialViewModel", "SubBab berhasil dibuat dengan ID: ${createdSubBab.id}")

                // Debug: Cek pendingUploads
                Log.d("MaterialViewModel", "Jumlah file yang akan diupload: ${pendingUploads.size}")
                pendingUploads.forEach { (type, fileData) ->
                    Log.d("MaterialViewModel", "File type: $type, title: ${fileData.second}")
                }

                // Upload semua file yang pending
                val totalFiles = pendingUploads.size
                var completedFiles = 0

                pendingUploads.forEach { (type, fileData) ->
                    try {
                        val (file, title) = fileData
                        if (!file.exists()) {
                            throw Exception("File tidak ditemukan: ${file.absolutePath}")
                        }

                        Log.d("MaterialViewModel", "Memulai upload file: $title dengan tipe: $type")

                        withTimeoutOrNull(60000) { // 60 second timeout per file
                            when (type) {
                                MaterialRepository.MaterialType.VIDEO.name -> {
                                    materialRepository.uploadVideoMaterial(file, title, createdSubBab.id)
                                    _materialState.value = MaterialState.Success(
                                        type = MaterialRepository.MaterialType.VIDEO.name,
                                        title = title
                                    )
                                }
                                MaterialRepository.MaterialType.AUDIO.name -> {
                                    materialRepository.uploadAudioMaterial(file, title, createdSubBab.id)
                                    _materialState.value = MaterialState.Success(
                                        type = MaterialRepository.MaterialType.AUDIO.name,
                                        title = title
                                    )
                                }
                                MaterialRepository.MaterialType.PDF.name -> {
                                    materialRepository.uploadPdfMaterial(file, title, createdSubBab.id)
                                    _materialState.value = MaterialState.Success(
                                        type = MaterialRepository.MaterialType.PDF.name,
                                        title = title
                                    )
                                }
                            }
                        } ?: throw Exception("Timeout saat upload file: $title")

                        completedFiles++
                        _uploadProgress.value = UploadProgress(
                            completed = completedFiles,
                            total = totalFiles,
                            message = "Mengupload file $completedFiles dari $totalFiles"
                        )

                    } catch (e: Exception) {
                        Log.e("MaterialViewModel", "Gagal upload file: ${fileData.second}", e)
                        _materialState.value = MaterialState.Error(
                            message = "Gagal upload ${fileData.second}: ${e.message}"
                        )
                        throw e
                    }
                }
            } catch (e: Exception) {
                Log.e("MaterialViewModel", "Gagal membuat SubBab atau upload file", e)
                _materialState.value = MaterialState.Error(
                    message = when (e) {
                        is kotlinx.coroutines.CancellationException -> "Operasi dibatalkan"
                        else -> "Gagal membuat SubBab atau upload file: ${e.message}"
                    }
                )
            }
        }
    }

    fun updateSubBabAndUploadFiles(subBab: SubBab, pendingUploads: Map<String, Pair<File, String>>) {
        viewModelScope.launch {
            try {
                _materialState.value = MaterialState.Loading
                
                // Ambil data SubBab yang ada untuk memastikan mediaUrls yang tidak diedit tetap terjaga
                val existingSubBab = subBabRepository.getSubBab(subBab.id)
                
                // Gabungkan mediaUrls yang ada dengan yang baru
                val updatedMediaUrls = mutableMapOf<String, String>().apply {
                    // Pertahankan URL yang ada
                    putAll(existingSubBab.mediaUrls)
                    // Update dengan URL baru (baik yang sudah ada maupun belum)
                    putAll(subBab.mediaUrls.filter { (_, value) -> value.isNotEmpty() })
                }
                
                // Update SubBab dengan mediaUrls yang sudah digabungkan
                val updatedSubBab = subBab.copy(mediaUrls = updatedMediaUrls)
                
                // Update SubBab
                subBabRepository.updateSubBab(updatedSubBab)
                Log.d("MaterialViewModel", "SubBab berhasil diupdate: ${updatedSubBab.id}")
                
                // Debug: Cek pendingUploads
                Log.d("MaterialViewModel", "Jumlah file yang akan diupload: ${pendingUploads.size}")
                pendingUploads.forEach { (type, fileData) ->
                    Log.d("MaterialViewModel", "File type: $type, title: ${fileData.second}")
                }
                
                // Upload semua file yang pending
                val totalFiles = pendingUploads.size
                var completedFiles = 0
                
                pendingUploads.forEach { (type, fileData) ->
                    try {
                        val (file, title) = fileData
                        if (!file.exists()) {
                            throw Exception("File tidak ditemukan: ${file.absolutePath}")
                        }
                        
                        Log.d("MaterialViewModel", "Memulai upload file: $title dengan tipe: $type")
                        
                        withTimeoutOrNull(60000) { // 60 second timeout per file
                            when (type) {
                                MaterialRepository.MaterialType.VIDEO.name -> {
                                    materialRepository.uploadVideoMaterial(file, title, updatedSubBab.id)
                                    _materialState.value = MaterialState.Success(
                                        type = MaterialRepository.MaterialType.VIDEO.name,
                                        title = title
                                    )
                                }
                                MaterialRepository.MaterialType.AUDIO.name -> {
                                    materialRepository.uploadAudioMaterial(file, title, updatedSubBab.id)
                                    _materialState.value = MaterialState.Success(
                                        type = MaterialRepository.MaterialType.AUDIO.name,
                                        title = title
                                    )
                                }
                                MaterialRepository.MaterialType.PDF.name -> {
                                    materialRepository.uploadPdfMaterial(file, title, updatedSubBab.id)
                                    _materialState.value = MaterialState.Success(
                                        type = MaterialRepository.MaterialType.PDF.name,
                                        title = title
                                    )
                                }
                            }
                        } ?: throw Exception("Timeout saat upload file: $title")
                        
                        completedFiles++
                        _uploadProgress.value = UploadProgress(
                            completed = completedFiles,
                            total = totalFiles,
                            message = "Mengupload file $completedFiles dari $totalFiles"
                        )

                    } catch (e: Exception) {
                        Log.e("MaterialViewModel", "Gagal upload file: ${fileData.second}", e)
                        _materialState.value = MaterialState.Error(
                            message = "Gagal upload ${fileData.second}: ${e.message}"
                        )
                        throw e
                    }
                }
            } catch (e: Exception) {
                Log.e("MaterialViewModel", "Gagal update SubBab atau upload file", e)
                _materialState.value = MaterialState.Error(
                    message = when (e) {
                        is kotlinx.coroutines.CancellationException -> "Operasi dibatalkan"
                        else -> "Gagal update SubBab atau upload file: ${e.message}"
                    }
                )
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
}