
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")       // ✅ Keep only this
    id("org.jetbrains.kotlin.kapt")          // ✅ For annotation processor support
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.firetv"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.firetv"
        minSdk = 21
        targetSdk = 35
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
    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.leanback)
    implementation("com.google.code.gson:gson:2.10.1")

    // Glide
    implementation(libs.glide)

    // Firebase
    implementation("com.google.firebase:firebase-analytics-ktx:21.4.0")
    implementation("com.google.firebase:firebase-database-ktx:20.3.0")

    // ✅ GMS (play-services) - aligned versions
    implementation("com.google.android.gms:play-services-measurement-api:21.4.0")
    implementation("com.google.android.gms:play-services-measurement-impl:21.4.0")
    implementation("com.google.android.gms:play-services-measurement:21.4.0")
    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")

}


