plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    `maven-publish`
}

android {
    namespace = "com.surf.surfhubds"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures { buildConfig = false }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    sourceSets {
        getByName("main") { java.srcDirs("src/main/kotlin") }
    }

    publishing {
        singleVariant("release") { withSourcesJar() }
    }
}

dependencies {
    api(libs.androidx.core.ktx)
    api(libs.androidx.appcompat)
    api(libs.material)
    api(libs.androidx.constraintlayout)
    api(libs.androidx.recyclerview)
    api(libs.androidx.viewpager2)
    api(libs.androidx.lifecycle.runtime.ktx)
    api(libs.androidx.fragment.ktx)
    api(libs.glide)
    api(libs.zxing.android.embedded)
    api(libs.slidetoact)
    api(libs.blurview)

    // OCR de cartão (equivalente Android do Vision/AVFoundation usado no iOS):
    // CameraX para o preview/análise de frames + ML Kit Text Recognition para o OCR.
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.text.recognition)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = "surfhubds-core"
                version = project.version.toString()
            }
        }
    }
}
