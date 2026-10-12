plugins {
    id("tally.android.application")
}

android {
    namespace = "org.tally.app"

    defaultConfig {
        applicationId = "org.tally.app"
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    // F-Droid : pas de bloc de métadonnées de dépendances signé par Google.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":domain"))
    implementation(project(":data"))
    implementation(project(":core-ui"))
    implementation(project(":nutrition"))
    implementation(project(":body"))
    implementation(project(":activity"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
}

// Garde-fou permissions : le manifeste release fusionné doit rester dans la liste blanche.
val checkPermissions = tasks.register("checkPermissions") {
    group = "verification"
    description = "Compare les permissions du manifeste release fusionné à config/permissions-allowlist.txt."
    dependsOn("processReleaseManifest")
    val manifest = layout.buildDirectory.file(
        "intermediates/merged_manifests/release/processReleaseManifest/AndroidManifest.xml",
    )
    val allowlist = rootProject.layout.projectDirectory.file("config/permissions-allowlist.txt")
    val applicationId = "org.tally.app"
    doLast {
        val allowed = allowlist.asFile.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .map { it.replace("\${applicationId}", applicationId) }
            .toSet()
        val used = Regex("""<uses-permission[^>]*android:name="([^"]+)"""")
            .findAll(manifest.get().asFile.readText())
            .map { it.groupValues[1] }
            .toSet()
        val text = manifest.get().asFile.readText()
        if ("android.permission.CAMERA" in used) {
            Regex("""<uses-feature[^>]*android:name="(android\.hardware\.camera[^"]*)"[^>]*>""").findAll(text).forEach { m ->
                if (!m.value.contains("""android:required="false"""")) {
                    throw GradleException("uses-feature ${m.groupValues[1]} doit être required=\"false\" (app installable sans caméra)")
                }
            }
        }
        val extra = used - allowed
        if (extra.isNotEmpty()) {
            throw GradleException("Permissions non autorisées dans le manifeste release: $extra")
        }
        logger.lifecycle("Permissions OK: ${used.sorted()}")
    }
}
