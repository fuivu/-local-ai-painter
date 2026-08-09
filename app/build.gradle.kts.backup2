plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.room)
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.localaipainter"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.localaipainter"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 10
        versionName = "3.7.0"

        // 仅保留 arm64-v8a，砍掉 armeabi-v7a 节省 ~15MB
        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }

        // CMake C++ 引擎
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17 -O3 -flto -fopenmp"
                arguments += "-DANDROID_STL=c++_shared"
            }
        }

        ndk {
            // Vulkan + OpenCL + NNAPI 支持
            ldLibs += listOf("vulkan", "OpenCL", "neuralnetworks")
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            applicationIdSuffix = ".debug"
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            androidResources {
                noCompress += listOf("model", "safetensors", "gguf", "onnx", "mnn")
            }
        }
    }

    buildFeatures {
        compose = true
        aidl = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    packaging {
        resources {
            excludes += listOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/DEPENDENCIES",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*"
            )
        }
        jniLibs {
            useLegacyPackaging = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1+"
        }
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java")
            res.srcDirs("src/main/res")
            manifest.srcFile("src/main/AndroidManifest.xml")
        }
    }
}

dependencies {
    // ─── Core ────────────────────────────────────────
    implementation(libs.androidx.core)
    implementation(libs.androidx.appcompat)

    // ─── Compose BOM ────────────────────────────────
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)

    // ─── Activity & Navigation ──────────────────────
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // ─── ViewModel & Lifecycle ──────────────────────
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // ─── Coroutines ──────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    // ─── Serialization ───────────────────────────────
    implementation(libs.kotlinx.serialization.json)

    // ─── Room ────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // ─── DataStore ───────────────────────────────────
    implementation(libs.androidx.datastore.preferences)

    // ─── Coil 图片加载 ───────────────────────────────
    implementation(libs.coil.compose)
    implementation(libs.coil.gif)

    // ─── Biometric ───────────────────────────────────
    implementation(libs.androidx.biometric)

    // ─── ONNX Runtime (可选推理后端) ────────────────
    implementation(libs.onnx.runtime.android)

    // ─── GPUImage (滤镜/后处理) ─────────────────────
    implementation("jp.co.cyberagent.android:gpuimage:2.1.0")

    // ─── WorkManager ─────────────────────────────────
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // ─── SplashScreen ────────────────────────────────
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ─── 调试工具 ─────────────────────────────────────
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.test.manifest)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
