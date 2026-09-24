// File: <PROJECT_ROOT>/app/build.gradle.kts

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    kotlin("kapt") // This is the Kotlin-DSL way to write "id 'kotlin-kapt'"
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.kal"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.kal"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        compose = true
    }

    // ⬇️ FIX 1: Added this block to match Kotlin 1.9.23 ⬇️
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
} // <-- ❗ FIX 2: The 'android' block ends here

// ❗ FIX 2: The 'dependencies' block must be outside the 'android' block
dependencies {
    // --- Jetpack Compose ---
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // --- Core & UI Dependencies ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.activity:activity-compose")

    // --- Lifecycle & ViewModel ---
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.0")

    // --- Navigation ---
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // --- Room Database (Local Storage) ---
    implementation("androidx.room:room-runtime:2.6.1") // <-- NEW
    implementation("androidx.room:room-ktx:2.6.1")     // <-- NEW
    kapt("androidx.room:room-compiler:2.6.1")      // <-- NEW

    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-functions-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // --- AdMob ---
    // ⬇️ FIX 3: Replaced 'libs.play.services.ads.api' ⬇️
    implementation("com.google.android.gms:play-services-ads:23.1.0")
    implementation("com.google.android.material:material:1.12.0")

    // --- Hilt (Dependency Injection) ---
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")

    // --- Coroutines (Updated) ---
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // --- Other Libraries (Gemini Updated) ---
    implementation("io.ktor:ktor-client-android:2.3.9")
    implementation("io.coil-kt:coil-compose:2.6.0")
    //
    // ✅ THIS IS THE CORRECTED LINE:
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    //
    implementation("io.ktor:ktor-client-content-negotiation:2.3.9")
    implementation("io.ktor:ktor-serialization-gson:2.3.9")

    // --- Testing ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.05.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}