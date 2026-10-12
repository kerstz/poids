// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.lib("junit"))
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
