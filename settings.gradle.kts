// settings.gradle.kts

pluginManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
        gradlePluginPortal()
    }
    plugins {
        id("com.android.application") version "8.13.2"
        id("com.google.gms.google-services") version "4.4.4"
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io") // necesario para MPAndroidChart
        maven(url = "https://artifactory.appodeal.com/appodeal") // Appodeal SDK
        maven(url = "https://artifactory.appodeal.com/appodeal-public/") // Appodeal SDK (alternative)
    }
}

rootProject.name = "CareTemplate"
include(":app")
