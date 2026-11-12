import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinCompose)
}

// Ajustes de compilación: primero -Pnombre=valor y luego frontend/local.properties
// (no se sube a Git), para que el botón Run de Android Studio use la misma API.
val localSettings = Properties().apply {
    rootProject.file("local.properties").takeIf { it.isFile }?.inputStream()?.use(::load)
}

fun buildSetting(name: String): String =
    (providers.gradleProperty(name).orNull ?: localSettings.getProperty(name) ?: "").trim()

android {
    namespace = "com.healmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.healmate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        val googleClientId = buildSetting("healthMateGoogleClientId")
        require(googleClientId.isEmpty() || googleClientId.matches(Regex("[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com"))) {
            "healthMateGoogleClientId debe ser el ID del cliente OAuth web de Google"
        }
        buildConfigField("String", "HEALTHMATE_GOOGLE_CLIENT_ID", "\"$googleClientId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // URL de la API. Si no se define healthMateApiUrl, las compilaciones de depuración
    // (incluido el botón Run de Android Studio) usan la API local vista desde el emulador.
    val apiUrl = buildSetting("healthMateApiUrl")
    require(apiUrl.isEmpty() || URI(apiUrl).host != null) {
        "healthMateApiUrl debe ser una URL absoluta"
    }
    buildTypes {
        debug {
            val debugApiUrl = apiUrl.ifEmpty { "http://10.0.2.2:8000" }
            buildConfigField("String", "HEALTHMATE_API_URL", "\"$debugApiUrl\"")
        }
        release {
            buildConfigField("String", "HEALTHMATE_API_URL", "\"$apiUrl\"")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.ktor.client.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.credential.manager)
    implementation(libs.androidx.credential.play.services)
    implementation(libs.google.id)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
