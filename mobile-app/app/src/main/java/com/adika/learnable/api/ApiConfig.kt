package com.adika.learnable.api

import com.adika.learnable.BuildConfig
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {

    private const val IMGUR_BASE_URL = BuildConfig.BASE_URL_IMGUR_API

    const val IMGUR_CLIENT_ID = BuildConfig.IMGUR_CLIENT_ID // Ganti dengan Client ID Imgur Anda

    private const val CONNECT_TIMEOUT = 30L
    private const val READ_TIMEOUT = 30L
    private const val WRITE_TIMEOUT = 30L

    private val imgurClient = OkHttpClient.Builder()
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("Authorization", "Client-ID $IMGUR_CLIENT_ID")
                .method(original.method, original.body)
                .build()
            chain.proceed(request)
        }
        .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
        .build()

    private val imgurRetrofit = Retrofit.Builder()
        .baseUrl(IMGUR_BASE_URL)
        .client(imgurClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val imgurApi: ImgurApi = imgurRetrofit.create(ImgurApi::class.java)

    // Add more API configurations here as needed
}