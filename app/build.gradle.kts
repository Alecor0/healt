import java.net.URI

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.healmate"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.healmate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        val apiUrl = providers.gradleProperty("healthMateApiUrl").orElse("").get().trim()
        require(apiUrl.isEmpty() || URI(apiUrl).host != null) {
            "healthMateApiUrl debe ser una URL absoluta"
        }
        buildConfigField("String", "HEALTHMATE_API_URL", "\"$apiUrl\"")
        val googleClientId = providers.gradleProperty("healthMateGoogleClientId").orElse("").get().trim()
        require(googleClientId.isEmpty() || googleClientId.matches(Regex("[A-Za-z0-9_-]+\\.apps\\.googleusercontent\\.com"))) {
            "healthMateGoogleClientId debe ser el ID del cliente OAuth web de Google"
        }
        buildConfigField("String", "HEALTHMATE_GOOGLE_CLIENT_ID", "\"$googleClientId\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
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
