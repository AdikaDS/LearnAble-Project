plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.hilt)
    alias(libs.plugins.safe.args)
    kotlin("kapt")
    id("kotlin-parcelize")
}

android {
    namespace = "com.adika.learnable"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.adika.learnable"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BASE_URL_IMGUR_API", "\"https://api.imgur.com/3/\"")
        buildConfigField("String", "BASE_URL_TRANSCRIPTION_API", "\"http://192.168.0.108:8000/\"")
        buildConfigField("String", "BASE_URL_EMAILJS_API", "\"https://api.emailjs.com/\"")
//        https://learnable-whisper-api-app.azurewebsites.net/
        buildConfigField("String", "IMGUR_CLIENT_ID", "\"0084c0adcd8d6f2\"")
        buildConfigField("String", "AWS_ACCESS_KEY", "\"AKIAXFKVXH4LEZAU6VP7\"")
        buildConfigField("String", "AWS_SECRET_KEY", "\"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\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += "META-INF/gradle/incremental.annotation.processors"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.circle.image)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.glide)
    implementation(libs.image.picker)
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.gson)
    implementation(libs.okhttp3)
    implementation(libs.navigation.ui)
    implementation(libs.navigation.fragment)
    implementation(libs.hilt.android)
    implementation(libs.androidx.swiperefreshlayout)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation)
    implementation(libs.splash.screen)
    implementation(libs.lottie)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ExoPlayer untuk video dan audio
    implementation(libs.exoplayer.core)
    implementation(libs.exoplayer.ui)

    // AWS
    implementation(libs.aws.sdk)
    implementation(libs.aws.sdk.mobile.client)
    
    // PDF Viewer
    implementation(libs.pdf.viewer)
}