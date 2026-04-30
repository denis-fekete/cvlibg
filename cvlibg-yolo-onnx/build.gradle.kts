plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

android {
    publishing {
        singleVariant("release")
    }

    namespace = "com.fekete.cvlibg.detection.yolo.onnx"
    compileSdk = 36

    defaultConfig {
        minSdk = 29

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlin {
        jvmToolchain(11)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
}

dependencies {
    implementation(project(":cvlibg"))
    implementation("androidx.core:core-ktx:1.18.0")
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.25.0")
}


afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = "com.github.denis-fekete"
                artifactId = "cvlibg-yolo-onnx"
                version = "1.0.0-dev"

                from(components.findByName("release") ?: components["default"])
            }
        }
    }
}