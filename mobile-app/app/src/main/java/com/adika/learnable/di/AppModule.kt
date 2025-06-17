package com.adika.learnable.di

import android.content.Context
import com.adika.learnable.BuildConfig
import com.adika.learnable.api.EmailJSService
import com.adika.learnable.api.ImgurService
import com.adika.learnable.api.TranscriptionService
import com.adika.learnable.util.ResourceProvider
import com.adika.learnable.util.ResourceProviderImp
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    // Network Configuration
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    // Firebase Services
    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    // API Services
    @Provides
    @Singleton
    @Named("imgur")
    fun provideImgurRetrofit(
        okHttpClient: OkHttpClient
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_IMGUR_API)
            .client(okHttpClient.newBuilder()
                .addInterceptor { chain ->
                    val original = chain.request()
                    val request = original.newBuilder()
                        .header("Authorization", "Client-ID ${BuildConfig.IMGUR_CLIENT_ID}")
                        .method(original.method, original.body)
                        .build()
                    chain.proceed(request)
                }
                .build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideImgurService(@Named ("imgur") imgurRetrofit: Retrofit): ImgurService {
        return imgurRetrofit.create(ImgurService::class.java)
    }

    @Provides
    @Singleton
    @Named ("transcribe")
    fun provideTranscribeRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_TRANSCRIPTION_API)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTranscribeService(@Named("transcribe") transcribeRetrofit: Retrofit): TranscriptionService {
        return transcribeRetrofit.create(TranscriptionService::class.java)
    }

    @Provides
    @Singleton
    @Named ("emailjs")
    fun provideEmailJSRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_EMAILJS_API)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideEmailJSService(@Named ("emailjs") emailJSService: Retrofit) : EmailJSService {
        return emailJSService.create(EmailJSService::class.java)
    }

    // Resource Provider
    @Provides
    @Singleton
    fun provideResourceProvider(
        @ApplicationContext context: Context
    ): ResourceProvider = ResourceProviderImp(context)

    // AWS Credentials
    @Provides
    @Singleton
    fun provideAwsCredentials(): BasicAWSCredentials {
        return BasicAWSCredentials(
            BuildConfig.AWS_ACCESS_KEY,
            BuildConfig.AWS_SECRET_KEY
        )
    }

    // S3 Client
    @Provides
    @Singleton
    fun provideS3Client(credentials: BasicAWSCredentials): AmazonS3Client {
        return AmazonS3Client(credentials).apply {
            setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))
        }
    }
} 