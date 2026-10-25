// StreamLive (Platforma) — конфигурация модуля app, готовая к релизу
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "app.platforma.streamlive"
    compileSdk = 34

    defaultConfig {
        applicationId = "app.platforma.streamlive"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    // Сигнатура Release: создайте keystore и пропишите пути/секреты через переменные окружения
    signingConfigs {
        create("release") {
            // storeFile = file(System.getenv("KEYSTORE_PATH") ?: "release.keystore")
            // storePassword = System.getenv("KEYSTORE_PASSWORD") ?: ""
            // keyAlias = System.getenv("KEY_ALIAS") ?: ""
            // keyPassword = System.getenv("KEY_PASSWORD") ?: ""
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true          // R8 — сжатие и обфускация кода
            isShrinkResources = true        // Удаляет неиспользуемые ресурсы
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("release")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Базовые AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Официальный API Splash Screen (AndroidX Core)
    implementation(libs.androidx.core.splashscreen)

    // Jetpack Compose (BOM гарантирует совместимость версий библиотек)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // OkHttp — HEAD-запросы для умного Failover
    implementation(libs.okhttp)

    // Отладочные инструменты Compose
    debugImplementation(libs.androidx.ui.tooling)
}
