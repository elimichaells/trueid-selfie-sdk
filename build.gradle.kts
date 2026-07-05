plugins {
    id("com.android.library") version "8.11.1"
    id("org.jetbrains.kotlin.android") version "2.2.20"
    id("org.jetbrains.kotlin.plugin.parcelize") version "2.2.20"
    id("maven-publish")
}

repositories {
    google()
    mavenCentral()
}

android {
    namespace = "com.trueid.sdk.selfie"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-proguard-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // CameraX
    val cameraxVersion = "1.4.2"
    implementation("androidx.camera:camera-core:$cameraxVersion")
    implementation("androidx.camera:camera-camera2:$cameraxVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraxVersion")
    implementation("androidx.camera:camera-view:$cameraxVersion")

    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.7")

    // AndroidX
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

    // EXIF
    implementation("androidx.exifinterface:exifinterface:1.4.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                // JitPack rewrites coordinates to com.github.<user>:<repo>:<tag>,
                // so these are the canonical coordinates for direct Maven hosting.
                groupId = "com.trueid.sdk"
                artifactId = "trueid-selfie-sdk"
                version = "2.1.0"

                pom {
                    name.set("TrueID Selfie SDK")
                    description.set(
                        "Android SDK for TrueID identity verification: selfie capture with " +
                            "face detection and Ghana Card (NIA) verification."
                    )
                    url.set("https://app.trueid.info/docs")
                }
            }
        }
        repositories {
            // Local repo layout staged for hosting at https://<trueid-server>/sdk/android
            maven {
                name = "localSdkRepo"
                url = uri(layout.buildDirectory.dir("sdk-maven-repo"))
            }
        }
    }
}
