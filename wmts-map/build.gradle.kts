plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    iosX64()

    sourceSets {
        commonMain.dependencies {
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "com.example.wmtsmap"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }
}
