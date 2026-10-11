plugins {
    id("tally.android.library")
    id("tally.android.compose")
}

android {
    namespace = "org.tally.coreui"
}

dependencies {
    implementation(project(":core"))
    implementation(libs.vico.compose.m3)
}
