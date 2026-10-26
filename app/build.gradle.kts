plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.alexender.fileconverter"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.convectorfiles"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "0.2.0-alpha02"

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
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    implementation("androidx.recyclerview:recyclerview:1.4.0")
}