plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.narendar.hub"
    compileSdk = 35

    defaultConfig {
        // Separate package name prevents failed upgrades caused by older debug APKs
        // signed with a different temporary debug keystore.
        applicationId = "com.narendar.studypilotai"
        minSdk = 21
        targetSdk = 35
        versionCode = 3
        versionName = "2.1.0"
    }

    buildFeatures { compose = true }

    // Java 8 bytecode gives the widest practical runtime compatibility while
    // the build itself still uses the Java 17 toolchain required by modern AGP/Kotlin.
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions { jvmTarget = "1.8" }
}

dependencies {
    val bom = platform("androidx.compose:compose-bom:2025.06.01")
    implementation(bom)
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.1")
    debugImplementation("androidx.compose.ui:ui-tooling")
}
