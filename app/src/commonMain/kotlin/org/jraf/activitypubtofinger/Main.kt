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

package org.jraf.activitypubtofinger

import io.ktor.network.selector.SelectorManager
import io.ktor.network.sockets.ServerSocket
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
import kotlinx.coroutines.runBlocking
import org.jraf.activitypubtofinger.activitypub.ActivityPubApi
import org.jraf.activitypubtofinger.util.logd
import org.jraf.activitypubtofinger.util.logi
import org.jraf.activitypubtofinger.util.wrapped

private class ActivityPubToFinger() {
  companion object {
    private const val PORT = 79
    private val addressRegex = Regex("^@[^@]+@[^@]+$")
  }

  private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
  private val activityPubApi = ActivityPubApi(coroutineScope)

  suspend fun run() {
    logi("BoD activitypub-to-finger v1.0.0")
    val selectorManager = SelectorManager(Dispatchers.IO)
    val serverSocket: ServerSocket = aSocket(selectorManager).tcp().bind("127.0.0.1", PORT)
    while (true) {
      val clientSocket = serverSocket.accept()
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
    if (address.isNullOrBlank() || address.equals("BoD", ignoreCase = true)) {
      address = "@BoD@mastodon.social"
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
    println("outboxUrl: $outboxUrl")
    if (outboxUrl == null) {
      writeChannel.writeString("User $address not found\n")
      clientSocket.close()
      return
    }
    val paginatedOutboxUrl = activityPubApi.getPaginatedOutboxUrl(outboxUrl)
    println("paginatedOutboxUrl: $paginatedOutboxUrl")
    if (paginatedOutboxUrl == null) {
      writeChannel.writeString("User $address not found\n")
      clientSocket.close()
      return
    }
    val outbox = activityPubApi.getOutbox(paginatedOutboxUrl, 3)
    if (outbox == null || outbox.isEmpty()) {
      writeChannel.writeString("Posts from $address not found\n")
      clientSocket.close()
      return
    }
    val intro = "Last ${outbox.size} posts from $address:"
    writeChannel.writeString("$intro\n")
    val separator = buildString { repeat(72) { append("-") } }
    writeChannel.writeString("\n$separator\n\n")
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
      writeChannel.writeString("\n$separator\n\n")
    }
    writeChannel.writeString("See more posts at $href\n")
    writeChannel.writeString("Have a nice day!\n")
    clientSocket.close()
  }
}

fun main(av: Array<String>) {
  runBlocking {
    ActivityPubToFinger().run()
  }
}
