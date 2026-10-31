plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)

    //AboutLibraries
    id("com.mikepenz.aboutlibraries.plugin.android")


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
        versionCode = 26103101
        versionName = "3.7.7"
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

    //AboutLibraries  (执行 ./gradlew :app:prepareLibraryDefinitionsDebug 生成到 build/generated/aboutLibraries/ 复制到res/raw )
    implementation("com.mikepenz:aboutlibraries-compose-m3:15.3.0")



    //ExoPlayer Media3
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.transformer)
    //OkHttp
    implementation(libs.okhttp)


    //compose
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.foundation)
    implementation(libs.compose.connect.activity)   //activity的compose支持
    implementation(libs.compose.connect.viewmodel)   //viewmodel的compose支持
    implementation(platform(libs.compose.bom))   //自动版本匹配
    implementation(libs.compose.extended.icons)   //图标扩展
    //room
    ksp(libs.room.compiler)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    //recyclerView
    implementation(libs.androidx.paging.support.recycler.view)  //recyclerView的分页支持
    //paging
    implementation(libs.androidx.paging.core)  //分页核心库
    //lifeCycle
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.service)
    //datastore
    implementation(libs.androidx.datastore.core)
    //databinding
    implementation(libs.androidx.databinding.runtime)
    implementation(libs.androidx.databinding.core)
    //android core (原则上自行选出)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.activity)
    implementation(libs.androidx.core.appcompat)
    implementation(libs.androidx.core.view.constraint)
    implementation(libs.androidx.core.view.animation)
    implementation(libs.androidx.core.view.material)
    implementation(libs.fragment.core.ktx)


}