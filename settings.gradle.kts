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
// FIX: Converted from Groovy to Kotlin DSL for .kts file
dependencyResolutionManagement {
    // FIX: Using Kotlin property assignment syntax to fix 'Unexpected tokens' error
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
        // FIX: MPAndroidChart is hosted on JitPack, so we must add the repository here.
        // FIX: Changed 'url = "..."' to 'uri("...")' to resolve the 'Assignment type mismatch' error (expecting java.net.URI).
        maven { url = uri("https://www.jitpack.io") }
    }
}

rootProject.name = "PDDMate"
include(":app")
