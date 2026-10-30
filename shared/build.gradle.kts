import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

// Override in local.properties: melova.api.baseUrl=http://YOUR_MAC_IP:8000/
val scoringApiBaseUrl: String = localProperties.getProperty("melova.api.baseUrl")
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.let { url -> if (url.endsWith("/")) url else "$url/" }
    ?: "http://10.0.2.2:8000/"

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        outputModuleName.set("melova")
        browser {
            commonWebpackConfig {
                outputFileName = "melova.js"
            }
        }
        binaries.executable()
    }

    // Fallback for browsers without WasmGC (iOS before 18.2): the same app compiled to JS.
    js {
        outputModuleName.set("melova")
        browser {
            commonWebpackConfig {
                outputFileName = "melova.js"
            }
        }
        binaries.executable()
    }

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.ui)
            implementation(libs.compose.ui.backhandler)
            implementation(libs.compose.material3)
            implementation(libs.compose.material.icons.extended)
            implementation(libs.compose.components.resources)
            implementation(libs.jetbrains.lifecycle.viewmodel.compose)
            implementation(libs.jetbrains.lifecycle.runtime.compose)
            implementation(libs.jetbrains.navigation.compose)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.logging)
        }
        androidMain.dependencies {
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.credentials)
            implementation(libs.androidx.credentials.play.services.auth)
            implementation(libs.google.identity.googleid)
            implementation(libs.kotlinx.coroutines.android)
            implementation(libs.ktor.client.android)
        }
        webMain.dependencies {
            implementation(libs.ktor.client.js)
            implementation(libs.kotlinx.browser)
        }
    }
}

android {
    namespace = "app.melova.shared"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
        buildConfigField("String", "SCORING_API_BASE_URL", "\"$scoringApiBaseUrl\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
    }
}

compose.resources {
    publicResClass = true
    packageOfResClass = "app.melova.resources"
}
