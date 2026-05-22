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
