plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "com.suming.player"
    compileSdk = 36


    defaultConfig {
        applicationId = "com.suming.player"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "2.4.0"
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-oss-licenses:17.2.2")
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
    implementation("io.coil-kt:coil:2.4.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.leanback.paging)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.media3.transformer)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.runtime.saved.instance.state)
    implementation(libs.androidx.tools.core)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.service)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}