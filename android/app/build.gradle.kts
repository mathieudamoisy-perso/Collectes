import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

kotlin {
    jvmToolchain(17)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.collectes.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.collectes.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "1.9.0"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
    ksp("androidx.room:room-compiler:2.7.2")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jsoup:jsoup:1.18.3")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
}

// Stage l’APK + assure le daemon « Reset Collectes » sur l’émulateur (pas de watcher PC).
afterEvaluate {
    tasks.named("installDebug").configure {
        doLast {
            val apk = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk").get().asFile
            if (!apk.exists()) {
                logger.warn("APK debug introuvable, stage reset ignoré: ${apk.absolutePath}")
                return@doLast
            }
            val adbHome = System.getenv("ANDROID_HOME")
                ?: System.getenv("ANDROID_SDK_ROOT")
                ?: "${System.getenv("LOCALAPPDATA")}/Android/Sdk"
            val adbExt = if (System.getProperty("os.name").startsWith("Windows")) ".exe" else ""
            val adb = "$adbHome/platform-tools/adb$adbExt"
            val serial = System.getenv("ANDROID_SERIAL") ?: "emulator-5554"
            val push = ProcessBuilder(
                adb, "-s", serial, "push", apk.absolutePath, "/data/local/tmp/collectes-debug.apk"
            ).redirectErrorStream(true).start()
            val pushOut = push.inputStream.bufferedReader().readText()
            val pushCode = push.waitFor()
            if (pushCode != 0) {
                logger.warn("Stage reset APK échoué ($pushCode): $pushOut")
                return@doLast
            }
            logger.lifecycle("APK stagé pour Reset Collectes → /data/local/tmp/collectes-debug.apk")

            val ensureScript = rootProject.projectDir.resolve("../tools/ensure-reset-daemon.ps1")
            if (!ensureScript.isFile) {
                logger.warn("ensure-reset-daemon.ps1 introuvable: ${ensureScript.absolutePath}")
                return@doLast
            }
            val ensure = ProcessBuilder(
                "powershell",
                "-NoProfile",
                "-ExecutionPolicy",
                "Bypass",
                "-File",
                ensureScript.absolutePath
            ).apply {
                directory(ensureScript.parentFile)
                redirectErrorStream(true)
                environment()["ANDROID_SERIAL"] = serial
            }.start()
            val ensureOut = ensure.inputStream.bufferedReader().readText()
            val ensureCode = ensure.waitFor()
            if (ensureCode != 0) {
                logger.warn("Reset daemon ensure échoué ($ensureCode): $ensureOut")
            } else {
                ensureOut.lineSequence().filter { it.isNotBlank() }.forEach { logger.lifecycle(it) }
            }
        }
    }
}
