// Módulo :app — DualStreamPlayer Pro
// compileSdk 36 | targetSdk 36 | minSdk 26 | Java 17 | Kotlin 2.1.20

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace   = "com.dualstreamplayer.pro"
    compileSdk  = 36

    defaultConfig {
        applicationId  = "com.dualstreamplayer.pro"
        minSdk         = 26
        targetSdk      = 36
        versionCode    = 1
        versionName    = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") // debug key para testes; troque por keystore de produção
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    // ── Java 17 obrigatório para AGP 8.9+ e Kotlin 2.1 ──────────────────────
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
        // Opt-ins e otimizações recomendadas para Kotlin 2.x
        freeCompilerArgs += listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-opt-in=androidx.media3.common.util.UnstableApi"
        )
    }

    // ── View Binding (substitui findViewById) ───────────────────────────────
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    // ── Packaging — evitar duplicatas em builds release ──────────────────────
    packaging {
        resources {
            excludes += setOf(
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Dependências
// ──────────────────────────────────────────────────────────────────────────────
dependencies {

    // ── AndroidX Core ────────────────────────────────────────────────────────
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.transition:transition:1.5.1")

    // ── Material Design 3 ────────────────────────────────────────────────────
    implementation("com.google.android.material:material:1.13.0-beta01")

    // ── AndroidX Media3 (ExoPlayer HLS) ──────────────────────────────────────
    // Versão 1.6.1 — última estável com suporte a API 36
    val media3Version = "1.6.1"
    implementation("androidx.media3:media3-exoplayer:$media3Version")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Version")
    implementation("androidx.media3:media3-ui:$media3Version")
    implementation("androidx.media3:media3-common:$media3Version")
    implementation("androidx.media3:media3-datasource:$media3Version")
    implementation("androidx.media3:media3-session:$media3Version")

    // ── Lifecycle ────────────────────────────────────────────────────────────
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.0")
    implementation("androidx.lifecycle:lifecycle-process:2.9.0")

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.1")

    // ── Testes ───────────────────────────────────────────────────────────────
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
