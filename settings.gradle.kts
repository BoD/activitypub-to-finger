@file:Suppress("UnstableApiUsage")

pluginManagement {
  repositories {
    mavenCentral()
    gradlePluginPortal()
    mavenLocal()
  }
}

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    mavenCentral()
    mavenLocal()
  }
}

plugins {
  id("org.gradle.toolchains.foojay-resolver-convention").version("1.0.0")

  // See https://jmfayard.github.io/refreshVersions
  id("de.fayard.refreshVersions").version("0.60.5")
}

rootProject.name = "activitypub-to-finger"

include(
  ":app",
  ":main-jvm",
)
