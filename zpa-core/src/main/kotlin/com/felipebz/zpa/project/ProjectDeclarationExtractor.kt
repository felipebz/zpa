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
package com.felipebz.zpa.project

import com.felipebz.zpa.lexer.PlSqlLexer
import com.felipebz.zpa.project.extraction.DiscardedSourceText
import com.felipebz.zpa.project.extraction.UnitDispatcher
import com.felipebz.zpa.squid.PlSqlConfiguration
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * Extracts the small, project-relevant declaration slice without constructing an AST.
 * This scanner deliberately understands declaration boundaries only; it does not parse
 * PL/SQL statements or expressions.
 */
fun interface ProjectDeclarationSourceExtractor {
    fun extract(fileId: FileId, source: String): List<ProjectDeclaration>
}

class ProjectDeclarationExtractor(
    configuration: PlSqlConfiguration = PlSqlConfiguration(StandardCharsets.UTF_8, true)
) : ProjectDeclarationSourceExtractor {
    /** FLR lexers keep mutable cursor state, so a concurrent preparation needs one per worker. */
    private val lexers = ThreadLocal.withInitial { PlSqlLexer.create(configuration) }

    override fun extract(fileId: FileId, source: String): List<ProjectDeclaration> = extractFile(fileId, source).declarations

    internal fun extractFile(fileId: FileId, source: String): ProjectFileExtraction {
        val tokens = lexers.get().lex(source)
        return UnitDispatcher(fileId, tokens, DiscardedSourceText(source)).extract()
    }
}

/**
 * Facts of one file plus name-only standalone coverage: the qualified names of standalone procedure/function
 * headings that were identified strictly but produced no fact. Only namespace evidence; never a candidate.
 */
internal class ProjectFileExtraction(
    val declarations: List<ProjectDeclaration>,
    omittedStandaloneCallables: Set<QualifiedName>,
    alteredTypeAttributes: Set<QualifiedName> = emptySet(),
    droppedTypeAttributes: Map<QualifiedName, Set<OracleIdentifier>> = emptyMap()
) {
    val droppedTypeAttributes: Map<QualifiedName, Set<OracleIdentifier>> = Collections.unmodifiableMap(HashMap(droppedTypeAttributes))
    val omittedStandaloneCallables: Set<QualifiedName> = Collections.unmodifiableSet(HashSet(omittedStandaloneCallables))
    val alteredTypeAttributes: Set<QualifiedName> = Collections.unmodifiableSet(HashSet(alteredTypeAttributes))
}
