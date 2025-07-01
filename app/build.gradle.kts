
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")       // ✅ Keep only this
    id("org.jetbrains.kotlin.kapt")          // ✅ For annotation processor support
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.firetv"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.firetv"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    lint {
        abortOnError = false
    }
    buildFeatures {
        viewBinding = true
    }
}



dependencies {
    // AndroidX Core + UI
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.fragment:fragment-ktx:1.5.0")
    implementation(libs.androidx.fragment)

    // ✅ Leanback (downgraded for AGP 8.2.0 & SDK 34)
    implementation("androidx.leanback:leanback:1.1.0-alpha02")
    implementation("androidx.leanback:leanback-grid:1.0.0-alpha01")

    // Material Design & Shimmer
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.facebook.shimmer:shimmer:0.5.0")

    // Firebase
    implementation("com.google.firebase:firebase-analytics-ktx:21.4.0")
    implementation("com.google.firebase:firebase-database-ktx:20.3.0")

    // Glide for image loading
    implementation("com.github.bumptech.glide:glide:4.15.1")
    implementation(libs.androidx.adapters)
    implementation(libs.androidx.material3.android)
    kapt("com.github.bumptech.glide:compiler:4.15.1") // ✅ Kotlin annotation processing

    // Networking + JSON
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.google.code.gson:gson:2.10.1") // Keep the latest Gson only
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.1")
    implementation("androidx.leanback:leanback:1.0.0")
    implementation("jp.wasabeef:glide-transformations:4.3.0")
    // Cronet
    implementation(libs.cronet.embedded)
    implementation("com.google.android.material:material:1.12.0")
    // GMS (Google Play Services Measurement)
    implementation("com.google.android.gms:play-services-measurement-api:21.4.0")
    implementation("com.google.android.gms:play-services-measurement-impl:21.4.0")
    implementation("com.google.android.gms:play-services-measurement:21.4.0")
}
