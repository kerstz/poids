plugins {
    id("com.android.application")
    id("tally.android.compose")
    id("tally.android.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    compileSdk = COMPILE_SDK

    defaultConfig {
        minSdk = MIN_SDK
        targetSdk = TARGET_SDK
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
