plugins {
    id("tally.android.feature")
    id("tally.android.screenshot")
}

android {
    namespace = "org.tally.nutrition"
}

dependencies {
    implementation(libs.androidx.compose.material.icons.core)
}
