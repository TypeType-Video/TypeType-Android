pluginManagement {
    includeBuild("build-logic")
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
    }
}

rootProject.name = "TypeType-Android"

include(":app")
include(":baseline-profile")
include(":core:data")
include(":core:model")
include(":core:resources")
include(":core:ui")
include(":feature:channel")
include(":feature:home")
include(":feature:library")
include(":feature:menu")
include(":feature:notifications")
include(":feature:podcast")
include(":feature:publicplaylist")
include(":feature:search")
include(":feature:setup")
include(":feature:subscriptions")
include(":player")
