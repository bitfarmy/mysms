plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.personale.messaggi"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.personale.messaggi"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    signingConfigs {
        create("personale") {
            storeFile = rootProject.file("firma/messaggi-personali.jks")
            storePassword = "messaggi123"
            keyAlias = "messaggi"
            keyPassword = "messaggi123"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("personale")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("personale")
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

// Nessuna libreria esterna: solo Android + Kotlin, come nel progetto della tastiera.
