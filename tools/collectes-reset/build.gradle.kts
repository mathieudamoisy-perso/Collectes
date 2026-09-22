plugins {
    id("com.android.application") version "9.4.1"
    id("org.jetbrains.kotlin.android") version "2.2.10"
}

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "com.collectes.reset"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.collectes.reset"
        minSdk = 26
        targetSdk = 36
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
}

dependencies {
    // aucune — Activity pure
}
