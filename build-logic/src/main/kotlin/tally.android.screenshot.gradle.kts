// SPDX-License-Identifier: GPL-3.0-or-later
// Tests de capture Roborazzi (bibliothèque seule, sans plugin Gradle Roborazzi).
// Écrit les PNG en mode record (défaut) ; -Proborazzi.mode=verify compare aux références.
plugins {
    id("tally.android.library")
    id("tally.android.compose")
}

android {
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    "testImplementation"(libs.lib("robolectric"))
    "testImplementation"(libs.lib("androidx-test-core"))
    "testImplementation"(libs.lib("androidx-compose-ui-test-junit4"))
    "testImplementation"(libs.lib("roborazzi"))
    "testImplementation"(libs.lib("roborazzi-compose"))
    "testImplementation"(libs.lib("roborazzi-junit-rule"))
    "debugImplementation"(libs.lib("androidx-compose-ui-test-manifest"))
}

val screenshotMode = providers.gradleProperty("roborazzi.mode").getOrElse("record")

tasks.withType<Test>().configureEach {
    systemProperty(
        if (screenshotMode == "verify") "roborazzi.test.verify" else "roborazzi.test.record",
        "true",
    )
}
