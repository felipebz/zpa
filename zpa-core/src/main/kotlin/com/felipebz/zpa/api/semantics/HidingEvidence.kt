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
package com.felipebz.zpa.api.semantics

import com.felipebz.flr.api.AstNode
import com.felipebz.zpa.api.annotations.ZpaExperimentalApi
import java.net.URI

/**
 * Which declaration a declaration hides, as the local name resolution of its file decides it.
 *
 * Only [HidesNothing] is a proven absence; [Unknown] is never one.
 */
@ZpaExperimentalApi
sealed interface HidingEvidence {
    /** The declaration hides [hidden]: the outermost one of the same name visible where it is written. */
    class Hides(val hidden: HiddenDeclaration) : HidingEvidence {
        override fun toString() = "Hides($hidden)"
    }

    /** Proven: no enclosing region visible where the declaration is written declares the name, and every one is complete. */
    object HidesNothing : HidingEvidence {
        override fun toString() = "HidesNothing"
    }

    /** Neither a hidden declaration nor a proven absence; [reasons] says what the analysis could not see. */
    class Unknown(reasons: Set<UsageUncertainty>) : HidingEvidence {
        val reasons: Set<UsageUncertainty> = reasons.toSet()

        override fun equals(other: Any?) = other is Unknown && reasons == other.reasons

        override fun hashCode() = reasons.hashCode()

        override fun toString() = "Unknown$reasons"
    }

    /** No evidence at all: see [reason]. A check reports nothing for such a declaration. */
    class Unavailable(val reason: UsageUnavailability) : HidingEvidence {
        override fun equals(other: Any?) = other is Unavailable && reason == other.reason

        override fun hashCode() = reason.hashCode()

        override fun toString() = "Unavailable($reason)"
    }
}

/**
 * Where a hidden declaration is: its name's [line] and [column], with its name [node] when it is in the same file, or
 * the other project file that holds it (a package specification). [fileId] is that file's project identity as the
 * runner gave it (a URI, a path, or a name); [fileName] is its last path segment, the same on every machine.
 */
@ZpaExperimentalApi
class HiddenDeclaration(val line: Int, val column: Int, val fileId: String?, val node: AstNode?) {
    /** The file name of [fileId], without the scheme and the directories of a URI or a path; null in the same file. */
    val fileName: String? = fileId?.let(::lastSegment)

    override fun toString() = (fileId?.let { "$it:" } ?: "") + "$line:$column"

    private companion object {
        /** A scheme such as `file:` (two letters at least, so a Windows drive like `C:` is a path). */
        val SCHEME = Regex("^[A-Za-z][A-Za-z0-9+.-]+:")

        fun lastSegment(id: String): String {
            val path = if (SCHEME.containsMatchIn(id)) runCatching { URI(id).path }.getOrNull() ?: id else id
            return path.trimEnd('/', '\\').substringAfterLast('/').substringAfterLast('\\')
        }
    }
}
