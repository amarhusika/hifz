plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.amar.hifz"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.amar.hifz"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            // Mali i brz APK: izbacuje nekorišten kod i resurse
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Potpisano debug ključem da se može odmah instalirati na telefon
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

// Bez AppCompat/Material biblioteka – samo Android framework (lagano, malo baterije)
dependencies {
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
