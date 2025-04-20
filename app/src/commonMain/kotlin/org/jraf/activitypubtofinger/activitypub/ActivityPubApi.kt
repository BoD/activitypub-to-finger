/*
 * This source is part of the
 *      _____  ___   ____
 *  __ / / _ \/ _ | / __/___  _______ _
 * / // / , _/ __ |/ _/_/ _ \/ __/ _ `/
 * \___/_/|_/_/ |_/_/ (_)___/_/  \_, /
 *                              /___/
 * repository.
 *
 * Copyright (C) 2025-present Benoit 'BoD' Lubek (BoD@JRAF.org)
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

package org.jraf.activitypubtofinger.activitypub

import com.fleeksoft.ksoup.Ksoup
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import org.jraf.activitypubtofinger.activitypub.json.JsonAnnounceOutboxItem
import org.jraf.activitypubtofinger.activitypub.json.JsonAttachment
import org.jraf.activitypubtofinger.activitypub.json.JsonCreateOutboxItem
import org.jraf.activitypubtofinger.activitypub.json.JsonGetOutboxUrlResults
import org.jraf.activitypubtofinger.activitypub.json.JsonGetPaginatedOutboxUrlResults
import org.jraf.activitypubtofinger.activitypub.json.JsonNoteItem
import org.jraf.activitypubtofinger.activitypub.json.JsonOutboxResults
import org.jraf.activitypubtofinger.activitypub.json.JsonWebFingerResults
import org.jraf.activitypubtofinger.util.logd
import org.jraf.activitypubtofinger.util.logw
import org.jraf.activitypubtofinger.util.wrapped

class ActivityPubApi(
  private val coroutineScope: CoroutineScope,
) {
  private val httpClient = HttpClient {
    install(ContentNegotiation) {
      json(
        Json {
          ignoreUnknownKeys = true
          useAlternativeNames = false
        },
      )
    }
    install(HttpTimeout) {
      requestTimeoutMillis = 60_000
      connectTimeoutMillis = 60_000
      socketTimeoutMillis = 60_000
    }
    // Setup logging if requested
    install(Logging) {
      logger = object : Logger {
        override fun log(message: String) {
          logd("http - $message")
        }
      }
      level = LogLevel.ALL
    }
  }

  suspend fun webFinger(address: String): String? {
    val server = address.substringAfterLast('@')
    val acct = address.removePrefix("@")
    return runCatching {
      val webFingerResults: JsonWebFingerResults = httpClient.get("https://$server/.well-known/webfinger?resource=acct:$acct").body()
      webFingerResults.links.firstOrNull {
        it.rel == "self"
      }
        ?.href
    }
      .onFailure { t ->
        logw(t, "WebFinger failed")
      }
      .getOrNull()
  }

  suspend fun getOutboxUrl(href: String): String? {
    return runCatching {
      val outboxUrlResults: JsonGetOutboxUrlResults = httpClient.get(href) {
        header("Accept", "application/activity+json")
      }.body()
      outboxUrlResults.outbox
    }
      .onFailure { t ->
        logw(t, "Get Outbox URL failed")
      }
      .getOrNull()
  }

  suspend fun getPaginatedOutboxUrl(outboxUrl: String): String? {
    return runCatching {
      val paginatedOutboxUrlResults: JsonGetPaginatedOutboxUrlResults = httpClient.get(outboxUrl) {
        header("Accept", "application/activity+json")
      }.body()
      paginatedOutboxUrlResults.first
    }
      .onFailure { t ->
        logw(t, "Get paginated Outbox URL failed")
      }
      .getOrNull()
  }

  suspend fun getOutbox(
    paginatedOutboxUrl: String,
    limit: Int,
  ): List<Note>? {
    return runCatching {
      val outboxResults: JsonOutboxResults = httpClient.get(paginatedOutboxUrl) {
        header("Accept", "application/activity+json")
      }.body()
      outboxResults.orderedItems
        // Filter out replies
        .filterNot {
          it is JsonCreateOutboxItem && it.`object`.inReplyTo != null
        }
        .take(limit)
        .map {
          when (it) {
            is JsonAnnounceOutboxItem -> coroutineScope.async { getNote(it.`object`)?.toNote(isRepost = true) }
            is JsonCreateOutboxItem -> CompletableDeferred(it.`object`.toNote(isRepost = false))
          }
        }
        .awaitAll()
        .filterNotNull()
    }
      .onFailure { t ->
        logw(t, "Get Outbox failed")
      }
      .getOrNull()
  }

  suspend fun getNote(noteUrl: String): JsonNoteItem? {
    return runCatching {
      val note: JsonNoteItem = httpClient.get(noteUrl) {
        header("Accept", "application/activity+json")
      }.body()
      note
    }
      .onFailure { t ->
        logw(t, "Get Note failed")
      }
      .getOrNull()
  }

  data class Note(
    val attributedTo: String?,
    val published: String,
    val content: String,
    val attachment: List<Attachment>,
  )

  data class Attachment(
    val url: String,
  )

  private fun JsonNoteItem.toNote(isRepost: Boolean): Note {
    val document = Ksoup.parse(content)
    document.select("p").before("\n").after("\n")
    val textContent = document.wholeText().trim()
    return Note(
      attributedTo = if (isRepost) attributedTo else null,
      published = Instant.parse(published).toLocalDateTime(TimeZone.currentSystemDefault()).format(DATE_TIME_FORMAT),
      content = textContent,
      attachment = attachment.map { it.toAttachment() },
    )
  }

  private fun JsonAttachment.toAttachment() = Attachment(url = url)

  companion object {
    private val DATE_TIME_FORMAT by lazy {
      LocalDateTime.Format {
        @OptIn(FormatStringsInDatetimeFormats::class)
        byUnicodePattern("yyyy-MM-dd, HH:mm")
      }
    }
  }
}

suspend fun main() {
  val activityPubApi = ActivityPubApi(CoroutineScope(Dispatchers.IO + SupervisorJob()))
  val href = activityPubApi.webFinger("@botteaap@androiddev.social")
  println("href: $href")
  if (href == null) return
  val outboxUrl = activityPubApi.getOutboxUrl(href)
  println("outboxUrl: $outboxUrl")
  if (outboxUrl == null) return
  val paginatedOutboxUrl = activityPubApi.getPaginatedOutboxUrl(outboxUrl)
  println("paginatedOutboxUrl: $paginatedOutboxUrl")
  if (paginatedOutboxUrl == null) return
  val outbox = activityPubApi.getOutbox(paginatedOutboxUrl, 3)
  println("outbox: ${outbox?.joinToString("\n\n") { it.content.wrapped(72) }}")
  if (outbox == null) return
}
