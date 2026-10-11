plugins {
    id("tally.android.library")
    id("tally.android.hilt")
    id("tally.kotlin.serialization")
}

android {
    namespace = "org.tally.data"

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

// Schémas exportés visibles des tests de migration (MigrationTestHelper lit les assets).
androidComponents {
    onVariants { variant ->
        variant.nestedComponents
            .filterIsInstance<com.android.build.api.variant.UnitTest>()
            .forEach { it.sources.assets?.addStaticSourceDirectory("$projectDir/schemas") }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.room.runtime)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    ksp(libs.room.compiler)

    testImplementation(libs.robolectric)
    testImplementation(libs.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
