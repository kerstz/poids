plugins {
    id("tally.android.library")
    id("tally.android.compose")
    id("tally.android.hilt")
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    "implementation"(project(":core"))
    "implementation"(project(":domain"))
    "implementation"(project(":core-ui"))
    "implementation"(libs.lib("kotlinx-coroutines-core"))
    "implementation"(libs.lib("kotlinx-serialization-json"))
    "implementation"(libs.lib("androidx-navigation-compose"))
    "implementation"(libs.lib("androidx-lifecycle-runtime-compose"))
    "implementation"(libs.lib("androidx-lifecycle-viewmodel-compose"))
    "implementation"(libs.lib("androidx-hilt-navigation-compose"))
}
