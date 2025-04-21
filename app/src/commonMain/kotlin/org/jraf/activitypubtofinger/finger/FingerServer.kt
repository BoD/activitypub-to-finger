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

package org.jraf.activitypubtofinger.finger

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.Socket
import io.ktor.network.sockets.aSocket
import io.ktor.network.sockets.openReadChannel
import io.ktor.network.sockets.openWriteChannel
import io.ktor.utils.io.readUTF8Line
import io.ktor.utils.io.writeString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.jraf.activitypubtofinger.activitypub.ActivityPubApi
import org.jraf.activitypubtofinger.activitypub.Identity
import org.jraf.activitypubtofinger.util.logd
import org.jraf.activitypubtofinger.util.wrapped

/**
 * Finger is on port 79.
 * But while in dev mode ports < 1024 are annoying (need to run as super user), so use 7900 instead.
 * When running inside Docker this is easily mapped to port 79.
 */
private const val PORT = 7900
private val addressRegex = Regex("^@[^@]+@[^@]+$")

class FingerServer(
  identity: Identity,
  publicHttpServerBaseUrl: String,
  private val defaultAddress: String,
  private val defaultAddressAlias: String,
) {
  private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val activityPubApi = ActivityPubApi(
    coroutineScope = coroutineScope,
    identity = identity,
    publicHttpServerBaseUrl = publicHttpServerBaseUrl,
  )

  suspend fun start() {
    logd("Starting Finger server on port $PORT")
    val serverSocket = aSocket(SelectorManager(Dispatchers.IO)).tcp().bind(port = PORT)
    while (true) {
      val clientSocket = serverSocket.accept()
      logd("Accepted connection from ${clientSocket.remoteAddress}")
      coroutineScope.launch {
        handleClient(clientSocket)
      }
    }
  }

  private suspend fun handleClient(clientSocket: Socket) {
    val readChannel = clientSocket.openReadChannel()
    val writeChannel = clientSocket.openWriteChannel(autoFlush = true)
    var address = readChannel.readUTF8Line(max = 1024)
    logd("address: $address")
    if (address.isNullOrBlank() || address.equals(defaultAddressAlias, ignoreCase = true)) {
      address = defaultAddress
    }
    if (!address.matches(addressRegex)) {
      writeChannel.writeString("Invalid address\n")
      clientSocket.close()
      return
    }
    val href = activityPubApi.webFinger(address)
    logd("href: $href")
    if (href == null) {
      writeChannel.writeString("User $address not found\n")
      clientSocket.close()
      return
    }
    val outboxUrl = activityPubApi.getOutboxUrl(href)
    logd("outboxUrl: $outboxUrl")
    if (outboxUrl == null) {
      writeChannel.writeString("User $address not found\n")
      clientSocket.close()
      return
    }
    val paginatedOutboxUrl = activityPubApi.getPaginatedOutboxUrl(outboxUrl)
    logd("paginatedOutboxUrl: $paginatedOutboxUrl")
    if (paginatedOutboxUrl == null) {
      writeChannel.writeString("User $address not found\n")
      clientSocket.close()
      return
    }
    val outbox = activityPubApi.getOutbox(paginatedOutboxUrl, 3)
    if (outbox.isNullOrEmpty()) {
      writeChannel.writeString("Posts from $address not found\n")
      clientSocket.close()
      return
    }
    val intro = if (outbox.size > 1) {
      "Here are the latest ${outbox.size} posts from $address:"
    } else {
      "Here is the latest post from $address:"
    }
    writeChannel.writeString("$intro\n")
    val separator = buildString { repeat(72) { append("-") } }
    writeChannel.writeString("\n$separator\n")
    for (note in outbox) {
      writeChannel.writeString("${note.published}\n\n")
      if (note.attributedTo != null) {
        writeChannel.writeString("Repost from ${note.attributedTo}:\n")
      }
      writeChannel.writeString("${note.content.wrapped(72)}\n")
      if (note.attachment.isNotEmpty()) {
        val attachmentTitle = if (note.attachment.size == 1) {
          "Attachment"
        } else {
          "Attachments"
        }
        val prefix = if (note.attachment.size > 1) "- " else ""
        writeChannel.writeString("\n$attachmentTitle:\n${note.attachment.joinToString("\n") { "$prefix${it.url}" }}\n")
      }
      writeChannel.writeString("\n$separator\n")
    }
    writeChannel.writeString("\nSee more posts at $href.\n")
    writeChannel.writeString("Have a nice day!\n")
    clientSocket.close()
  }
}
