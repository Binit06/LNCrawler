plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.halovoid.lncrawler.api"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.halovoid.lncrawler.api"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.jsoup)
    implementation(libs.okhttp)
}