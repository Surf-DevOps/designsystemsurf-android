plugins {
    alias(libs.plugins.android.library)
    `maven-publish`
}

android {
    namespace = "com.surf.surfhubds.brand.paguemenos"
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
    // Tema Theme.App herda de Theme.Material3.* → precisa do Material no classpath de link.
    api(libs.material)
}

afterEvaluate {
    publishing {
        publications {
            register<MavenPublication>("release") {
                from(components["release"])
                groupId = project.group.toString()
                artifactId = "surfhubds-brand-paguemenos"
                version = project.version.toString()
            }
        }
    }
}
