plugins {
    id("tally.android.feature")
    id("tally.android.screenshot")
}

android {
    namespace = "org.tally.nutrition"
}

dependencies {
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    implementation(libs.zxing.cpp)
}
