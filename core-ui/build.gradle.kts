plugins {
    id("tally.android.library")
    id("tally.android.compose")
    id("tally.android.screenshot")
}

android {
    namespace = "org.tally.coreui"
}

dependencies {
    implementation(project(":core"))
    implementation(libs.vico.compose.m3)
}
