//
// SPDX-FileCopyrightText: 2015 Felipe Zorzo
// SPDX-License-Identifier: LGPL-3.0-or-later
//

package com.felipebz.zpa.api.project

import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import com.felipebz.zpa.project.immutableList
import java.nio.file.Path
import java.util.*

/**
 * The schemas source files compile in, by path: an ordered list of glob patterns, relative to [baseDirectory] with `/`
 * separators, each mapped to a schema. The first pattern that matches a file gives its schema. In a pattern `**`
 * matches any number of directories, `*` any characters but `/`, and `?` one of them.
 */
@ZpaExperimentalApi
class ProjectSchemaPaths private constructor(
    /** The directory patterns are relative to; null when file identities are already relative paths. */
    val baseDirectory: Path?,
    entries: List<Entry>
) {
    /** One pattern and the schema of the files it matches. */
    class Entry(val pattern: String, val schema: ProjectIdentifier) {
        internal val regex = glob(pattern)

        override fun toString() = "$pattern=$schema"
    }

    val entries: List<Entry> = immutableList(entries)

    /** The schema of the first pattern matching the file [fileId] (a path or a `file:` URI); empty when none matches. */
    fun schemaOf(fileId: String): Optional<ProjectIdentifier> {
        if (entries.isEmpty()) return Optional.empty()
        val relative = relativePath(fileId)
        return Optional.ofNullable(entries.firstOrNull { it.regex.matches(relative) }?.schema)
    }

    private fun relativePath(fileId: String): String {
        // A file identity is a path or a `file:` URI (as the Sonar plugin writes it).
        val path = runCatching { if (fileId.startsWith("file:")) Path.of(java.net.URI(fileId)) else Path.of(fileId) }.getOrNull()
        val relative = if (baseDirectory != null && path != null && path.isAbsolute && path.normalize().startsWith(baseDirectory)) {
            baseDirectory.relativize(path.normalize()).toString()
        } else fileId
        return relative.replace('\\', '/')
    }

    override fun toString() = entries.joinToString(",")

    companion object {
        private val NONE = ProjectSchemaPaths(null, emptyList())

        @JvmStatic
        fun none(): ProjectSchemaPaths = NONE

        @JvmStatic
        fun of(baseDirectory: Path?, entries: List<Entry>): ProjectSchemaPaths =
            ProjectSchemaPaths(baseDirectory?.toAbsolutePath()?.normalize(), entries)

        /**
         * Parses `pattern=SCHEMA,pattern=SCHEMA`: schema names follow Oracle identifier rules (quote a name to keep its
         * case). Blank text gives [none].
         */
        @JvmStatic
        fun parse(baseDirectory: Path?, text: String): ProjectSchemaPaths {
            if (text.isBlank()) return NONE
            val entries = text.split(',').filter { it.isNotBlank() }.map { item ->
                val separator = item.lastIndexOf('=')
                require(separator > 0 && separator < item.length - 1) { "expected pattern=SCHEMA, got '${item.trim()}'" }
                Entry(item.substring(0, separator).trim(), ProjectIdentifier.fromSource(item.substring(separator + 1).trim()))
            }
            return of(baseDirectory, entries)
        }

        private fun glob(pattern: String): Regex {
            val regex = StringBuilder()
            var i = 0
            while (i < pattern.length) {
                val c = pattern[i]
                when {
                    pattern.startsWith("**/", i) -> { regex.append("(?:.*/)?"); i += 2 }
                    pattern.startsWith("**", i) -> { regex.append(".*"); i++ }
                    c == '*' -> regex.append("[^/]*")
                    c == '?' -> regex.append("[^/]")
                    else -> regex.append(Regex.escape(c.toString()))
                }
                i++
            }
            return Regex(regex.toString())
        }
    }
}
