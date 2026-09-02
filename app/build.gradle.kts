plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    //Compose
    alias(libs.plugins.compose.compiler)

    //ksp
    id("com.google.devtools.ksp")


}

android {
    namespace = "com.suming.player"
    compileSdk = 37

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.suming.player"
        minSdk = 26
        maxSdk = 37
        //noinspection OldTargetApi
        targetSdk = 36
        versionCode = 1
        versionName = "3.7.0"
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

}

dependencies {


    //ExoPlayer Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.transformer)


    //Compose
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.compose.runtime:runtime")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core:1.7.8")
    implementation("androidx.compose.material:material-icons-extended-android:1.7.8")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling")
    implementation(libs.androidx.compose.remote.creation.core)

    //Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    implementation(libs.androidx.library)
    implementation(libs.foundation.layout)
    implementation(libs.androidx.datastore.core)
    implementation(libs.androidx.compose.foundation.foundation.layout)
    implementation(libs.androidx.core.animation)

    //ksp
    ksp("androidx.room:room-compiler:2.8.4")

    //RxJava RxKotlin RxAndroid (已停用)
    //implementation("io.reactivex.rxjava3:rxjava:3.1.12")
    //implementation("io.reactivex.rxjava3:rxkotlin:3.0.1")
    //implementation("io.reactivex.rxjava3:rxandroid:3.0.2")

    //LocalBroadcastManager (已废弃)
    //implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    //Gson (暂未使用)
    //implementation("com.google.code.gson:gson:2.14.0")

    //图片加载库 (暂未使用:手搓方案还没到性能瓶颈,不使用库)
    //Glide
    //implementation("com.github.bumptech.glide:glide:5.0.9")
    //annotationProcessor("com.github.bumptech.glide:compiler:5.0.9")
    //coil
    //implementation("io.coil-kt:coil:2.7.0")

    //OkHttp
    implementation("com.squareup.okhttp3:okhttp:5.5.0")

    //Core
    implementation("androidx.fragment:fragment-ktx:1.9.0")

    //Basic Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.paging.common)
    implementation(libs.androidx.leanback.paging)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.runtime.saved.instance.state)
    implementation(libs.androidx.tools.core)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.service)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}