/**
 * Z PL/SQL Analyzer
 * Copyright (C) 2015-2026 Felipe Zorzo
 * mailto:felipe AT felipezorzo DOT com DOT br
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.felipebz.flr.internal.toolkit

import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes
import java.util.*

internal const val PROJECT_SOURCES_PROPERTY_NAME = "Project sources"

/** Discovers the files indexed only by the Project System. */
internal object ProjectSourceFiles {

    /** Entries are separated by [File.pathSeparatorChar]; blank entries are dropped, the rest trimmed. */
    fun entries(value: String): List<String> =
        value.split(File.pathSeparatorChar).map { it.trim() }.filter { it.isNotEmpty() }

    /** "" when every entry exists (file or directory); otherwise a message for the first bad entry. "" for an empty value. */
    fun validate(value: String): String {
        for (entry in entries(value)) {
            val path = try {
                Path.of(entry)
            } catch (e: InvalidPathException) {
                return "Invalid path: $entry"
            }
            if (!Files.exists(path)) return "Path not found: $entry"
        }
        return ""
    }

    /** Comma-separated; trimmed, a leading '.' removed, lowercased with [Locale.ROOT], blanks dropped. */
    fun suffixes(value: String): Set<String> =
        value.split(',')
            .map { it.trim().removePrefix(".").lowercase(Locale.ROOT) }
            .filter { it.isNotEmpty() }
            .toSet()

    /** "" when [suffixes] of [value] is non-empty. */
    fun validateSuffixes(value: String): String =
        if (suffixes(value).isEmpty()) "At least one suffix is required" else ""

    /**
     * Absolute, normalized, de-duplicated paths in natural [Path] order. Explicit files are kept whatever
     * their suffix; folders contribute files matching [suffixes]. Missing, invalid and unreadable entries are skipped.
     */
    fun collect(sources: String, suffixes: String): List<Path> {
        val extensions = suffixes(suffixes).map { ".$it" }
        val result = TreeSet<Path>()
        for (entry in entries(sources)) {
            val path = try {
                Path.of(entry).toAbsolutePath().normalize()
            } catch (e: InvalidPathException) {
                continue
            }
            when {
                Files.isRegularFile(path) -> result.add(path)
                Files.isDirectory(path) -> Files.walkFileTree(path, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val name = file.fileName.toString().lowercase(Locale.ROOT)
                        if (attrs.isRegularFile && extensions.any { name.endsWith(it) }) {
                            result.add(file.toAbsolutePath().normalize())
                        }
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFileFailed(file: Path, exc: IOException): FileVisitResult =
                        FileVisitResult.CONTINUE
                })
            }
        }
        return result.toList()
    }

    /** Existing entries, then the absolute normalized paths of [added]; duplicates dropped keeping the first occurrence. */
    fun merge(value: String, added: List<File>): String =
        (entries(value) + added.map { it.toPath().toAbsolutePath().normalize().toString() })
            .distinct()
            .joinToString(File.pathSeparator)
}
