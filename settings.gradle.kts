@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    mavenLocal()
    mavenCentral()
    gradlePluginPortal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenLocal()
    mavenCentral()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention").version("0.10.0")

  // See https://jmfayard.github.io/refreshVersions
  id("de.fayard.refreshVersions") version "0.60.5"
}

rootProject.name = "activitypub-to-finger"

include(
  ":app",
  ":main-jvm",
)
