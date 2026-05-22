plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.surf.surfhubds.brand.branddefault"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildFeatures { buildConfig = false }

    publishing {
        singleVariant("release") { withSourcesJar() }
    }
}

dependencies {
    // Brand modules são resource-only; sem deps obrigatórias.
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = "surfhubds-brand-default"
                version = project.version.toString()
            }
        }
    }
}
