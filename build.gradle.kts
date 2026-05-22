plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
}

allprojects {
    group = project.findProperty("GROUP") as String
    version = project.findProperty("VERSION_NAME") as String
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
