plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.ucompensar.vetcare"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.ucompensar.vetcare"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = true
                packageScope = setOf("androidx.**", "kotlin.**", "kotlinx.**")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    // ===== RETROFIT: consumir la API REST =====
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // ===== OKHTTP: ver las peticiones en el Logcat =====
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // ===== COROUTINES: esperar al servidor sin congelar la pantalla =====
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // ===== TOKEN CIFRADO =====
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // ===== LISTAS =====
    implementation("androidx.recyclerview:recyclerview:1.3.2")
}