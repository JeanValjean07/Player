plugins {
    //basic advanced (compose,room,oss)
    //alias(libs.plugins.oss.licenses)
    //basic traditional
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)



}

@Suppress("DEPRECATION")
android {
    namespace = "com.suming.player"
    //编译sdk
    compileSdk = 37

    defaultConfig {
        applicationId = "com.suming.player"
        minSdk = 26
        maxSdk = 37
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 26091801
        versionName = "3.7.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk{
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    packaging{
        jniLibs{
            useLegacyPackaging = true
        }
        dex{
            useLegacyPackaging = true
        }
        resources{
            excludes.add("META-INF/DEPENDENCIES")
            excludes.add("fonts/**")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        viewBinding = true
        aidl = true
    }

}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {


    //ExoPlayer Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.transformer)
    //Exoplayer ffmpeg  https://github.com/anilbeesetti
    implementation("io.github.anilbeesetti:nextlib-media3ext:1.10.0-0.12.1")
    //Exoplayer 音频解码器


    //OkHttp
    implementation("com.squareup.okhttp3:okhttp:5.5.0")


    //basic advanced (compose,room,oss,bybom,datastore)
    implementation(platform(libs.compose.bom))
    implementation(libs.androidx.compose.remote.creation.core)
    implementation(libs.androidx.compose.foundation.foundation.layout)
    implementation(libs.androidx.library)
    implementation(libs.foundation.layout)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.core.animation)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.material)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp("androidx.room:room-compiler:2.8.4")
    implementation(libs.play.services.oss.licenses)

    //basic traditional (kotlin,agp,ksp)
    implementation("androidx.fragment:fragment-ktx:1.9.0")
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.leanback.paging)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.runtime.saved.instance.state)
    implementation(libs.androidx.tools.core)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.ui.android)
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.compose.material)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.foundation.layout)

}