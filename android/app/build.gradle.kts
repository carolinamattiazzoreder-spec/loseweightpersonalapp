import java.net.URI

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val figtreeResDir: File = layout.buildDirectory.dir("generated/figtree/res").get().asFile
val figtreeUrl = "https://raw.githubusercontent.com/google/fonts/main/ofl/figtree/Figtree%5Bwght%5D.ttf"

android {
    namespace = "com.weighttracker.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.weighttracker.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    sourceSets["main"].res.srcDir(figtreeResDir)
}

// Figtree (SIL Open Font License), the design's typeface. Downloaded at build time
// instead of committed; if the download fails the app falls back to the system font.
val downloadFigtree by tasks.registering {
    val target = figtreeResDir.resolve("font/figtree.ttf")
    outputs.dir(figtreeResDir)
    doLast {
        if (target.length() > 0) return@doLast
        target.parentFile.mkdirs()
        try {
            URI(figtreeUrl).toURL().openStream().use { input ->
                target.outputStream().use { output -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            target.delete()
            logger.warn("Figtree font not downloaded (${e.message}); using the system font.")
        }
    }
}

tasks.named("preBuild") { dependsOn(downloadFigtree) }

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // Real org.json for JVM unit tests (android.jar only ships stubs).
    testImplementation("org.json:json:20240303")
}
