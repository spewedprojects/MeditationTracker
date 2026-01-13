plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.gratus.meditationtrakcer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.gratus.meditationtrakcer"
        minSdk = 31
        targetSdk = 36
        versionCode = 64
        versionName = "9.0.a" // Format: Major (4), Minor (0), Patch (a)

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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.mpandroidchart) // Charts support
    implementation(libs.core) // Markdown support
    implementation(libs.ext.tables) // 📌 Add Table support
    implementation(libs.core.ktx) // Or the latest version
}