plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
        targetSdk = 36
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

    viewBinding.enable = true
}

dependencies {


        implementation("io.github.rosemoe:editor:0.23.7")


    // Kotlin 协程核心库
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
// Kotlin 协程 Android 支持（包括 Dispatchers.Main 和 lifecycleScope）
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
// AndroidX Lifecycle 扩展，提供 lifecycleScope
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    implementation(project(":editor"))
    implementation(project(":utils"))
    implementation(project(":jfacetext"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}