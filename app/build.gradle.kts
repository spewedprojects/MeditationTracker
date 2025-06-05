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
        versionCode = 35
        versionName = "3.0.g" // Format: Major (2), Minor (16), Patch (e)

        // Pass versionName to the app as a resource
        resValue(
            type = "string",
            name = "app_version",
            value = "Current: v" + versionName!! + " (" + versionCode!! + ")"
        )

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
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2") // 📌 Add Table support
    implementation("androidx.core:core-ktx:1.15.0") // Or the latest version
}