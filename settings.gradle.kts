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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MokeHudAndroid"
include(":demo:app")
include(":hud")
include(":demo:core:analytics:api")
include(":demo:core:analytics:prod")
include(":demo:core:analytics:debug")
include(":demo:core:analytics:impl")
include(":demo:feature:mokera:api")
include(":demo:feature:mokera:impl")
