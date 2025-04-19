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

package org.jraf.activitypubtofinger.util

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern
import kotlinx.datetime.toLocalDateTime

enum class LogLevel {
  DEBUG,
  INFO,
  WARNING,
  ERROR,
  NONE,
}


var logLevel = LogLevel.DEBUG

private val DATE_TIME_FORMAT by lazy {
  LocalDateTime.Format {
    @OptIn(FormatStringsInDatetimeFormats::class)
    byUnicodePattern("yyyy-MM-dd HH:mm:ss")
  }
}

private fun log(
  level: LogLevel,
  message: String,
  throwable: Throwable? = null,
) {
  if (level < logLevel) return
  print(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).format(DATE_TIME_FORMAT))
  print(
    when (level) {
      LogLevel.DEBUG -> " D "
      LogLevel.INFO -> " I "
      LogLevel.WARNING -> " W "
      LogLevel.ERROR -> " E "
      LogLevel.NONE -> {
        // Should never happen
      }
    },
  )
  println(message)
  if (throwable != null) {
    println(throwable.stackTraceToString())
  }
}

fun logd(s: String) {
  log(LogLevel.DEBUG, s)
}

fun logi(s: String) {
  log(LogLevel.INFO, s)
}

fun logw(t: Throwable, s: String) {
  log(LogLevel.WARNING, s, t)
}

fun logw(s: String) {
  log(LogLevel.WARNING, s)
}

fun loge(s: String) {
  log(LogLevel.ERROR, s)
}

fun loge(t: Throwable, s: String) {
  log(LogLevel.ERROR, s, t)
}
