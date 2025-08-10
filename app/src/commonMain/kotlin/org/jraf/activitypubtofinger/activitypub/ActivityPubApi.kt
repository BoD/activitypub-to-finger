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
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import org.jraf.klibnanolog.logd
import org.jraf.klibnanolog.logw
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ActivityPubApi(
  private val coroutineScope: CoroutineScope,
  private val identity: Identity,
  private val publicHttpServerBaseUrl: String,
) {
  private val httpSignature = HttpSignature(identity)

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
//      level = LogLevel.ALL
      level = LogLevel.INFO
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
        httpSignature(href)
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
        httpSignature(outboxUrl)
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
        httpSignature(paginatedOutboxUrl)
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
        httpSignature(noteUrl)
      }.body()
      note
    }
      .onFailure { t ->
        logw(t, "Get Note failed")
      }
      .getOrNull()
  }

  private suspend fun HttpRequestBuilder.httpSignature(url: String) {
    val host = url.substringAfter("https://").substringBefore("/")
    header("host", host)
    val httpFormattedDate = getHttpFormattedDate()
    header("date", httpFormattedDate)
    val resource = url.substringAfter("https://").substringAfter("/")
    val requestTarget = "get /$resource"
    val signatureString = "(request-target): $requestTarget\nhost: $host\ndate: $httpFormattedDate"
    val signedBase64 = httpSignature.base64Sign(signatureString)
    val signatureHeader =
      """keyId="$publicHttpServerBaseUrl/${identity.userName}",headers="(request-target) host date",signature="$signedBase64""""
    header("signature", signatureHeader)
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

  @OptIn(ExperimentalTime::class)
  private fun JsonNoteItem.toNote(isRepost: Boolean): Note {
    val document = Ksoup.parse(content)
    document.select("p").before("\n").after("\n")
    val textContent = document.wholeText().trim()
    return Note(
      attributedTo = if (isRepost) attributedTo else null,
      published = Instant.parse(published).toLocalDateTime(TimeZone.currentSystemDefault()).format(ACTIVITY_PUB_DATE_TIME_FORMAT),
      content = textContent,
      attachment = attachment.map { it.toAttachment() },
    )
  }

  private fun JsonAttachment.toAttachment() = Attachment(url = url)


  companion object {
    private val ACTIVITY_PUB_DATE_TIME_FORMAT by lazy {
      LocalDateTime.Format {
        @OptIn(FormatStringsInDatetimeFormats::class)
        byUnicodePattern("yyyy-MM-dd', 'HH:mm' GMT'")
      }
    }

    private val HTTP_DATE_TIME_FORMAT by lazy {
      LocalDateTime.Format {
        @OptIn(FormatStringsInDatetimeFormats::class)
        byUnicodePattern("'<day>', dd '<month>' yyyy HH:mm:ss 'GMT'")
      }
    }

    @OptIn(ExperimentalTime::class)
    private fun getHttpFormattedDate(): String {
      val localDateTime = Clock.System.now().toLocalDateTime(TimeZone.UTC)
      return localDateTime.format(HTTP_DATE_TIME_FORMAT)
        .replace(
          "<day>",
          localDateTime.dayOfWeek.name.lowercase()
            .take(3)
            .replaceFirstChar { it.titlecase() },
        )
        .replace(
          "<month>",
          localDateTime.month.name.lowercase()
            .take(3)
            .replaceFirstChar { it.titlecase() },
        )
    }
  }
}
