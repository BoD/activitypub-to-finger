/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2024-present Benoit 'BoD' Lubek (BoD@JRAF.org)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jraf.activitypubtofinger.http

import io.ktor.http.ContentType
import io.ktor.http.HeaderValueParam
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import org.jraf.activitypubtofinger.activitypub.HttpSignature
import org.jraf.activitypubtofinger.activitypub.Identity

private const val PORT = 8042

class HttpServer(
  publicHttpServerName: String,
  publicHttpServerBaseUrl: String,
  private val identity: Identity,
) {
  private val httpSignature = HttpSignature(identity)
  private val webFingerSubject = "acct:${identity.userName}@$publicHttpServerName"
  private val userNameUrl = "$publicHttpServerBaseUrl/${identity.userName}"

  suspend fun start() {
    embeddedServer(
      factory = CIO,
      port = PORT,
      module = { mainModule() },
    ).startSuspend(wait = true)
  }

  private fun Application.mainModule() {
    install(DefaultHeaders)

    routing {
      get("/${identity.userName}") {
        val publicKeyPemJsonEncoded = httpSignature.getPublicKeyPem().replace("\n", "\\n")
        call.respondText(
          // language=JSON
          """
            {
              "@context": [
                "https://www.w3.org/ns/activitystreams",
                "https://w3id.org/security/v1"
              ],
              "id": "$userNameUrl",
              "type": "Person",
              "preferredUsername": "${identity.userName}",
              "inbox": "$userNameUrl/inbox",
              "outbox": "$userNameUrl/outbox",
              "publicKey": {
                "id": "$userNameUrl#main-key",
                "owner": "$userNameUrl",
                "publicKeyPem": "$publicKeyPemJsonEncoded"
              }
            }
          """.trimIndent(),
          ContentType(
            contentType = "application",
            contentSubtype = "ld+json",
            listOf(HeaderValueParam("profile", "https://www.w3.org/ns/activitystreams")),
          ),
        )
      }

      get("/.well-known/webfinger") {
        call.respondText(
          // language=JSON
          """
            {
              "subject": "$webFingerSubject",
              "aliases": [
                "$userNameUrl"
              ],
              "links": [
                {
                  "rel": "http://webfinger.net/rel/profile-page",
                  "type": "text/html",
                  "href": "$userNameUrl"
                },
                {
                  "rel": "self",
                  "type": "application/activity+json",
                  "href": "$userNameUrl"
                }
              ]
            }
          """.trimIndent(),
          ContentType(contentType = "application", contentSubtype = "jrd+json"),
        )
      }
    }
  }
}
