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

  sourceSets {
    commonMain {
      dependencies {
        // Date/time
        implementation(KotlinX.datetime)

        // Ktor server
        implementation(Ktor.plugins.network)

        // Ktor http server
        implementation(Ktor.server.core)
        implementation(Ktor.server.defaultHeaders)

        // Ktor client
        implementation(Ktor.client.core)
        implementation(Ktor.server.cio)
        implementation(Ktor.client.contentNegotiation)
        implementation(Ktor.client.logging)
        implementation(Ktor.plugins.serialization.kotlinx.json)

        // KSoup
        implementation("com.fleeksoft.ksoup:ksoup-kotlinx:_")

        // Crypto
        implementation("dev.whyoleg.cryptography:cryptography-core:_")

        // Env variables
        implementation("dev.scottpierce:kotlin-env-var:_")

        // Logging
        implementation("org.jraf:klibnanolog:_")
      }
    }

    jvmMain {
      dependencies {

        // Ktor client
        implementation(Ktor.client.okHttp)

        // Disable slf4j warning
        implementation("org.slf4j:slf4j-nop:_")

        // Crypto
        implementation("dev.whyoleg.cryptography:cryptography-provider-jdk:_")
      }
    }

    nativeMain {
      dependencies {
        // Ktor client
        // Note: on Linux we need libcurl installed, e.g.:
        // sudo apt-get install libcurl4-gnutls-dev
        // See https://ktor.io/docs/client-engines.html#curl
        implementation(Ktor.client.curl)

        // Crypto
        // See https://whyoleg.github.io/cryptography-kotlin/modules/cryptography-provider-openssl3/#using-in-your-projects
        implementation("dev.whyoleg.cryptography:cryptography-provider-openssl3-prebuilt:_")
      }
    }
  }
}
