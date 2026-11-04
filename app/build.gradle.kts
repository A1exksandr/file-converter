plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.alexender.fileconverter"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.alexender.fileconverter"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "0.3.0 - alpha03"

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

    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("androidx.exifinterface:exifinterface:1.4.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.11.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}