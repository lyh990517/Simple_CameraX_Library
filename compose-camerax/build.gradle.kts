plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    `maven-publish`
}

android {
    namespace = "com.example.compose_camerax"
    compileSdk = 33

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // CameraX core library using camera2 implementation
    implementation("androidx.camera:camera-camera2:1.4.0-alpha02")
    // CameraX Lifecycle Library
    implementation("androidx.camera:camera-lifecycle:1.4.0-alpha02")
    // CameraX View class
    implementation ("androidx.camera:camera-view:1.4.0-alpha02")
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = "com.github.lyh990517"
                artifactId = "Simple_CameraX_Library"
                version = "1.0.0"

                pom {
                    name.set("cameraX")
                    description.set("This is an easy-to-use modularized example of the CameraX library with Jetpack Compose.")
                }
            }
        }
    }
}