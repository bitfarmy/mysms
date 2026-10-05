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
        targetSdk = 35
        versionCode = 1
        versionName = "0.1"
    }

    // La chiave di firma non sta nel repository (è pubblico): arriva da variabili d'ambiente
    // (in GitHub Actions, dai secret). Senza, la build usa la chiave di debug standard di Android.
    val fileFirma = System.getenv("FIRMA_FILE")
    if (fileFirma != null) {
        signingConfigs {
            create("personale") {
                storeFile = file(fileFirma)
                storePassword = System.getenv("FIRMA_PASSWORD")
                keyAlias = System.getenv("FIRMA_ALIAS")
                keyPassword = System.getenv("FIRMA_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            signingConfigs.findByName("personale")?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
            signingConfigs.findByName("personale")?.let { signingConfig = it }
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
