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

        buildConfigField("String", "BASE_URL_BACKEND", "\"https://learnable-project.onrender.com/\"")
        buildConfigField("String", "BASE_URL_DIALOGFLOW", "\"https://dialogflow.googleapis.com/\"")
        buildConfigField("String", "BASE_URL_REGION", "\"https://emsifa.github.io/api-wilayah-indonesia/\"")
        buildConfigField("String", "BASE_URL_FEEDBACK", "\"https://script.google.com/macros/s/AKfycbx2kyJp70KkiSDCF6d53I5jLVSUs085C2n-rOMbWBUCCVYgf9uJMo7DGpKMfITxG8RA/\"")
        buildConfigField("String", "AWS_ACCESS_KEY", "\"AKIAXFKVXH4LEZAU6VP7\"")
        buildConfigField("String", "AWS_SECRET_KEY", "\"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\"")
        buildConfigField("String", "S3_BUCKET_NAME", "\"learnable-lessons-bucket\"")

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
        isCoreLibraryDesugaringEnabled = true
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
    implementation(libs.flex.box)
    implementation(libs.textdrawable)
    implementation(libs.ui.firestore)
    implementation(libs.kizitonwose.calendar)
    coreLibraryDesugaring(libs.desugar)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ExoPlayer untuk video dengan fitur lengkap
    implementation(libs.media3.exoplayer.core)
    implementation(libs.media3.exoplayer.ui)
    implementation(libs.media3.exoplayer.dash)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.exoplayer.rtsp)
    implementation(libs.media3.datasource)
    implementation(libs.media3.datasource.okhttp)

    // AWS
    implementation(libs.aws.sdk)
    implementation(libs.aws.sdk.mobile.client)
    
    // PDF Viewer
    implementation(libs.pdf.viewer)
}
