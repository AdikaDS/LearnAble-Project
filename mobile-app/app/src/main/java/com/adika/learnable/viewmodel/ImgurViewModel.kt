package com.adika.learnable.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.adika.learnable.api.ApiConfig
import com.adika.learnable.model.ImgurResponse
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class ImgurViewModel(application: Application) : AndroidViewModel(application) {

    private val _uploadResult = MutableLiveData<ImgurResponse?>()
    val uploadResult: LiveData<ImgurResponse?> = _uploadResult

    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun uploadImage(file: File) {
        _loading.value = true
        _error.value = null

        val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
        val imagePart = MultipartBody.Part.createFormData("image", file.name, requestFile)

        ApiConfig.imgurService.uploadImage("Client-ID ${ApiConfig.IMGUR_CLIENT_ID}", imagePart)
            .enqueue(object : Callback<ImgurResponse> {
                override fun onResponse(
                    call: Call<ImgurResponse>,
                    response: Response<ImgurResponse>
                ) {
                    _loading.value = false
                    if (response.isSuccessful) {
                        _uploadResult.value = response.body()
                    } else {
                        _error.value = "Error: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<ImgurResponse>, t: Throwable) {
                    _loading.value = false
                    _error.value = "Error: ${t.message}"
                }
            })
    }

    fun clearResult() {
        _uploadResult.value = null
        _error.value = null
    }
} 