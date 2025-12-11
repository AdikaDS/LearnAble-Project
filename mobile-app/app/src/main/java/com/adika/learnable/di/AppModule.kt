package com.adika.learnable.di

import android.content.Context
import com.adika.learnable.BuildConfig
import com.adika.learnable.api.DialogflowService
import com.adika.learnable.api.FeedbackService
import com.adika.learnable.api.GeminiApiService
import com.adika.learnable.api.RegionService
import com.adika.learnable.api.SendEmailService
import com.adika.learnable.api.TokenDialogflowService
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

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    @Singleton
    @Named("dialogflow")
    fun provideDialogflowRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_DIALOGFLOW)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideDialogflowService(@Named("dialogflow") dialogflowRetrofit: Retrofit): DialogflowService {
        return dialogflowRetrofit.create(DialogflowService::class.java)
    }

    @Provides
    @Singleton
    @Named("backend")
    fun provideBackendRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_BACKEND)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideTokenDialogflowService(@Named("backend") backendRetrofit: Retrofit): TokenDialogflowService {
        return backendRetrofit.create(TokenDialogflowService::class.java)
    }

    @Provides
    @Singleton
    fun provideGeminiApiService(@Named("backend") backendRetrofit: Retrofit): GeminiApiService {
        return backendRetrofit.create(GeminiApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSendEmailService(@Named("backend") backendRetrofit: Retrofit): SendEmailService {
        return backendRetrofit.create(SendEmailService::class.java)
    }

    @Provides
    @Singleton
    @Named("region")
    fun provideRegionRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_REGION)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideRegionService(@Named("region") regionRetrofit: Retrofit): RegionService {
        return regionRetrofit.create(RegionService::class.java)
    }

    @Provides
    @Singleton
    @Named("feedback")
    fun provideFeedbackRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL_FEEDBACK)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideFeedbackService(@Named("feedback") feedbackRetrofit: Retrofit): FeedbackService {
        return feedbackRetrofit.create(FeedbackService::class.java)
    }

    @Provides
    @Singleton
    fun provideResourceProvider(
        @ApplicationContext context: Context
    ): ResourceProvider = ResourceProviderImp(context)

    @Provides
    @Singleton
    fun provideAwsCredentials(): BasicAWSCredentials {
        return BasicAWSCredentials(
            BuildConfig.AWS_ACCESS_KEY,
            BuildConfig.AWS_SECRET_KEY
        )
    }

    @Provides
    @Singleton
    fun provideS3Client(credentials: BasicAWSCredentials): AmazonS3Client {
        return AmazonS3Client(credentials).apply {
            setRegion(Region.getRegion(Regions.AP_SOUTHEAST_1))
        }
    }
} 