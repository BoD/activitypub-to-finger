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

import dev.scottpierce.envvar.EnvVar
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.jraf.activitypubtofinger.activitypub.Identity
import org.jraf.activitypubtofinger.finger.FingerServer
import org.jraf.activitypubtofinger.http.HttpServer
import org.jraf.klibnanolog.logd
import org.jraf.klibnanolog.logi

fun main() {
  logi("BoD activitypub-to-finger v1.0.0")

  // e.g. finger.example.com
  val publicHttpServerName = EnvVar.require("PUBLIC_HTTP_SERVER_NAME")
  logd("publicHttpServerName: $publicHttpServerName")

  val publicHttpServerBaseUrl = "https://$publicHttpServerName"
  logd("publicHttpServerBaseUrl: $publicHttpServerBaseUrl")

  // e.g. @BoD@mastodon.social
  val defaultAddress = EnvVar.require("DEFAULT_ADDRESS")
  logd("defaultAddress: $defaultAddress")

  // e.g. BoD
  val defaultAddressAlias = EnvVar.require("DEFAULT_ADDRESS_ALIAS")
  logd("defaultAddressAlias: $defaultAddressAlias")

  val identity = Identity()
  logd("Identity userName: ${identity.userName}")

  runBlocking {
    @OptIn(DelicateCoroutinesApi::class)
    GlobalScope.launch {
      HttpServer(
        publicHttpServerName = publicHttpServerName,
        publicHttpServerBaseUrl = publicHttpServerBaseUrl,
        identity = identity,
      ).start()
    }

    FingerServer(
      identity = identity,
      publicHttpServerBaseUrl = publicHttpServerBaseUrl,
      defaultAddress = defaultAddress,
      defaultAddressAlias = defaultAddressAlias,
    ).start()
  }
}
