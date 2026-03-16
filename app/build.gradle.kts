plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.chaquopy)
}

android {
    namespace = "com.wlaxid.scriptflow"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.wlaxid.scriptflow"
        minSdk = 30
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2-alpha"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }
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
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.ui.text)
    implementation(libs.androidx.runtime.saveable)
    implementation(libs.androidx.ui.test)
    implementation(libs.androidx.documentfile)

    implementation(libs.codeview)
    implementation(libs.androidx.animation.core)
}

chaquopy {
    defaultConfig {
        version = "3.14"
    }
}