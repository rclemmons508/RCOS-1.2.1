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
        // LiveKit Android pulls its `audioswitch` fork from JitPack
        // (com.github.davidliu:audioswitch) — Maven Central does not host it.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "RC Solutions"
include(":app")
