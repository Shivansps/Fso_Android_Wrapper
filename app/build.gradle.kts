plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.shivansps.fsowrapper"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.shivansps.fsowrapper"
        minSdk = 28
        targetSdk = 36
        versionCode = 10
        versionName = "2.0"
        ndkVersion = "28.2.13676358"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++17"
                val driverTools = file(providers.gradleProperty("fsoAdrenotoolsDir")
                    .getOrElse("src/main/cpp/third_party/libadrenotools"))
                val driverToolsPath = if (driverTools.resolve("CMakeLists.txt").isFile) {
                    driverTools.absolutePath.replace('\\', '/')
                } else {
                    ""
                }
                arguments += "-DFSO_ADRENOTOOLS_DIR=$driverToolsPath"
            }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }
    packaging {
        jniLibs.useLegacyPackaging = true
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}