plugins {
  kotlin("multiplatform").apply(false)
  kotlin("plugin.serialization").apply(false)
  kotlin("jvm").apply(false)
}

// `./gradlew refreshVersions` to update dependencies
// `./gradlew build` to build executables
