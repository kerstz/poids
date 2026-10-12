// SPDX-License-Identifier: GPL-3.0-or-later
plugins {
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

dependencies {
    "implementation"(libs.lib("hilt-android"))
    "ksp"(libs.lib("hilt-compiler"))
}
