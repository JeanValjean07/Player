import org.jetbrains.kotlin.descriptors.annotations.composeAnnotations

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    //Compose
    alias(libs.plugins.compose.compiler)
    //Oss Licenses Plugin
    id("com.google.android.gms.oss-licenses-plugin")
}

android {
    namespace = "com.suming.player"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
        compose = true
    }


    defaultConfig {
        applicationId = "com.suming.player"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "2.9.0"
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
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.media3:media3-session:1.8.0")
    implementation("io.coil-kt:coil:2.4.0")
    implementation("androidx.fragment:fragment-ktx:1.8.5")


    //Oss Licenses Plugin
    implementation("com.google.android.gms:play-services-oss-licenses:17.1.0")


    //Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling")



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
    implementation(libs.play.services.oss.licenses)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}