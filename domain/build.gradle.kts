plugins {
    id("tally.jvm.library")
}

dependencies {
    api(project(":core"))
    implementation(libs.kotlinx.coroutines.core)
}

dependencies {
    testImplementation(libs.kotlinx.coroutines.test)
}
