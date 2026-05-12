plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") // Add this line here
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.mobicare"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.mobicare"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.gridlayout)

    implementation(libs.firebase.database)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // 1. Import the Firebase BoM (Keep this)
    implementation(platform("com.google.firebase:firebase-bom:33.0.0"))

    // 2. Add Firebase dependencies WITHOUT versions (BoM handles it)
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.itextpdf:itextg:5.5.10")
    implementation("com.google.firebase:firebase-auth")
}