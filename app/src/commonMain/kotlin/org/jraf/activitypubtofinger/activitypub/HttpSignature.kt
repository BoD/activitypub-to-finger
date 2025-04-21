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

import dev.whyoleg.cryptography.algorithms.RSA
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class HttpSignature(
  private val identity: Identity,
) {
  private suspend fun sign(data: ByteArray): ByteArray {
    return identity.getKeyPair().privateKey.signatureGenerator().generateSignature(data)
  }

  suspend fun base64Sign(data: String): String {
    val signed = sign(data.encodeToByteArray())
    @OptIn(ExperimentalEncodingApi::class)
    return Base64.Default.encode(signed)
  }

  suspend fun getPublicKeyPem(): String {
    return identity.getKeyPair().publicKey.encodeToByteArray(RSA.PublicKey.Format.PEM).decodeToString()
  }
}
