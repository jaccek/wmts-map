plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget()

    listOf(
        iosArm64(),
        iosSimulatorArm64(),
        iosX64(),
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Example"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":wmts-map"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }

        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
        }
    }
}

android {
    namespace = "com.example.wmtsmap.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.wmtsmap.example"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
}
