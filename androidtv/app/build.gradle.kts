plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.vacubots.tv"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vacubots.tvapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 128
        versionName = "3.12"
    }

    // The app is signed with our private key, which only the build in GitHub Actions has (repository secrets VB_KEYSTORE_B64 and the rest).
    // The workflow stops if those secrets are missing, so an APK signed with the wrong key can never be published.
    // On your own computer, with no key set, Android's own debug key is used: fine to try the app, but it cannot update an installed release.
    signingConfigs {
        getByName("debug") {
            val privateKey = System.getenv("VB_KEYSTORE_FILE")
            if (privateKey != null) {
                storeFile = file(privateKey)
                storePassword = System.getenv("VB_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("VB_KEY_ALIAS")
                keyPassword = System.getenv("VB_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // Sideloaded onto our own TVs, not published to Google Play.
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.webkit:webkit:1.12.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
}
