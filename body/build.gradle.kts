plugins {
    id("tally.android.feature")
    id("tally.android.screenshot")
}

android {
    namespace = "org.tally.body"
}

dependencies {
    testImplementation(libs.kotlinx.coroutines.test)
}
