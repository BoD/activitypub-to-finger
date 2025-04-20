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

private fun String.split(maxWidth: Int, firstLineMaxWidth: Int): List<String> {
  val lines = mutableListOf<String>()
  var currentLine = ""
  var currentMaxWidth = firstLineMaxWidth
  for (c in this) {
    if (currentLine.length + 1 > currentMaxWidth) {
      lines += currentLine
      currentLine = c.toString()
      currentMaxWidth = maxWidth
    } else {
      currentLine += c
    }
  }
  lines += currentLine
  return lines
}

data class Line(
  val text: String,
  val maxWidth: Int = -1,
)

fun Line.wrapped(maxWidth: Int): List<Line> {
  if (text.length <= maxWidth) {
    return listOf(this.copy(maxWidth = maxWidth))
  }
  val lines = mutableListOf<Line>()
  val words = text.split(" ").toMutableList()
  var currentLine = ""
  while (words.isNotEmpty()) {
    val word = words.removeAt(0)
    if (currentLine.isEmpty()) {
      currentLine = word
    } else {
      val newLine = "$currentLine $word"
      if (newLine.length > maxWidth) {
        if (word.length > maxWidth) {
          words.addAll(0, word.split(maxWidth, maxWidth - currentLine.length - 1))
          continue
        } else {
          lines.add(copy(text = currentLine, maxWidth = maxWidth))
          currentLine = word
        }
      } else {
        currentLine = newLine
      }
    }
  }
  if (currentLine.isNotEmpty()) {
    lines.add(copy(text = currentLine, maxWidth = maxWidth))
  }
  return lines
}

fun String.wrapped(maxWidth: Int): String {
  val lines = this.split("\n").map { Line(it) }.flatMap { line ->
    line.wrapped(maxWidth)
  }
  return lines.joinToString("\n") { it.text }.trim()
}
