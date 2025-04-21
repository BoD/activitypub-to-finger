plugins {
  kotlin("multiplatform").apply(false)
  kotlin("plugin.serialization").apply(false)
  kotlin("jvm").apply(false)
}

tasks.register<Exec>("dockerBuild") {
  dependsOn(subprojects.map { it.tasks.named("build") })
  commandLine("docker", "image", "rm", "bodlulu/activitypub-to-finger:latest")
  commandLine("docker", "build", "--platform", "linux/x86_64", "-t", "bodlulu/activitypub-to-finger", ".")
}

// `./gradlew dockerBuild` to build the docker image
// `./gradlew refreshVersions` to update dependencies
// `./gradlew build` to build executables
