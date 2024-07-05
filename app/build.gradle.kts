plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id ("kotlin-kapt")
    id ("kotlin-android")
    id ("androidx.navigation.safeargs") version "2.7.7"
    id("de.undercouch.download") version "5.6.0"
}

apply(plugin= "androidx.navigation.safeargs")

android {
    namespace = "com.example.findobject"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.findobject"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
//    androidResources {
//        noCompress 'tflite'
//    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation ("androidx.fragment:fragment-ktx:1.8.1")


    // Navigation library
    implementation ("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation ("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Kotlin
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Feature module Support
    implementation("androidx.navigation:navigation-dynamic-features-fragment:2.7.7")

    // Testing Navigation
    androidTestImplementation("androidx.navigation:navigation-testing:2.7.7")

    // Jetpack Compose Integration
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // CameraX core library
    implementation ("androidx.camera:camera-core:1.3.4")

    // CameraX Camera2 extensions
    implementation ("androidx.camera:camera-camera2:1.3.4")

    // CameraX Lifecycle library
    implementation ("androidx.camera:camera-lifecycle:1.3.4")

    // CameraX View class
    implementation ("androidx.camera:camera-view:1.3.4")

    implementation ("com.google.mlkit:barcode-scanning:17.2.0")
    implementation ("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.0")
    implementation ("com.google.android.material:material:1.12.0")
    implementation ("com.intuit.sdp:sdp-android:1.1.0")

    implementation ("com.google.zxing:core:3.4.1")

    //WindowManager
    implementation ("androidx.window:window:1.3.0")

    implementation ("com.google.mediapipe:tasks-vision:0.20230731")
    implementation ("np.com.susanthapa:curved_bottom_navigation:0.6.5")


    implementation("com.google.mlkit:vision-common:17.3.0")
    implementation ("com.quickbirdstudios:opencv:4.5.3.0")

    implementation ("com.github.bumptech.glide:glide:4.16.0")
    implementation ("com.google.code.gson:gson:2.10.1")
    implementation ("com.google.guava:guava:31.1-android")
    implementation ("org.tensorflow:tensorflow-lite:2.13.0")
    implementation ("org.tensorflow:tensorflow-lite-api:2.13.0")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.10.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.4")

    implementation ("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    implementation ("org.tensorflow:tensorflow-lite-task-vision:0.4.0")
    // Import the GPU delegate plugin Library for GPU inferenc
    implementation ("org.tensorflow:tensorflow-lite-gpu-delegate-plugin:0.4.4")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.10.0")


    implementation("androidx.camera:camera-mlkit-vision:1.4.0-beta02")
    // If you want to additionally use the CameraX Extensions library
    implementation("androidx.camera:camera-extensions:1.4.0-beta02")
    implementation("com.google.mlkit:text-recognition:16.0.0")

}

project.ext["ASSET_DIR"] = "$projectDir/src/main/assets/"
project.ext["TEST_ASSET_DIR"] = "$projectDir/src/androidTest/assets/"
apply(from = "download_tasks.gradle")