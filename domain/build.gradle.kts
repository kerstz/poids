// :domain — module Kotlin pur (aucune dépendance Android). Logique métier testable :
// calculs BMR/TDEE/MET, modèles, interfaces de repository.
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.bundles.test.jvm)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
