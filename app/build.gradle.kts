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
        // In GitHub Actions cresce a ogni build, così ogni APK si può installare sopra il precedente.
        versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toIntOrNull() ?: 1
        versionName = "0.2"
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
            // Senza la chiave personale (build locale) si usa quella di debug, altrimenti l'APK non sarebbe installabile.
            signingConfig = signingConfigs.findByName("personale") ?: signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

// Nessuna libreria esterna nell'app: solo Android + Kotlin, come nel progetto della tastiera.
// JUnit serve soltanto ai test e non finisce nell'APK.
dependencies {
    testImplementation("junit:junit:4.13.2")
}
