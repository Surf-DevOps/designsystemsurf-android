pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "SurfHubDS-Android"

include(":surfhubds-core")
include(":surfhubds-brand-default")
include(":surfhubds-brand-matizconecta")
include(":surfhubds-brand-uber")
include(":surfhubds-brand-ifood")
include(":surfhubds-brand-bandsports")
include(":surfhubds-brand-flachip")
include(":surfhubds-brand-conecta")
include(":surfhubds-brand-mega")
include(":surfhubds-brand-fluxo")
include(":surfhubds-brand-pafer")
include(":surfhubds-brand-paguemenos")
include(":surfhubds-brand-carrefourchip")
include(":surfhubds-brand-correioscelular")
include(":surfhubds-brand-pernambucanaschip")
