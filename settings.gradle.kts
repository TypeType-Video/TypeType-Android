pluginManagement {
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
include(":player")
include(":tv")

val typeTypeSdkPath = providers.gradleProperty("typeTypeSdkPath")
    .orElse(providers.environmentVariable("TYPETYPE_SDK_PATH"))
    .orElse("../TypeType-SDK")
    .get()
val typeTypeSdkDirectory = file(typeTypeSdkPath)
if (typeTypeSdkDirectory.resolve("settings.gradle.kts").isFile) {
    includeBuild(typeTypeSdkDirectory) {
        dependencySubstitution {
            substitute(module("video.typetype:sdk-core"))
                .using(project(":sdk-core"))
            substitute(module("video.typetype:sdk-android"))
                .using(project(":sdk-android"))
            substitute(module("video.typetype:sdk-media3"))
                .using(project(":sdk-media3"))
        }
    }
}
