pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Vaulta"
include(":app")
include(":core-crypto")
include(":core-vault")
include(":core-storage")
include(":core-sync")
include(":feature-onboarding")
include(":feature-browser")
include(":feature-viewer")
include(":feature-settings")
