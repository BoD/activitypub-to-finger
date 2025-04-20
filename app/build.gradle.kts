plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
}

kotlin {
  jvmToolchain(17)

  jvm()

  linuxX64 {
    binaries {
      executable {
        entryPoint = "org.jraf.activitypubtofinger.main"
        baseName = "activitypub-to-finger"
      }
    }
  }

  linuxArm64 {
    binaries {
      executable {
        entryPoint = "org.jraf.activitypubtofinger.main"
        baseName = "activitypub-to-finger"
      }
    }
  }

  macosArm64 {
    binaries {
      executable {
        entryPoint = "org.jraf.activitypubtofinger.main"
        baseName = "activitypub-to-finger"
      }
    }
  }

  sourceSets {
    commonMain {
      dependencies {
        // Argument parsing
        implementation("com.github.ajalt.clikt:clikt:_")

        // Date/time
        implementation(KotlinX.datetime)

        // Ktor server
        implementation(Ktor.plugins.network)

        // Ktor client
        implementation(Ktor.client.core)
        implementation(Ktor.client.contentNegotiation)
        implementation(Ktor.client.logging)
        implementation(Ktor.plugins.serialization.kotlinx.json)

        // KSoup
        implementation("com.fleeksoft.ksoup:ksoup-kotlinx:_")
      }
    }

    jvmMain {
      dependencies {
        // Ktor client
        implementation(Ktor.client.okHttp)

        // Disable slf4j warning
        implementation("org.slf4j:slf4j-nop:_")
      }
    }

    nativeMain {
      dependencies {
        // Ktor client
        // Note: on Linux we need libcurl installed, e.g.:
        // sudo apt-get install libcurl4-gnutls-dev
        // See https://ktor.io/docs/client-engines.html#curl
        implementation(Ktor.client.curl)
      }
    }
  }
}
