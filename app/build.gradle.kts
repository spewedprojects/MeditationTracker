plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.gratus.meditationtrakcer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.gratus.meditationtrakcer"
        minSdk = 31
        targetSdk = 35
        versionCode = 15
        versionName = "1.15.b" // Format: Major (1), Minor (13), Patch (2)

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
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
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
}