plugins {
  kotlin("jvm")
  application
}

group = "org.jraf"
version = "1.0.0"

kotlin {
  jvmToolchain(17)
}

dependencies {
  implementation(project(":app"))
}

application {
  mainClass.set("org.jraf.activitypubtofinger.MainKt")
  applicationName = "activitypub-to-finger"
}
