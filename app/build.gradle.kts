plugins {
  kotlin("multiplatform")
  kotlin("plugin.serialization")
}

kotlin {
  jvm()
  jvmToolchain(11)
  linuxX64 {
    binaries {
      executable {
        entryPoint = "org.jraf.activitypubtofinger.main"
        baseName = "ws-to-minitel"
      }
    }
  }
  linuxArm64 {
    binaries {
      executable {
        entryPoint = "org.jraf.activitypubtofinger.main"
        baseName = "ws-to-minitel"
      }
    }
  }
  macosArm64 {
    binaries {
      executable {
        entryPoint = "org.jraf.activitypubtofinger.main"
        baseName = "ws-to-minitel"
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
