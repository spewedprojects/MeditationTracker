import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.gratus.meditationtrakcer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gratus.meditationtrakcer"
        minSdk = 31
        targetSdk = 36
        versionCode = 84
        versionName = "13.2.0" // Format: Major (4), Minor (0), Patch (a)

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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    buildFeatures {
        // Ensure this is explicitly enabled
        compose = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.ui)
    implementation(libs.runtime)
    implementation(libs.foundation)
    implementation(libs.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.mpandroidchart) // Charts support
    implementation(libs.core) // Markdown support
    implementation(libs.ext.tables) // 📌 Add Table support
    implementation(libs.html) // Add HTML support
    implementation(libs.core.ktx)
    debugImplementation(libs.ui.tooling) // Or the latest version
    implementation(libs.material.icons.extended)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui.tooling.preview)
    implementation(libs.ui.graphics)
}